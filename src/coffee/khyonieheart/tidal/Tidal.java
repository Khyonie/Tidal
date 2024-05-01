package coffee.khyonieheart.tidal;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Scanner;

import org.bukkit.command.Command;

import coffee.khyonieheart.hyacinth.Hyacinth;
import coffee.khyonieheart.hyacinth.Logger;
import coffee.khyonieheart.hyacinth.module.HyacinthModule;
import coffee.khyonieheart.hyacinth.util.Collections;

public class Tidal implements HyacinthModule
{
	private static Tidal instance;

	@Override
	public void onEnable() 
	{
		instance = this;

		File excludeFile = new File("./exclude.txt");
		if (!excludeFile.exists())
		{
			return;
		}

		try (Scanner scanner = new Scanner(excludeFile)) 
		{
			Method m = Hyacinth.getCommandManager().getCommandMap().getClass().getDeclaredMethod("getKnownCommands");
			@SuppressWarnings("unchecked")
			Map<String, Command> actualCommandMap = (Map<String, Command>) m.invoke(Hyacinth.getCommandManager().getCommandMap());
			Logger.verbose("Known commands: [ " + Collections.toString(actualCommandMap.keySet(), ", ", s -> "/" + s) + " ]");
			while (scanner.hasNextLine())
			{
				String command = scanner.nextLine();
				if (actualCommandMap.containsKey(command))
				{
					actualCommandMap.remove(command);
					continue;
				}

				Logger.log("Cannot remove unknown command /" + command);
			}
		} catch (FileNotFoundException | SecurityException | IllegalArgumentException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onDisable() 
	{
		
	}

	public static Tidal getInstance()
	{
		return instance;
	}
}
