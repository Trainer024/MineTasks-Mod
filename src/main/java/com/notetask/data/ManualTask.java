package com.notetask.data;

public class ManualTask extends Task {

    /** Required for Gson deserialisation. */
    public ManualTask() { super(); }

    public ManualTask(String name) {
        super(name, "manual");
    }

    @Override
    public String getProgressText() {
        return completed ? "done" : "to-do";
    }

    @Override
    public float getProgressFraction() {
        return completed ? 1.0f : 0.0f;
    }
}
