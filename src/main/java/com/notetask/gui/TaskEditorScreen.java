package com.notetask.gui;

import com.notetask.config.ModConfig;
import com.notetask.data.*;
import com.notetask.integration.JEICompat;
import com.notetask.util.ItemIdResolver;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class TaskEditorScreen extends Screen {

    // ── Layout constants ──────────────────────────────────────────────────
    private static final int PW             = 300;
    private static final int PH             = 350;  // expanded from 240 to fit subtask section

    // Subtask list geometry (all Y values are relative to pt())
    private static final int SUBTASK_LIST_Y  = 214;
    private static final int SUBTASK_LIST_H  = 60;
    private static final int SUBTASK_ROW_H   = 16;
    private static final int SUBTASK_INPUT_Y = 278;

    // ── State ─────────────────────────────────────────────────────────────
    private final Screen parent;
    private final Task   editTarget;
    private final String preFillItemId;

    private int     taskType      = 0;   // 0 = item, 1 = manual
    private boolean countStorage;
    private String  errorMsg      = "";
    private String  resolvedName  = "";
    private int     priority      = Task.PRIORITY_NORMAL;
    private int     subtaskScroll = 0;

    /**
     * Subtasks accumulated while the user is filling in a brand-new task.
     * For existing tasks, Task#subtasks is edited directly.
     */
    private final List<Subtask> pendingSubtasks = new ArrayList<>();

    // ── Widgets ───────────────────────────────────────────────────────────
    private TextFieldWidget nameField;
    private TextFieldWidget itemIdField;
    private TextFieldWidget targetField;
    private TextFieldWidget progressField;
    private TextFieldWidget subtaskInput;

    private ButtonWidget countStorageBtn;
    private ButtonWidget typeItemBtn;
    private ButtonWidget typeManualBtn;
    private ButtonWidget browseBtn;
    private ButtonWidget priorityBtn;
    private ButtonWidget jeiRecipesBtn;
    private ButtonWidget jeiUsesBtn;

    /* ─── Constructors ───────────────────────────────────────────────────── */

    public TaskEditorScreen(Screen parent, Task editTarget) {
        this(parent, editTarget, null);
    }

    public TaskEditorScreen(Screen parent, Task editTarget, String preFillItemId) {
        super(Text.literal(editTarget == null ? "Add task" : "Edit task"));
        this.parent        = parent;
        this.editTarget    = editTarget;
        this.preFillItemId = preFillItemId;
    }

    private int pl() { return (this.width  - PW) / 2; }
    private int pt() { return (this.height - PH) / 2; }

    /** The live subtask list: the existing task's list when editing, pending list when creating. */
    private List<Subtask> activeSubtasks() {
        return editTarget != null ? editTarget.subtasks : pendingSubtasks;
    }

    /* ─── Init ───────────────────────────────────────────────────────────── */

    @Override
    protected void init() {
        int px = pl(), py = pt();
        boolean editing = editTarget != null;

        // Pre-load priority so priorityBtn is initialised with the right label
        if (editing) priority = editTarget.priority;

        // ── Name ─────────────────────────────────────────────────────────
        nameField = new TextFieldWidget(textRenderer, px + 4, py + 34, PW - 8, 20, Text.empty());
        nameField.setMaxLength(60);
        nameField.setPlaceholder(Text.literal("Task name…"));
        if (editing) nameField.setText(editTarget.name);
        addDrawableChild(nameField);

        // ── Type buttons (locked when editing) ───────────────────────────
        typeItemBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Item task"), b -> {
            if (!editing) { taskType = 0; updateVisibility(); }
        }).dimensions(px + 4, py + 68, 100, 18).build());

        typeManualBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Manual task"), b -> {
            if (!editing) { taskType = 1; updateVisibility(); }
        }).dimensions(px + 108, py + 68, 110, 18).build());

        // ── Priority cycle button: LOW → NORMAL → HIGH → LOW ─────────────
        priorityBtn = addDrawableChild(ButtonWidget.builder(Text.literal(priorityLabel()), b -> {
            priority = (priority + 1) % 3;
            b.setMessage(Text.literal(priorityLabel()));
        }).dimensions(px + 222, py + 68, 74, 18).build());

        // ── Item ID field ─────────────────────────────────────────────────
        itemIdField = new TextFieldWidget(textRenderer, px + 4, py + 100, PW - 74, 20, Text.empty());
        itemIdField.setMaxLength(120);
        itemIdField.setPlaceholder(Text.literal("e.g. minecraft:oak_planks"));
        itemIdField.setChangedListener(s -> refreshResolvedName());
        addDrawableChild(itemIdField);

        // Browse — ItemPickerScreen.confirm() calls setScreen(parent) THEN onPick,
        // so init() recreates itemIdField before the lambda writes to it.
        browseBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Browse…"), b ->
                this.client.setScreen(new ItemPickerScreen(this, id -> {
                    this.itemIdField.setText(id);
                    this.refreshResolvedName();
                }))
        ).dimensions(px + PW - 68, py + 100, 64, 20).build());

        // ── Goal / progress fields ────────────────────────────────────────
        targetField = new TextFieldWidget(textRenderer, px + 4, py + 132, 70, 20, Text.empty());
        targetField.setMaxLength(7);
        targetField.setPlaceholder(Text.literal("Goal"));
        addDrawableChild(targetField);

        progressField = new TextFieldWidget(textRenderer, px + 80, py + 132, 70, 20, Text.empty());
        progressField.setMaxLength(7);
        progressField.setPlaceholder(Text.literal("Progress"));
        addDrawableChild(progressField);

        // ── Count-storage toggle ──────────────────────────────────────────
        countStorage = ModConfig.get().countStorage;
        countStorageBtn = toggle(px + 4, py + 156, PW - 8,
                "Count chests & ender chest", countStorage, v -> countStorage = v);
        addDrawableChild(countStorageBtn);

        // ── Use held item shortcut ────────────────────────────────────────
        addDrawableChild(ButtonWidget.builder(Text.literal("Use held item"), b -> {
            MinecraftClient mc = this.client;
            if (mc == null || mc.player == null) { errorMsg = "Player not available."; return; }
            Item held = mc.player.getMainHandStack().getItem();
            itemIdField.setText(Registries.ITEM.getId(held).toString());
            refreshResolvedName();
            errorMsg = "";
        }).dimensions(px + 4, py + 180, 110, 20).build());

        // ── JEI buttons ───────────────────────────────────────────────────
        if (JEICompat.LOADED) {
            jeiRecipesBtn = addDrawableChild(ButtonWidget.builder(
                            Text.literal("Recipes in JEI"), b -> openInJEI(false))
                    .dimensions(px + 120, py + 180, 90, 20).build());
            jeiRecipesBtn.visible = false;

            jeiUsesBtn = addDrawableChild(ButtonWidget.builder(
                            Text.literal("Uses in JEI"), b -> openInJEI(true))
                    .dimensions(px + 216, py + 180, 80, 20).build());
            jeiUsesBtn.visible = false;
        }

        // ── Subtask input + Add button ────────────────────────────────────
        subtaskInput = new TextFieldWidget(
                textRenderer, px + 4, py + SUBTASK_INPUT_Y, PW - 66, 18, Text.empty());
        subtaskInput.setMaxLength(80);
        subtaskInput.setPlaceholder(Text.literal("New subtask…"));
        addDrawableChild(subtaskInput);

        addDrawableChild(ButtonWidget.builder(Text.literal("+ Add"), b -> addSubtask())
                .dimensions(px + PW - 60, py + SUBTASK_INPUT_Y, 56, 18).build());

        // ── Save / Cancel ─────────────────────────────────────────────────
        addDrawableChild(ButtonWidget.builder(
                        Text.literal(editing ? "Save" : "Add"), b -> trySave())
                .dimensions(px + 4, py + PH - 26, 80, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"),
                        b -> this.client.setScreen(parent))
                .dimensions(px + 90, py + PH - 26, 72, 20).build());

        // ── Populate from existing task or pre-fill string ────────────────
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

    /* ─── Subtask helpers ────────────────────────────────────────────────── */

    private void addSubtask() {
        String name = subtaskInput.getText().trim();
        if (name.isEmpty()) return;
        activeSubtasks().add(new Subtask(name));
        if (editTarget != null) SaveData.save();
        subtaskInput.setText("");
    }

    /* ─── Priority ───────────────────────────────────────────────────────── */

    private String priorityLabel() {
        return switch (priority) {
            case Task.PRIORITY_LOW  -> "▼ Low";
            case Task.PRIORITY_HIGH -> "▲ High";
            default                 -> "● Normal";
        };
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
        String raw   = itemIdField != null ? itemIdField.getText().trim() : "";
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
            try { id = ItemIdResolver.resolve(rawId); }
            catch (IllegalArgumentException e) { errorMsg = e.getMessage(); return; }

            if (!Registries.ITEM.containsId(id)) {
                errorMsg = "Warning: item not found. Double-check the ID.";
            }

            if (editTarget instanceof ItemTask it) {
                it.name         = name;
                it.itemId       = id.toString();
                it.target       = target;
                it.countStorage = countStorage;
                it.priority     = priority;
                // subtasks are already edited in-place via activeSubtasks()
                if (!progressField.getText().trim().isEmpty()) {
                    try {
                        int prog = Integer.parseInt(progressField.getText().trim());
                        it.current = Math.max(0, Math.min(target, prog));
                    } catch (NumberFormatException e) {
                        errorMsg = "Progress must be a whole number."; return;
                    }
                }
                it.completed = it.current >= it.target;
            } else {
                ItemTask t   = new ItemTask(name, id.toString(), target, countStorage);
                t.priority   = priority;
                t.subtasks   = new ArrayList<>(pendingSubtasks);
                if (editTarget != null) t.pinned = editTarget.pinned;
                SaveData.getTasks().add(t);
            }

        } else {
            if (editTarget instanceof ManualTask mt) {
                mt.name     = name;
                mt.priority = priority;
                // subtasks already edited in-place
            } else {
                ManualTask t = new ManualTask(name);
                t.priority   = priority;
                t.subtasks   = new ArrayList<>(pendingSubtasks);
                if (editTarget != null) t.pinned = editTarget.pinned;
                SaveData.getTasks().add(t);
            }
        }

        SaveData.save();
        if (!errorMsg.startsWith("Warning")) this.client.setScreen(parent);
    }

    /* ─── Input ──────────────────────────────────────────────────────────── */

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int px = pl(), py = pt();
        double mx = click.x(), my = click.y();
        int slx = px + 4, sly = py + SUBTASK_LIST_Y, slw = PW - 8;

        if (mx >= slx && mx < slx + slw && my >= sly && my < sly + SUBTASK_LIST_H) {
            List<Subtask> subs = activeSubtasks();
            int row = (int)((my - sly + subtaskScroll) / SUBTASK_ROW_H);
            if (row >= 0 && row < subs.size()) {
                double relX = mx - slx;
                if (relX < 16) {
                    // Toggle subtask completion
                    subs.get(row).completed = !subs.get(row).completed;
                    if (editTarget != null) SaveData.save();
                } else if (relX > slw - 16) {
                    // Delete subtask
                    subs.remove(row);
                    if (editTarget != null) SaveData.save();
                }
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        int px = pl(), py = pt();
        int slx = px + 4, sly = py + SUBTASK_LIST_Y, slw = PW - 8;
        if (mx >= slx && mx < slx + slw && my >= sly && my < sly + SUBTASK_LIST_H) {
            List<Subtask> subs = activeSubtasks();
            int maxScroll = Math.max(0, subs.size() * SUBTASK_ROW_H - SUBTASK_LIST_H);
            subtaskScroll = Math.max(0, Math.min(maxScroll,
                    subtaskScroll - (int)(dy * SUBTASK_ROW_H)));
            return true;
        }
        return super.mouseScrolled(mx, my, dx, dy);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        // Press Enter in the subtask field to add without clicking the button
        if (input.key() == GLFW.GLFW_KEY_ENTER
                && subtaskInput != null && subtaskInput.isFocused()) {
            addSubtask();
            return true;
        }
        return super.keyPressed(input);
    }

    /* ─── Render ─────────────────────────────────────────────────────────── */

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        int px = pl(), py = pt();
        ctx.fill(0, 0, this.width, this.height, 0x99000000);
        ctx.fill(px, py, px + PW, py + PH, 0xEE0E0E1C);
        drawBorder(ctx, px, py, PW, PH, 0xFF5555AA);

        // ── Labels ────────────────────────────────────────────────────────
        ctx.drawText(textRenderer, editTarget == null ? "Add task" : "Edit task",
                px + 5, py + 5, 0xFFAAAAFF, false);
        ctx.drawText(textRenderer, "Name:",     px + 4,   py + 24, 0xFF7788AA, false);
        ctx.drawText(textRenderer, "Type:",     px + 4,   py + 58, 0xFF7788AA, false);
        ctx.drawText(textRenderer, "Priority:", px + 222, py + 58, 0xFF7788AA, false);

        if (taskType == 0) {
            ctx.drawText(textRenderer, "Item ID:", px + 4, py + 90, 0xFF7788AA, false);
            if (!resolvedName.isEmpty()) {
                boolean ok  = resolvedName.startsWith("✔");
                int col = ok ? 0xFF55DD88 : 0xFFFF6655;
                ctx.drawText(textRenderer,
                        textRenderer.trimToWidth(resolvedName, PW - 8),
                        px + 4, py + 122, col, false);
            } else {
                ctx.drawText(textRenderer, "Goal:", px + 4, py + 122, 0xFF7788AA, false);
            }
            if (editTarget != null) {
                ctx.drawText(textRenderer, "Progress:", px + 80, py + 122, 0xFF7788AA, false);
            }
        }

        // ── Subtask section ───────────────────────────────────────────────
        renderSubtaskSection(ctx, px, py);

        // ── Error / warning ───────────────────────────────────────────────
        if (!errorMsg.isEmpty()) {
            int col = errorMsg.startsWith("Warning") ? 0xFFFFAA00 : 0xFFFF5555;
            ctx.drawText(textRenderer,
                    textRenderer.trimToWidth(errorMsg, PW - 8),
                    px + 4, py + PH - 44, col, false);
        }

        super.render(ctx, mx, my, delta);
    }

    private void renderSubtaskSection(DrawContext ctx, int px, int py) {
        List<Subtask> subs = activeSubtasks();
        int slx = px + 4, sly = py + SUBTASK_LIST_Y, slw = PW - 8;

        // Label with running count
        String label = subs.isEmpty() ? "Subtasks:"
                : "Subtasks: " + subs.stream().filter(s -> s.completed).count()
                  + "/" + subs.size();
        ctx.drawText(textRenderer, label, px + 4, py + 204, 0xFF7788AA, false);

        // List background + border
        ctx.fill(slx, sly, slx + slw, sly + SUBTASK_LIST_H, 0x44000000);
        drawBorder(ctx, slx, sly, slw, SUBTASK_LIST_H, 0x44AAAAFF);

        if (subs.isEmpty()) {
            ctx.drawText(textRenderer, "No subtasks — type below and press Enter",
                    slx + 4, sly + 5, 0xFF445566, false);
        } else {
            ctx.enableScissor(slx, sly, slx + slw, sly + SUBTASK_LIST_H);
            for (int i = 0; i < subs.size(); i++) {
                Subtask sub = subs.get(i);
                int ry = sly + i * SUBTASK_ROW_H - subtaskScroll;
                if (ry + SUBTASK_ROW_H <= sly || ry >= sly + SUBTASK_LIST_H) continue;

                // Alternating row tint
                ctx.fill(slx, ry, slx + slw, ry + SUBTASK_ROW_H,
                        i % 2 == 0 ? 0x22FFFFFF : 0x11FFFFFF);

                // Checkbox  [□ / ✔]
                ctx.drawText(textRenderer, sub.completed ? "✔" : "□",
                        slx + 3, ry + 4,
                        sub.completed ? 0xFF55DD88 : 0xFF8899AA, false);

                // Name — dimmed when done
                String name = textRenderer.trimToWidth(sub.name, slw - 30);
                ctx.drawText(textRenderer, name, slx + 15, ry + 4,
                        sub.completed ? 0xFF778877 : 0xFFCCCCCC, false);

                // Delete  [×]
                ctx.drawText(textRenderer, "×", slx + slw - 11, ry + 4, 0xFFAA4444, false);
            }
            ctx.disableScissor();

            // Scrollbar (appears only when list overflows)
            if (subs.size() * SUBTASK_ROW_H > SUBTASK_LIST_H) {
                int totalH  = subs.size() * SUBTASK_ROW_H;
                int thumbH  = Math.max(8, SUBTASK_LIST_H * SUBTASK_LIST_H / totalH);
                int maxY    = SUBTASK_LIST_H - thumbH;
                int thumbY  = totalH <= SUBTASK_LIST_H ? 0
                        : (int)((float) subtaskScroll / (totalH - SUBTASK_LIST_H) * maxY);
                ctx.fill(slx + slw - 3, sly + thumbY,
                        slx + slw,     sly + thumbY + thumbH, 0xBBAAAAFF);
            }
        }
    }

    /* ─── Helpers ────────────────────────────────────────────────────────── */

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