package org.apache.maven.tools.plugin.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Inherited
@Retention( RUNTIME )
@Target( { TYPE } )
public @interface ThreadSafe
{
    boolean value() default true;
}
