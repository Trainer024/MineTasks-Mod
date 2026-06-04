package com.notetask.data;

import java.util.UUID;

public class Note {
    public String id;
    public String title;
    public String body;
    public long createdAt;
    public boolean pinned;

    /** Required for Gson deserialisation. */
    public Note() {}

    public Note(String title, String body) {
        this.id        = UUID.randomUUID().toString();
        this.title     = title;
        this.body      = body;
        this.createdAt = System.currentTimeMillis();
    }
}
