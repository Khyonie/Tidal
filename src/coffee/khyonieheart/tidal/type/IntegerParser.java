package coffee.khyonieheart.tidal.type;

import java.util.List;

import org.bukkit.command.CommandSender;

import coffee.khyonieheart.hyacinth.util.marker.Range;
import coffee.khyonieheart.tidal.TypeParser;
import coffee.khyonieheart.tidal.error.CommandError;
import coffee.khyonieheart.tidal.structure.branch.Branch;

public class IntegerParser extends TypeParser<Integer>
{
	public IntegerParser()
	{
		super(Integer.TYPE);
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
					return new CommandError("Integer out of range, valid range: " + range.minimum() + "-" + range.maximum(), index, false);
				}
			}
			
			return null;
		} catch (NumberFormatException intE) {
			CommandError error = new CommandError("Cannot parse \"" + argument + "\" as an integer", index, false);

			try {
				float val = Float.parseFloat(argument);

				error.setResolution("Round " + argument + " to " + Math.round(val));
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
					return new CommandError("Argument out of range: " + range.minimum() + "-" + range.maximum(), index, false);
				}
			}
			
			return null;
		} catch (NumberFormatException intE) {
			return new CommandError("Invalid integer \"" + argument + "\"", index, false);
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
