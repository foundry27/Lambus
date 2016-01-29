package me.foundry.lambus.filter;

import me.foundry.lambus.Link;
import me.foundry.lambus.event.Event;

import java.util.function.BiPredicate;

/**
 * @author Mark Johnson
 */

public interface Filter<T extends Event> extends BiPredicate<Link<T>, T> {
    @Override
    boolean test(Link<T> link, T event);
}
