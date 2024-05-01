package coffee.khyonieheart.tidal;

import java.lang.constant.Constable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import coffee.khyonieheart.hyacinth.Gradient;
import coffee.khyonieheart.hyacinth.Logger;
import coffee.khyonieheart.hyacinth.Message;
import coffee.khyonieheart.hyacinth.killswitch.Feature;
import coffee.khyonieheart.hyacinth.killswitch.FeatureIdentifier;
import coffee.khyonieheart.hyacinth.module.ModuleOwned;
import coffee.khyonieheart.hyacinth.module.marker.PreventAutoLoad;
import coffee.khyonieheart.hyacinth.util.ArrayIterator;
import coffee.khyonieheart.hyacinth.util.Arrays;
import coffee.khyonieheart.hyacinth.util.marker.NotNull;
import coffee.khyonieheart.hyacinth.util.marker.Nullable;
import coffee.khyonieheart.tidal.concatenation.Concatenate;
import coffee.khyonieheart.tidal.concatenation.ConcatenationException;
import coffee.khyonieheart.tidal.error.CommandError;
import coffee.khyonieheart.tidal.error.CommandError.AppendLocation;
import coffee.khyonieheart.tidal.structure.Protected;
import coffee.khyonieheart.tidal.structure.Root;
import coffee.khyonieheart.tidal.structure.Static;
import coffee.khyonieheart.tidal.structure.branch.ArrayBranch;
import coffee.khyonieheart.tidal.structure.branch.Branch;
import coffee.khyonieheart.tidal.structure.branch.StaticBranch;
import coffee.khyonieheart.tidal.structure.branch.TypedBranch;
import coffee.khyonieheart.tidal.tabcomplete.TidalTabCompleter;

@PreventAutoLoad
@FeatureIdentifier({ "tidalCommandExecution" })
public abstract class TidalCommand extends Command implements ModuleOwned, Feature
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
	private TabCompleter tabCompleter = new TidalTabCompleter();

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

			if (!CommandSender.class.isAssignableFrom(method.getParameterTypes()[0]))
			{
				continue;
			}
			
			// Local command executor
			// Used in commands that do not use a subcommand
			//--------------------------------------------------------------------------------
			Root rootData = method.getAnnotation(Root.class);
			if (rootData.isLocalExecutor())
			{
				localCommandBranchExecutor = convertToBranches(method.getParameters(), "", rootData, method);
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
				branches = convertToBranches(method.getParameters(), rootName, rootData, method);
			} catch (IllegalStateException e) {
				Logger.log("§cFailed to setup command using method \"" + method.getName() + "\" in class \"" + this.getClass().getName() + "\" (" + e.getMessage() + ")");
				e.printStackTrace();
				continue;
			}

			// Handle command "merging"
			//-------------------------------------------------------------------------------- 
			if (roots.containsKey(rootName))
			{
				List<Branch> currentBranches = roots.get(rootName);
				Branch root = currentBranches.get(0);
				root.mergeAll(branches);

				continue;
			}

			roots.put(rootName, branches);
		}
		Logger.debug("/" + name);
		for (String s : roots.keySet())
		{
			roots.get(s).get(0).probeDepth();
			display(roots.get(s).get(0), 0);
		}
	}

	private void display(Branch branch, int depth)
	{
		Logger.debug("- ".repeat(depth) + (branch.getExecutor() != null ? "§a" : "") + branch.getLabel());
		for (Branch b : branch.getBranches())
		{
			display(b, depth + 1);
		}
	}

	@SuppressWarnings("unchecked")
	private static List<Branch> convertToBranches(
		Parameter[] params,
		String rootName,
		Root rootData,
		Method method
	)
		throws IllegalStateException
	{
		List<Branch> branches = new ArrayList<>();

		Branch previousBranch = new StaticBranch(rootName, null);
		if (!rootData.permission().equals("NOT_APPLICABLE"))
		{
			previousBranch.addPermission(rootData.permission());
		}
		previousBranch.addSenderType((Class<? extends CommandSender>) method.getParameterTypes()[0]);
		branches.add(previousBranch);

		for (int i = 1; i < params.length; i++)
		{
			Parameter param = params[i];

			Branch branch;
			if (param.isAnnotationPresent(Static.class))
			{
				String label = param.getAnnotation(Static.class).value();
				branch = new StaticBranch(label == null ? "<unnamed>" : label.equals("USE_ARG_NAME") ? param.getName() : label, param.isAnnotationPresent(Protected.class) ? param.getAnnotation(Protected.class) : null);
				branch.addAnnotations(param.getAnnotations());
				branches.add(branch);
				previousBranch.attach(branch);

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
				branch.addAnnotations(param.getAnnotations());
				branches.add(branch);
				previousBranch.attach(branch);
				previousBranch = branch;

				continue;
			}

			branch = new TypedBranch<>(param.getName(), param.getType(), param.isAnnotationPresent(Protected.class) ? param.getAnnotation(Protected.class) : null);
			branch.addAnnotations(param.getAnnotations());
			branches.add(branch);
			previousBranch.attach(branch);
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

		// Preprocessing
		//-------------------------------------------------------------------------------- 

		// Quoted argument processing
		try {
			args = Concatenate.concatenate('"', ' ', false, true, args);
		} catch (ConcatenationException e) {
			CommandError error = switch (e.getType())
			{
				case UNEXPECTED_END -> new CommandError("Unexpected quoted argument end", args[e.getIndex()], e.getIndex()).setBounds(args[e.getIndex()].length() - 1, args[e.getIndex()].length());
				case UNEXPECTED_START -> new CommandError("Unexpected quoted argument start", args[e.getIndex()], e.getIndex()).setBounds(0, 1);
				case UNTERMINATED_END -> new CommandError("Unterminated quoted argument", args[e.getIndex()], e.getIndex() + 1).appendAt(AppendLocation.END, "\" (missing)").setBounds(0, 1);
			};

			displayErrors(sender, commandLabel, args, List.of(), error);
			return true;
		};
		
		// Array processing
		try {
			args = Concatenate.concatenate('(', ')', '\u0000', false, true, args);
		} catch (ConcatenationException e) {
			CommandError error = switch (e.getType())
			{
				case UNEXPECTED_END -> new CommandError("Unexpected array argument end", args[e.getIndex()], e.getIndex()).setBounds(args[e.getIndex()].length() - 1, args[e.getIndex()].length());
				case UNEXPECTED_START -> new CommandError("Unexpected array argument start", args[e.getIndex()], e.getIndex()).setBounds(0, 1);
				case UNTERMINATED_END -> new CommandError("Unterminated array argument", args[e.getIndex()], e.getIndex() + 1).appendAt(AppendLocation.END, ") (missing)").setBounds(0, 1);
			};

			displayErrors(sender, commandLabel, args, List.of(), error);
			return true;
		};

		List<CommandError> errors = new ArrayList<>();

		// Local executor
		//-------------------------------------------------------------------------------- 
		if (this.localCommandBranchExecutor != null)
		{
			Branch branch = this.localCommandBranchExecutor.get(0);
			Object[] methodParameters = new Object[this.localCommandBranchExecutor.size()];

			List<String> modifiableArgs = Arrays.toArrayList(args);
			branch = traverse(branch, modifiableArgs, 0, sender, commandLabel, CommandContext.EXECUTION, methodParameters, errors);
			args = modifiableArgs.toArray(new String[modifiableArgs.size()]);

			if (!errors.isEmpty())
			{
				displayErrors(sender, commandLabel, args, errors);
				return true;
			}

			if (branch.getExecutor() == null)
			{
				if (!branch.isLeaf())
				{
					displayErrors(sender, commandLabel, args, errors, new CommandError("Incomplete command", "...", args.length).appendAt(AppendLocation.END, "..."));

					return true;
				}

				displayErrors(sender, commandLabel, args, errors, new CommandError("§d§oUnterminated command, this is a Tidal issue§c", args[args.length - 1], args.length - 1));

				return true;
			}

			try {
				branch.getExecutor().setAccessible(true);
				branch.getExecutor().invoke(this, methodParameters);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				Message.send(sender, "§cAn exception occurred when attempting to invoke this command.");
				e.printStackTrace();
			}

			return true;
		}

		// Subcommand executor
		//-------------------------------------------------------------------------------- 
		if (!this.roots.containsKey(args[0]))
		{
			new CommandError("Unknown subcommand \"" + args[0] + "\"", args[0], 0).display(1, sender, commandLabel, args);
			return true;
		}

		Branch branch = this.roots.get(args[0]).get(0);

		if (!branch.isAuthorized(sender))
		{
			new CommandError("Unknown subcommand \"" + args[0] + "\"", args[0], 0).display(1, sender, commandLabel, args);
			return true;
		}

		Object[] methodParameters = new Object[this.roots.get(args[0]).get(0).getDepth()];
		List<String> modifiableArgs = Arrays.toArrayList(args);

		branch = traverse(branch, modifiableArgs, 1, sender, commandLabel, CommandContext.EXECUTION, methodParameters, errors);
		args = modifiableArgs.toArray(new String[modifiableArgs.size()]);

		if (branch.getExecutor() == null)
		{
			if (!branch.isLeaf())
			{
				displayErrors(sender, commandLabel, args, errors, new CommandError("Incomplete command", "...", args.length).appendAt(AppendLocation.END, "..."));

				return true;
			}

			displayErrors(sender, commandLabel, args, errors, new CommandError("§d§oUnterminated command, this is a Tidal issue§c", args[args.length - 1], args.length - 1));

			return true;
		}

		if (!errors.isEmpty())
		{
			displayErrors(sender, commandLabel, args, errors);
			return true;
		}

		// Trim down excess arguments
		Object[] newMethodParameters = new Object[branch.getExecutor().getParameterCount()];
		for (int i = 0; i < newMethodParameters.length; i++)
		{
			newMethodParameters[i] = methodParameters[i];
		}
		methodParameters = newMethodParameters;

		try {
			branch.getExecutor().setAccessible(true);
			branch.getExecutor().invoke(this, methodParameters);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			Message.send(sender, "§cAn exception occurred when attempting to invoke this command.");
			e.printStackTrace();
		}

		return true;
	}

	private Branch traverse(
		Branch root,
		List<String> modifiableArgs,
		int startIndex,
		CommandSender sender,
		String label,
		CommandContext context,
		Object[] methodParameters,
		List<CommandError> errors
	) {
		Branch branch = root;
		String[] args = modifiableArgs.toArray(new String[modifiableArgs.size()]);
		methodParameters[0] = sender;

		int index = startIndex;
		int parameterIndex = 1;
		String arg;
		while (!branch.isLeaf())
		{
			if (index >= args.length)
			{
				return branch;
			}

			arg = args[index];
			switch (branch.getConnectedBranchType())
			{
				case STATIC -> {
					methodParameters[parameterIndex++] = arg;
					index++;

					if (!branch.hasStaticBranch(arg))
					{
						errors.add(new CommandError("Unknown option \"" + arg + "\"", arg, index - 1));
						if (branch.getBranchesCount() == 1)
						{
							branch = branch.getVariableBranch(); // Skip ahead
							
							if (!branch.isAuthorized(sender))
							{
								return branch;
							}

							continue;
						}

						return branch;
					}

					branch = branch.getStaticBranch(arg);
					if (!branch.isAuthorized(sender))
					{
						errors.add(new CommandError("Unknown option \"" + arg + "\"", arg, index - 1));
						return branch;
					}
				}
				case TYPED -> {
					branch = branch.getVariableBranch();
					
					if (!branch.isAuthorized(sender))
					{
						errors.add(new CommandError("You do not have permission to use this command", arg, index));
						return branch;
					}

					methodParameters[parameterIndex++] = ((TypedBranch<?>) branch).parse(sender, label, args, index, arg, errors, context);
					index++;
				}
				case ARRAY -> {
					branch = branch.getVariableBranch();

					if (!branch.isAuthorized(sender))
					{
						errors.add(new CommandError("You do not have permission to use this command", arg, index));
						return branch;
					}
					
					ArrayBranch<?> arrBranch = (ArrayBranch<?>) branch;

					if (arrBranch.isVarArgs())
					{
						StringBuilder builder = new StringBuilder();
						ArrayIterator<String> iter = Arrays.iterator(args, index);
						while (iter.hasNext())
						{
							builder.append(iter.next());

							if (iter.hasNext())
							{
								builder.append("\u0000");
							}
						}
						arg = builder.toString();
						String[] newArgs = new String[index + 1];
						for (int o = 0; o < newArgs.length; o++)
						{
							newArgs[o] = args[o];
						}
						newArgs[newArgs.length - 1] = arg;
						args = newArgs;
						modifiableArgs.clear();
						modifiableArgs.addAll(java.util.Arrays.asList(args));
					}
					
					methodParameters[parameterIndex++] = arrBranch.parse(sender, label, args, index, arg, errors, context);
					index++;
				}
			}
		}
		return branch;
	}

	public static void displayErrors(
		CommandSender sender,
		String label,
		String[] args,
		List<CommandError> errors,
		CommandError... immediateErrors
	) {
		sender.spigot().sendMessage(new Gradient("#FF5555", "#FFFFFF").createComponents("Could not execute command /" + label + " " + Arrays.toString(args, " ", a -> a.replace('\u0000', ' '))));
		for (int i = 0; i < errors.size(); i++)
		{
			errors.get(i).display(i + 1, sender, label, args);
		}

		for (int i = 0; i < immediateErrors.length; i++)
		{
			immediateErrors[i].display((i + 1) + errors.size(), sender, label, args);
		}
	}

	@Override
	public List<String> tabComplete(
		CommandSender sender,
		String label,
		String[] args
	) {
		if (this.tabCompleter != null)
		{
			try {
				List<String> output = this.tabCompleter.onTabComplete(sender, this, label, args);
				if (output == null)
				{
					return List.of();
				}
				return output;
			} catch (Exception e) {
				Message.send(sender, "§cAn exception occurred when attempting to handle tab-completion.");
				e.printStackTrace();
			}
		}
		return super.tabComplete(sender, label, args);
	}

	public boolean hasLocalExecutor()
	{
		return this.localCommandBranchExecutor != null;
	}

	@NotNull
	public Branch getLocalExecutor()
	{
		return this.localCommandBranchExecutor.get(0);
	}

	public boolean hasRoot(
		@NotNull String label
	) {
		Objects.requireNonNull(label);

		return this.roots.containsKey(label);
	}

	@Nullable
	public Branch getRoot(
		@NotNull String root
	) {
		Objects.requireNonNull(root);

		return this.roots.get(root).get(0);
	}

	public List<String> getRoots()
	{
		return new ArrayList<>(this.roots.keySet());
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
