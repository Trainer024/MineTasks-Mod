package com.notetask.integration;

import com.notetask.util.ItemBrowseCache;
import com.notetask.util.ItemSearch;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.Collection;

@JeiPlugin
public class NoteTaskJeiPlugin implements IModPlugin {

    private static final Identifier PLUGIN_ID = Identifier.of("notetask", "jei_plugin");

    @Override
    public Identifier getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // 1. Capture the visibility helper during the setup phase to fix the IJeiRuntime method error
        ItemSearch.setIngredientVisibility(registration.getJeiHelpers().getIngredientVisibility());
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        // 2. Grab JEI's normalized/filtered item stacks and hand them off to your browser cache
        Collection<ItemStack> ingredients = jeiRuntime.getIngredientManager().getAllIngredients(VanillaTypes.ITEM_STACK);
        ItemBrowseCache.setJeiIngredients(ingredients);

        // 3. Feed the active runtime to the GUI compatibility layer to enable the TaskEditorScreen buttons
        JEICompat.setRuntime(jeiRuntime);
    }
}