package coffee.khyonieheart.tidal.structure;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.bukkit.command.CommandSender;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Protected
{
	String[] permission() default { };
	Class<? extends CommandSender> senderType() default CommandSender.class;
}
