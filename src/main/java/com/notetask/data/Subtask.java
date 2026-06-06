package com.notetask.data;

import java.util.UUID;

public class Subtask {
    public String  id;
    public String  name;
    public boolean completed;

    /** Required for deserialization. */
    public Subtask() {}

    public Subtask(String name) {
        this.id        = UUID.randomUUID().toString();
        this.name      = name;
        this.completed = false;
    }
}