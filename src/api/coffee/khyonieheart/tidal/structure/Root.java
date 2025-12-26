package coffee.khyonieheart.tidal.structure;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Root
{
	String value() default "USE_METHOD_NAME";
	String permission() default "NOT_APPLICABLE";
	boolean isLocalExecutor() default false;
	boolean isRootExecutor() default false;
}
