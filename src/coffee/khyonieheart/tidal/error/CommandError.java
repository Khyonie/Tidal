package coffee.khyonieheart.tidal.error;

import java.util.Objects;

import org.bukkit.command.CommandSender;

import coffee.khyonieheart.hyacinth.Message;
import coffee.khyonieheart.hyacinth.api.RuntimeConditions;
import coffee.khyonieheart.hyacinth.util.Arrays;
import coffee.khyonieheart.hyacinth.util.marker.NotNull;
import coffee.khyonieheart.hyacinth.util.marker.Range;

public class CommandError
{
	private String message;
	private int index;
	private String appendText;
	private AppendLocation appendLocation;
	private String resolution = null;
	private int errorStart = 0;
	private int errorEnd;

	public CommandError(
		@NotNull String message,
		@NotNull String offendingArgument,
		@Range(minimum = 0, maximum = Integer.MAX_VALUE) int index
	) {
		this.message = Objects.requireNonNull(message);
		this.index = RuntimeConditions.requirePositive(index);
		this.errorEnd = offendingArgument.length();
	}

	public CommandError appendAt(
		@NotNull AppendLocation appendLocation,
		@NotNull String message
	) {
		this.appendLocation = Objects.requireNonNull(appendLocation);
		this.appendText = Objects.requireNonNull(message);

		return this;
	}

	public CommandError appendMessage(
		@NotNull String message
	) {
		this.message += Objects.requireNonNull(message);
		return this;
	}

	public CommandError setBounds(
		int start, 
		int end
	) {
		this.errorStart = RuntimeConditions.requirePositive(start);
		this.errorEnd = end;
		return this;
	}

	public void display(
		int errorNumber,
		CommandSender sender,
		String commandLabel,
		String[] args
	) {
		// Sanitize args for null codepoint, used in array processing
		args = args.clone();
		for (int i = 0; i < args.length; i++)
		{
			args[i] = args[i].replace('\u0000', ' ');
		}
		Message.send(sender, "§cError " + errorNumber + ": " + message + " at position " + index);

		// Apply appendage
		if (appendLocation != null)
		{
			String[] newArgs = new String[args.length + 1];
			switch (this.appendLocation)
			{
				case END -> {
					for (int i = 0; i < args.length; i++)
					{
						newArgs[i] = args[i];
					}

					newArgs[args.length] = this.appendText;
					args = newArgs;
				}
				case ARG -> {
					for (int i = 0; i < args.length; i++)
					{
						if (i == index)
						{
							newArgs[i] = appendText;
							continue;
						}

						newArgs[i] = args[i];
					}

					args = newArgs;
				}
			}
		}

		// Display
		args[index] = "§e" + new StringBuilder(args[index])
			.replace(this.errorStart, this.errorEnd, "§c§n" + args[index].substring(this.errorStart, this.errorEnd) + "§e")
			.toString() + "§7"; 

		Message.send(sender, "§c§l⤷ §7/" + commandLabel + " " + Arrays.toString(args, " "));

		if (this.resolution != null)
		{
			Message.send(sender, "§9 §9 Possible fix: " + this.resolution);
		}
	}

	public String displaySimple()
	{
		return this.message + " @ position " + (this.index + 1);
	}

	public void setResolution(
		@NotNull String resolution
	) {
		this.resolution = Objects.requireNonNull(resolution);
	}

	public static enum AppendLocation
	{
		END,
		ARG,
		;
	}
}
