package coffee.khyonieheart.tidal.type;

import java.util.List;

import org.bukkit.command.CommandSender;

import coffee.khyonieheart.tidal.TypeParser;
import coffee.khyonieheart.tidal.error.CommandError;
import coffee.khyonieheart.tidal.structure.branch.Branch;

public class StringParser extends TypeParser<String>
{
	public StringParser() 
	{
		super(String.class);
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
		return null;
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
		return null;
	}

	/** @implNote This method returns the input unmodified. */
	@Override
	public String parseType(
		String input
	) {
		return input;
	}

	@Override
	public List<String> generateCompletions() 
	{
		return null;
	}

}
