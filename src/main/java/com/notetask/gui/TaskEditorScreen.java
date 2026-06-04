package com.notetask.gui;

import com.notetask.config.ModConfig;
import com.notetask.data.ItemTask;
import com.notetask.data.ManualTask;
import com.notetask.data.SaveData;
import com.notetask.data.Task;
import com.notetask.integration.JEICompat;
import com.notetask.util.ItemIdResolver;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class TaskEditorScreen extends Screen {

    private static final int PW = 300, PH = 240;

    private final Screen parent;
    private final Task   editTarget;      // null → creating a new task

    /** Item ID to pre-fill, e.g. passed from ItemPickerScreen. May be null. */
    private final String preFillItemId;

    private int     taskType     = 0;     // 0 = item, 1 = manual
    private boolean countStorage;
    private String  errorMsg     = "";
    private String  resolvedName = "";    // live preview of the validated item name

    private TextFieldWidget nameField;
    private TextFieldWidget itemIdField;
    private TextFieldWidget targetField;
    private TextFieldWidget progressField;

    private ButtonWidget countStorageBtn;
    private ButtonWidget typeItemBtn;
    private ButtonWidget typeManualBtn;
    private ButtonWidget browseBtn;       // opens ItemPickerScreen
    private ButtonWidget jeiRecipesBtn;   // "Recipes in JEI" — only when JEI is ready
    private ButtonWidget jeiUsesBtn;      // "Uses in JEI"    — only when JEI is ready

    /* ─── Constructors ───────────────────────────────────────────────────── */

    /** Standard: edit existing task, or add new one (editTarget == null). */
    public TaskEditorScreen(Screen parent, Task editTarget) {
        this(parent, editTarget, null);
    }

    /**
     * Used by {@link ItemPickerScreen} callback to pre-fill the item ID field.
     *
     * @param preFillItemId Namespaced ID string, e.g. {@code "minecraft:oak_planks"}.
     */
    public TaskEditorScreen(Screen parent, Task editTarget, String preFillItemId) {
        super(Text.literal(editTarget == null ? "Add task" : "Edit task"));
        this.parent        = parent;
        this.editTarget    = editTarget;
        this.preFillItemId = preFillItemId;
    }

    private int pl() { return (this.width  - PW) / 2; }
    private int pt() { return (this.height - PH) / 2; }

    /* ─── Init ───────────────────────────────────────────────────────────── */

    @Override
    protected void init() {
        int px = pl(), py = pt();
        boolean editing = editTarget != null;

        // Task name
        nameField = new TextFieldWidget(textRenderer, px + 4, py + 34, PW - 8, 20, Text.empty());
        nameField.setMaxLength(60);
        nameField.setPlaceholder(Text.literal("Task name…"));
        if (editing) nameField.setText(editTarget.name);
        addDrawableChild(nameField);

        // Type buttons (locked when editing)
        typeItemBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Item task"), b -> {
            if (!editing) { taskType = 0; updateVisibility(); }
        }).dimensions(px + 4, py + 68, 100, 18).build());

        typeManualBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Manual task"), b -> {
            if (!editing) { taskType = 1; updateVisibility(); }
        }).dimensions(px + 108, py + 68, 110, 18).build());

        // Item ID field (narrowed to leave room for Browse button)
        itemIdField = new TextFieldWidget(textRenderer, px + 4, py + 106, PW - 74, 20, Text.empty());
        itemIdField.setMaxLength(120);
        itemIdField.setPlaceholder(Text.literal("e.g. minecraft:oak_planks"));
        itemIdField.setChangedListener(s -> refreshResolvedName());
        addDrawableChild(itemIdField);

        // Browse button — opens ItemPickerScreen, which calls setScreen(parent) first,
        // then invokes onPick so itemIdField already refers to the re-initialised widget.
        browseBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Browse…"), b -> {
            this.client.setScreen(new ItemPickerScreen(this, (String id) -> {
                this.itemIdField.setText(id);
                this.refreshResolvedName();
            }));
        }).dimensions(px + PW - 68, py + 106, 64, 20).build());

        // Goal / progress fields
        targetField = new TextFieldWidget(textRenderer, px + 4, py + 138, 70, 20, Text.empty());
        targetField.setMaxLength(7);
        targetField.setPlaceholder(Text.literal("Goal"));
        addDrawableChild(targetField);

        progressField = new TextFieldWidget(textRenderer, px + 80, py + 138, 70, 20, Text.empty());
        progressField.setMaxLength(7);
        progressField.setPlaceholder(Text.literal("Progress"));
        addDrawableChild(progressField);

        // Count-storage toggle
        countStorage = ModConfig.get().countStorage;
        countStorageBtn = toggle(px + 4, py + 162, PW - 8,
                "Count chests & ender chest", countStorage, v -> countStorage = v);
        addDrawableChild(countStorageBtn);

        // "Use held item" shortcut
        addDrawableChild(ButtonWidget.builder(Text.literal("Use held item"), b -> {
            MinecraftClient mc = this.client;
            if (mc == null || mc.player == null) { errorMsg = "Player not available."; return; }
            Item held = mc.player.getMainHandStack().getItem();
            itemIdField.setText(Registries.ITEM.getId(held).toString());
            refreshResolvedName();
            errorMsg = "";
        }).dimensions(px + 4, py + 186, 110, 20).build());

        // ── JEI buttons ──────────────────────────────────────────────────
        // Only constructed when JEI is on the classpath; start hidden and
        // appear only after a valid item ID has been entered.
        if (JEICompat.LOADED) {
            jeiRecipesBtn = addDrawableChild(ButtonWidget.builder(
                            Text.literal("Recipes in JEI"), b -> openInJEI(false))
                    .dimensions(px + 120, py + 186, 90, 20).build());
            jeiRecipesBtn.visible = false;

            jeiUsesBtn = addDrawableChild(ButtonWidget.builder(
                            Text.literal("Uses in JEI"), b -> openInJEI(true))
                    .dimensions(px + 216, py + 186, 80, 20).build());
            jeiUsesBtn.visible = false;
        }

        // Save / Cancel
        addDrawableChild(ButtonWidget.builder(
                        Text.literal(editing ? "Save" : "Add"), b -> trySave())
                .dimensions(px + 4, py + PH - 26, 80, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"),
                        b -> this.client.setScreen(parent))
                .dimensions(px + 90, py + PH - 26, 72, 20).build());

        // Populate from existing task or pre-fill string
        if (editing && editTarget instanceof ItemTask it) {
            taskType = 0;
            itemIdField.setText(it.itemId);
            targetField.setText(String.valueOf(it.target));
            progressField.setText(String.valueOf(it.current));
            countStorage = it.countStorage;
            refreshResolvedName();
        } else if (editing && editTarget instanceof ManualTask) {
            taskType = 1;
        } else if (preFillItemId != null && !preFillItemId.isEmpty()) {
            itemIdField.setText(preFillItemId);
            refreshResolvedName();
        }

        typeItemBtn.active   = !editing;
        typeManualBtn.active = !editing;
        updateVisibility();
    }

    /* ─── JEI ────────────────────────────────────────────────────────────── */

    private void openInJEI(boolean showUses) {
        String raw = itemIdField.getText().trim();
        if (raw.isEmpty()) return;
        try {
            Identifier id = ItemIdResolver.resolve(raw);
            if (!Registries.ITEM.containsId(id)) return;
            ItemStack stack = new ItemStack(Registries.ITEM.get(id));
            if (showUses) JEICompat.showUses(stack);
            else          JEICompat.showRecipes(stack);
        } catch (IllegalArgumentException ignored) {}
    }

    /* ─── Live item name preview ─────────────────────────────────────────── */

    private void refreshResolvedName() {
        String raw = itemIdField != null ? itemIdField.getText().trim() : "";
        boolean valid = false;
        if (raw.isEmpty()) {
            resolvedName = "";
        } else {
            try {
                Identifier id = ItemIdResolver.resolve(raw);
                if (Registries.ITEM.containsId(id)) {
                    resolvedName = "✔  " + Registries.ITEM.get(id).getName().getString();
                    valid = true;
                } else {
                    resolvedName = "Item not found";
                }
            } catch (IllegalArgumentException e) {
                resolvedName = e.getMessage();
            }
        }
        // Show JEI buttons only once a valid item is confirmed
        if (jeiRecipesBtn != null) jeiRecipesBtn.visible = valid && JEICompat.isReady();
        if (jeiUsesBtn    != null) jeiUsesBtn.visible    = valid && JEICompat.isReady();
    }

    /* ─── Visibility ─────────────────────────────────────────────────────── */

    private void updateVisibility() {
        boolean item    = taskType == 0;
        boolean editing = editTarget != null;
        itemIdField.visible      = item;
        browseBtn.visible        = item;
        targetField.visible      = item;
        progressField.visible    = item && editing;
        if (countStorageBtn != null) countStorageBtn.visible = item;
        typeItemBtn.active       = !editing;
        typeManualBtn.active     = !editing;
    }

    /* ─── Save ───────────────────────────────────────────────────────────── */

    private void trySave() {
        errorMsg = "";
        String name = nameField.getText().trim();
        if (name.isEmpty()) { errorMsg = "Task name cannot be empty!"; return; }

        if (taskType == 0) {
            String rawId = itemIdField.getText().trim();
            if (rawId.isEmpty()) { errorMsg = "Item ID cannot be empty!"; return; }

            int target;
            try {
                target = Integer.parseInt(targetField.getText().trim());
                if (target < 1) { errorMsg = "Goal must be at least 1."; return; }
            } catch (NumberFormatException e) {
                errorMsg = "Goal must be a whole number."; return;
            }

            Identifier id;
            try {
                id = ItemIdResolver.resolve(rawId);
            } catch (IllegalArgumentException e) {
                errorMsg = e.getMessage(); return;
            }

            if (!Registries.ITEM.containsId(id)) {
                errorMsg = "Warning: item not found. Double-check the ID.";
            }

            if (editTarget instanceof ItemTask it) {
                it.name         = name;
                it.itemId       = id.toString();
                it.target       = target;
                it.countStorage = countStorage;
                if (!progressField.getText().trim().isEmpty()) {
                    try {
                        int prog  = Integer.parseInt(progressField.getText().trim());
                        it.current = Math.max(0, Math.min(target, prog));
                    } catch (NumberFormatException e) {
                        errorMsg = "Progress must be a whole number."; return;
                    }
                }
                it.completed = it.current >= it.target;
            } else {
                ItemTask t = new ItemTask(name, id.toString(), target, countStorage);
                if (editTarget != null) t.pinned = editTarget.pinned;
                SaveData.getTasks().add(t);
            }
        } else {
            if (editTarget instanceof ManualTask mt) {
                mt.name = name;
            } else {
                ManualTask t = new ManualTask(name);
                if (editTarget != null) t.pinned = editTarget.pinned;
                SaveData.getTasks().add(t);
            }
        }

        SaveData.save();
        if (!errorMsg.startsWith("Warning")) this.client.setScreen(parent);
    }

    /* ─── Render ─────────────────────────────────────────────────────────── */

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        int px = pl(), py = pt();
        ctx.fill(0, 0, this.width, this.height, 0x99000000);
        ctx.fill(px, py, px + PW, py + PH, 0xEE0E0E1C);
        drawBorder(ctx, px, py, PW, PH, 0xFF5555AA);

        ctx.drawText(textRenderer, editTarget == null ? "Add task" : "Edit task",
                px + 5, py + 5, 0xFFAAAAFF, false);
        ctx.drawText(textRenderer, "Name:", px + 4, py + 24, 0xFF7788AA, false);
        ctx.drawText(textRenderer, "Type:", px + 4, py + 58, 0xFF7788AA, false);

        if (taskType == 0) {
            ctx.drawText(textRenderer, "Item ID:", px + 4, py + 96, 0xFF7788AA, false);

            // Live item name preview below the ID field
            if (!resolvedName.isEmpty()) {
                boolean ok  = resolvedName.startsWith("✔");
                int nameCol = ok ? 0xFF55DD88 : 0xFFFF6655;
                ctx.drawText(textRenderer,
                        textRenderer.trimToWidth(resolvedName, PW - 8),
                        px + 4, py + 128, nameCol, false);
            } else {
                ctx.drawText(textRenderer, "Goal:", px + 4, py + 128, 0xFF7788AA, false);
            }

            if (editTarget != null) {
                ctx.drawText(textRenderer, "Progress:", px + 80, py + 128, 0xFF7788AA, false);
            }
        }

        if (!errorMsg.isEmpty()) {
            int col = errorMsg.startsWith("Warning") ? 0xFFFFAA00 : 0xFFFF5555;
            ctx.drawText(textRenderer,
                    textRenderer.trimToWidth(errorMsg, PW - 8),
                    px + 4, py + PH - 44, col, false);
        }

        super.render(ctx, mx, my, delta);
    }

    /* ─── Helpers ─────────────────────────────────────────────────────────── */

    private ButtonWidget toggle(int x, int y, int w, String label, boolean initial,
                                java.util.function.Consumer<Boolean> onChange) {
        final boolean[] v = {initial};
        return ButtonWidget.builder(Text.literal(label + ": " + (v[0] ? "ON" : "OFF")), b -> {
            v[0] = !v[0];
            onChange.accept(v[0]);
            b.setMessage(Text.literal(label + ": " + (v[0] ? "ON" : "OFF")));
        }).dimensions(x, y, w, 20).build();
    }

    private void drawBorder(DrawContext ctx, int x, int y, int w, int h, int c) {
        ctx.fill(x,       y,       x + w,     y + 1,     c);
        ctx.fill(x,       y + h-1, x + w,     y + h,     c);
        ctx.fill(x,       y,       x + 1,     y + h,     c);
        ctx.fill(x + w-1, y,       x + w,     y + h,     c);
    }

    @Override public boolean shouldPause() { return false; }
}