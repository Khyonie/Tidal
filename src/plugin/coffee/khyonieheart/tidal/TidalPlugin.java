package coffee.khyonieheart.tidal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.bukkit.plugin.java.JavaPlugin;

public class TidalPlugin extends JavaPlugin 
{
	private static HashMap<String, TidalCommand> registeredTidalCommands = new HashMap<>();

	@Override
	public void onEnable()
	{
		initDefaultCommands();
	}

	protected static void addTidalCommand(
		TidalCommand command
	) {
		registeredTidalCommands.put(command.getName(), command);
	}

	public static List<String> getTidalCommandNames()
	{
		List<String> names = new ArrayList<>(registeredTidalCommands.keySet());
		Collections.sort(names);

		return names;
	}

	public static Set<String> getTidalCommandNamesUnsorted()
	{
		return Collections.unmodifiableSet(registeredTidalCommands.keySet());
	}

	public static TidalCommand getCommandByName(
		String name
	) {
		return registeredTidalCommands.get(name);
	}

	public static void initDefaultCommands()
	{
		new SyntaxCommand().register();
	}
}
