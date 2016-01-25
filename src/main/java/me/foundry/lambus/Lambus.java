package me.foundry.lambus;

import me.foundry.lambus.event.Event;

/**
 * Created by Mark on 1/24/2016.
 */
public interface Lambus {
    boolean subscribe(Class<? extends Event> e, Object o);
    <T extends Event> Link<T> subscribeDirect(Link<T> link);
    boolean subscribeAll(Object o);
    boolean unsubscribeAll(Object o);
    boolean unsubscribeDirect(Link<?> link);
    <T extends Event> T post(T event);
}
