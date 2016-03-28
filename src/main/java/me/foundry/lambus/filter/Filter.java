package me.foundry.lambus.filter;

import me.foundry.lambus.Subscriber;
import me.foundry.lambus.event.Event;

import java.util.function.BiPredicate;

/**
 * @author Mark Johnson
 */

public interface Filter<T extends Event> extends BiPredicate<Subscriber<T>, T> {
    @Override
    boolean test(Subscriber<T> link, T event);
}
