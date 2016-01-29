package me.foundry.lambus;

import me.foundry.lambus.event.Event;

import java.io.Serializable;

/**
 * @author Mark Johnson
 */

@FunctionalInterface
public interface Link<T extends Event> extends Serializable {
    void invoke(T event);
}
