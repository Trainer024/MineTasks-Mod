package com.notetask.hud;

import com.notetask.config.ModConfig;
import com.notetask.data.ItemTask;
import com.notetask.data.SaveData;
import com.notetask.data.Task;
import com.notetask.gui.UiTheme;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.Comparator;
import java.util.List;

public class TaskHud {

    private static final int MARGIN = 8;
    private static final int HEADER_H = 18;
    private static final int ICON = 14;

    public static void register() {
        HudRenderCallback.EVENT.register((ctx, tickCounter) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (!shouldRender(client)) return;
            renderSidePanel(ctx, client);
        });
    }

    static boolean shouldRender(MinecraftClient client) {
        if (!ModConfig.get().hudEnabled) return false;
        if (ModConfig.get().hudCollapsed) return false;
        if (client.player == null) return false;
        if (client.currentScreen != null) return false;
        if (client.options.hudHidden) return false;
        return true;
    }

    static void renderSidePanel(DrawContext ctx, MinecraftClient client) {
        ModConfig cfg = ModConfig.get();
        int panelW = cfg.panelWidth();
        int rowH = cfg.rowHeight();
        int barH = cfg.barHeight();
        int maxTasks = cfg.maxTasksShown();
        boolean icons = cfg.showHudIcons;

        List<Task> active = SaveData.getTasks().stream()
                .filter(t -> !t.completed || !cfg.hideCompletedInHud)
                .sorted(Comparator.comparing((Task t) -> !t.pinned).thenComparing(t -> t.name))
                .limit(maxTasks)
                .toList();

        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();

        int contentRows = Math.max(1, active.size());
        int panelH = HEADER_H + contentRows * rowH + 10;

        int x = cfg.hudOnRight ? screenW - panelW - MARGIN : MARGIN;
        int y = Math.max(MARGIN, screenH / 2 - panelH / 2);

        ctx.fill(x, y, x + panelW, y + panelH, 0xE0101018);
        UiTheme.drawBorder(ctx, x, y, panelW, panelH, UiTheme.BORDER);

        int activeCount = (int) SaveData.getTasks().stream().filter(t -> !t.completed).count();
        ctx.drawTextWithShadow(client.textRenderer, "Tasks (" + activeCount + ")", x + 6, y + 5, UiTheme.TEXT_TITLE);

        int rowY = y + HEADER_H + 2;
        if (active.isEmpty()) {
            ctx.drawTextWithShadow(client.textRenderer, "No active tasks", x + 6, rowY + 2, UiTheme.TEXT_MUTED);
            ctx.drawTextWithShadow(client.textRenderer, "Press N to open", x + 6, rowY + 12, UiTheme.TEXT_MUTED);
            return;
        }

        int barW = panelW - 12 - (icons ? ICON + 2 : 0);
        int textX = x + 6 + (icons ? ICON + 2 : 0);

        for (Task task : active) {
            if (icons && task instanceof ItemTask it) {
                ItemStack stack = stackFor(it.itemId);
                if (!stack.isEmpty()) {
                    ctx.drawItem(stack, x + 6, rowY + 2);
                }
            }

            String pin = task.pinned ? "★ " : "";
            String name = pin + task.name;
            String progress = task.getProgressText();
            if (cfg.showPercentInHud && task instanceof ItemTask it && it.target > 0) {
                int pct = (int) (100f * it.getProgressFraction());
                progress = progress + " (" + pct + "%)";
            }

            int maxNameW = barW - client.textRenderer.getWidth("  " + progress);
            if (client.textRenderer.getWidth(name) > maxNameW) {
                name = client.textRenderer.trimToWidth(name,
                        Math.max(0, maxNameW - client.textRenderer.getWidth("…"))) + "…";
            }

            ctx.drawTextWithShadow(client.textRenderer, name, textX, rowY, 0xFFE8E8FF);
            int progX = x + panelW - 6 - client.textRenderer.getWidth(progress);
            ctx.drawTextWithShadow(client.textRenderer, progress, progX, rowY, 0xFF99BBDD);

            if (cfg.showStorageInHud && task instanceof ItemTask it && it.countStorage) {
                ctx.drawTextWithShadow(client.textRenderer, it.getStorageHint(), textX, rowY + 10, 0xFF778899);
            }

            int barY = rowY + (task instanceof ItemTask ? 18 : 12);
            UiTheme.drawProgressBar(ctx, textX, barY, barW, barH, task.getProgressFraction(), task.completed);

            rowY += rowH;
        }
    }

    private static ItemStack stackFor(String itemId) {
        try {
            Identifier id = Identifier.of(itemId);
            if (Registries.ITEM.containsId(id)) {
                Item item = Registries.ITEM.get(id);
                return new ItemStack(item);
            }
        } catch (Exception ignored) {
        }
        return ItemStack.EMPTY;
    }
}
