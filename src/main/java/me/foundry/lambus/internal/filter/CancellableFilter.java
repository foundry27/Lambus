package me.foundry.lambus.internal.filter;

import me.foundry.lambus.Subscriber;
import me.foundry.lambus.event.Cancellable;
import me.foundry.lambus.event.Event;
import me.foundry.lambus.filter.Filter;

/**
 * @author Mark Johnson
 */

public class CancellableFilter<T extends Event & Cancellable> implements Filter<T> {
    @Override
    public boolean test(Subscriber<T> link, T event) {
        return !event.isCancelled();
    }
}
