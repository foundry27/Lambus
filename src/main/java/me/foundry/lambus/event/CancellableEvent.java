package me.foundry.lambus.event;

/**
 * Created by Mark on 1/24/2016.
 */
public class CancellableEvent extends Event implements Cancellable {
    private boolean cancelled;

    @Override
    public void setCancelled(boolean flag) {
        cancelled = flag;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void cancel() {
        setCancelled(true);
    }
}
