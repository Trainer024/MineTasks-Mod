package com.notetask.data;

public class ItemTask extends Task {
    public String itemId;
    public int    target;
    public int    current;
    public boolean countStorage = true;

    /** Last scanned counts (for UI). */
    public transient int lastInvCount;
    public transient int lastStorageCount;

    public ItemTask() { super(); }

    public ItemTask(String name, String itemId, int target) {
        this(name, itemId, target, true);
    }

    public ItemTask(String name, String itemId, int target, boolean countStorage) {
        super(name, "item");
        this.itemId       = itemId;
        this.target       = target;
        this.current      = 0;
        this.countStorage = countStorage;
    }

    @Override
    public String getProgressText() {
        return current + "/" + target;
    }

    public String getStorageHint() {
        if (!countStorage) return "";
        return "inv " + lastInvCount + " + stor " + lastStorageCount;
    }

    @Override
    public float getProgressFraction() {
        return target == 0 ? 0f : Math.min(1f, (float) current / target);
    }
}
