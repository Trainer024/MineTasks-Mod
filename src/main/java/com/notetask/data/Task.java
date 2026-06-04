package com.notetask.data;

import java.util.UUID;

public abstract class Task {
    public String  id;
    public String  name;
    public boolean completed;
    public String  type; // "item" | "manual"
    /** Pinned tasks appear first on the HUD. */
    public boolean pinned;

    public Task() {}

    public Task(String name, String type) {
        this.id        = UUID.randomUUID().toString();
        this.name      = name;
        this.type      = type;
        this.completed = false;
        this.pinned    = false;
    }

    public abstract String getProgressText();

    public abstract float getProgressFraction();
}
