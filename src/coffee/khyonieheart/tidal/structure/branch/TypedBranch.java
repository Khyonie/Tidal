package coffee.khyonieheart.tidal.structure.branch;

import java.util.List;
import java.util.Objects;

import org.bukkit.command.CommandSender;

import coffee.khyonieheart.tidal.CommandContext;
import coffee.khyonieheart.tidal.TypeManager;
import coffee.khyonieheart.tidal.TypeParser;
import coffee.khyonieheart.tidal.error.CommandError;
import coffee.khyonieheart.tidal.structure.BranchType;
import coffee.khyonieheart.tidal.structure.Protected;

public class TypedBranch<T> extends Branch
{
	private final Class<T> type;

	public TypedBranch(
		String label,
		Class<T> type,
		Protected commandProtectionData
	) {
		super(label, commandProtectionData);

		this.type = Objects.requireNonNull(type);
	}

	public BranchType getBranchType()
	{
		return BranchType.TYPED;
	}

	@SuppressWarnings("unchecked")
	public T parse(
		CommandSender sender,
		String label,
		String[] allArgs,
		int index,
		String arg,
		List<CommandError> errors,
		CommandContext context
	) {
		if (!TypeManager.hasParserFor(this.type))
		{
			CommandError error = new CommandError("Unregistered type " + this.type.getName(), arg, index);
			error.setResolution("Create a type parser missing type " + this.type.getName());
			errors.add(error);

			return null;
		}

		TypeParser<T> parser = (TypeParser<T>) TypeManager.getParser(this.type);

		CommandError error = switch (context)
		{
			case EXECUTION -> parser.validateExecution(sender, label, index, this, arg, allArgs);
			case TABCOMPLETE -> parser.validateTabComplete(sender, label, index, this, arg, allArgs);
		};

		if (error != null)
		{
			errors.add(error);
			return null;
		}

		try {
			return parser.parseType(arg);
		} catch (Exception e) {
			return null;
		}
	}

	public Class<T> getType()
	{
		return this.type;
	}
}
