package coffee.khyonieheart.tidal.tabcomplete;

import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import coffee.khyonieheart.anenome.NotNull;
import coffee.khyonieheart.tidal.TidalCommand;

public abstract class TabCompleter implements org.bukkit.command.TabCompleter
{
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args)
	{
		return this.onTabComplete(sender, (TidalCommand) command, args);
	}

	public abstract List<String> onTabComplete(
		@NotNull CommandSender sender, 
		@NotNull TidalCommand command,
		String[] args
	);
}
