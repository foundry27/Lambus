package me.foundry.lambus.filter;

import me.foundry.lambus.Link;
import me.foundry.lambus.event.Event;

import java.util.function.BiPredicate;

/**
 * Created by Mark on 1/24/2016.
 */
public interface Filter<T extends Event> extends BiPredicate<Link<T>, T> {
    @Override
    boolean test(Link<T> link, T event);
}
