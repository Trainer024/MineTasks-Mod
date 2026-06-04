package com.notetask.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.*;

public final class ItemSearch {

    private static final int MAX_RESULTS = 48;
    // Capture the visibility interface directly instead of the whole runtime
    private static mezz.jei.api.runtime.IIngredientVisibility ingredientVisibility;

    private ItemSearch() {}

    public static List<ItemMatch> search(String rawQuery, int limit) {
        String query = rawQuery == null ? "" : rawQuery.trim();
        if (query.isEmpty()) {
            return List.of();
        }
        return fromRegistry(query, limit);
    }

    /**
     * Supplies JEI's visibility manager to verify item visibility dynamically.
     */
    public static void setIngredientVisibility(mezz.jei.api.runtime.IIngredientVisibility visibility) {
        ingredientVisibility = visibility;
    }

    public static Optional<ItemMatch> bestMatch(String rawQuery) {
        List<ItemMatch> matches = search(rawQuery, 12);
        if (matches.isEmpty()) return Optional.empty();
        return Optional.of(matches.getFirst());
    }

    public static Optional<Identifier> resolveId(String rawQuery) {
        return bestMatch(rawQuery).map(ItemMatch::id);
    }

    private static List<ItemMatch> fromRegistry(String query, int limit) {
        String q = query.toLowerCase(Locale.ROOT);
        String normalized = q.replace(' ', '_');

        Map<Identifier, ItemMatch> scored = new LinkedHashMap<>();
        int invBoost = inventoryBoostSet();

        try {
            Identifier direct = q.contains(":")
                    ? Identifier.of(q)
                    : Identifier.of("minecraft", normalized);
            if (Registries.ITEM.containsId(direct) && isVisible(Registries.ITEM.get(direct))) {
                put(scored, direct, 1000 + invBoost, "exact id");
            }
        } catch (Exception ignored) {
        }

        TagKey<Item> hidden = TagKey.of(Registries.ITEM.getKey(), Identifier.of("c", "hidden_from_recipe_viewers"));

        for (Item item : Registries.ITEM) {
            if (!isVisible(item)) continue;
            Identifier id = Registries.ITEM.getId(item);
            if (scored.containsKey(id)) continue;

            ItemStack stack = new ItemStack(item);
            if (stack.isIn(hidden)) continue;

            String path = id.getPath().toLowerCase(Locale.ROOT);
            String full = id.toString().toLowerCase(Locale.ROOT);
            String name = item.getName(stack).getString().toLowerCase(Locale.ROOT);
            String namespace = id.getNamespace().toLowerCase(Locale.ROOT);

            int score = scoreMatch(q, normalized, path, full, name, namespace);
            if (score > 0) {
                if (inventoryContains(item)) score += invBoost;
                put(scored, id, score, full);
            }
        }

        return scored.values().stream()
                .sorted(Comparator.comparingInt(ItemMatch::score).reversed())
                .limit(Math.min(limit, MAX_RESULTS))
                .toList();
    }

    private static int scoreMatch(String q, String normalized, String path, String full, String name, String namespace) {
        if (full.equals(q) || path.equals(normalized)) return 900;
        if (full.equals(normalized) || path.equals(q)) return 850;
        if (name.equals(q)) return 800;
        if (path.startsWith(normalized) || full.startsWith(q)) return 600;
        if (name.startsWith(q)) return 550;
        if (path.contains(normalized) || full.contains(q)) return 400;
        if (name.contains(q)) return 350;
        if (namespace.contains(q) && q.length() >= 3) return 200;
        if (q.startsWith(":") && (path.contains(q.substring(1)) || full.contains(q.substring(1)))) return 500;
        if (q.startsWith("@") && name.contains(q.substring(1))) return 450;
        return 0;
    }

    private static void put(Map<Identifier, ItemMatch> map, Identifier id, int score, String detail) {
        Item item = Registries.ITEM.get(id);
        ItemStack stack = new ItemStack(item);
        map.put(id, new ItemMatch(id, stack.getName().getString(), stack, score, detail));
    }

    public static boolean isVisible(Item item) {
        if (item == null) return false;
        ItemStack stack = new ItemStack(item);
        if (stack.isEmpty()) return false;

        // Use the captured visibility manager
        if (ingredientVisibility != null) {
            return ingredientVisibility.isIngredientVisible(
                    mezz.jei.api.constants.VanillaTypes.ITEM_STACK, stack
            );
        }
        return true;
    }

    private static int inventoryBoostSet() {
        return 40;
    }

    private static boolean inventoryContains(Item item) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return false;
        for (int i = 0; i < client.player.getInventory().size(); i++) {
            if (client.player.getInventory().getStack(i).isOf(item)) {
                return true;
            }
        }
        return false;
    }
}