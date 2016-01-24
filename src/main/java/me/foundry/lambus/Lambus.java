package me.foundry.lambus;

/**
 * Created by Mark on 1/24/2016.
 */
public interface Lambus {
    boolean subscribe(Class<? extends Event> e, Object o);
    boolean subscribeAll(Object o);
    boolean unsubscribeAll(Object o);
    <T extends Event> T post(T event);
}
