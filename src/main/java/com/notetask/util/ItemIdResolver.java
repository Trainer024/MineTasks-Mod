package com.notetask.util;

import net.minecraft.util.Identifier;

import java.util.List;
import java.util.stream.Collectors;

public final class ItemIdResolver {

    private ItemIdResolver() {}

    public static Identifier resolve(String rawInput) {
        String input = rawInput == null ? "" : rawInput.trim();
        if (input.isEmpty()) {
            throw new IllegalArgumentException("Item ID cannot be empty!");
        }

        return ItemSearch.resolveId(input)
                .orElseThrow(() -> new IllegalArgumentException(buildErrorMessage(input)));
    }

    public static List<ItemMatch> suggest(String rawInput, int limit) {
        return ItemSearch.search(rawInput, limit);
    }

    private static String buildErrorMessage(String input) {
        List<ItemMatch> near = ItemSearch.search(input, 5);
        if (near.isEmpty()) {
            return "Item not found. Try Browse (RRV) or hold the item and click 'Use held'.";
        }
        if (near.size() == 1) {
            return "Did you mean " + near.getFirst().id() + "?";
        }
        String list = near.stream()
                .limit(3)
                .map(m -> m.id().toString())
                .collect(Collectors.joining(", "));
        return "Multiple matches — pick from Browse: " + list;
    }
}
