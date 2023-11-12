package coffee.khyonieheart.tidal.structure.branch;

import java.lang.reflect.Array;
import java.util.List;

import org.bukkit.command.CommandSender;

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
	private int maxArgs = 0;

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
			// TODO New command error infrastructure
			return data;
		}

		if (args.length < minArgs)
		{
			// TODO New command error infrastructure
			return data;
		}

		if (args.length > maxArgs)
		{
			// TODO New command error infrastructure
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
				// TODO Add error for validated parser failure
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
