package com.notetask.util;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/** Fast default list for the browse screen (avoids loading the entire registry at once if JEI is available). */
public final class ItemBrowseCache {

    private static List<ItemMatch> cached;
    private static Collection<ItemStack> jeiIngredients;

    private ItemBrowseCache() {}

    public static List<ItemMatch> defaultBrowse(int limit) {
        if (cached == null) {
            cached = build();
        }
        return cached.subList(0, Math.min(limit, cached.size()));
    }

    /**
     * Supplies JEI's active ingredient list to this cache.
     * Automatically invalidates any existing cache to refresh the view.
     */
    public static void setJeiIngredients(Collection<ItemStack> ingredients) {
        jeiIngredients = ingredients;
        invalidate();
    }

    private static List<ItemMatch> build() {
        List<ItemMatch> out = new ArrayList<>();

        // 1. Try loading from JEI if data has been supplied
        if (jeiIngredients != null && !jeiIngredients.isEmpty()) {
            int added = 0;
            for (ItemStack stack : jeiIngredients) {
                if (stack == null || stack.isEmpty()) continue;
                Identifier id = Registries.ITEM.getId(stack.getItem());
                out.add(new ItemMatch(id, stack.getName().getString(), stack.copyWithCount(1), 0, id.toString()));
                if (++added >= 400) break; // Preserving your original 400-item safety cap
            }
            if (!out.isEmpty()) {
                out.sort(Comparator.comparing(m -> m.displayName().toLowerCase()));
                return out;
            }
        }

        // 2. Fallback to default registry scanning if JEI isn't active or ready yet
        for (Item item : Registries.ITEM) {
            if (!ItemSearch.isVisible(item)) continue;
            ItemStack stack = new ItemStack(item);
            Identifier id = Registries.ITEM.getId(item);
            out.add(new ItemMatch(id, stack.getName().getString(), stack, 0, id.toString()));
        }
        out.sort(Comparator.comparing(m -> m.displayName().toLowerCase()));
        return out;
    }

    public static void invalidate() {
        cached = null;
    }
}