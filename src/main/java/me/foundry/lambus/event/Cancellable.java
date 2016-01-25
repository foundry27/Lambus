package me.foundry.lambus.event;

/**
 * Created by Mark on 1/24/2016.
 */
public interface Cancellable {
    void setCancelled(boolean state);

    boolean isCancelled();

    void cancel();
}
