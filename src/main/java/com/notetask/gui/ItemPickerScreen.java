package com.notetask.gui;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ItemPickerScreen extends Screen {

    private static final int PW = 380, PH = 282, CELL = 20, GRID_TOP = 48, FOOT_PAD = 26;
    private static final int COLS = (PW - 10) / CELL;
    private static volatile List<Item> ALL_ITEMS;

    private static void ensureItems() {
        if (ALL_ITEMS != null) return;
        List<Item> list = new ArrayList<>();
        for (Item item : Registries.ITEM) {
            if (item != Items.AIR) list.add(item);
        }
        list.sort(Comparator.comparing(i -> i.getName().getString().toLowerCase()));
        ALL_ITEMS = list;
    }

    private final Screen parent;
    private final Consumer<String> onPick;
    private TextFieldWidget searchBox;
    private List<Item> filtered;
    private int scroll = 0, selected = -1, hovered = -1;

    public ItemPickerScreen(Screen parent, Consumer<String> onPick) {
        super(Text.literal("Browse Items"));
        this.parent = parent;
        this.onPick = onPick;
        ensureItems();
        this.filtered = new ArrayList<>(ALL_ITEMS);
    }

    private int pl() { return (this.width - PW) / 2; }
    private int pt() { return (this.height - PH) / 2; }
    private int gridH() { return PH - GRID_TOP - FOOT_PAD; }

    @Override
    protected void init() {
        int px = pl(), py = pt();
        searchBox = new TextFieldWidget(textRenderer, px + 4, py + 24, PW - 8, 18, Text.empty());
        searchBox.setChangedListener(q -> { rebuildFilter(); scroll = 0; selected = -1; });
        addDrawableChild(searchBox);
        setInitialFocus(searchBox);

        int fy = py + PH - 22;
        addDrawableChild(ButtonWidget.builder(Text.literal("Use Selected"), b -> confirm()).dimensions(px + PW - 110, fy, 106, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), b -> close()).dimensions(px + PW - 220, fy, 106, 18).build());
    }

    private void rebuildFilter() {
        String q = searchBox.getText().trim().toLowerCase();
        filtered = ALL_ITEMS.stream().filter(item -> item.getName().getString().toLowerCase().contains(q) || Registries.ITEM.getId(item).toString().contains(q)).collect(Collectors.toList());
    }

    private void confirm() {
        if (selected >= 0 && selected < filtered.size()) {
            String id = Registries.ITEM.getId(filtered.get(selected)).toString();
            // setScreen(parent) runs first — this calls init() on the TaskEditorScreen,
            // creating a fresh itemIdField. onPick then writes into that new widget.
            this.client.setScreen(parent);
            onPick.accept(id);
        }
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, this.width, this.height, 0x99000000);
        int px = pl(), py = pt();
        ctx.fill(px, py, px + PW, py + PH, 0xF00D0D1A);
        drawBorder(ctx, px, py, PW, PH, 0xFF5555AA);

        // --- Grid Rendering ---
        int gx = px + 5, gy = py + GRID_TOP, gh = gridH();
        ctx.enableScissor(gx, gy, gx + PW - 10, gy + gh);
        int startRow = scroll / CELL;
        for (int i = 0; i < filtered.size(); i++) {
            int row = i / COLS, col = i % COLS;
            int ix = gx + col * CELL, iy = gy + row * CELL - scroll;
            if (iy + CELL >= gy && iy < gy + gh) {
                if (i == selected) ctx.fill(ix, iy, ix + CELL, iy + CELL, 0xAA2244AA);
                ctx.drawItem(new ItemStack(filtered.get(i)), ix + 2, iy + 2);
            }
        }
        ctx.disableScissor();
        super.render(ctx, mx, my, delta);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) return true;
        int gx = pl() + 5, gy = pt() + GRID_TOP;
        int col = (int)((click.x() - gx) / CELL);
        int row = (int)((click.y() - gy + scroll) / CELL);
        int idx = row * COLS + col;
        if (idx >= 0 && idx < filtered.size()) {
            selected = idx;
            if (doubled) confirm();
            return true;
        }
        return false;
    }

    @Override
    public void close() { this.client.setScreen(parent); }

    private void drawBorder(DrawContext ctx, int x, int y, int w, int h, int c) {
        ctx.fill(x, y, x + w, y + 1, c); ctx.fill(x, y + h - 1, x + w, y + h, c);
        ctx.fill(x, y, x + 1, y + h, c); ctx.fill(x + w - 1, y, x + w, y + h, c);
    }
}