package com.notetask.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.notetask.NoteTaskMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig {

    private static final Path CONFIG_FILE = FabricLoader.getInstance()
            .getConfigDir().resolve("notetask").resolve("client.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static ModConfig instance = new ModConfig();

    public boolean hudEnabled = true;
    public boolean hudOnRight = true;
    public int hudSize = 1;

    public boolean countStorage = true;
    public int storageScanRadius = 48;

    public boolean hideCompletedInMenu = false;
    public boolean hideCompletedInHud = true;
    public boolean showStorageInHud = true;
    public boolean showPercentInHud = true;
    public boolean playCompletionSound = true;
    public boolean notifyMilestones = true;
    public boolean showHudIcons = true;
    public boolean hudCollapsed = false;

    public int quickAddDefaultTarget = 64;
    public int taskSortMode = 0; // 0 manual, 1 name, 2 progress

    @SuppressWarnings("unused")
    private Boolean hudShowInGame;

    public static ModConfig get() {
        return instance;
    }

    public int panelWidth() {
        return switch (clampHudSize(hudSize)) {
            case 0 -> 150;
            case 2 -> 260;
            default -> 200;
        };
    }

    public int rowHeight() {
        return switch (clampHudSize(hudSize)) {
            case 0 -> 20;
            case 2 -> 34;
            default -> 26;
        };
    }

    public int maxTasksShown() {
        return switch (clampHudSize(hudSize)) {
            case 0 -> 6;
            case 2 -> 14;
            default -> 10;
        };
    }

    public int barHeight() {
        return clampHudSize(hudSize) == 2 ? 5 : 4;
    }

    private static int clampHudSize(int size) {
        return Math.max(0, Math.min(2, size));
    }

    public static void load() {
        if (!Files.exists(CONFIG_FILE)) {
            instance = new ModConfig();
            save();
            return;
        }
        try (Reader r = Files.newBufferedReader(CONFIG_FILE, StandardCharsets.UTF_8)) {
            ModConfig loaded = GSON.fromJson(r, ModConfig.class);
            instance = loaded != null ? loaded : new ModConfig();
            instance.storageScanRadius = Math.max(16, Math.min(96, instance.storageScanRadius));
            instance.hudSize = clampHudSize(instance.hudSize);
        } catch (IOException e) {
            NoteTaskMod.LOGGER.error("Failed to load NoteTask config", e);
            instance = new ModConfig();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            try (Writer w = Files.newBufferedWriter(CONFIG_FILE, StandardCharsets.UTF_8)) {
                GSON.toJson(instance, w);
            }
        } catch (IOException e) {
            NoteTaskMod.LOGGER.error("Failed to save NoteTask config", e);
        }
    }
}
