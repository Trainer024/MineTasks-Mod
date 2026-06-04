package com.notetask.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * A lightweight multiline text area that handles Enter, Backspace, Delete,
 * and arrow-key navigation. Used in the note editor screen.
 */
public class SimpleTextArea {

    private final TextRenderer tr;
    private final int x, y, w, h;
    private final int maxLines;
    private static final int LINE_H = 11;

    private final List<String> lines = new ArrayList<>();
    private int curLine = 0;
    private int curChar = 0;
    private boolean focused = false;

    public SimpleTextArea(TextRenderer tr, int x, int y, int w, int h, int maxLines) {
        this.tr       = tr;
        this.x        = x;
        this.y        = y;
        this.w        = w;
        this.h        = h;
        this.maxLines = maxLines;
        lines.add("");
    }

    // ─── Render ──────────────────────────────────────────────────────────────

    public void render(DrawContext ctx) {
        // Background
        ctx.fill(x, y, x + w, y + h, focused ? 0xCC0D1525 : 0xCC080C18);

        // Border
        int bc = focused ? 0xFF5577AA : 0xFF334466;
        ctx.fill(x,       y,       x + w,     y + 1,     bc);
        ctx.fill(x,       y + h-1, x + w,     y + h,     bc);
        ctx.fill(x,       y,       x + 1,     y + h,     bc);
        ctx.fill(x + w-1, y,       x + w,     y + h,     bc);

        // Text lines
        for (int i = 0; i < lines.size(); i++) {
            int ty = y + 3 + i * LINE_H;
            if (ty + LINE_H > y + h) break; // clip
            ctx.drawText(tr, lines.get(i), x + 3, ty, 0xFFCCCCFF, false);
        }

        // Blinking cursor
        if (focused && (System.currentTimeMillis() / 500) % 2 == 0 && curLine < lines.size()) {
            String before = lines.get(curLine).substring(0, safeMin(curChar, lines.get(curLine).length()));
            int cx = x + 3 + tr.getWidth(before);
            int cy = y + 3 + curLine * LINE_H;
            ctx.fill(cx, cy, cx + 1, cy + LINE_H - 1, 0xFFFFFFFF);
        }
    }

    // ─── Input ───────────────────────────────────────────────────────────────

    public boolean mouseClicked(double mx, double my) {
        boolean inBounds = mx >= x && mx < x + w && my >= y && my < y + h;
        focused = inBounds;
        if (inBounds) {
            int clickedLine = (int)((my - y - 3) / LINE_H);
            clickedLine = safeMin(Math.max(0, clickedLine), lines.size() - 1);
            curLine = clickedLine;
            int relX = (int)(mx - x - 3);
            curChar = tr.trimToWidth(lines.get(curLine), relX).length();
        }
        return inBounds;
    }

    public boolean charTyped(int codepoint) {
        if (!focused || !Character.isValidCodePoint(codepoint) || Character.isISOControl(codepoint)) return false;
        if (curLine >= lines.size()) return false;
        String line = lines.get(curLine);
        String insert = new String(Character.toChars(codepoint));
        lines.set(curLine, line.substring(0, curChar) + insert + line.substring(curChar));
        curChar += insert.length();
        return true;
    }

    public boolean keyPressed(int key, int scan, int mods) {
        if (!focused) return false;
        switch (key) {
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                if (lines.size() < maxLines) {
                    String line = lines.get(curLine);
                    String rest = line.substring(curChar);
                    lines.set(curLine, line.substring(0, curChar));
                    lines.add(curLine + 1, rest);
                    curLine++;
                    curChar = 0;
                }
                return true;
            }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (curChar > 0) {
                    String line = lines.get(curLine);
                    lines.set(curLine, line.substring(0, curChar - 1) + line.substring(curChar));
                    curChar--;
                } else if (curLine > 0) {
                    String line    = lines.get(curLine);
                    int    prevLen = lines.get(curLine - 1).length();
                    lines.set(curLine - 1, lines.get(curLine - 1) + line);
                    lines.remove(curLine);
                    curLine--;
                    curChar = prevLen;
                }
                return true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                String line = lines.get(curLine);
                if (curChar < line.length()) {
                    lines.set(curLine, line.substring(0, curChar) + line.substring(curChar + 1));
                } else if (curLine < lines.size() - 1) {
                    lines.set(curLine, line + lines.get(curLine + 1));
                    lines.remove(curLine + 1);
                }
                return true;
            }
            case GLFW.GLFW_KEY_LEFT -> {
                if (curChar > 0) curChar--;
                else if (curLine > 0) { curLine--; curChar = lines.get(curLine).length(); }
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                if (curLine < lines.size()) {
                    if (curChar < lines.get(curLine).length()) curChar++;
                    else if (curLine < lines.size() - 1)      { curLine++; curChar = 0; }
                }
                return true;
            }
            case GLFW.GLFW_KEY_UP -> {
                if (curLine > 0) { curLine--; curChar = safeMin(curChar, lines.get(curLine).length()); }
                return true;
            }
            case GLFW.GLFW_KEY_DOWN -> {
                if (curLine < lines.size() - 1) { curLine++; curChar = safeMin(curChar, lines.get(curLine).length()); }
                return true;
            }
            case GLFW.GLFW_KEY_HOME -> { curChar = 0; return true; }
            case GLFW.GLFW_KEY_END  -> { curChar = lines.get(curLine).length(); return true; }
        }
        return false;
    }

    // ─── Accessors ───────────────────────────────────────────────────────────

    public String getText() { return String.join("\n", lines); }

    public void setText(String text) {
        lines.clear();
        if (text == null || text.isEmpty()) {
            lines.add("");
        } else {
            for (String line : text.split("\n", -1)) {
                lines.add(line);
            }
        }
        curLine = 0;
        curChar = 0;
    }

    public boolean isFocused()         { return focused; }
    public void    setFocused(boolean v) { focused = v; }

    private int safeMin(int a, int b) { return Math.min(a, b); }
}
