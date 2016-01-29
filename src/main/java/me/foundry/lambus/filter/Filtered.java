package me.foundry.lambus.filter;

import java.lang.annotation.*;

/**
 * @author Mark Johnson
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Filtered {
    /* ElementType.LOCAL_VARIABLE is not supported by the Java compiler */
    Class<? extends Filter>[] value();
}
