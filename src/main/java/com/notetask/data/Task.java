package com.notetask.data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class Task {

    public static final int PRIORITY_LOW    = 0;
    public static final int PRIORITY_NORMAL = 1;
    public static final int PRIORITY_HIGH   = 2;

    public String        id;
    public String        name;
    public boolean       completed;
    public String        type;
    public boolean       pinned;
    public int           priority = PRIORITY_NORMAL;
    public List<Subtask> subtasks = new ArrayList<>();

    public Task() {}

    public Task(String name, String type) {
        this.id        = UUID.randomUUID().toString();
        this.name      = name;
        this.type      = type;
        this.completed = false;
        this.pinned    = false;
        this.priority  = PRIORITY_NORMAL;
        this.subtasks  = new ArrayList<>();
    }

    public abstract String getProgressText();
    public abstract float  getProgressFraction();

    /** Returns e.g. "2/3" if subtasks exist, otherwise "". */
    public String getSubtaskSummary() {
        if (subtasks == null || subtasks.isEmpty()) return "";
        long done = subtasks.stream().filter(s -> s.completed).count();
        return done + "/" + subtasks.size();
    }

    public boolean allSubtasksDone() {
        if (subtasks == null || subtasks.isEmpty()) return true;
        return subtasks.stream().allMatch(s -> s.completed);
    }
}