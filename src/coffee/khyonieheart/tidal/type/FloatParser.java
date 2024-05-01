package coffee.khyonieheart.tidal.type;

import java.util.List;

import org.bukkit.command.CommandSender;

import coffee.khyonieheart.tidal.TypeParser;
import coffee.khyonieheart.tidal.error.CommandError;
import coffee.khyonieheart.tidal.structure.branch.Branch;

public class FloatParser extends TypeParser<Float>
{
	public FloatParser()
	{
		super(Float.TYPE);
	}

	@Override
	public CommandError validateExecution(
		CommandSender sender,
		String commandLabel,
		int index,
		Branch branch,
		String argument,
		String[] args
	) {
		try {
			float value = Float.parseFloat(argument);

			if (Float.isNaN(value))
			{
				return new CommandError("Float must be a number", argument, index);
			}

			if (Float.isInfinite(value))
			{
				return new CommandError("Float must be real", argument, index);
			}

			return null;
		} catch (NumberFormatException e) {
			return new CommandError("Cannot parse \"" + argument + "\" as a float", argument, index);
		}
	}

	@Override
	public CommandError validateTabComplete(
		CommandSender sender,
		String commandLabel,
		int index,
		Branch branch,
		String argument,
		String[] args
	) {
		try {
			float value = Float.parseFloat(argument);

			if (Float.isNaN(value) || Float.isInfinite(value))
			{
				throw new NumberFormatException();
			}
			
			return null;
		} catch (NumberFormatException e) {
			return new CommandError("Invalid float \"" + argument + "\"", argument, index);
		}
	}

	@Override
	public Float parseType(
		String input
	) {
		return Float.parseFloat(input);
	}

	@Override
	public List<String> generateCompletions() 
	{
		return null;
	}
}
