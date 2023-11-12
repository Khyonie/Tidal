package coffee.khyonieheart.tidal;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

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
import coffee.khyonieheart.hyacinth.print.Grammar;
import coffee.khyonieheart.hyacinth.util.Arrays;
import coffee.khyonieheart.hyacinth.util.Collections;
import coffee.khyonieheart.hyacinth.util.marker.NotNull;
import coffee.khyonieheart.hyacinth.util.marker.Nullable;
import coffee.khyonieheart.tidal.concatenation.Concatenate;
import coffee.khyonieheart.tidal.concatenation.ConcatenationException;
import coffee.khyonieheart.tidal.error.CommandError;
import coffee.khyonieheart.tidal.structure.Branch;
import coffee.khyonieheart.tidal.structure.Protected;
import coffee.khyonieheart.tidal.structure.Root;
import coffee.khyonieheart.tidal.structure.Static;
import coffee.khyonieheart.tidal.tabcomplete.TidalTabCompleter;

@PreventAutoLoad
@FeatureIdentifier({ "tidalCommandExecution" })
public abstract class TidalCommand extends Command implements ModuleOwned, Feature, TabCompleter
{
	private static boolean isEnabled = true;
	private static Pattern argNamePattern = Pattern.compile("arg\\d");

	private Map<String, Branch> branches = new HashMap<>();
	private TabCompleter tabCompleter = new TidalTabCompleter();

	private Method redirectAllExecutor = null;
	private List<Branch> redirectAllBranches = null;

	private Method noSubCommandExecutor = null;

	private static final Map<Class<?>, Class<?>> PRIMITIVE_CLASSES = Map.of(
		Byte.TYPE, Byte.class,
		Short.TYPE, Short.class,
		Integer.TYPE, Integer.class,
		Long.TYPE, Long.class,
		Float.TYPE, Float.class,
		Double.TYPE, Double.class,
		Boolean.TYPE, Boolean.class
	);
	
	private TidalCommand() 
	{
		super(null, null, null, null);
	}

	@SuppressWarnings("unchecked")
	public TidalCommand(
		String name,
		String description,
		String usageMessage,
		String permission,
		String... aliases
	) {
		super(name, description, usageMessage, java.util.Arrays.asList(aliases));
		this.setPermission(permission);

		boolean warnedAboutParameterNames = false;

		Logger.verbose("Processing command /" + name);

		// Preprocess methods
		for (Method m : this.getClass().getDeclaredMethods())
		{
			Logger.verbose(" - Command method: " + m.getName());

			if (!m.isAnnotationPresent(Root.class))
			{
				Logger.verbose("§e - Not a command method, missing @Root");
				continue;
			}

			if (m.getParameterCount() == 0)
			{
				Logger.verbose("§e - Not a command method, does not contain any parameters");
				continue;
			}

			if (!CommandSender.class.isAssignableFrom(m.getParameterTypes()[0]))
			{
				Logger.verbose("§e - Not a command method, first parameter is not a CommandSender type");
				continue;
			}

			Logger.verbose(" - Is a command method!");

			Root data = m.getAnnotation(Root.class);

			if (data.isRedirectAllExecutor())
			{
				Logger.debug("§6- Method has a redirect-all root!");
				this.redirectAllExecutor = m;
				this.redirectAllBranches = new ArrayList<>();

				Branch previousBranch = null;
				for (int i = 1; i < m.getParameterCount(); i++)
				{
					Parameter param = m.getParameters()[i];
					if (param.isAnnotationPresent(Static.class))
					{
						Branch branch = new Branch(param.getName(), true, null, param.isAnnotationPresent(Protected.class) ? param.getAnnotation(Protected.class).permission() : null, param.isAnnotationPresent(Protected.class) ? param.getAnnotation(Protected.class).senderType() : null, String.class);
						if (previousBranch != null)
						{
							previousBranch.addBranch(branch.getLabel(), branch);
							previousBranch = branch;
						} else {
							previousBranch = branch;
						}

						this.redirectAllBranches.add(branch);
						continue;
					}

					Branch branch = new Branch(param.getName(), false, null, param.isAnnotationPresent(Protected.class) ? param.getAnnotation(Protected.class).permission() : null, param.isAnnotationPresent(Protected.class) ? param.getAnnotation(Protected.class).senderType() : null, param.getType());
					if (previousBranch != null)
					{
						previousBranch.addBranch(branch.getLabel(), branch);
						previousBranch = branch;
					} else {
						previousBranch = branch;
					}

					if (param.isVarArgs())
					{
						branch.setIsVarArgs();
					}

					this.redirectAllBranches.add(branch);
				}
				Logger.debug("§aRedirect-all branches: [ " + Collections.toString(redirectAllBranches, ", ", b -> b.getLabel()) + " ]");
				Logger.debug("§aRedirect-all types: [ " + Collections.toString(redirectAllBranches, ", ", b -> b.getType().getName()) + " ]");
				break;
			}

			Branch branch = new Branch(
				data.value().equals("USE_METHOD_NAME") ? m.getName().toLowerCase().replace("_", "") : data.value(), 
				true,
				m.getParameterCount() == 1 ? m : null,
				data.permission(), 
				(Class<? extends CommandSender>) m.getParameterTypes()[0], 
				String.class
			);
			Logger.verbose(" - Instantiated new branch");

			branch.addAnnotations(m.getParameters()[0].getAnnotations());

			if (branches.containsKey(branch.getLabel()))
			{
				if (!branches.get(branch.getLabel()).equals(branch))
				{
					Logger.log("§cWarning: attempted to register a duplicate branch with differing characteristics using method " + m.getName());
					continue;
				}

				branch = branches.get(branch.getLabel());
			}

			Branch b = branch;
			int merges = 1;
			int defaultArgNames = 1;
			for (int i = 1; i < m.getParameters().length; i++)
			{
				Parameter param = m.getParameters()[i];
				Logger.debug("Parameter \"" + param.getName() + "\" annotations: " + Arrays.toString(param.getAnnotations(), ", ", a -> a.annotationType().getName()));
				
				if (argNamePattern.matcher(param.getName()).matches())
				{
					defaultArgNames++;
				}

				if (param.isAnnotationPresent(Static.class))
				{
					if (b.hasStaticBranch(param.getAnnotation(Static.class).value()))
					{
						b = b.getVariableBranch();
						b.addAnnotations(param.getAnnotations());
						merges++;
						continue;
					}

					Branch newBranch = new Branch(
						param.getAnnotation(Static.class).value().equals("USE_METHOD_NAME") ? param.getName() : param.getAnnotation(Static.class).value(), 
						true, 
						null,
						param.getAnnotation(Static.class).permission(), 
						param.getAnnotation(Static.class).senderType(), 
						param.getType()
					);

					b.addBranch(newBranch.getLabel(), newBranch);
					b = newBranch;
					b.addAnnotations(param.getAnnotations());

					continue;
				}

				if (b.hasVariableBranch())
				{
					if (!b.getVariableBranch().getType().equals(param.getType()))
					{
						Logger.log("§cWarning: command /" + name + " " + branch.getLabel() + " will be inaccessable due to conflicting variable argument types at position " + i);
						break;
					}

					b = b.getVariableBranch();
					merges++;
					continue;
				}

				Branch newBranch = new Branch(
					param.getName(),
					false,
					null,
					param.isAnnotationPresent(Protected.class) ? param.getAnnotation(Protected.class).permission() : null,
					param.isAnnotationPresent(Protected.class) ? param.getAnnotation(Protected.class).senderType() : null,
					param.getType()
				);

				Logger.debug("Is variable branch varargs? " + param.isVarArgs());
				if (param.isVarArgs())
				{
					// TODO Test this
					Logger.debug("Varargs type: " + param.getType().getComponentType());
					newBranch.setIsVarArgs();
					b.addBranch(newBranch.getLabel(), newBranch);
					newBranch.setMethod(m);
					newBranch.addAnnotations(param.getAnnotations());
					break;
				}

				b.addBranch(newBranch.getLabel(), newBranch);
				b = newBranch;
			}

			if (b.getMethod() == null)
			{
				b.setMethod(m);
			}


			if (defaultArgNames == m.getParameterCount())
			{
				warnedAboutParameterNames = true;
			}

			if (merges == m.getParameterCount())
			{
				Logger.log("§cWarning: command /" + name + " " + branch.getLabel() + " will be inaccesable due to another command already matching its signature");
				continue;
			}

			this.branches.put(branch.getLabel(), branch);
		}

		if (warnedAboutParameterNames)
		{
			Logger.log("§cWarning: command parameter names are unavailable. Recompile your plugin with \"-parameters\" to make them available in tab-complete and error displays.");
		}
	}

	@Override
	public boolean execute(CommandSender sender, String commandLabel, String[] args)
	{
		long startTime = System.currentTimeMillis();
		boolean status = this.execute0(sender, commandLabel, args); // TODO Debug
		Logger.verbose("Command executed in " + (System.currentTimeMillis() - startTime) + "ms");

		return status;
	}

	/**
	 * Execution logic.
	 */
	private boolean execute0(CommandSender sender, String commandLabel, String[] args)
	{
		Logger.debug("Executing command " + commandLabel + " with args [" + Arrays.toString(args, ", ", null) + "]");
		if (this.getPermission() != null)
		{
			if (!sender.hasPermission(this.getPermission()))
			{
				sender.sendMessage(Bukkit.spigot().getConfig().getString("messages.unknown-command"));
				return true;
			}
		}

		if (!isEnabled)
		{
			sender.sendMessage("§cThis command has been temporarily disabled.");
			return true;
		}

		if (args.length == 0)
		{
			Logger.debug("No subcommand executor");
			// FIXME No subcommand executor
			return false;
		}

		List<CommandError> errors = new ArrayList<>();

		// 
		// Argument preprocessing
		//-------------------------------------------------------------------------------------------------------------------------------------------- 
		//
		try {
			args = Concatenate.concatenate('"', args);
			Logger.debug("Concatenation successful, output: [" + Arrays.toString(args, ", ", null) + "]");
		} catch (ConcatenationException e) {
			Logger.debug("Concatenation failed.");
			switch (e.getType())
			{
				case UNEXPECTED_START -> {
					CommandError error = new CommandError("Unexpected quoted argument start", args[e.getIndex()], e.getIndex(), 0, 1);
					error.setPossibleResolution("Remove the '\"' at the start of " + error.getArgument());
					errors.add(error);
				}
				case UNEXPECTED_END -> {
					CommandError error = new CommandError("Unexpected quoted argument end", args[e.getIndex()], e.getIndex(), args[e.getIndex()].length() - 1, args[e.getIndex()].length());
					error.setPossibleResolution("Remove the '\"' at the end of " + error.getArgument());
					errors.add(error);
				}
				case UNTERMINATED_END -> {
					CommandError error = new CommandError("Unterminated quoted argument", args[e.getIndex()], e.getIndex(), args[e.getIndex()].length() - 1, args[e.getIndex()].length());
					error.setPossibleResolution("Add an '\"' at the end of " + error.getArgument());
					errors.add(error);
				}
			}
		}

		//
		// All-redirection command
		// --------------------------------------------------------------------------------------------------------------------------------------------                                                                          
		//
		Logger.debug("Checking if command is redirect-all...");
		if (this.redirectAllExecutor != null)
		{
			Logger.debug("It is! Performing sender type check... (" + this.redirectAllExecutor.getParameterTypes()[0].isAssignableFrom(sender.getClass()) + ")");
			if (!this.redirectAllExecutor.getParameterTypes()[0].isAssignableFrom(sender.getClass()))
			{
				sender.sendMessage(Bukkit.spigot().getConfig().getString("messages.unknown-command"));
				return true;
			}

			Deque<Branch> involvedBranches = new ArrayDeque<>(this.redirectAllBranches);
			Object[] params = new Object[this.redirectAllExecutor.getParameterCount()];
			params[0] = sender;

			// Validation
			int index = 1;
			Branch branch = null;
			while (!involvedBranches.isEmpty() && index < (args.length + 1))
			{
				Logger.debug("Argument @ " + index + ": " + args[index - 1]);
				Logger.debug("Arguments: [ " + Arrays.toString(params, ", ", p -> p == null ? "(null)" : p.toString()) + " ]");
				Logger.debug("Number of branches remaining: " + involvedBranches.size() + " | index: " + index + "/" + (args.length + 1));
				branch = involvedBranches.pop();
				Logger.debug("Branch: " + branch.getLabel() + " | static: " + branch.isStatic() + " | type: " + branch.getType().getSimpleName() + " | varargs: " + branch.isVarargs());

				if (branch.isStatic())
				{
					params[index] = args[index - 1];
					index++;
					continue;
				}

				if (branch.isVarargs())
				{
					Logger.debug("Branch is varargs! Checking if type " + branch.getType().getComponentType().getSimpleName() + " is supported... (" + TypeManager.hasParserFor(branch.getType().getComponentType()) + ")");
					if (!TypeManager.hasParserFor(branch.getType().getComponentType()))
					{
						CommandError error = new CommandError("No type parser has been registered for " + branch.getType().getComponentType().getName(), args[index - 1], index - 1, 0, args[index - 1].length());
						error.setPossibleResolution("Create type parser for " + branch.getType().getComponentType().getName());

						errors.add(error);
						break;
					}

					String[] slice = java.util.Arrays.copyOfRange(args, index - 1, args.length);
					Logger.debug("String slice: [ " + Arrays.toString(slice, ", ") + " ]");
					Object[] varargParams = parseGeneric(branch.getType().getComponentType(), slice);

					params[index] = varargParams;
					break;
				}

				Logger.debug("Checking if type " + branch.getType().getSimpleName() + " is supported... (" + TypeManager.hasParserFor(branch.getType()) + ")");
				if (!TypeManager.hasParserFor(branch.getType()))
				{
					CommandError error = new CommandError("No type parser has been registered for " + branch.getType().getName(), args[index - 1], index - 1, 0, args[index - 1].length());
					error.setPossibleResolution("Create type parser for " + branch.getType().getName());

					errors.add(error);
					index++;
					continue;
				}

				CommandError error = TypeManager.getParser(branch.getType()).validateExecution(sender, commandLabel, index - 1, branch, args[index - 1], args);

				if (error != null)
				{
					Logger.debug("Validation failed!");
					errors.add(error);
					index++;
					continue;
				}

				params[index] = TypeManager.getParser(branch.getType()).parseType(args[index - 1]);
				index++;
			}
			Logger.debug("§aFinal arguments: [ " + Arrays.toString(params, ", ", p -> p == null ? "(null)" : p.toString()) + " ]");

			if (branch == null)
			{
				Message.send(sender, "§cFIXME Tidal does currently handle redirection commands with no arguments"); // Red FIXME
				return true;
			}

			if (!branch.isLeaf())
			{
				if (branch.leadsToStatic())
				{
					CommandError error = new CommandError("Incomplete command", args[args.length - 1], args.length - 1, 0, 3);
					error.setPossibleResolution("Append argument named \"" + branch.getVariableBranch().getLabel() + "\"");
				}
				Branch varBranch = branch.getVariableBranch();
				if (varBranch == null)
				{
					CommandError error = new CommandError("Incomplete command", args[args.length - 1], args.length - 1, 0, 3);
					errors.add(error);
				}
			}

			if (!errors.isEmpty())
			{
				printFailures(errors, sender, "(redirect)", args);
			}

			redirectAllExecutor.setAccessible(true);
			try
			{
				redirectAllExecutor.invoke(this, params);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				e.printStackTrace();
			}

			return true;
		}

		//
		// Root access checks
		// -------------------------------------------------------------------------------------------------------------------------------------------- 
		//
		Logger.debug("Grabbing branch...");

		// Attempt to perform traversal
		Branch root = this.branches.get(args[0]);
		
		Logger.debug("Branch \"" + args[0] + "\" exists? " + (root != null));
		if (root == null)
		{
			errors.add(new CommandError("No such subcommand \"" + args[0] + "\"", args[0], 0, 0, args[0].length()));
			printFailures(errors, sender, commandLabel, args);
			return true;
		}

		Logger.debug("Running accessor check");
		if (!root.getSenderType().isAssignableFrom(sender.getClass()))
		{
			sender.sendMessage(Bukkit.spigot().getConfig().getString("messages.unknown-command"));
			return true;
		}

		Logger.debug("Possibly returning errors if they were encountered");
		if (!errors.isEmpty())
		{
			printFailures(errors, sender, commandLabel, args);
			return true;
		}

		//
		// Traversal
		// --------------------------------------------------------------------------------------------------------------------------------------------  
		//
		Logger.debug("Performing traversal");
		// Perform traversal
		Deque<Class<?>> types = new ArrayDeque<>();
		Deque<Branch> involvedBranches = traverse(args[0], sender, commandLabel, args, CommandContext.EXECUTION, errors, types);
		Logger.debug("- Traversal complete. -");
		Logger.debug("Types: [" + Collections.toString(types, ", ", c -> c.getSimpleName()) + "]");
		Logger.debug("Branch labels: [" + Collections.toString(involvedBranches, ", ", b -> b.getLabel()) + "]");

		if (!errors.isEmpty())
		{
			printFailures(errors, sender, commandLabel, args);
			return true;
		}

		//
		// Invokation
		// -------------------------------------------------------------------------------------------------------------------------------------------- 
		//

		// Flip type deque
		Deque<Class<?>> flippedTypes = new ArrayDeque<>(types.size());
		while (!types.isEmpty())
		{
			flippedTypes.push(types.pop());
		}
		types = flippedTypes;

		Method method = null;
		try {
			method = involvedBranches.peek().getMethod();

			if (method == null)
			{
				throw new NoSuchMethodException();
			}

			method.setAccessible(true);

			// Parse args
			Object[] params = processArguments(involvedBranches, sender, args);

			Logger.debug("Method param types: [ " + Arrays.toString(method.getParameterTypes(), ", ", t -> t.getName()) + " ]");
			Logger.debug("Given types: [ " + Arrays.toString(params, ", ", t -> t == null ? "(null!)" : t.getClass().getName()) + " ]");

			method.invoke(this, params);
			return true;
		} catch (NoSuchMethodException e) {
			if (!involvedBranches.peek().isLeaf())
			{
				String[] newArgs = new String[args.length + 1];
				for (int i = 0; i < args.length; i++)
				{
					newArgs[i] = args[i];
				}
				newArgs[newArgs.length - 1] = "...";
				args = newArgs;

				CommandError error = new CommandError("Incomplete command", args[args.length - 1], args.length - 1, 0, 3);
				if (involvedBranches.peek().getBranches().size() == 1)
				{
					error.setPossibleResolution("Append argument named \"" + involvedBranches.peek().getVariableBranch().getLabel() + "\"");
				} else {
					error.setPossibleResolution("Append one of the following: [" + Collections.toString(involvedBranches.peek().getBranches(), " | ", (b) -> b.getLabel()) + "]");
				}

				errors.add(error);

				printFailures(errors, sender, commandLabel, args);
				return true;
			}

			e.printStackTrace();

			CommandError error = new CommandError("A traversal error seems to have occurred", args[args.length - 1], args.length - 1, 0, args[args.length - 1].length());
			error.setPossibleResolution("Traversal output: [" + Collections.toString(types, ", ", (t) -> t.getName()) + "]");

			errors.add(error);
		} catch (InvocationTargetException e) {
			Message.send(sender, "An exception occurred while attempting to invoke this command.");
			e.printStackTrace();
			return true;
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			Message.send(sender, "An access exception has occurred. Please try again later.");
			return true;
		}

		if (!errors.isEmpty())
		{
			printFailures(errors, sender, commandLabel, args);
		}

		return true;
	}

	private void printFailures(
		@NotNull List<CommandError> errors,
		@NotNull CommandSender sender,
		@NotNull String label,
		@NotNull String[] args
	) {
		sender.spigot().sendMessage(new Gradient("#FF5555", "#FFFFFF").createComponents("Could not execute command /" + label + " " + Arrays.toString(args, " ", null)));
		for (int i = 0; i < errors.size(); i++)
		{
			CommandError error = errors.get(i);

			String[] aArgs = java.util.Arrays.copyOf(args, args.length + (error.appendsBlank() ? 1 : 0));
			if (error.appendsBlank())
			{
				aArgs[aArgs.length - 1] = "...";
			}
			args = aArgs;

			Message.send(sender, "§cError " + (i + 1) + ": " + error.getMessage() + " at position " + (error.getIndex() + 1));

			args[error.getIndex()] = "§e" + new StringBuilder(args[error.getIndex()])
				.replace(error.getErrorStart(), error.getErrorEnd(), "§c§n" + args[error.getIndex()].substring(error.getErrorStart(), error.getErrorEnd()) + "§e")
				.toString() + "§7";
			
			Message.send(sender, "§c§l⤷ §7/" + label + " " + Arrays.toString(args, " ", null));

			args[error.getIndex()] = error.getArgument();

			if (error.hasPossibleResolution())
			{
				Message.send(sender, "§7 §9 §o Possible fix: " + error.getPossibleResolution());
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> T[] parseGeneric(
		Class<T> type,
		String[] inputs
	) {
		T[] data = (T[]) Array.newInstance(type, inputs.length);

		TypeParser<T> parser = (TypeParser<T>) TypeManager.getParser(type);
		for (int i = 0; i < inputs.length; i++)
		{
			data[i] = parser.parseType(inputs[i]);
		}

		return data;
	}

	public Deque<Branch> traverse(
		String root, 
		CommandSender sender,
		String commandLabel,
		String[] args,
		CommandContext context,
		List<CommandError> errors,
		Deque<Class<?>> types
	) {
		Deque<Branch> branches = new ArrayDeque<>();
		Branch current = this.branches.get(root);

		branches.push(current);
		types.push(current.getSenderType());

		for (int i = 1; i < args.length; i++)
		{
			Logger.debug("Current branch: " + current.getLabel() + " (Leads to: [ " + Collections.toString(current.getBranches(), ", ", b -> b.getLabel()) + " ])");
			if (current.isLeaf())
			{
				break;
			}

			if (current.leadsToStatic())
			{
				if (current.hasStaticBranch(args[i]))
				{
					current = current.getStaticBranch(args[i]);

					if (current.hasPermission())
					{
						if (!sender.hasPermission(current.getPermission()))
						{
							errors.add(new CommandError("Unknown option \"" + args[i] + "\"", args[i], i, 0, args[i].length()));
							return branches;
						}
					}

					branches.push(current);
					types.push(String.class);
					continue;
				}

				errors.add(new CommandError("Unknown option \"" + args[i] + "\"", args[i], i, 0, args[i].length()));
				// TODO Provide fuzzy search
				return branches;
			}

			Branch varBranch = current.getVariableBranch();
			if (varBranch.hasPermission())
			{
				if (!sender.hasPermission(varBranch.getPermission()))
				{
					errors.add(new CommandError("You do not have permission to perform this command", args[i], i, 0, args[i].length()));
					return branches;
				}
			}

			Logger.debug("Is variable branch " + varBranch.getLabel() + " varargs? " + varBranch.isVarargs());
			if (varBranch.isVarargs())
			{
				Logger.debug("Branch requires minimum number of (var) arguments? " + varBranch.hasAnnotation(MinVarargs.class));
				if (varBranch.hasAnnotation(MinVarargs.class))
				{
					int min = varBranch.getAnnotation(MinVarargs.class).min();
					Logger.debug("Present varargs: " + (args.length - i) + "/" + min);
					if ((args.length - i) + 1 < min)
					{
						CommandError error = new CommandError("Parameter \"" + varBranch.getLabel() + "\" requires at least " + min + " " + Grammar.plural(min, "argument", "arguments"), args[i], i, 0, args[i].length());
						error.setAppendingBlank();
						error.setPossibleResolution("Append argument named \"" + varBranch.getLabel() + "\"");
						errors.add(error);
					}
				}

				if (varBranch.getType().getComponentType().isPrimitive())
				{
					CommandError error = new CommandError("Java does not support generic arrays with primitive types, this is a developer issue", args[i], i, 0, args[i].length());
					if (varBranch.getType().describeConstable().isPresent())
					{
						error.setPossibleResolution("Change the parameter type " + varBranch.getType().getComponentType().getSimpleName() + "... to " + PRIMITIVE_CLASSES.get(varBranch.getType().getComponentType()).getSimpleName() + "...");
					}
					errors.add(error);

					current = varBranch;

					return branches;
				}

				if (!TypeManager.hasParserFor(varBranch.getType().getComponentType()))
				{
					CommandError error = new CommandError("No type parser has been registered for " + varBranch.getType().getName(), args[i], i, 0, args[i].length());
					error.setPossibleResolution("Create type parser for " + varBranch.getType().getName());
					errors.add(error);
					
					current = varBranch;
					//branches.push(current);
					//types.push(current.getType());

					return branches;
				}

				//types.push(varBranch.getType());
				//branches.push(varBranch);
				Logger.debug("Varargs type: " + varBranch.getType().getComponentType().getName());

				// Validate varargs
				while (i < args.length)
				{
					CommandError error = switch (context)
					{
						case EXECUTION -> TypeManager.getParser(varBranch.getType().getComponentType()).validateExecution(sender, commandLabel, i, varBranch, args[i], args);
						case TABCOMPLETE -> TypeManager.getParser(varBranch.getType().getComponentType()).validateTabComplete(sender, commandLabel, i, varBranch, args[i], args);
					};

					if (error != null)
					{
						errors.add(error);
					}

					i++;
				}

				break;
			}

			if (!TypeManager.hasParserFor(varBranch.getType()))
			{
				CommandError error = new CommandError("No type parser has been registered for " + varBranch.getType().getName(), args[i], i, 0, args[i].length());
				error.setPossibleResolution("Create type parser for " + varBranch.getType().getName());
				errors.add(error);

				// Fail slowly, remember
				current = varBranch;
				branches.push(current);
				types.push(current.getType());

				continue;
			}

			CommandError error = switch (context)
			{
				case EXECUTION -> TypeManager.getParser(varBranch.getType()).validateExecution(sender, commandLabel, i, varBranch, args[i], args);
				case TABCOMPLETE -> TypeManager.getParser(varBranch.getType()).validateTabComplete(sender, commandLabel, i, varBranch, args[i], args);
			};

			if (error != null)
			{
				errors.add(error);
			}

			current = varBranch;
			branches.push(current);
			types.push(current.getType());
		}

		if (!current.isLeaf())
		{
			Logger.debug("Current branch is not a leaf. This could indicate an incomplete command, or a variable branch with 0 arguments passed to it. Checking now.");
			Branch vBranch;
			if ((vBranch = current.getVariableBranch()) != null)
			{
				Logger.debug("Next branch is a variable branch, checking if it's varargs..."); 
				if (vBranch.isVarargs())
				{
					if (vBranch.hasAnnotation(MinVarargs.class))
					{
						int min = vBranch.getAnnotation(MinVarargs.class).min();
						Logger.debug("Required arguments met: " + (min < (args.length - branches.size())));
						if ((args.length - branches.size()) < min)
						{
							CommandError error = new CommandError("Parameter \"" + vBranch.getLabel() + "\" requires at least " + min + " " + Grammar.plural(min, "argument", "arguments"), args[args.length - 1], args.length - 1, 0, args[args.length - 1].length());
							error.setAppendingBlank();
							error.setPossibleResolution("Append argument named \"" + vBranch.getLabel() + "\"");
							errors.add(error);
						}
					}
					Logger.debug("It is! Pushing its type (" + vBranch.getType().getName() + ") to the type stack and adding it as a branch.");
					types.push(vBranch.getType());
					branches.push(vBranch);
				}
			}
		}

		return branches;
	}

	/**
	 * Converts a list of String arguments into method arguments. This method assumes all arguments have already been parsed, 
	 * and no errors are present. If an error IS encountered, the errors list may be populated.
	 *
	 * @param branches Branches in this traversal
	 * @param arguments Array of validated and sanitized player-given String arguments
	 * @param errors List of errors, to potentially be populated.
	 */
	private Object[] processArguments(
		Deque<Branch> branches, 
		CommandSender sender,
		String[] arguments
	) {
		Object[] data = new Object[branches.size()];
		data[0] = sender;

		int index = 1;
		branches.pollLast();

		while (!branches.isEmpty())
		{
			Branch branch = branches.pollLast();
			Logger.debug("Argument " + index + ": " + arguments[index]);

			if (branch.isStatic())
			{
				data[index] = arguments[index];
				index++;
				continue;
			}

			if (branch.isVarargs())
			{
				String[] slice = java.util.Arrays.copyOfRange(arguments, index, arguments.length); // TODO Test this
				data[index] = parseGeneric(branch.getType().getComponentType(), slice);

				break;
			}

			try {
				data[index] = TypeManager.getParser(branch.getType()).parseType(arguments[index]);
			} catch (Exception e) {
				e.printStackTrace();
			}
			index++;
		}

		return data;
	}

	@Override
	public List<String> tabComplete(
		CommandSender sender,
		String alias,
		String[] args
	) {
		return this.onTabComplete(sender, this, alias, args);
	}

	@Override
	public List<String> onTabComplete(
		CommandSender sender,
		Command command,
		String label,
		String[] args
	) {
		if (this.tabCompleter != null)
		{
			return this.tabCompleter.onTabComplete(sender, command, label, args);
		}

		return java.util.Collections.emptyList();
	}

	/**
	 * Sets the current tab completion provider. Null tab completers will disable tab completion for this command.
	 *
	 * @param tabCompleter Tab completion engine
	 *
	 * @since 1.0.0
	 */
	public void setTabCompleter(
		@Nullable TabCompleter tabCompleter
	) {
		this.tabCompleter = tabCompleter;
	}

	public Set<String> getRoots()
	{
		return java.util.Collections.unmodifiableSet(this.branches.keySet());
	}

	// TODO This
	@Override
	public boolean isEnabled(String target)
	{
		return false;
	}

	@Override
	public boolean kill(String target)
	{
		return false;
	}

	@Override
	public boolean reenable(String target)
	{
		return false;
	}
}
