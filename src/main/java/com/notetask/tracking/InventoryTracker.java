package com.notetask.tracking;

import com.notetask.config.ModConfig;
import com.notetask.data.ItemTask;
import com.notetask.data.SaveData;
import com.notetask.data.Task;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class InventoryTracker {

    private static int tickCounter = 0;
    private static final Map<String, Integer> lastMilestone = new HashMap<>();

    public static void tick(MinecraftClient client) {
        if (client.player == null) return;

        tickCounter++;
        boolean scanStorage = tickCounter % 10 == 0;

        boolean changed = false;
        ModConfig cfg = ModConfig.get();

        for (Task task : SaveData.getTasks()) {
            if (!(task instanceof ItemTask it)) continue;

            Identifier id;
            try {
                id = Identifier.of(it.itemId);
            } catch (Exception e) {
                continue;
            }

            if (!Registries.ITEM.containsId(id)) continue;
            Item item = Registries.ITEM.get(id);

            boolean useStorage = it.countStorage && cfg.countStorage;
            boolean doScan = scanStorage || !useStorage;

            if (doScan) {
                ItemCountBreakdown breakdown = ItemCounter.countBreakdown(
                        client, item, useStorage, cfg.storageScanRadius);
                it.lastInvCount = breakdown.inventory();
                it.lastStorageCount = breakdown.storage();

                if (!it.completed) {
                    int total = breakdown.total();
                    int prev = it.current;
                    it.current = Math.min(it.target, Math.max(it.current, total));
                    if (it.current != prev) {
                        changed = true;
                        maybeMilestone(client, it);
                    }

                    if (total >= it.target) {
                        it.current = it.target;
                        it.completed = true;
                        changed = true;
                        notifyComplete(client, it.name);
                    }
                }
            }
        }

        if (changed) {
            SaveData.save();
        }
    }

    private static void maybeMilestone(MinecraftClient client, ItemTask it) {
        if (!ModConfig.get().notifyMilestones || it.target <= 0) return;
        int pct = (int) (100f * it.current / it.target);
        int milestone = pct >= 75 ? 75 : pct >= 50 ? 50 : pct >= 25 ? 25 : 0;
        if (milestone == 0) return;
        Integer prev = lastMilestone.get(it.id);
        if (prev != null && prev >= milestone) return;
        lastMilestone.put(it.id, milestone);
        client.player.sendMessage(
                Text.literal("[NoteTask] " + it.name + ": " + milestone + "%")
                        .formatted(Formatting.AQUA),
                true
        );
    }

    private static void notifyComplete(MinecraftClient client, String name) {
        client.player.sendMessage(
                Text.literal("[NoteTask] Task complete: " + name + "!")
                        .formatted(Formatting.GREEN),
                false
        );
        if (ModConfig.get().playCompletionSound && client.player != null) {
            client.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.2f);
        }
    }
}
