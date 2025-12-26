package coffee.khyonieheart.tidal;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import coffee.khyonieheart.tidal.structure.Root;
import coffee.khyonieheart.tidal.structure.Static;

public class TidalTestCommand extends TidalCommand 
{
	public TidalTestCommand()
	{
		super("ttest", "Tidal testing command", "ttest", null);
	}

	@Root
	public void types(
		Player player,
		boolean bool,
		@Static String staticp,
		int integer,
		float f32,
		OfflinePlayer players,
		String str,
		TestingType type
	) {
		player.spigot().sendMessage(new Gradient("#7F7FFF", "#FF7F7F").createComponents("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat."));
	}
}
