package coffee.khyonieheart.tidal;

import java.lang.constant.Constable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import coffee.khyonieheart.hyacinth.Logger;
import coffee.khyonieheart.hyacinth.Message;
import coffee.khyonieheart.hyacinth.killswitch.Feature;
import coffee.khyonieheart.hyacinth.killswitch.FeatureIdentifier;
import coffee.khyonieheart.hyacinth.module.ModuleOwned;
import coffee.khyonieheart.hyacinth.module.marker.PreventAutoLoad;
import coffee.khyonieheart.tidal.error.CommandError;
import coffee.khyonieheart.tidal.structure.Protected;
import coffee.khyonieheart.tidal.structure.Root;
import coffee.khyonieheart.tidal.structure.Static;
import coffee.khyonieheart.tidal.structure.branch.ArrayBranch;
import coffee.khyonieheart.tidal.structure.branch.Branch;
import coffee.khyonieheart.tidal.structure.branch.StaticBranch;
import coffee.khyonieheart.tidal.structure.branch.TypedBranch;

@PreventAutoLoad
@FeatureIdentifier({ "tidalCommandExecution" })
public abstract class TidalCommand extends Command implements ModuleOwned, Feature, TabCompleter
{
	private static boolean isEnabled = true;
	/* Static lookups for converting primitive classes to Constable classes */
	private static Map<Class<?>, Class<? extends Constable>> CONSTABLE_CLASSES = Map.of(
		Byte.TYPE, Byte.class,
		Short.TYPE, Short.class,
		Integer.TYPE, Integer.class,
		Long.TYPE, Long.class,
		Float.TYPE, Float.class,
		Double.TYPE, Double.class,
		Boolean.TYPE, Boolean.class,
		Character.TYPE, Character.class
	);

	private Map<String, List<Branch>> roots = new HashMap<>();
	private List<Branch> localCommandBranchExecutor = null;
	private Branch rootExecutor = null;

	private TidalCommand() 
	{
		super(null, null, null, null);
	}

	public TidalCommand(
		String name,
		String description,
		String usageMessage,
		String permission,
		String... aliases
	) {
		super(name, description, usageMessage, java.util.Arrays.asList(aliases));
		this.setPermission(permission);

		for (Method method : this.getClass().getDeclaredMethods())
		{
			// Method validation
			//--------------------------------------------------------------------------------
			if (!method.isAnnotationPresent(Root.class))
			{
				continue;
			}

			if (method.getParameterCount() == 0)
			{
				continue;
			}

			if (CommandSender.class.isAssignableFrom(method.getParameterTypes()[0]))
			{
				continue;
			}

			// Local command executor
			// Used in commands that do not use a subcommand
			//--------------------------------------------------------------------------------
			Root rootData = method.getAnnotation(Root.class);
			if (rootData.isLocalExecutor())
			{
				localCommandBranchExecutor = convertToBranches(method.getParameters(), method);
				break;
			}

			// Root command executor
			// Used when no arguments are given
			//-------------------------------------------------------------------------------- 
			if (rootData.isRootExecutor())
			{
				rootExecutor = new StaticBranch(null, method.isAnnotationPresent(Protected.class) ? method.getAnnotation(Protected.class) : null);
				rootExecutor.setExecutor(method);
				continue;
			}

			// Create new argument tree
			//-------------------------------------------------------------------------------- 
			String rootName = rootData.value().equals("USE_METHOD_NAME") ? method.getName() : rootData.value();

			List<Branch> branches;
			try {
				branches = convertToBranches(method.getParameters(), method);
			} catch (IllegalStateException e) {
				Logger.log("§cFailed to setup command using method \"" + method.getName() + "\" in class \"" + this.getClass().getName() + "\" (" + e.getMessage() + ")");
				continue;
			}

			// Handle command "merging"
			//-------------------------------------------------------------------------------- 
			if (roots.containsKey(rootName))
			{
				List<Branch> currentBranches = roots.get(rootName);
				int i = 0;
				for (; i < branches.size(); i++)
				{
					if (i >= currentBranches.size())
					{
						break;
					}

					if (!currentBranches.get(i).equals(branches.get(i)))
					{
						currentBranches.get(i).merge(branches.get(i));
					}
				}

				if ((i + 1) < branches.size())
				{
					currentBranches.get(currentBranches.size() - 1).attach(branches.get(i));
					currentBranches.addAll(branches.subList(i, branches.size()));
				}

				continue;
			}

			roots.put(rootName, branches);
		}
	}

	@SuppressWarnings("unchecked")
	private static List<Branch> convertToBranches(
		Parameter[] params,
		Method method
	)
		throws IllegalStateException
	{
		List<Branch> branches = new ArrayList<>();

		Branch previousBranch = null;
		for (int i = 1; i < params.length; i++)
		{
			Parameter param = params[i];

			Branch branch;
			if (param.isAnnotationPresent(Static.class))
			{
				branch = new StaticBranch(param.getAnnotation(Static.class).value(), param.isAnnotationPresent(Protected.class) ? param.getAnnotation(Protected.class) : null);
				if (previousBranch != null)
				{
					previousBranch.attach(branch);
				}
				previousBranch = branch;

				continue;
			}

			if (param.getType().isArray())
			{
				if (param.getType().getComponentType().isPrimitive())
				{
					throw new IllegalStateException("Primitive arrays are not supported as a command parameter, use \"" + CONSTABLE_CLASSES.get(param.getType().getComponentType()).getName() + "[]\" instead");
				}

				if (param.getType().getComponentType().isArray())
				{
					throw new IllegalStateException("Multi-dimensional arrays are not supported as a command parameter");
				}

				branch = new ArrayBranch<>(param.getName(), param.getType().getComponentType(), param.isAnnotationPresent(Protected.class) ? param.getAnnotation(Protected.class) : null, param.isAnnotationPresent(ArgCount.class) ? param.getAnnotation(ArgCount.class) : null, param.isVarArgs());
				if (previousBranch != null)
				{
					previousBranch.attach(branch);
				}
				previousBranch = branch;

				continue;
			}

			branch = new TypedBranch<>(param.getName(), param.getType(), param.isAnnotationPresent(Protected.class) ? param.getAnnotation(Protected.class) : null);
			if (previousBranch != null)
			{
				previousBranch.attach(branch);
			}
			previousBranch = branch;
		}

		// Set bottom branch as the type of the first parameter
		branches.get(0).setSenderType((Class<? extends CommandSender>) method.getParameterTypes()[0]);
		// Set top branch as executor
		previousBranch.setExecutor(method);

		return branches;
	}

	// Command logic
	//-------------------------------------------------------------------------------- 

	@Override
	public boolean execute(CommandSender sender, String commandLabel, String[] args)
	{
		if (this.getPermission() != null)
		{
			if (!sender.hasPermission(this.getPermission()))
			{
				Message.send(sender, Bukkit.spigot().getConfig().getString("messages.unknown-command"));
				return true;
			}
		}

		if (!isEnabled)
		{
			Message.send(sender, "§cTidal command execution has been temporarily disabled.");
			return true;
		}

		// Root executor
		//--------------------------------------------------------------------------------
		if (args.length == 0)
		{
			if (this.rootExecutor == null)
			{
				Message.send(sender, "§cCommand \"/" + commandLabel + "\" expects at least one argument.");
				return true;
			}

			if (!rootExecutor.isAuthorized(sender))
			{
				Message.send(sender, Bukkit.spigot().getConfig().getString("messages.unknown-command"));
				return true;
			}

			try {
				rootExecutor.getExecutor().setAccessible(true);
				rootExecutor.getExecutor().invoke(this, sender);
			} catch (Exception e) {
				Message.send(sender, "§cAn exception occurred when attempting to invoke this command.");
				e.printStackTrace();
			}
			return true;
		}

		// Local executor
		//-------------------------------------------------------------------------------- 
		if (this.localCommandBranchExecutor != null)
		{
			Branch branch = this.localCommandBranchExecutor.get(0);
			Object[] methodParameters = new Object[this.localCommandBranchExecutor.size()];


		}

		return true;
	}

	private void traverse(
		Branch root,
		String[] args,
		int startIndex,
		CommandSender sender,
		Object[] methodParameters,
		List<CommandError> errors
	) {
		Branch branch = root;
		methodParameters[0] = sender;

		int index = startIndex;
		int parameterIndex = 1;
		while (!branch.isLeaf())
		{
			switch (branch.getBranchType())
			{
				case STATIC -> {
					
				}
			}
		}
	}

	// Feature logic
	//--------------------------------------------------------------------------------

	@Override
	public boolean isEnabled(String target)
	{
		return isEnabled;
	}

	@Override
	public boolean kill(String target)
	{
		return switch (target)
		{
			case "tidalCommandExecution" -> {
				yield isEnabled ? !(isEnabled = false) : false;
			}
			default -> false;
		};
	}

	@Override
	public boolean reenable(String target)
	{
		return switch (target)
		{
			case "tidalCommandExecution" -> {
				yield isEnabled ? isEnabled = true : false;
			}
			default -> false;
		};
	}
}
