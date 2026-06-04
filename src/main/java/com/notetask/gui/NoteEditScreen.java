package com.notetask.gui;

import com.notetask.data.Note;
import com.notetask.data.SaveData;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

public class NoteEditScreen extends Screen {

    private static final int PW = 300, PH = 210;

    private final Screen parent;
    private final Note   editTarget; // null = creating a new note

    private TextFieldWidget titleField;
    private SimpleTextArea  bodyArea;

    public NoteEditScreen(Screen parent, Note editTarget) {
        super(Text.literal(editTarget == null ? "New note" : "Edit note"));
        this.parent     = parent;
        this.editTarget = editTarget;
    }

    private int pl() { return (this.width  - PW) / 2; }
    private int pt() { return (this.height - PH) / 2; }

    @Override
    protected void init() {
        int px = pl(), py = pt();

        // Title field
        titleField = new TextFieldWidget(textRenderer, px + 4, py + 36, PW - 8, 20, Text.empty());
        titleField.setMaxLength(100);
        titleField.setPlaceholder(Text.literal("Note title…"));
        if (editTarget != null) titleField.setText(editTarget.title);
        addDrawableChild(titleField);

        // Multiline body (12 lines, 110 px tall)
        bodyArea = new SimpleTextArea(textRenderer, px + 4, py + 68, PW - 8, 110, 12);
        if (editTarget != null) bodyArea.setText(editTarget.body);

        // Save / Cancel
        addDrawableChild(ButtonWidget.builder(Text.literal("Save"), b -> save())
                .dimensions(px + 4, py + PH - 26, 80, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"),
                b -> this.client.setScreen(parent))
                .dimensions(px + 90, py + PH - 26, 72, 20).build());
    }

    private void save() {
        String title = titleField.getText().trim();
        String body  = bodyArea.getText();
        if (title.isEmpty()) title = "(Untitled)";

        if (editTarget == null) {
            SaveData.getNotes().add(new Note(title, body));
        } else {
            editTarget.title = title;
            editTarget.body  = body;
        }
        SaveData.save();
        this.client.setScreen(parent);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        int px = pl(), py = pt();

        // Overlay + panel
        ctx.fill(0, 0, this.width, this.height, 0x99000000);
        ctx.fill(px, py, px + PW, py + PH, 0xEE0E0E1C);
        drawBorder(ctx, px, py, PW, PH, 0xFF5555AA);

        // Labels
        String screenTitle = editTarget == null ? "New note" : "Edit note";
        ctx.drawText(textRenderer, screenTitle, px + 5, py + 5,  0xFFAAAAFF, false);
        ctx.drawText(textRenderer, "Title:",    px + 4, py + 26, 0xFF7788AA, false);
        ctx.drawText(textRenderer, "Body:",     px + 4, py + 58, 0xFF7788AA, false);

        // Render the body text area (manually managed widget)
        bodyArea.render(ctx);

        // Render standard widgets (title field, buttons)
        super.render(ctx, mx, my, delta);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        boolean handled = bodyArea.mouseClicked(click.x(), click.y());
        if (handled) {
            titleField.setFocused(false);
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (bodyArea.isFocused() && bodyArea.charTyped(input.codepoint())) {
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (bodyArea.isFocused() && bodyArea.keyPressed(input.key(), input.scancode(), input.modifiers())) {
            return true;
        }
        return super.keyPressed(input);
    }

    private void drawBorder(DrawContext ctx, int x, int y, int w, int h, int c) {
        ctx.fill(x,       y,       x + w,     y + 1,     c);
        ctx.fill(x,       y + h-1, x + w,     y + h,     c);
        ctx.fill(x,       y,       x + 1,     y + h,     c);
        ctx.fill(x + w-1, y,       x + w,     y + h,     c);
    }

    @Override public boolean shouldPause() { return false; }
}
