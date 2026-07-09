package coffee.khyonieheart.tidal;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import coffee.khyonieheart.anenome.NotNull;
import coffee.khyonieheart.anenome.Nullable;
import coffee.khyonieheart.tidal.type.BooleanParser;
import coffee.khyonieheart.tidal.type.ConstantIntegerParser;
import coffee.khyonieheart.tidal.type.FloatParser;
import coffee.khyonieheart.tidal.type.IntegerParser;
import coffee.khyonieheart.tidal.type.OfflinePlayerParser;
import coffee.khyonieheart.tidal.type.OnlinePlayerParser;
import coffee.khyonieheart.tidal.type.TidalCommandParser;
import coffee.khyonieheart.tidal.type.StringParser;
import coffee.khyonieheart.tidal.type.TypeParser;

public class TypeManager
{
	private static Map<Class<?>, TypeParser<?>> registeredTypes = new HashMap<>();

	static {
		registeredTypes.put(Boolean.TYPE, new BooleanParser());
		registeredTypes.put(Integer.TYPE, new IntegerParser());
		registeredTypes.put(Integer.class, new ConstantIntegerParser());
		registeredTypes.put(Float.TYPE, new FloatParser());
		registeredTypes.put(OfflinePlayer.class, new OfflinePlayerParser());
		registeredTypes.put(Player.class, new OnlinePlayerParser());
		registeredTypes.put(String.class, new StringParser());
		registeredTypes.put(TidalCommand.class, new TidalCommandParser());
	}

	/** This type should not be instantiated */
	private TypeManager() {}

	public static void register(
		@NotNull Class<?> type,
		@NotNull TypeParser<?> parser
	) {
		Objects.requireNonNull(type);
		Objects.requireNonNull(parser);

		registeredTypes.put(type, parser);
	}

	@Nullable
	public static TypeParser<?> getParser(
		@NotNull Class<?> type
	) {
		Objects.requireNonNull(type);
		return registeredTypes.get(type);
	}

	public static boolean hasParserFor(
		@NotNull Class<?> type
	) {
		Objects.requireNonNull(type);
		return registeredTypes.containsKey(type);
	}
}
