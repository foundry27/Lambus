package me.foundry.lambus.event;

/**
 * @author Mark Johnson
 */

public abstract class CancellableEvent extends Event implements Cancellable {
    private boolean cancelled;

    @Override
    public void setCancelled(boolean state) {
        cancelled = state;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }
}
