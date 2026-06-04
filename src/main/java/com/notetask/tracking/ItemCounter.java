package com.notetask.tracking;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.HashSet;
import java.util.Set;

public final class ItemCounter {

    private ItemCounter() {}

    public static ItemCountBreakdown countBreakdown(MinecraftClient client, Item item,
                                                    boolean includeStorage, int storageRadius) {
        if (client.player == null) {
            return new ItemCountBreakdown(0, 0, 0);
        }

        int inv = countInventory(client.player.getInventory(), item);
        int storage = 0;
        if (includeStorage && client.world != null) {
            storage += countNearbyStorages(client.world, client.player, item, storageRadius);
            storage += countInventory(client.player.getEnderChestInventory(), item);
        }
        return new ItemCountBreakdown(inv, storage, inv + storage);
    }

    public static int countForPlayer(MinecraftClient client, Item item, boolean includeStorage, int storageRadius) {
        return countBreakdown(client, item, includeStorage, storageRadius).total();
    }

    private static int countNearbyStorages(ClientWorld world, PlayerEntity player, Item item, int radius) {
        BlockPos center = player.getBlockPos();
        int r2 = radius * radius;
        int chunkRadius = (radius >> 4) + 1;
        int pcx = center.getX() >> 4;
        int pcz = center.getZ() >> 4;

        int total = 0;
        Set<Inventory> seen = new HashSet<>();

        for (int cx = pcx - chunkRadius; cx <= pcx + chunkRadius; cx++) {
            for (int cz = pcz - chunkRadius; cz <= pcz + chunkRadius; cz++) {
                if (!world.isChunkLoaded(cx, cz)) continue;

                WorldChunk chunk = world.getChunk(cx, cz);
                for (var entry : chunk.getBlockEntities().entrySet()) {
                    BlockPos pos = entry.getKey();
                    if (pos.getSquaredDistance(center) > r2) continue;

                    if (entry.getValue() instanceof Inventory inv) {
                        total += countInventoryOnce(inv, item, seen);
                    }
                }
            }
        }

        return total;
    }

    private static int countInventoryOnce(Inventory inv, Item item, Set<Inventory> seen) {
        if (!seen.add(inv)) return 0;

        int count = 0;
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (!stack.isEmpty() && stack.isOf(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static int countInventory(Inventory inv, Item item) {
        int count = 0;
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (!stack.isEmpty() && stack.isOf(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }
}
