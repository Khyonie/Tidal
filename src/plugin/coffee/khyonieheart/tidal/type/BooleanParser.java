package coffee.khyonieheart.tidal.type;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;

import coffee.khyonieheart.tidal.error.CommandError;
import coffee.khyonieheart.tidal.structure.branch.Branch;

public class BooleanParser extends TypeParser<Boolean>
{
	public BooleanParser()
	{
		super(Boolean.TYPE);
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
		return switch (argument.toLowerCase())
		{
			case "true" -> null;
			case "false" -> null;
			default -> new CommandError("Cannot parse \"" + argument + "\" as a boolean", argument, index);
		};
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
		return switch (argument.toLowerCase())
		{
			case "true" -> null;
			case "false" -> null;
			default -> new CommandError("Invalid boolean \"" + argument + "\"", argument, index);
		};
	}

	@Override
	public Boolean parseType(
		String input
	) {
		return Boolean.parseBoolean(input);
	}

	@Override
	public List<String> generateCompletions() 
	{
		return new ArrayList<>(List.of("true", "false"));
	}
}
