package me.foundry.lambus;

import me.foundry.lambus.event.Event;

/**
 * @author Mark Johnson
 */

public interface Lambus {

    boolean subscribe(Class<? extends Event> e, Object o);

    boolean unsubscribe(Class<? extends Event> e, Object o);

    <T extends Event> Link<T> subscribeDirect(Link<T> link);

    boolean unsubscribeDirect(Link<?> link);

    boolean subscribeAll(Object o);

    boolean unsubscribeAll(Object o);

    <T extends Event> T post(T event);
}
