package me.foundry.lambus.priority;

/**
 * @author Mark Johnson
 */

public enum Priority {
    HIGHEST,
    HIGH,
    NORMAL,
    LOW,
    LOWEST;

    public static final int NUM_PRIORITIES = 5;

    public static final Priority[] fromOrdinal = new Priority[] {
            HIGHEST,
            HIGH,
            NORMAL,
            LOW,
            LOWEST
    };
}
