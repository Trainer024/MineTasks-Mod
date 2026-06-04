package com.notetask;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {
    public static KeyBinding openNoteTaskKey;
    public static KeyBinding quickAddTaskKey;
    public static KeyBinding toggleHudKey;

    public static void register() {
        var category = KeyBinding.Category.create(Identifier.of("notetask", "notetask"));
        openNoteTaskKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.notetask.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_N,
                category
        ));
        quickAddTaskKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.notetask.quick_add",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                category
        ));
        toggleHudKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.notetask.toggle_hud",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                category
        ));
    }
}