package com.notetask.util;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

public final class ClipboardHelper {

    private ClipboardHelper() {}

    public static void copy(String text) {
        if (text == null || text.isEmpty()) return;
        long window = MinecraftClient.getInstance().getWindow().getHandle();
        GLFW.glfwSetClipboardString(window, text);
    }
}
