package coffee.khyonieheart.tidal.type;

import java.util.List;

import org.bukkit.command.CommandSender;

import coffee.khyonieheart.hyacinth.util.marker.Range;
import coffee.khyonieheart.tidal.TypeParser;
import coffee.khyonieheart.tidal.error.CommandError;
import coffee.khyonieheart.tidal.structure.Branch;

public class ConstantIntegerParser extends TypeParser<Integer>
{
	public ConstantIntegerParser()
	{
		super(Integer.class);
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
			int val = Integer.parseInt(argument);

			if (branch.hasAnnotation(Range.class))
			{
				Range range = branch.getAnnotation(Range.class);
				if (val < range.minimum() || val > range.maximum())
				{
					return new CommandError("Integer out of range, valid range: " + range.minimum() + "-" + range.maximum(), argument, index, 0, argument.length());
				}
			}
			
			return null;
		} catch (NumberFormatException intE) {
			CommandError error = new CommandError("Cannot parse \"" + argument + "\" as an integer", argument, index, 0, argument.length());

			try {
				float val = Float.parseFloat(argument);

				error.setPossibleResolution("Round " + argument + " to " + Math.round(val));
			} catch (NumberFormatException floatE) { }

			return error;
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
			int val = Integer.parseInt(argument);

			if (branch.hasAnnotation(Range.class))
			{
				Range range = branch.getAnnotation(Range.class);

				if (val < range.minimum() || val > range.maximum())
				{
					return new CommandError("Argument out of range: " + range.minimum() + "-" + range.maximum(), argument, val, val, val);
				}
			}
			
			return null;
		} catch (NumberFormatException intE) {
			return new CommandError("Invalid integer \"" + argument + "\"", argument, index, 0, argument.length());
		}
	}

	@Override
	public Integer parseType(
		String input
	) {
		return Integer.parseInt(input);
	}

	@Override
	public List<String> generateCompletions() 
	{
		return null;
	}
}
