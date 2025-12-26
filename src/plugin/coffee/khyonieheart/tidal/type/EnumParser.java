package coffee.khyonieheart.tidal.type;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;

import coffee.khyonieheart.tidal.error.CommandError;
import coffee.khyonieheart.tidal.structure.branch.Branch;

public class EnumParser<E extends Enum<E>> extends TypeParser<E>
{
	private List<String> values = new ArrayList<>();
	private final Class<E> enumType;

	public EnumParser(
		Class<E> type
	) {
		super(type);
		this.enumType = type;
		for (E e : type.getEnumConstants())
		{
			values.add(e.name().toLowerCase());
		}
	}

	@Override
	public List<String> generateCompletions() 
	{
		return values;
	}

	@Override
	public E parseType(
		String input
	) {
		return Enum.valueOf(this.enumType, input.toUpperCase());
	}

	@Override
	public CommandError validateExecution(
		CommandSender sender,
		String label,
		int index,
		Branch branch,
		String argument,
		String[] args
	) {
		try {
			Enum.valueOf(this.enumType, argument.toUpperCase());

			return null;
		} catch (IllegalArgumentException e) {
			return new CommandError("Unknown option \"" + argument + "\"", argument, index);
		}
	} 

	@Override
	public CommandError validateTabComplete(
		CommandSender sender,
		String label,
		int index,
		Branch branch,
		String argument,
		String[] args
	) {
		try {
			Enum.valueOf(this.enumType, argument.toUpperCase());

			return null;
		} catch (IllegalArgumentException e) {
			return new CommandError("Unknown option \"" + argument + "\"", argument, index);
		}
	}
}
