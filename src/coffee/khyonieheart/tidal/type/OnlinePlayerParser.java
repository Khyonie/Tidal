package coffee.khyonieheart.tidal.type;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import coffee.khyonieheart.hyacinth.util.Lists;
import coffee.khyonieheart.tidal.TypeParser;
import coffee.khyonieheart.tidal.error.CommandError;
import coffee.khyonieheart.tidal.structure.branch.Branch;

public class OnlinePlayerParser extends TypeParser<Player>
{
	public OnlinePlayerParser() 
	{
		super(Player.class);
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
		if (Bukkit.getPlayerExact(argument) != null)
		{
			return null;
		}

		CommandError error = new CommandError("No player named \"" + argument + "\" is online", argument, index);
		
		return error;
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
		if (Bukkit.getPlayerExact(argument) != null)
		{
			return null;
		}

		CommandError error = new CommandError("No player named \"" + argument + "\" is online", argument, index);
		
		return error;
	}

	@Override
	public Player parseType(
		String input
	) {
		return Bukkit.getPlayerExact(input);
	}

	@Override
	public List<String> generateCompletions() 
	{
		return Lists.map(Bukkit.getOnlinePlayers(), (p) -> p.getName());
	}
}
