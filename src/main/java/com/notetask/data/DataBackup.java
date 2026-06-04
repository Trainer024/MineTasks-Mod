package com.notetask.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.notetask.NoteTaskMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DataBackup {

    private static final Path BACKUP_DIR = FabricLoader.getInstance()
            .getConfigDir().resolve("notetask").resolve("backups");

    private DataBackup() {}

    public static Path exportBackup() throws IOException {
        Files.createDirectories(BACKUP_DIR);
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path out = BACKUP_DIR.resolve("notetask-backup-" + stamp + ".json");
        Files.copy(SaveData.saveFilePath(), out);
        return out;
    }

    public static void importBackup(Path file) throws IOException {
        String json = Files.readString(file, StandardCharsets.UTF_8);
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        if (!root.has("tasks") && !root.has("notes")) {
            throw new IOException("Not a NoteTask backup file");
        }
        Files.writeString(SaveData.saveFilePath(), json, StandardCharsets.UTF_8);
        SaveData.load();
        NoteTaskMod.LOGGER.info("Imported NoteTask backup from {}", file);
    }
}
