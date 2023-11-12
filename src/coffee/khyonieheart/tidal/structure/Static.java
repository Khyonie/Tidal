package coffee.khyonieheart.tidal.structure;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.bukkit.command.CommandSender;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Static
{
	String value() default "USE_ARG_NAME";
	String permission() default "NOT_APPLICABLE";
	Class<? extends CommandSender> senderType() default CommandSender.class;
}
