package com.notetask.util;

import com.notetask.config.ModConfig;
import com.notetask.data.ItemTask;
import com.notetask.data.SaveData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class QuickTasks {

    private QuickTasks() {}

    /** Creates an item task from the player's main-hand stack. */
    public static boolean addFromHeldItem(MinecraftClient client) {
        if (client.player == null) return false;
        ItemStack held = client.player.getMainHandStack();
        if (held.isEmpty()) {
            client.player.sendMessage(
                    Text.literal("[NoteTask] Hold an item to quick-add a task.").formatted(Formatting.YELLOW),
                    true);
            return false;
        }
        Item item = held.getItem();
        String id = Registries.ITEM.getId(item).toString();
        String name = held.getName().getString();
        int target = ModConfig.get().quickAddDefaultTarget;
        ItemTask task = new ItemTask(name, id, target, ModConfig.get().countStorage);
        SaveData.getTasks().add(0, task);
        SaveData.save();
        client.player.sendMessage(
                Text.literal("[NoteTask] Added: ").formatted(Formatting.GRAY)
                        .append(Text.literal(name + " (" + target + ")").formatted(Formatting.AQUA)),
                true);
        return true;
    }
}
