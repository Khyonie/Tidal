package coffee.khyonieheart.tidal.tabcomplete;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;

import coffee.khyonieheart.anenome.ArrayIterator;
import coffee.khyonieheart.anenome.Arrays;
import coffee.khyonieheart.tidal.CommandContext;
import coffee.khyonieheart.tidal.TidalCommand;
import coffee.khyonieheart.tidal.TypeManager;
import coffee.khyonieheart.tidal.concatenation.Concatenate;
import coffee.khyonieheart.tidal.concatenation.ConcatenationException;
import coffee.khyonieheart.tidal.error.CommandError;
import coffee.khyonieheart.tidal.structure.BranchType;
import coffee.khyonieheart.tidal.structure.branch.ArrayBranch;
import coffee.khyonieheart.tidal.structure.branch.Branch;
import coffee.khyonieheart.tidal.structure.branch.TypedBranch;

public class TidalTabCompleter extends TabCompleter
{
	private static boolean isEnabled = true;

	@Override
	public List<String> onTabComplete(
		CommandSender sender,
		TidalCommand command,
		String[] args
	) {
		if (!isEnabled)
		{
			return List.of();
		}
		
		if (args.length == 0)
		{
			return List.of();
		}

		// Preprocessing
		//-------------------------------------------------------------------------------- 

		try {
			args = Concatenate.concatenate('"', ' ', true, false, args);
		} catch (ConcatenationException e) {
			switch (e.getType()) 
			{
				case UNEXPECTED_END -> { return List.of("§c(⚠ Unexpected quoted argument end @ position " + (e.getIndex() + 1) + ")"); }
				case UNEXPECTED_START -> { return List.of("§c(⚠ Unexpected quoted argument start @ position " + (e.getIndex() + 1) + ")"); }
				case UNTERMINATED_END -> { } // Left blank
			}
		}

		try {
			args = Concatenate.concatenate('(', ')', '\u0000', true, false, args);
		} catch (ConcatenationException e) {
			switch (e.getType()) 
			{
				case UNEXPECTED_END -> { return List.of("§c(⚠ Unexpected array end @ position " + (e.getIndex() + 1) + ")"); }
				case UNEXPECTED_START -> { return List.of("§c(⚠ Unexpected array start @ position " + (e.getIndex() + 1) + ")"); }
				case UNTERMINATED_END -> { } // Left blank
			}
		}

		// Traversal
		//-------------------------------------------------------------------------------- 

		List<CommandError> errors = new ArrayList<>();
		List<String> suggestions;

		List<String> modifiableArgs = Arrays.toArrayList(args);
		if (command.hasLocalExecutor())
		{
			suggestions = traverse(sender, errors, command, command.getLocalExecutor(), 0, modifiableArgs);

			if (!errors.isEmpty())
			{
				return List.of("§c(⚠ " + errors.get(errors.size() - 1).displaySimple() + (errors.size() > 1 ? " §c§o+" + (errors.size() - 1) + " more" : "") + "§c)");
			}

			return suggestions;
		}

		if (args.length == 1 || args.length == 0)
		{
			return command.getRoots();
		}

		if (!command.hasRoot(args[0]))
		{
			return List.of("§c(⚠ Invalid subcommand \"" + args[0].replace('\u0000', ' ') + "\")");
		}

		suggestions = traverse(sender, errors, command, command.getRoot(args[0]), 1, modifiableArgs);

		if (!errors.isEmpty())
		{
			return List.of("§c(⚠ " + errors.get(errors.size() - 1).displaySimple() + (errors.size() > 1 ? " §c§o+" + (errors.size() - 1) + " more" : "") + "§c)");
		}

		return suggestions;
	}

	private List<String> traverse(
		CommandSender sender,
		List<CommandError> errors,
		TidalCommand command,
		Branch root,
		int startingIndex,
		List<String> modifiableArgs
	) {
		String[] args = modifiableArgs.toArray(new String[modifiableArgs.size()]);
		Branch branch = root;
		String arg = "";
		for (int i = startingIndex; i < args.length; i++)
		{
			arg = args[i];
			if (branch.isLeaf())
			{
				if (branch.getBranchType() == BranchType.ARRAY)
				{
					if (((ArrayBranch<?>) branch).isVarArgs())
					{
						ArrayBranch<?> aBranch = (ArrayBranch<?>) branch;
						if (!TypeManager.hasParserFor(aBranch.getComponentType()))
						{
							return List.of("§c(⚠ No available parser for type \"" + aBranch.getComponentType().getSimpleName() + "\")");
						}

						aBranch.parse(sender, command.getLabel(), args, i, "", errors, CommandContext.TABCOMPLETE);

						return TypeManager.getParser(aBranch.getComponentType()).generateCompletions();
					}
				}

				break;
			}

			switch (branch.getConnectedBranchType())
			{
				case STATIC -> {
					if (branch.hasStaticBranch(arg))
					{
						if (branch.getStaticBranch(arg).isAuthorized(sender))
						{
							branch = branch.getStaticBranch(arg);
							continue;
						}
					}

					if (i == (args.length - 1))
					{
						List<String> validBranches = branch.getBranches()
							.stream()
							.filter(b -> b.isAuthorized(sender))
							.map(b -> b.getLabel())
							.toList();

						return validBranches;
					}

					return List.of("§c(⚠ Invalid option \"" + arg.replace('\u0000', ' ') + "\" @ position " + i + ")");
				}
				case TYPED -> {
					branch = branch.getVariableBranch();
					if (!branch.isAuthorized(sender))
					{
						return List.of();
					}

					TypedBranch<?> tBranch = (TypedBranch<?>) branch;

					if (!TypeManager.hasParserFor(tBranch.getType()))
					{
						return List.of("§c(⚠ No available parser for type \"" + tBranch.getType().getSimpleName() + "\")");
					}

					if (i == (args.length - 1))
					{
						List<String> suggestions = TypeManager.getParser(tBranch.getType()).generateCompletions();
						if (suggestions == null)
						{
							suggestions = List.of("§7[<" + tBranch.getLabel() + ">]");
						}
						return new ArrayList<>(suggestions);
					}

					CommandError error = TypeManager.getParser(tBranch.getType()).validateTabComplete(sender, command.getLabel(), i, branch, arg, args);

					if (error != null)
					{
						return List.of("§c(⚠ " + error.displaySimple() + ")");
					}

					continue;
				}
				case ARRAY -> {
					branch = branch.getVariableBranch();
					if (!branch.isAuthorized(sender))
					{
						return List.of();
					}

					ArrayBranch<?> aBranch = (ArrayBranch<?>) branch;

					if (!TypeManager.hasParserFor(aBranch.getComponentType()))
					{
						return List.of("§c(⚠ No available parser for type \"" + aBranch.getComponentType().getSimpleName() + "\")");
					}

					if (aBranch.isVarArgs())
					{
						StringBuilder builder = new StringBuilder();
						ArrayIterator<String> iter = new ArrayIterator<>(args, i);
						while (iter.hasNext())
						{
							builder.append(iter.next());

							if (iter.hasNext())
							{
								builder.append("\u0000");
							}
						}

						arg = builder.toString();
						String[] newArgs = new String[i + 1];
						for (int o = 0; o < newArgs.length; o++)
						{
							newArgs[o] = args[o];
						}
						newArgs[newArgs.length - 1] = arg;
						args = newArgs;
						modifiableArgs.clear();
						modifiableArgs.addAll(java.util.Arrays.asList(args));
					}

					Object[] array = aBranch.parse(sender, command.getLabel(), args, i, arg, errors, CommandContext.TABCOMPLETE);

					if (!errors.isEmpty())
					{
						return List.of("§c(⚠ " + errors.get(errors.size() - 1).displaySimple() + (errors.size() > 1 ? " §c§o+" + (errors.size() - 1) + " more" : "") + "§c)");
					}

					if (i == (args.length - 1))
					{
						List<String> suggestions = TypeManager.getParser(aBranch.getComponentType()).generateCompletions();
						if (suggestions == null)
						{
							suggestions = new ArrayList<>();
						}

						try {
							suggestions.addAll(Collections.emptyList());
							suggestions.add(0, "DUMMY");
							suggestions.remove(0);
						} catch (UnsupportedOperationException e) {
							suggestions = new ArrayList<>(suggestions);
						}
						
						if (aBranch.getMinimumArgs() != 0 || aBranch.getMaxArgs() != Integer.MAX_VALUE)
						{
							boolean conditionsMet = (array.length >= aBranch.getMinimumArgs() && array.length <= aBranch.getMaxArgs());
							suggestions.add(0, "# §7[<" + branch.getLabel() + "> " + array.length + "/" + aBranch.getMinimumArgs() + "-" + (aBranch.getMaxArgs() == Integer.MAX_VALUE ? "∞" : aBranch.getMaxArgs()) + " " + (conditionsMet ? "§a§l✓§8" : "§c§l✗§8") + " ]");
						}

						return suggestions;
					}

					continue;
				}
			}
		}

		return List.of();
	}
}
