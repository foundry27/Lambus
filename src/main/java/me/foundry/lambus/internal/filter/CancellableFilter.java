package me.foundry.lambus.internal.filter;

import me.foundry.lambus.Link;
import me.foundry.lambus.event.Cancellable;
import me.foundry.lambus.event.Event;
import me.foundry.lambus.filter.Filter;

/**
 * Created by Mark on 1/24/2016.
 */
public class CancellableFilter<T extends Event & Cancellable> implements Filter<T> {
    @Override
    public boolean test(Link<T> link, T event) {
        return !event.isCancelled();
    }
}
