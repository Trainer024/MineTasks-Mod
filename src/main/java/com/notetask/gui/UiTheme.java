package com.notetask.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/** Shared panel styling for NoteTask screens. */
public final class UiTheme {

    public static final int OVERLAY = 0x99000000;
    public static final int PANEL_BG = 0xF0101018;
    public static final int PANEL_BG_ALT = 0xF0181824;
    public static final int HEADER_BG = 0xFF12121E;
    public static final int BORDER = 0xFF5A6AD8;
    public static final int BORDER_DIM = 0xFF333355;
    public static final int ACCENT = 0xFF7788FF;
    public static final int ACCENT_DIM = 0xFF5566AA;
    public static final int TEXT_TITLE = 0xFFDDDDFF;
    public static final int TEXT_LABEL = 0xFF8899BB;
    public static final int TEXT_MUTED = 0xFF556677;
    public static final int TEXT_SUCCESS = 0xFFFFCC66;
    public static final int ROW_EVEN = 0x33222844;
    public static final int ROW_ODD = 0x221A1A30;
    public static final int ROW_HOVER = 0x55334477;
    public static final int ROW_SELECT = 0xAA3A4A88;
    public static final int PROGRESS_BG = 0xFF0E0E18;
    public static final int PROGRESS_FILL = 0xFF44EE77;
    public static final int PROGRESS_DONE = 0xFFFFAA33;

    private UiTheme() {}

    public static void drawScreenBackdrop(DrawContext ctx, int screenW, int screenH) {
        ctx.fill(0, 0, screenW, screenH, OVERLAY);
    }

    public static void drawPanel(DrawContext ctx, int x, int y, int w, int h) {
        ctx.fill(x, y, x + w, y + h, PANEL_BG);
        drawBorder(ctx, x, y, w, h, BORDER);
        ctx.fill(x + 1, y + 1, x + w - 1, y + 2, 0x44FFFFFF);
    }

    public static void drawHeaderBar(DrawContext ctx, TextRenderer tr, String title, String subtitle,
                                     int x, int y, int w) {
        ctx.fill(x, y, x + w, y + 22, HEADER_BG);
        ctx.fill(x, y + 21, x + w, y + 22, BORDER_DIM);
        ctx.drawText(tr, title, x + 6, y + 4, TEXT_TITLE, false);
        if (subtitle != null && !subtitle.isEmpty()) {
            int sw = tr.getWidth(subtitle);
            ctx.drawText(tr, subtitle, x + w - sw - 6, y + 5, TEXT_MUTED, false);
        }
    }

    public static void drawTab(DrawContext ctx, TextRenderer tr, String label, int x, int y, int w, int h,
                               boolean selected, boolean hovered) {
        int bg = selected ? 0xFF2A2A44 : (hovered ? 0xFF1E1E32 : 0xFF141422);
        ctx.fill(x, y, x + w, y + h, bg);
        if (selected) {
            ctx.fill(x, y + h - 2, x + w, y + h, ACCENT);
        }
        int tw = tr.getWidth(label);
        ctx.drawText(tr, label, x + (w - tw) / 2, y + (h - 8) / 2,
                selected ? TEXT_TITLE : TEXT_LABEL, false);
    }

    public static void drawProgressBar(DrawContext ctx, int x, int y, int w, int h, float fraction, boolean done) {
        ctx.fill(x, y, x + w, y + h, PROGRESS_BG);
        int fill = (int) (w * Math.min(1f, Math.max(0f, fraction)));
        if (fill > 0) {
            ctx.fill(x, y, x + fill, y + h, done ? PROGRESS_DONE : PROGRESS_FILL);
        }
    }

    public static void drawBorder(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }

    public static void drawTooltip(DrawContext ctx, TextRenderer tr, String text, int mx, int my) {
        if (text == null || text.isEmpty()) return;
        int tw = tr.getWidth(text);
        int th = 10;
        int tx = mx + 8;
        int ty = my - 14;
        ctx.fill(tx - 3, ty - 2, tx + tw + 3, ty + th + 2, 0xF0101020);
        UiTheme.drawBorder(ctx, tx - 3, ty - 2, tw + 6, th + 4, BORDER_DIM);
        ctx.drawText(tr, text, tx, ty, TEXT_TITLE, false);
    }
}
