package com.lach.noted.client;

import com.lach.noted.Note;
import com.lach.noted.StickyNoteBlockEntity;
import com.lach.noted.Noted;
import com.lach.noted.net.EditNotePayload;
import com.lach.noted.net.TearNotePayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public class NotesScreen extends Screen {
    private static final int SIZE = 120;
    private static final int TIMEOUT_TICKS = 40;

    private final BlockPos pos;
    private int index;
    @Nullable
    private Note loaded;
    private int waited;
    private boolean torn;
    private final EditBox[] boxes = new EditBox[Note.LINES];
    private Button prev, next;
    private int left, top;

    public NotesScreen(BlockPos pos, int index) {
        super(Component.translatable("screen.noted.notes"));
        this.pos = pos;
        this.index = index;
    }

    @Nullable
    private StickyNoteBlockEntity be() {
        return minecraft != null && minecraft.level != null && minecraft.level.getBlockEntity(pos) instanceof StickyNoteBlockEntity be ? be : null;
    }

    private int count() {
        StickyNoteBlockEntity be = be();
        return be == null ? 0 : be.getNotes().size();
    }

    @Override
    protected void init() {
        left = (width - SIZE) / 2;
        top = (height - SIZE) / 2 - 16;
        for (int i = 0; i < boxes.length; i++) {
            String prevValue = boxes[i] != null ? boxes[i].getValue() : "";
            EditBox b = new EditBox(font, left + 10, top + 34 + i * 16, SIZE - 20, 12, Component.empty());
            b.setBordered(false);
            b.setTextShadow(false);
            b.setMaxLength(Noted.MAX_LINE);
            b.setValue(prevValue);
            b.setEditable(loaded != null);
            boxes[i] = addRenderableWidget(b);
        }
        prev = addRenderableWidget(Button.builder(Component.literal("<"), b -> page(-1)).bounds(left - 26, top + SIZE / 2 - 10, 20, 20).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), b -> page(1)).bounds(left + SIZE + 6, top + SIZE / 2 - 10, 20, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.noted.tear"), b -> tear())
                .bounds(width / 2 - 102, top + SIZE + 10, 100, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(width / 2 + 2, top + SIZE + 10, 100, 20).build());
        if (loaded != null) applyStyle(loaded.color());
        updateButtons();
        setInitialFocus(boxes[0]);
    }

    private void load() {
        StickyNoteBlockEntity be = be();
        if (be == null || index < 0 || index >= be.getNotes().size()) return;
        loaded = be.getNotes().get(index);
        for (int i = 0; i < boxes.length; i++) {
            boxes[i].setValue(loaded.lines().get(i));
            boxes[i].setEditable(true);
        }
        applyStyle(loaded.color());
        updateButtons();
    }

    private void applyStyle(DyeColor color) {
        for (EditBox b : boxes) b.setTextColor(ClientHooks.textColor(color));
    }

    private void updateButtons() {
        if (prev == null) return;
        prev.active = loaded != null && index > 0;
        next.active = loaded != null && index < count() - 1;
    }

    private List<String> current() {
        List<String> lines = new ArrayList<>();
        for (EditBox b : boxes) lines.add(b.getValue());
        return lines;
    }

    private void save() {
        if (loaded == null || torn) return;
        List<String> lines = Note.sanitize(current());
        if (lines.equals(loaded.lines())) return;
        PacketDistributor.sendToServer(new EditNotePayload(pos, index, lines));
        loaded = loaded.withLines(lines);
    }

    private void page(int delta) {
        save();
        int target = index + delta;
        if (target < 0 || target >= count()) return;
        index = target;
        loaded = null;
        load();
        setFocused(boxes[0]);
    }

    private void tear() {
        if (loaded == null) return;
        torn = true;
        PacketDistributor.sendToServer(new TearNotePayload(pos, index));
        onClose();
    }

    @Override
    public void tick() {
        if (be() == null) {
            onClose();
            return;
        }
        if (loaded == null) {
            load();
            if (loaded == null && ++waited > TIMEOUT_TICKS) onClose();
        }
        updateButtons();
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        int n = count();
        int rgb = loaded == null ? 0xDDDDDD : loaded.color().getTextureDiffuseColor() & 0xFFFFFF;
        for (int i = Math.min(n - 1, 3); i > 0; i--)
            g.fill(left + i * 3, top + i * 3, left + SIZE + i * 3, top + SIZE + i * 3, 0xFF000000 | darken(rgb, 0.85f - i * 0.1f));
        g.fill(left, top, left + SIZE, top + SIZE, 0xFF000000 | rgb);
        g.fill(left, top, left + SIZE, top + 18, 0x30000000);
        g.drawCenteredString(font, title, width / 2, top - 14, 0xFFFFFF);
        if (loaded == null) return;
        int tc = ClientHooks.textColor(loaded.color());
        g.drawString(font, (index + 1) + "/" + n, left + 6, top + 5, tc, false);
        if (!loaded.author().isEmpty()) {
            Component by = Component.translatable("screen.noted.by", loaded.author());
            g.drawString(font, by, left + SIZE - 6 - font.width(by), top + 5, tc, false);
        }
    }

    private static int darken(int rgb, float f) {
        int r = (int) ((rgb >> 16 & 255) * f), gr = (int) ((rgb >> 8 & 255) * f), b = (int) ((rgb & 255) * f);
        return r << 16 | gr << 8 | b;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        int focused = -1;
        for (int i = 0; i < boxes.length; i++) if (boxes[i] == getFocused()) focused = i;
        if (focused >= 0) {
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER || key == GLFW.GLFW_KEY_DOWN) {
                if (focused == boxes.length - 1 && key != GLFW.GLFW_KEY_DOWN) onClose();
                else setFocused(boxes[Math.min(focused + 1, boxes.length - 1)]);
                return true;
            }
            if (key == GLFW.GLFW_KEY_UP) {
                setFocused(boxes[Math.max(focused - 1, 0)]);
                return true;
            }
            if (key == GLFW.GLFW_KEY_PAGE_UP) {
                page(-1);
                return true;
            }
            if (key == GLFW.GLFW_KEY_PAGE_DOWN) {
                page(1);
                return true;
            }
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void removed() {
        save();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
