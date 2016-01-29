package me.foundry.lambus.event;

/**
 * @author Mark Johnson
 */

public interface Cancellable {
    void setCancelled(boolean state);

    boolean isCancelled();
}
