package coffee.khyonieheart.tidal;

import org.bukkit.plugin.java.JavaPlugin;

public class TidalPlugin extends JavaPlugin 
{
	@Override
	public void onEnable()
	{
		new TidalTestCommand().register();
	}
}
