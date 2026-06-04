package com.notetask.data;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.notetask.NoteTaskMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class SaveData {

    public static String pendingItemId = null;
    private static final Path SAVE_DIR  = FabricLoader.getInstance()
            .getConfigDir().resolve("notetask");
    private static final Path SAVE_FILE = SAVE_DIR.resolve("data.json");

    /** Plain Gson for notes only — tasks use manual JSON to avoid adapter recursion. */
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static List<Note> notes = new ArrayList<>();
    private static List<Task> tasks = new ArrayList<>();

    public static List<Note> getNotes() { return notes; }
    public static List<Task> getTasks() { return tasks; }

    public static Path saveFilePath() {
        return SAVE_FILE;
    }

    public static void load() {
        if (!Files.exists(SAVE_FILE)) return;
        try (Reader r = Files.newBufferedReader(SAVE_FILE, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(r).getAsJsonObject();

            if (root.has("notes")) {
                Type t = new TypeToken<List<Note>>() {}.getType();
                List<Note> n = GSON.fromJson(root.get("notes"), t);
                notes = n != null ? n : new ArrayList<>();
            }

            if (root.has("tasks") && root.get("tasks").isJsonArray()) {
                tasks = new ArrayList<>();
                for (JsonElement el : root.getAsJsonArray("tasks")) {
                    tasks.add(TaskJson.read(el.getAsJsonObject()));
                }
            }
        } catch (IOException | JsonParseException e) {
            NoteTaskMod.LOGGER.error("Failed to load NoteTask data (resetting tasks/notes)", e);
            notes = new ArrayList<>();
            tasks = new ArrayList<>();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(SAVE_DIR);

            JsonObject root = new JsonObject();
            root.add("notes", GSON.toJsonTree(notes));

            JsonArray taskArr = new JsonArray();
            for (Task task : tasks) {
                taskArr.add(TaskJson.write(task));
            }
            root.add("tasks", taskArr);

            try (Writer w = Files.newBufferedWriter(SAVE_FILE, StandardCharsets.UTF_8)) {
                GSON.toJson(root, w);
            }
        } catch (IOException e) {
            NoteTaskMod.LOGGER.error("Failed to save NoteTask data", e);
        }
    }

    /** Manual task JSON — avoids Gson hierarchy adapter stack overflow on load/save. */
    private static final class TaskJson {

        static JsonObject write(Task task) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", task.id);
            obj.addProperty("name", task.name);
            obj.addProperty("completed", task.completed);
            obj.addProperty("type", task.type);
            obj.addProperty("pinned", task.pinned);

            if (task instanceof ItemTask it) {
                obj.addProperty("itemId", it.itemId);
                obj.addProperty("target", it.target);
                obj.addProperty("current", it.current);
                obj.addProperty("countStorage", it.countStorage);
            }
            return obj;
        }

        static Task read(JsonObject obj) throws JsonParseException {
            if (!obj.has("type")) {
                throw new JsonParseException("Task JSON missing 'type'");
            }
            return switch (obj.get("type").getAsString()) {
                case "item"   -> readItem(obj);
                case "manual" -> readManual(obj);
                default -> throw new JsonParseException(
                        "Unknown task type: " + obj.get("type").getAsString());
            };
        }

        private static ItemTask readItem(JsonObject obj) {
            ItemTask task = new ItemTask();
            readBase(obj, task);
            task.type = "item";
            task.itemId = obj.get("itemId").getAsString();
            task.target = obj.get("target").getAsInt();
            task.current = obj.has("current") ? obj.get("current").getAsInt() : 0;
            task.countStorage = !obj.has("countStorage") || obj.get("countStorage").getAsBoolean();
            return task;
        }

        private static ManualTask readManual(JsonObject obj) {
            ManualTask task = new ManualTask();
            readBase(obj, task);
            task.type = "manual";
            return task;
        }

        private static void readBase(JsonObject obj, Task task) {
            task.id = obj.get("id").getAsString();
            task.name = obj.get("name").getAsString();
            task.completed = obj.get("completed").getAsBoolean();
            task.pinned = obj.has("pinned") && obj.get("pinned").getAsBoolean();
        }
    }
}
