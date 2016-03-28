package me.foundry.lambus;

import me.foundry.lambus.event.Event;

/**
 * @author Mark Johnson
 */

public interface Lambus {

    boolean subscribe(Object o);

    boolean unsubscribe(Object o);

    boolean subscribe(Class<? extends Event> e, Object o);

    boolean unsubscribe(Class<? extends Event> e, Object o);

    <T extends Event> Subscriber<T> subscribeDirect(Subscriber<T> link);

    boolean unsubscribeDirect(Subscriber<?> link);

    <T extends Event> T post(T event);
}
