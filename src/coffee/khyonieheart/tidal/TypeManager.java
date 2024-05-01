package coffee.khyonieheart.tidal;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import coffee.khyonieheart.hyacinth.Logger;
import coffee.khyonieheart.hyacinth.module.nouveau.pipeline.ClassShader;
import coffee.khyonieheart.hyacinth.util.Reflect;
import coffee.khyonieheart.hyacinth.util.marker.NotNull;
import coffee.khyonieheart.hyacinth.util.marker.Nullable;

public class TypeManager implements ClassShader<TypeParser<?>>
{
	private static Map<Class<?>, TypeParser<?>> registeredTypes = new HashMap<>();

	@Override
	public Class<TypeParser<?>> getType() 
	{
		return TypeParser.TYPE;
	}

	@Override
	public TypeParser<?> process(
		Class<? extends TypeParser<?>> clazz,
		TypeParser<?> instance
	) {
		if (instance != null)
		{
			Logger.verbose("Shading parser " + clazz.getName() + " for type " + instance.getType().getName());
			registeredTypes.put(instance.getType(), instance);
			return null;
		}

		TypeParser<?> object = Reflect.simpleInstantiate(clazz);
		Logger.verbose("Shading parser " + clazz.getName() + " for type " + object.getType().getName());
		registeredTypes.put(object.getType(), object);
		return object;
	}

	public static void register(
		Class<?> type,
		TypeParser<?> parser
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
