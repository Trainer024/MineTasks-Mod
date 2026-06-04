package com.notetask.gui;

import com.notetask.config.ModConfig;
import com.notetask.data.DataBackup;
import com.notetask.integration.JEICompat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class NoteTaskSettingsScreen extends Screen {

    private static final int PW = 320;
    private static final int PH = 340;

    private final Screen parent;

    public NoteTaskSettingsScreen(Screen parent) {
        super(Text.literal("NoteTask Settings"));
        this.parent = parent;
    }

    private int pl() { return (this.width - PW) / 2; }
    private int pt() { return (this.height - PH) / 2; }

    @Override
    protected void init() {
        int px = pl(), py = pt();
        ModConfig cfg = ModConfig.get();
        int y = py + 32;
        int row = 22;

        addDrawableChild(toggle(px + 4, y, PW - 8, "Show tasks on screen", cfg.hudEnabled, v -> cfg.hudEnabled = v));
        y += row;
        addDrawableChild(toggle(px + 4, y, PW - 8, "Panel on right", cfg.hudOnRight, v -> cfg.hudOnRight = v));
        y += row;
        addDrawableChild(cycle(px + 4, y, PW - 8, "Panel size", () -> cfg.hudSize = (cfg.hudSize + 1) % 3));
        y += row;
        addDrawableChild(toggle(px + 4, y, PW - 8, "Count storage chests", cfg.countStorage, v -> cfg.countStorage = v));
        y += row;
        addDrawableChild(cycle(px + 4, y, PW - 8, "Storage scan range", () -> cycleRadius(cfg)));
        y += row;
        addDrawableChild(toggle(px + 4, y, PW - 8, "Show storage on HUD", cfg.showStorageInHud, v -> cfg.showStorageInHud = v));
        y += row;
        addDrawableChild(toggle(px + 4, y, PW - 8, "Show % on HUD", cfg.showPercentInHud, v -> cfg.showPercentInHud = v));
        y += row;
        addDrawableChild(toggle(px + 4, y, PW - 8, "Hide completed in HUD", cfg.hideCompletedInHud, v -> cfg.hideCompletedInHud = v));
        y += row;
        addDrawableChild(toggle(px + 4, y, PW - 8, "Hide completed in menu", cfg.hideCompletedInMenu, v -> cfg.hideCompletedInMenu = v));
        y += row;
        addDrawableChild(toggle(px + 4, y, PW - 8, "Completion sound", cfg.playCompletionSound, v -> cfg.playCompletionSound = v));
        y += row;
        addDrawableChild(toggle(px + 4, y, PW - 8, "Progress milestones", cfg.notifyMilestones, v -> cfg.notifyMilestones = v));
        y += row;
        addDrawableChild(toggle(px + 4, y, PW - 8, "HUD item icons", cfg.showHudIcons, v -> cfg.showHudIcons = v));
        y += row;
        addDrawableChild(cycle(px + 4, y, PW - 8, "Quick-add default", () -> cycleQuickTarget(cfg)));
        y += row + 4;

        addDrawableChild(ButtonWidget.builder(Text.literal("Export backup"), b -> {
            try {
                var path = DataBackup.exportBackup();
                notify("Backup saved: " + path.getFileName());
            } catch (Exception e) {
                notify("Export failed: " + e.getMessage());
            }
        }).dimensions(px + 4, y, 100, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Reload data"), b -> {
            com.notetask.data.SaveData.load();
            notify("Reloaded from disk");
        }).dimensions(px + 108, y, 100, 20).build());

        y += 24;
        addDrawableChild(ButtonWidget.builder(Text.literal("Open task menu (N)"), b ->
                        this.client.setScreen(new NoteTaskScreen()))
                .dimensions(px + 4, y, 150, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> {
            ModConfig.save();
            this.client.setScreen(parent);
        }).dimensions(px + PW - 84, py + PH - 26, 80, 20).build());
    }

    private static void cycleRadius(ModConfig cfg) {
        int[] steps = {16, 32, 48, 64, 96};
        int idx = 0;
        for (int i = 0; i < steps.length; i++) {
            if (steps[i] == cfg.storageScanRadius) { idx = i; break; }
        }
        cfg.storageScanRadius = steps[(idx + 1) % steps.length];
    }

    private static String hudSizeLabel(int size) {
        return switch (size) {
            case 0 -> "Small";
            case 2 -> "Large";
            default -> "Medium";
        };
    }

    private ButtonWidget toggle(int x, int y, int w, String label, boolean initial,
                                java.util.function.Consumer<Boolean> onChange) {
        final boolean[] value = {initial};
        return ButtonWidget.builder(Text.literal(label + ": " + (value[0] ? "ON" : "OFF")), b -> {
            value[0] = !value[0];
            onChange.accept(value[0]);
            b.setMessage(Text.literal(label + ": " + (value[0] ? "ON" : "OFF")));
        }).dimensions(x, y, w, 20).build();
    }

    private ButtonWidget cycle(int x, int y, int w, String label, Runnable onCycle) {
        ModConfig cfg = ModConfig.get();
        return ButtonWidget.builder(Text.literal(label + ": " + cycleValue(label, cfg)), b -> {
            onCycle.run();
            b.setMessage(Text.literal(label + ": " + cycleValue(label, cfg)));
        }).dimensions(x, y, w, 20).build();
    }

    private static void cycleQuickTarget(ModConfig cfg) {
        int[] steps = {16, 32, 64, 128, 256};
        int idx = 0;
        for (int i = 0; i < steps.length; i++) {
            if (steps[i] == cfg.quickAddDefaultTarget) { idx = i; break; }
        }
        cfg.quickAddDefaultTarget = steps[(idx + 1) % steps.length];
    }

    private static String cycleValue(String label, ModConfig cfg) {
        if (label.equals("Panel size")) return hudSizeLabel(cfg.hudSize);
        if (label.equals("Storage scan range")) return cfg.storageScanRadius + " blocks";
        if (label.equals("Quick-add default")) return cfg.quickAddDefaultTarget + " items";
        return "";
    }

    private void notify(String msg) {
        MinecraftClient c = this.client;
        if (c != null && c.player != null) {
            c.player.sendMessage(Text.literal("[NoteTask] " + msg), true);
        }
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        int px = pl(), py = pt();
        UiTheme.drawScreenBackdrop(ctx, this.width, this.height);
        UiTheme.drawPanel(ctx, px, py, PW, PH);

        // Updated to use JEICompat for the status line instead of RRV
        String statusLine = JEICompat.LOADED ? "JEI Active" : "No JEI detected";

        UiTheme.drawHeaderBar(ctx, textRenderer, "NoteTask Settings",
                statusLine, px + 1, py + 1, PW - 2);

        ctx.drawText(textRenderer, "Del=delete · Enter=edit · Ctrl+D=duplicate · Quick-add keybind in Controls",
                px + 5, py + 22, UiTheme.TEXT_MUTED, false);
        super.render(ctx, mx, my, delta);
    }

    private void drawBorder(DrawContext ctx, int x, int y, int w, int h, int c) {
        ctx.fill(x, y, x + w, y + 1, c);
        ctx.fill(x, y + h - 1, x + w, y + h, c);
        ctx.fill(x, y, x + 1, y + h, c);
        ctx.fill(x + w - 1, y, x + w, y + h, c);
    }

    @Override
    public void close() {
        ModConfig.save();
        super.close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}