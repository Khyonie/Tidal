package coffee.khyonieheart.tidal.type;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import coffee.khyonieheart.hyacinth.util.Arrays;
import coffee.khyonieheart.hyacinth.util.marker.NotNull;
import coffee.khyonieheart.hyacinth.util.marker.Nullable;
import coffee.khyonieheart.tidal.TypeParser;
import coffee.khyonieheart.tidal.error.CommandError;
import coffee.khyonieheart.tidal.structure.branch.Branch;

public class OfflinePlayerParser extends TypeParser<OfflinePlayer>
{
	public OfflinePlayerParser()
	{
		super(OfflinePlayer.class);
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
		if (getByName(argument) != null)
		{
			return null;
		}

		return new CommandError("Player \"" + argument + "\" is not known on this server", index, false);
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
		if (getByName(argument) != null)
		{
			return null;
		}

		return new CommandError("Player \"" + argument + "\" does not exist does not exist", index, false);
	}

	@Override
	public OfflinePlayer parseType(
		String input
	) {
		return getByName(input);
	}

	@Override
	public List<String> generateCompletions() 
	{
		return java.util.Arrays.asList(Arrays.map(Bukkit.getOfflinePlayers(), String.class, (p) -> p.getName()));
	}

	@Nullable
	private static OfflinePlayer getByName(
		@NotNull String argument
	) {
		for (OfflinePlayer player : Bukkit.getOfflinePlayers())
		{
			if (player.getName().equals(argument))
			{
				return player;
			}
		}

		return null;
	}
}
