package coffee.khyonieheart.tidal;

import java.util.List;

import org.bukkit.command.CommandSender;

import coffee.khyonieheart.hyacinth.module.marker.PreventAutoLoad;
import coffee.khyonieheart.hyacinth.util.marker.NotEmpty;
import coffee.khyonieheart.hyacinth.util.marker.NotNull;
import coffee.khyonieheart.hyacinth.util.marker.Nullable;
import coffee.khyonieheart.tidal.error.CommandError;
import coffee.khyonieheart.tidal.structure.Branch;

@PreventAutoLoad
@SuppressWarnings("unchecked")
public abstract class TypeParser<T>
{
	private final Class<T> type;
	public static Class<TypeParser<?>> TYPE;

	static {
		TYPE = (Class<TypeParser<?>>) new ReferenceType().getClass().getSuperclass();
	}

	public TypeParser(
		@NotNull Class<T> type
	) {
		this.type = type;
	}

	@Nullable
	public abstract CommandError validateExecution(
		@NotNull CommandSender sender,
		@NotNull String commandLabel,
		int index,
		@NotNull Branch branch,
		@NotNull String argument,
		@NotEmpty String[] args
	);

	@Nullable
	public abstract CommandError validateTabComplete(
		@NotNull CommandSender sender,
		@NotNull String commandLabel,
		int index,
		@NotNull Branch branch,
		@NotNull String argument,
		@NotEmpty String[] args
	);

	@NotNull
	public abstract T parseType(
		@NotNull String input
	);

	@Nullable
	public abstract List<String> generateCompletions();

	public Class<T> getType()
	{
		return this.type;
	}

	@PreventAutoLoad
	private static class ReferenceType extends TypeParser<Object>
	{
		public ReferenceType() 
		{
			super(Object.class);
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
			throw new UnsupportedOperationException("Reference type parser cannot parse on its own.");
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
			throw new UnsupportedOperationException("Reference type parser cannot parse on its own.");
		}

		@Override
		public Object parseType(
			String input
		) {
			throw new UnsupportedOperationException("Reference type parser cannot parse on its own.");
		}

		@Override
		public List<String> generateCompletions() {
			return null;
		}

	}
}
