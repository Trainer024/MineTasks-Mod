package com.notetask;

import com.notetask.config.ModConfig;
import com.notetask.data.SaveData;
import com.notetask.gui.NoteTaskScreen;
import com.notetask.gui.WelcomeScreen;
import com.notetask.hud.TaskHud;
import com.notetask.tracking.InventoryTracker;
import com.notetask.util.QuickTasks;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class NoteTaskClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Register the N keybind
        KeyBindings.register();

        // Load saved data when the client starts
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            ModConfig.load();
            SaveData.load();
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> SaveData.save());

        // Each game tick: check for keybind press + update item task progress
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            if (KeyBindings.openNoteTaskKey.wasPressed()) {
                // Show the welcome guide on first install; open normally on all subsequent runs
                client.setScreen(WelcomeScreen.shouldShow()
                        ? new WelcomeScreen(new NoteTaskScreen())
                        : new NoteTaskScreen());
            }
            if (KeyBindings.quickAddTaskKey.wasPressed()) {
                QuickTasks.addFromHeldItem(client);
            }
            if (KeyBindings.toggleHudKey.wasPressed()) {
                ModConfig cfg = ModConfig.get();
                cfg.hudCollapsed = !cfg.hudCollapsed;
                ModConfig.save();
            }

            InventoryTracker.tick(client);
        });

        // Register the HUD overlay
        TaskHud.register();
    }
}