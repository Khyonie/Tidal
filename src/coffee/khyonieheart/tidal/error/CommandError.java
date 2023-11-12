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
	private boolean appendDots;
	private String resolution = null;

	public CommandError(
		@NotNull String message,
		@Range(minimum = 0, maximum = Integer.MAX_VALUE) int index,
		boolean appendDots
	) {
		this.message = Objects.requireNonNull(message);
		this.index = RuntimeConditions.requirePositive(index);
		this.appendDots = appendDots;
	}

	public void display(
		int errorNumber,
		CommandSender sender,
		String commandLabel,
		String[] args
	) {
		Message.send(sender, "§cError " + errorNumber + ": " + message + " at position " + index);
		if (appendDots)
		{
			args = java.util.Arrays.copyOf(args, args.length + 1);
			args[args.length - 1] = "...";
		}

		args[index] = "§c§n" + args[index] + "§7";
		Message.send(sender, "§c↳ §7/" + commandLabel + " " + Arrays.toString(args, " "));

		if (this.resolution != null)
		{
			Message.send(sender, "§9 §9 Possible fix: " + this.resolution);
		}
	}

	public void setResolution(
		@NotNull String resolution
	) {
		this.resolution = Objects.requireNonNull(resolution);
	}
}
