package com.notetask.gui;

import com.notetask.KeyBindings;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class WelcomeScreen extends Screen {

    private static final int PW = 360;
    private static final int PH = 240;

    /** Marker file — if it exists the screen has already been shown. */
    private static final Path MARKER = FabricLoader.getInstance()
            .getConfigDir().resolve("notetask").resolve(".welcomed");

    // ── Page definitions ─────────────────────────────────────────────────

    private record Page(String title, String body) {}

    /** Built in the constructor so the last page can embed the live keybind name. */
    private final Page[] PAGES;

    // ── State ─────────────────────────────────────────────────────────────

    private final Screen next;   // screen to open when done / skipped
    private int page = 0;

    private ButtonWidget prevBtn;
    private ButtonWidget nextBtn;

    // ── First-run gate ────────────────────────────────────────────────────

    /**
     * Returns {@code true} when the welcome screen should be shown.
     * The only gate is the marker file — delete it to see the screen again.
     */
    public static boolean shouldShow() {
        return !Files.exists(MARKER);
    }

    private static void markSeen() {
        try {
            Files.createDirectories(MARKER.getParent());
            Files.writeString(MARKER, "1");
        } catch (IOException ignored) {}
    }

    // ── Constructor ───────────────────────────────────────────────────────

    /**
     * @param next The screen to open once the guide is finished or skipped.
     *             Pass {@code new NoteTaskScreen()} here.
     */
    public WelcomeScreen(Screen next) {
        super(Text.literal("Welcome to NoteTask"));
        this.next = next;

        // Read the actual bound keys so the last page is always accurate
        String openKey = KeyBindings.openNoteTaskKey.getBoundKeyLocalizedText().getString();
        String quickAddKey = KeyBindings.quickAddTaskKey.getBoundKeyLocalizedText().getString();

        PAGES = new Page[]{
                new Page(
                        "Hey there, adventurer!  ✦",
                        "I'm Notey, your in-game task companion.\n\n" +
                                "NoteTask keeps track of what you need to collect, build, or do — " +
                                "right inside Minecraft. No more alt-tabbing to check a text file!\n\n" +
                                "Let's get you up to speed real quick."
                ),
                new Page(
                        "Item Tasks  ✦",
                        "Item Tasks are the core of the mod.\n\n" +
                                "Set a goal like '64 Oak Logs' by typing the item ID or clicking " +
                                "Browse to find it. NoteTask automatically counts what's in your " +
                                "inventory and nearby chests.\n\n" +
                                "Once you hit your goal, the task marks itself done. ✔"
                ),
                new Page(
                        "Manual Tasks & Priority  ✦",
                        "Not everything is an item. Manual Tasks are simple to-dos " +
                                "you check off yourself — like 'Build the nether portal' " +
                                "or 'Find a village'.\n\n" +
                                "You can also set priorities: ▼ Low, ● Normal, or ▲ High. " +
                                "High-priority tasks get a red stripe on the left so they stand out."
                ),
                new Page(
                        "Subtasks & Notes  ✦",
                        "Big projects? Break them down into Subtasks inside the editor " +
                                "and check them off as you go.\n\n" +
                                "There's also a Notes tab for random thoughts. Use it for coordinates, " +
                                "build plans, or shopping lists. You can pin and search through them later."
                ),
                new Page(
                        "You're all set!  ✦",
                        "Your active tasks will show up right on your HUD while you play.\n\n" +
                                "Press [" + openKey + "] to open this dashboard, or press [" + quickAddKey + "] " +
                                "to quickly log a task on the fly without pausing.\n\n" +
                                "Good luck out there!  ✦"
                )
        };
    }

    private int pl() { return (this.width  - PW) / 2; }
    private int pt() { return (this.height - PH) / 2; }

    // ── Init ──────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        int px = pl(), py = pt();

        prevBtn = addDrawableChild(ButtonWidget.builder(Text.literal("← Back"), b -> {
            page--;
            rebuildNav();
        }).dimensions(px + 4, py + PH - 26, 70, 20).build());

        // Centre "Skip" button
        addDrawableChild(ButtonWidget.builder(Text.literal("Skip"), b -> finish())
                .dimensions(px + PW / 2 - 28, py + PH - 26, 56, 20).build());

        nextBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Next →"), b -> {
            if (page < PAGES.length - 1) { page++; rebuildNav(); }
            else finish();
        }).dimensions(px + PW - 80, py + PH - 26, 76, 20).build());

        rebuildNav();
    }

    private void rebuildNav() {
        prevBtn.active = page > 0;
        nextBtn.setMessage(Text.literal(page == PAGES.length - 1 ? "Done  ✔" : "Next →"));
    }

    private void finish() {
        markSeen();
        this.client.setScreen(next);
    }

    // Close / Escape behaves the same as Skip
    @Override
    public void close() { finish(); }

    // ── Render ────────────────────────────────────────────────────────────

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        int px = pl(), py = pt();

        // Panel backdrop
        ctx.fill(0, 0, this.width, this.height, 0x99000000);
        ctx.fill(px, py, px + PW, py + PH, 0xEE0E0E1C);
        drawBorder(ctx, px, py, PW, PH, 0xFF5555AA);

        // ── Header bar ────────────────────────────────────────────────────
        ctx.fill(px + 1, py + 1, px + PW - 1, py + 22, 0x220D0D2A);

        // "Notey" narrator label (left)
        ctx.drawText(textRenderer, "Notey", px + 8, py + 7, 0xFFAAAAFF, false);

        // Page counter (right)
        String counter = (page + 1) + " / " + PAGES.length;
        ctx.drawText(textRenderer, counter,
                px + PW - 6 - textRenderer.getWidth(counter), py + 7, 0xFF556677, false);

        // Separator below header
        ctx.fill(px + 4, py + 22, px + PW - 4, py + 23, 0x445555AA);

        // ── Page title ────────────────────────────────────────────────────
        Page p = PAGES[page];
        ctx.drawText(textRenderer, p.title(), px + 8, py + 29, 0xFFCCCCFF, false);

        // Thin rule below title
        ctx.fill(px + 4, py + 41, px + PW - 4, py + 42, 0x22AAAAFF);

        // ── Body text (paragraph-aware wrapping) ──────────────────────────
        int textX  = px + 8;
        int textY  = py + 47;
        int maxW   = PW - 16;
        int lineH  = textRenderer.fontHeight + 2;
        int clipY  = py + PH - 36;   // don't render into the button row

        for (String paragraph : p.body().split("\n")) {
            if (textY >= clipY) break;

            if (paragraph.isEmpty()) {
                textY += lineH / 2;   // blank line → half-gap
                continue;
            }

            List<OrderedText> lines = textRenderer.wrapLines(Text.literal(paragraph), maxW);
            for (OrderedText line : lines) {
                if (textY >= clipY) break;
                ctx.drawText(textRenderer, line, textX, textY, 0xFFBBBBBB, false);
                textY += lineH;
            }
        }

        // ── Page dots ─────────────────────────────────────────────────────
        int dotW    = 6, dotGap = 4;
        int totalDW = PAGES.length * dotW + (PAGES.length - 1) * dotGap;
        int dotX    = px + PW / 2 - totalDW / 2;
        int dotY    = py + PH - 9;
        for (int i = 0; i < PAGES.length; i++) {
            int dx  = dotX + i * (dotW + dotGap);
            int col = (i == page) ? 0xFFAAAAFF : 0x33AAAAFF;
            ctx.fill(dx, dotY, dx + dotW, dotY + 4, col);
        }

        super.render(ctx, mx, my, delta);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void drawBorder(DrawContext ctx, int x, int y, int w, int h, int c) {
        ctx.fill(x,       y,       x + w,     y + 1,     c);
        ctx.fill(x,       y + h-1, x + w,     y + h,     c);
        ctx.fill(x,       y,       x + 1,     y + h,     c);
        ctx.fill(x + w-1, y,       x + w,     y + h,     c);
    }

    @Override public boolean shouldPause() { return false; }
}