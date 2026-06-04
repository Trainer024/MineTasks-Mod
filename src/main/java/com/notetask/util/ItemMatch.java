package com.notetask.util;

import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public record ItemMatch(
        Identifier id,
        String displayName,
        ItemStack stack,
        int score,
        String detail
) {
    public String idString() {
        return id.toString();
    }
}
