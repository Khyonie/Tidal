package coffee.khyonieheart.tidal;

import org.bukkit.command.CommandSender;

import coffee.khyonieheart.hyacinth.Message;
import coffee.khyonieheart.hyacinth.module.HyacinthModule;
import coffee.khyonieheart.tidal.structure.Root;

public class TestCommand extends TidalCommand
{
	public TestCommand()
	{
		super("tidal", "Tidal test command", "/test", null);
	}

	@Root(isRootExecutor = true)
	public void noSubCommandExecutor(CommandSender sender)
	{
		Message.send(sender, "Root executor");
	}

	@Root(isLocalExecutor = true)
	public void foo(CommandSender sender, String[] operations, Integer[] integers, int operand)
	{
		for (String operation : operations)
		{
			for (int i : integers)
			{
				switch (operation.toLowerCase())
				{
					case "add" -> Message.send(sender, operation + ": " + (i + operand));
					case "sub" -> Message.send(sender, operation + ": " + (i - operand));
					case "mul" -> Message.send(sender, operation + ": " + (i * operand));
					case "div" -> Message.send(sender, operation + ": " + (i / operand));
				}
			}
		}
	}

	@Override
	public HyacinthModule getModule() 
	{
		return Tidal.getInstance();
	}
}
