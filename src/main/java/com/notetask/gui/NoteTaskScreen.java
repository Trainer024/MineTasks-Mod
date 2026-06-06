package com.notetask.gui;

import com.notetask.config.ModConfig;
import com.notetask.data.*;
import com.notetask.integration.JEICompat;
import com.notetask.util.ClipboardHelper;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class NoteTaskScreen extends Screen {

    private static final int PW       = 360;
    private static final int PH       = 268;
    private static final int ITEM_H   = 28;
    private static final int LIST_TOP = 58;
    private static final int LIST_H   = 142;
    private static final int FOOT_Y   = LIST_TOP + LIST_H + 6;

    // Priority indicator bar colours (3 px wide, left edge of each row)
    private static final int PRIORITY_HIGH_COLOR = 0xFFFF5544;
    private static final int PRIORITY_LOW_COLOR  = 0xFF4477AA;

    private int currentTab = 0;
    private int taskSel = -1;
    private int noteSel = -1;
    private int taskScroll = 0;
    private int noteScroll = 0;
    private boolean hideCompleted;
    private String tooltip = "";

    private TextFieldWidget searchField;

    private ButtonWidget addTaskBtn, editTaskBtn, deleteTaskBtn, markDoneBtn;
    private ButtonWidget dupTaskBtn, resetTaskBtn, pinTaskBtn, upTaskBtn, downTaskBtn, hideDoneBtn;
    private ButtonWidget craftBtn, usesBtn, copyBtn, clearDoneBtn, sortBtn;
    private ButtonWidget newNoteBtn, editNoteBtn, deleteNoteBtn, pinNoteBtn;
    private ButtonWidget settingsBtn;

    public NoteTaskScreen() {
        super(Text.literal("NoteTask"));
        hideCompleted = ModConfig.get().hideCompletedInMenu;
    }

    private int pl() { return (this.width - PW) / 2; }
    private int pt() { return (this.height - PH) / 2; }

    @Override
    protected void init() {
        // 1. Process pending items first
        if (SaveData.pendingItemId != null && !SaveData.pendingItemId.isEmpty()) {
            processPendingItem(SaveData.pendingItemId);
            SaveData.pendingItemId = null;
        }

        // 2. Initialize UI
        int px = pl(), py = pt();
        int bh = 18;
        int fy1 = py + FOOT_Y;
        int fy2 = fy1 + 22;

        settingsBtn = addDrawableChild(ButtonWidget.builder(Text.literal("⚙"), b ->
                        this.client.setScreen(new NoteTaskSettingsScreen(this)))
                .dimensions(px + PW - 24, py + 2, 20, 18).build());

        searchField = new TextFieldWidget(textRenderer, px + 4, py + 38, PW - 8, 18, Text.empty());
        searchField.setMaxLength(48);
        searchField.setPlaceholder(Text.literal("Search…"));
        addDrawableChild(searchField);

        addTaskBtn = addDrawableChild(ButtonWidget.builder(Text.literal("+ Add"),
                        b -> this.client.setScreen(new TaskEditorScreen(this, null)))
                .dimensions(px + 4, fy1, 52, bh).build());

        editTaskBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Edit"),
                        b -> openTaskEditor())
                .dimensions(px + 58, fy1, 44, bh).build());

        deleteTaskBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Del"),
                        b -> deleteSelectedTask())
                .dimensions(px + 104, fy1, 40, bh).build());

        markDoneBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Done"),
                        b -> toggleTaskDone())
                .dimensions(px + 146, fy1, 44, bh).build());

        craftBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Recipes"),
                        b -> openJeiRecipes())
                .dimensions(px + 192, fy1, 48, bh).build());

        usesBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Uses"),
                        b -> openJeiUses())
                .dimensions(px + 242, fy1, 44, bh).build());

        copyBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Copy"),
                        b -> copySelectedTask())
                .dimensions(px + 288, fy1, 44, bh).build());

        dupTaskBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Duplicate"),
                        b -> duplicateTask())
                .dimensions(px + 4, fy2, 72, bh).build());

        resetTaskBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Reset"),
                        b -> resetTaskProgress())
                .dimensions(px + 78, fy2, 48, bh).build());

        pinTaskBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Pin"),
                        b -> togglePin())
                .dimensions(px + 128, fy2, 40, bh).build());

        upTaskBtn = addDrawableChild(ButtonWidget.builder(Text.literal("↑"),
                        b -> moveTask(-1))
                .dimensions(px + 170, fy2, 28, bh).build());

        downTaskBtn = addDrawableChild(ButtonWidget.builder(Text.literal("↓"),
                        b -> moveTask(1))
                .dimensions(px + 200, fy2, 28, bh).build());

        sortBtn = addDrawableChild(ButtonWidget.builder(Text.literal(sortLabel()),
                        b -> cycleSort())
                .dimensions(px + 230, fy2, 58, bh).build());

        hideDoneBtn = addDrawableChild(toggleBtn(px + 292, fy2, 64, bh, "Hide done",
                hideCompleted, v -> {
                    hideCompleted = v;
                    ModConfig.get().hideCompletedInMenu = v;
                    ModConfig.save();
                }));

        clearDoneBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Clear done"),
                        b -> clearCompletedTasks())
                .dimensions(px + 4, fy2 + 22, 80, bh).build());
        clearDoneBtn.visible = false;

        newNoteBtn = addDrawableChild(ButtonWidget.builder(Text.literal("+ New note"),
                        b -> this.client.setScreen(new NoteEditScreen(this, null)))
                .dimensions(px + 4, fy1, 88, bh).build());

        editNoteBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Edit"),
                        b -> openNoteEditor())
                .dimensions(px + 94, fy1, 48, bh).build());

        deleteNoteBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Delete"),
                        b -> deleteSelectedNote())
                .dimensions(px + 144, fy1, 56, bh).build());

        pinNoteBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Pin note"),
                        b -> toggleNotePin())
                .dimensions(px + 202, fy1, 70, bh).build());

        rebuildButtons();
    }

    private void processPendingItem(String itemId) {
        this.client.setScreen(new TaskEditorScreen(this, new ItemTask("New Task", itemId, 1, false)));
    }

    /* ─── Sort ───────────────────────────────────────────────────────────── */

    private String sortLabel() {
        return switch (ModConfig.get().taskSortMode) {
            case 1 -> "Sort: A-Z";
            case 2 -> "Sort: %";
            default -> "Sort: Order";
        };
    }

    private void cycleSort() {
        ModConfig cfg = ModConfig.get();
        cfg.taskSortMode = (cfg.taskSortMode + 1) % 3;
        ModConfig.save();
        sortBtn.setMessage(Text.literal(sortLabel()));
    }

    /* ─── Helpers ────────────────────────────────────────────────────────── */

    private ButtonWidget toggleBtn(int x, int y, int w, int h, String label, boolean initial,
                                   java.util.function.Consumer<Boolean> onChange) {
        final boolean[] value = {initial};
        return ButtonWidget.builder(Text.literal(label + (value[0] ? " ✓" : "")), b -> {
            value[0] = !value[0];
            onChange.accept(value[0]);
            b.setMessage(Text.literal(label + (value[0] ? " ✓" : "")));
        }).dimensions(x, y, w, h).build();
    }

    private void rebuildButtons() {
        boolean tasks = currentTab == 0;
        addTaskBtn.visible = editTaskBtn.visible = deleteTaskBtn.visible = markDoneBtn.visible = tasks;
        dupTaskBtn.visible = resetTaskBtn.visible = pinTaskBtn.visible = tasks;
        upTaskBtn.visible = downTaskBtn.visible = hideDoneBtn.visible = sortBtn.visible = tasks;
        craftBtn.visible = usesBtn.visible = copyBtn.visible = tasks;
        clearDoneBtn.visible = false;
        searchField.visible = true;
        newNoteBtn.visible = editNoteBtn.visible = deleteNoteBtn.visible = pinNoteBtn.visible = !tasks;

        boolean jeiReady = JEICompat.isReady();
        craftBtn.active = usesBtn.active = jeiReady && hasSelectedItemTask();
    }

    private boolean hasSelectedItemTask() {
        return selectedTask() instanceof ItemTask;
    }

    private Task selectedTask() {
        List<Task> list = SaveData.getTasks();
        if (taskSel >= 0 && taskSel < list.size()) return list.get(taskSel);
        return null;
    }

    /* ─── JEI ────────────────────────────────────────────────────────────── */

    private void openJeiRecipes() {
        Task t = selectedTask();
        if (!(t instanceof ItemTask it)) return;
        ItemStack stack = stackForItem(it.itemId);
        if (!stack.isEmpty()) JEICompat.showRecipes(stack);
    }

    private void openJeiUses() {
        Task t = selectedTask();
        if (!(t instanceof ItemTask it)) return;
        ItemStack stack = stackForItem(it.itemId);
        if (!stack.isEmpty()) JEICompat.showUses(stack);
    }

    private ItemStack stackForItem(String itemId) {
        try {
            Identifier id = Identifier.of(itemId);
            if (Registries.ITEM.containsId(id)) return new ItemStack(Registries.ITEM.get(id));
        } catch (Exception ignored) {}
        return ItemStack.EMPTY;
    }

    /* ─── Task actions ───────────────────────────────────────────────────── */

    private void copySelectedTask() {
        Task t = selectedTask();
        if (t == null) return;
        String line = t.name + " — " + t.getProgressText();
        if (t instanceof ItemTask it) line += " (" + it.itemId + ")";
        ClipboardHelper.copy(line);
    }

    private void clearCompletedTasks() {
        SaveData.getTasks().removeIf(t -> t.completed);
        taskSel = -1;
        SaveData.save();
    }

    private void openTaskEditor() {
        Task t = selectedTask();
        if (t != null) this.client.setScreen(new TaskEditorScreen(this, t));
    }

    private void openNoteEditor() {
        List<Note> list = SaveData.getNotes();
        if (noteSel >= 0 && noteSel < list.size())
            this.client.setScreen(new NoteEditScreen(this, list.get(noteSel)));
    }

    private void deleteSelectedTask() {
        List<Task> list = SaveData.getTasks();
        if (taskSel >= 0 && taskSel < list.size()) {
            list.remove(taskSel);
            SaveData.save();
            taskSel = list.isEmpty() ? -1 : Math.min(taskSel, list.size() - 1);
        }
    }

    private void deleteSelectedNote() {
        List<Note> list = SaveData.getNotes();
        if (noteSel >= 0 && noteSel < list.size()) {
            list.remove(noteSel);
            SaveData.save();
            noteSel = list.isEmpty() ? -1 : Math.min(noteSel, list.size() - 1);
        }
    }

    private void toggleTaskDone() {
        Task t = selectedTask();
        if (t == null) return;
        t.completed = !t.completed;
        if (t instanceof ItemTask it && it.completed) it.current = it.target;
        SaveData.save();
    }

    private void resetTaskProgress() {
        Task t = selectedTask();
        if (t instanceof ItemTask it) {
            it.current   = 0;
            it.completed = false;
            SaveData.save();
        }
    }

    private void duplicateTask() {
        List<Task> list = SaveData.getTasks();
        if (taskSel < 0 || taskSel >= list.size()) return;
        Task t = list.get(taskSel);
        Task copy;
        if (t instanceof ItemTask it) {
            copy = new ItemTask(it.name + " (copy)", it.itemId, it.target, it.countStorage);
            ((ItemTask) copy).current    = it.current;
            ((ItemTask) copy).completed  = it.completed;
        } else {
            copy           = new ManualTask(t.name + " (copy)");
            copy.completed = t.completed;
        }
        copy.pinned   = t.pinned;
        copy.priority = t.priority;                 // carry over priority
        for (Subtask st : t.subtasks) {             // carry over subtasks (with state)
            Subtask stCopy = new Subtask(st.name);
            stCopy.completed = st.completed;
            copy.subtasks.add(stCopy);
        }
        list.add(taskSel + 1, copy);
        SaveData.save();
        taskSel++;
    }

    private void togglePin() {
        Task t = selectedTask();
        if (t != null) { t.pinned = !t.pinned; SaveData.save(); }
    }

    private void toggleNotePin() {
        List<Note> list = SaveData.getNotes();
        if (noteSel >= 0 && noteSel < list.size()) {
            list.get(noteSel).pinned = !list.get(noteSel).pinned;
            SaveData.save();
        }
    }

    private void moveTask(int dir) {
        if (ModConfig.get().taskSortMode != 0) return;
        List<Task> list = SaveData.getTasks();
        int j = taskSel + dir;
        if (taskSel < 0 || taskSel >= list.size() || j < 0 || j >= list.size()) return;
        Task a = list.get(taskSel);
        list.set(taskSel, list.get(j));
        list.set(j, a);
        taskSel = j;
        SaveData.save();
    }

    /* ─── Filtering / sorting ────────────────────────────────────────────── */

    private String filter() {
        return searchField != null ? searchField.getText().trim().toLowerCase() : "";
    }

    private boolean matchesTask(Task t) {
        if (hideCompleted && t.completed) return false;
        String f = filter();
        if (f.isEmpty()) return true;
        if (t.name.toLowerCase().contains(f)) return true;
        if (t instanceof ItemTask it && it.itemId.toLowerCase().contains(f)) return true;
        return false;
    }

    private boolean matchesNote(Note n) {
        String f = filter();
        if (f.isEmpty()) return true;
        return n.title.toLowerCase().contains(f) || n.body.toLowerCase().contains(f);
    }

    private List<Integer> visibleTaskIndices() {
        List<Task> sorted = sortedTasksView();
        List<Integer> out = new ArrayList<>();
        for (Task t : sorted) {
            int realIdx = SaveData.getTasks().indexOf(t);
            if (realIdx >= 0 && matchesTask(t)) out.add(realIdx);
        }
        return out;
    }

    /**
     * Sorts the task list for display.
     * Modes 1 (A-Z) and 2 (%) put HIGH priority tasks above NORMAL above LOW,
     * after pinned tasks.  Mode 0 (Order) preserves manual order; priority is
     * shown only as a visual indicator in that case.
     */
    private List<Task> sortedTasksView() {
        List<Task> copy = new ArrayList<>(SaveData.getTasks());
        int mode = ModConfig.get().taskSortMode;
        if (mode == 1) {
            copy.sort(Comparator.comparing((Task t) -> !t.pinned)
                    .thenComparingInt((Task t) -> -t.priority)   // HIGH(2) first
                    .thenComparing(t -> t.name.toLowerCase()));
        } else if (mode == 2) {
            copy.sort(Comparator.comparing((Task t) -> !t.pinned)
                    .thenComparingInt((Task t) -> -t.priority)   // HIGH(2) first
                    .thenComparingDouble(t -> -t.getProgressFraction())
                    .thenComparing(t -> t.name));
        }
        return copy;
    }

    private List<Integer> visibleNoteIndices() {
        List<Note> sorted = sortedNotesView();
        List<Integer> out = new ArrayList<>();
        for (Note n : sorted) {
            if (matchesNote(n)) {
                int idx = SaveData.getNotes().indexOf(n);
                if (idx >= 0) out.add(idx);
            }
        }
        return out;
    }

    private List<Note> sortedNotesView() {
        List<Note> copy = new ArrayList<>(SaveData.getNotes());
        copy.sort(Comparator.comparing((Note n) -> !n.pinned).thenComparing(n -> -n.createdAt));
        return copy;
    }

    /* ─── Render ─────────────────────────────────────────────────────────── */

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        tooltip = "";
        int px = pl(), py = pt();
        UiTheme.drawScreenBackdrop(ctx, this.width, this.height);
        UiTheme.drawPanel(ctx, px, py, PW, PH);

        int active = (int) SaveData.getTasks().stream().filter(t -> !t.completed).count();
        int total  = SaveData.getTasks().size();
        String subtitle = active + " active · " + total + " tasks";
        if (JEICompat.LOADED) subtitle += " · JEI";
        UiTheme.drawHeaderBar(ctx, textRenderer, "NoteTask", subtitle, px + 1, py + 1, PW - 2);

        int tabW = 78;
        boolean tabTasksHover = mx >= px + 6 && mx < px + 6 + tabW
                && my >= py + 24 && my < py + 36;
        boolean tabNotesHover = mx >= px + 6 + tabW + 4 && mx < px + 6 + tabW * 2 + 4
                && my >= py + 24 && my < py + 36;
        UiTheme.drawTab(ctx, textRenderer, "Tasks", px + 6,          py + 24, tabW, 14,
                currentTab == 0, tabTasksHover);
        UiTheme.drawTab(ctx, textRenderer, "Notes", px + 6 + tabW + 4, py + 24, tabW, 14,
                currentTab == 1, tabNotesHover);

        ctx.fill(px + 4, py + LIST_TOP - 2, px + PW - 4, py + LIST_TOP - 1, UiTheme.BORDER_DIM);
        ctx.fill(px + 4, py + LIST_TOP + LIST_H, px + PW - 4, py + LIST_TOP + LIST_H + 1,
                UiTheme.BORDER_DIM);

        if (currentTab == 0) renderTaskList(ctx, mx, my, px, py);
        else                 renderNoteList(ctx, mx, my, px, py);

        if (!tooltip.isEmpty()) UiTheme.drawTooltip(ctx, textRenderer, tooltip, mx, my);

        super.render(ctx, mx, my, delta);
    }

    private void renderTaskList(DrawContext ctx, int mx, int my, int px, int py) {
        List<Task> tasks   = SaveData.getTasks();
        List<Integer> visible = visibleTaskIndices();
        int lx = px + 4, ly = py + LIST_TOP, lw = PW - 12;
        int iconPad = 20;

        if (tasks.isEmpty()) {
            ctx.drawText(textRenderer,
                    "No tasks — click '+ Add' or use Quick Add keybind",
                    lx + 4, ly + 10, UiTheme.TEXT_MUTED, false);
            return;
        }
        if (visible.isEmpty()) {
            ctx.drawText(textRenderer, "No matching tasks", lx + 4, ly + 10, UiTheme.TEXT_MUTED, false);
            return;
        }

        ctx.enableScissor(lx, ly, lx + lw, ly + LIST_H);
        int startRow = taskScroll / ITEM_H;
        int endRow   = Math.min(visible.size(), startRow + LIST_H / ITEM_H + 2);

        for (int row = startRow; row < endRow; row++) {
            int  i    = visible.get(row);
            Task task = tasks.get(i);
            int  iy   = ly + row * ITEM_H - taskScroll;

            boolean hovered = mx >= lx && mx < lx + lw && my >= iy && my < iy + ITEM_H;
            if (hovered) {
                tooltip = task.name + " — " + task.getProgressText();
                if (task instanceof ItemTask it) tooltip += " · " + it.itemId;
                String subSum = task.getSubtaskSummary();
                if (!subSum.isEmpty()) tooltip += " [sub " + subSum + "]";
            }

            // ── Row background ──────────────────────────────────────────
            int bg = (i == taskSel)  ? UiTheme.ROW_SELECT
                    : hovered         ? UiTheme.ROW_HOVER
                      : (row % 2 == 0)  ? UiTheme.ROW_EVEN
                        : UiTheme.ROW_ODD;
            ctx.fill(lx, iy, lx + lw, iy + ITEM_H, bg);

            // ── Priority indicator bar (3 px, left edge) ────────────────
            if (task.priority == Task.PRIORITY_HIGH) {
                ctx.fill(lx, iy, lx + 3, iy + ITEM_H, PRIORITY_HIGH_COLOR);
            } else if (task.priority == Task.PRIORITY_LOW) {
                ctx.fill(lx, iy, lx + 3, iy + ITEM_H, PRIORITY_LOW_COLOR);
            }

            // ── Item icon ───────────────────────────────────────────────
            int textX = lx + 4 + iconPad;
            if (task instanceof ItemTask it) drawItemIcon(ctx, it, lx + 3, iy + 6);

            // ── Task name (line 1) ───────────────────────────────────────
            String prefix  = task.pinned ? "★ " : "";
            String name    = prefix + task.name;
            int nameCol    = task.completed ? UiTheme.TEXT_SUCCESS : UiTheme.TEXT_TITLE;
            ctx.drawText(textRenderer, trim(name, lw - iconPad - 54), textX, iy + 4, nameCol, false);

            // Progress text (right-aligned, line 1)
            String prog  = task.getProgressText();
            int progW    = textRenderer.getWidth(prog);
            ctx.drawText(textRenderer, prog, lx + lw - progW - 4, iy + 4,
                    task.completed ? UiTheme.TEXT_SUCCESS : UiTheme.ACCENT_DIM, false);

            // ── Second line: storage hint (left) + subtask count (right) ─
            if (task instanceof ItemTask it && it.countStorage) {
                ctx.drawText(textRenderer, it.getStorageHint(), textX, iy + 14, UiTheme.TEXT_MUTED, false);
            }

            String subSummary = task.getSubtaskSummary();
            if (!subSummary.isEmpty()) {
                String subText = "▪ " + subSummary;
                int    subW    = textRenderer.getWidth(subText);
                int    subCol  = task.allSubtasksDone() ? UiTheme.TEXT_SUCCESS : UiTheme.TEXT_MUTED;
                ctx.drawText(textRenderer, subText, lx + lw - subW - 4, iy + 14, subCol, false);
            }

            // ── Progress bar (bottom of row) ─────────────────────────────
            UiTheme.drawProgressBar(ctx, lx + iconPad, iy + ITEM_H - 5, lw - iconPad, 4,
                    task.getProgressFraction(), task.completed);
        }
        ctx.disableScissor();

        if (visible.size() * ITEM_H > LIST_H) {
            drawScrollbar(ctx, px + PW - 8, ly, 4, LIST_H, taskScroll, visible.size() * ITEM_H);
        }
    }

    private void renderNoteList(DrawContext ctx, int mx, int my, int px, int py) {
        List<Note>    notes   = SaveData.getNotes();
        List<Integer> visible = visibleNoteIndices();
        int lx = px + 4, ly = py + LIST_TOP, lw = PW - 12;

        if (notes.isEmpty()) {
            ctx.drawText(textRenderer, "No notes — click '+ New note'",
                    lx + 4, ly + 10, UiTheme.TEXT_MUTED, false);
            return;
        }
        if (visible.isEmpty()) {
            ctx.drawText(textRenderer, "No matching notes", lx + 4, ly + 10, UiTheme.TEXT_MUTED, false);
            return;
        }

        ctx.enableScissor(lx, ly, lx + lw, ly + LIST_H);
        int startRow = noteScroll / ITEM_H;
        int endRow   = Math.min(visible.size(), startRow + LIST_H / ITEM_H + 2);

        for (int row = startRow; row < endRow; row++) {
            int  i    = visible.get(row);
            Note note = notes.get(i);
            int  iy   = ly + row * ITEM_H - noteScroll;

            boolean hovered = mx >= lx && mx < lx + lw && my >= iy && my < iy + ITEM_H;
            if (hovered) tooltip = note.title;

            int bg = (i == noteSel) ? UiTheme.ROW_SELECT
                    : hovered        ? UiTheme.ROW_HOVER
                      : (row % 2 == 0) ? UiTheme.ROW_EVEN
                        : UiTheme.ROW_ODD;
            ctx.fill(lx, iy, lx + lw, iy + ITEM_H, bg);

            String pin   = note.pinned ? "★ " : "";
            String title = pin + (note.title.isEmpty() ? "(Untitled)" : note.title);
            ctx.drawText(textRenderer, trim(title, lw - 8), lx + 4, iy + 4, UiTheme.TEXT_TITLE, false);
            if (!note.body.isEmpty()) {
                String preview = note.body.replace('\n', ' ');
                ctx.drawText(textRenderer, trim(preview, lw - 8), lx + 4, iy + 15,
                        UiTheme.TEXT_MUTED, false);
            }
        }
        ctx.disableScissor();
    }

    /* ─── Scroll / click / key ───────────────────────────────────────────── */

    private void drawScrollbar(DrawContext ctx, int x, int y, int w, int h, int scroll, int total) {
        ctx.fill(x, y, x + w, y + h, 0x33FFFFFF);
        int thumbH = Math.max(12, h * h / total);
        int maxY   = h - thumbH;
        int thumbY = total <= h ? 0 : (int)((float) scroll / (total - h) * maxY);
        ctx.fill(x, y + thumbY, x + w, y + thumbY + thumbH, 0xBBAAAAFF);
    }

    private void drawItemIcon(DrawContext ctx, ItemTask it, int x, int y) {
        ItemStack stack = stackForItem(it.itemId);
        if (!stack.isEmpty()) ctx.drawItem(stack, x, y);
    }

    private String trim(String s, int maxW) {
        if (textRenderer.getWidth(s) <= maxW) return s;
        return textRenderer.trimToWidth(s, Math.max(0, maxW - textRenderer.getWidth("…"))) + "…";
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x(), my = click.y();
        int px = pl(), py = pt();
        int tabW = 78;

        if (my >= py + 24 && my < py + 38) {
            if (mx >= px + 6 && mx < px + 6 + tabW) {
                currentTab = 0; rebuildButtons(); return true;
            }
            if (mx >= px + 6 + tabW + 4 && mx < px + 6 + tabW * 2 + 4) {
                currentTab = 1; rebuildButtons(); return true;
            }
        }

        int lx = px + 4, ly = py + LIST_TOP, lw = PW - 12;

        if (currentTab == 0 && mx >= lx && mx < lx + lw && my >= ly && my < ly + LIST_H) {
            List<Integer> visible = visibleTaskIndices();
            int row = (int)((my - ly + taskScroll) / ITEM_H);
            if (row >= 0 && row < visible.size()) {
                taskSel = visible.get(row);
                rebuildButtons();
                if (doubled) openTaskEditor();
                return true;
            }
        } else if (currentTab == 1 && mx >= lx && mx < lx + lw && my >= ly && my < ly + LIST_H) {
            List<Integer> visible = visibleNoteIndices();
            int row = (int)((my - ly + noteScroll) / ITEM_H);
            if (row >= 0 && row < visible.size()) {
                noteSel = visible.get(row);
                if (doubled) openNoteEditor();
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        int px = pl(), py = pt();
        if (mx >= px + 4 && mx < px + PW - 4 && my >= py + LIST_TOP && my < py + LIST_TOP + LIST_H) {
            int amount = (int)(-dy * ITEM_H);
            if (currentTab == 0) {
                int max = Math.max(0, visibleTaskIndices().size() * ITEM_H - LIST_H);
                taskScroll = Math.max(0, Math.min(max, taskScroll + amount));
            } else {
                int max = Math.max(0, visibleNoteIndices().size() * ITEM_H - LIST_H);
                noteScroll = Math.max(0, Math.min(max, noteScroll + amount));
            }
            return true;
        }
        return super.mouseScrolled(mx, my, dx, dy);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_DELETE) {
            if (currentTab == 0) deleteSelectedTask(); else deleteSelectedNote();
            return true;
        }
        if (input.key() == GLFW.GLFW_KEY_ENTER) {
            if (currentTab == 0) openTaskEditor(); else openNoteEditor();
            return true;
        }
        if (input.key() == GLFW.GLFW_KEY_D && (input.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0) {
            if (currentTab == 0) duplicateTask();
            return true;
        }
        return super.keyPressed(input);
    }
}