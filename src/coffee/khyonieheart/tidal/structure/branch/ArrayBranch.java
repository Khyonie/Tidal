package coffee.khyonieheart.tidal.structure.branch;

import java.lang.reflect.Array;
import java.util.List;

import org.bukkit.command.CommandSender;

import coffee.khyonieheart.hyacinth.Message;
import coffee.khyonieheart.tidal.ArgCount;
import coffee.khyonieheart.tidal.CommandContext;
import coffee.khyonieheart.tidal.TypeManager;
import coffee.khyonieheart.tidal.TypeParser;
import coffee.khyonieheart.tidal.error.CommandError;
import coffee.khyonieheart.tidal.structure.BranchType;
import coffee.khyonieheart.tidal.structure.Protected;

public class ArrayBranch<T> extends Branch
{
	private final Class<T> component;
	private final boolean isVarArgs;
	private int minArgs = 0;
	private int maxArgs = Integer.MAX_VALUE;

	public ArrayBranch(
		String label,
		Class<T> component,
		Protected commandProtectionData,
		ArgCount argCountData,
		boolean isVarArgs
	)
		throws UnsupportedOperationException
	{
		super(label, commandProtectionData);

		this.component = component;
		this.isVarArgs = isVarArgs;

		if (argCountData != null)
		{
			minArgs = argCountData.min();
			maxArgs = argCountData.max();
		}
	}

	@Override
	public BranchType getBranchType()
	{
		return BranchType.ARRAY;
	}

	@SuppressWarnings("unchecked")
	public T[] parse(
		CommandSender sender,
		String label,
		String[] allArgs,
		int index,
		String argsRaw,
		List<CommandError> errors,
		CommandContext context
	) {
		String[] args = argsRaw.split("\u0000");
		T[] data = (T[]) Array.newInstance(component, args.length);
		if (!TypeManager.hasParserFor(component))
		{
			errors.add(new CommandError("§dNo parser exists for type " + this.getComponentType().getName() + "§c", index, false));
			return data;
		}

		if (args.length < minArgs)
		{
			if (maxArgs == Integer.MAX_VALUE)
			{
				errors.add(new CommandError("Branch \"" + this.getLabel() + " expect at least " + this.minArgs + " argument(s), received " + args.length, index, false));
				return data;
			}

			errors.add(new CommandError("Branch \"" + this.getLabel() + "\" expects " + this.minArgs + "-" + this.maxArgs + " argument(s), recieved " + args.length, index, false));
			return data;
		}

		if (args.length > maxArgs)
		{
			errors.add(new CommandError("Branch \"" + this.getLabel() + "\" expects " + this.minArgs + "-" + this.maxArgs + " argument(s), recieved " + args.length, index, false));
			return data;
		}

		TypeParser<T> parser = (TypeParser<T>) TypeManager.getParser(component);
		for (int i = 0; i < args.length; i++)
		{
			CommandError error = switch (context)
			{
				case EXECUTION -> parser.validateExecution(sender, label, index, this, args[i], allArgs);
				case TABCOMPLETE -> parser.validateTabComplete(sender, label, index, this, args[i], allArgs);
			};

			if (error != null)
			{
				errors.add(error);
				data[i] = null;
				continue;
			}

			try {
				data[i] = parser.parseType(args[i]);
			} catch (Exception e) {
				errors.add(new CommandError("Array argument parse error, after validation", index, false));
				Message.send(sender, "§cAn error occurred when parsing array " + argsRaw.replace('\u0000', ' ') + " of type " + this.component.getName());
				data[i] = null;
			}
		}

		return data;
	}

	public Class<T> getComponentType()
	{
		return this.component;
	}

	public boolean isVarArgs()
	{
		return this.isVarArgs;
	}

	public int getMinimumArgs()
	{
		return this.minArgs;
	}

	public int getMaxArgs()
	{
		return this.maxArgs;
	}
}
