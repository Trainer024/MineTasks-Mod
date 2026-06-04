package com.notetask.integration;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.IFocusFactory;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IJeiRuntime;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.ItemStack;

public final class JEICompat {

    // Safely checks if JEI is actually loaded in the current instance
    public static final boolean LOADED = FabricLoader.getInstance().isModLoaded("jei");
    private static IJeiRuntime runtime;

    private JEICompat() {}

    public static void setRuntime(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    public static boolean isReady() {
        return LOADED && runtime != null;
    }

    public static void showRecipes(ItemStack stack) {
        if (isReady() && stack != null && !stack.isEmpty()) {
            IFocusFactory focusFactory = runtime.getJeiHelpers().getFocusFactory();
            runtime.getRecipesGui().show(focusFactory.createFocus(RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, stack));
        }
    }

    public static void showUses(ItemStack stack) {
        if (isReady() && stack != null && !stack.isEmpty()) {
            IFocusFactory focusFactory = runtime.getJeiHelpers().getFocusFactory();
            runtime.getRecipesGui().show(focusFactory.createFocus(RecipeIngredientRole.INPUT, VanillaTypes.ITEM_STACK, stack));
        }
    }
}