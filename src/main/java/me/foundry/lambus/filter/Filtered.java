package me.foundry.lambus.filter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by Mark on 1/24/2016.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Filtered {
    /* ElementType.LOCAL_VARIABLE is not supported by the Java compiler */
    Class<? extends Filter>[] value();
}
