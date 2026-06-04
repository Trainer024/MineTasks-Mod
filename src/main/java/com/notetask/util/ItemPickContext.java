package com.notetask.util;

/**
 * Holds an item picked from browse/RRV until the task editor screen finishes {@code init()}.
 * (Returning to a Screen re-runs init and would otherwise wipe widget text.)
 */
public final class ItemPickContext {

    private static ItemMatch pending;

    private ItemPickContext() {}

    public static void set(ItemMatch match) {
        pending = match;
    }

    public static ItemMatch consume() {
        ItemMatch match = pending;
        pending = null;
        return match;
    }
}
