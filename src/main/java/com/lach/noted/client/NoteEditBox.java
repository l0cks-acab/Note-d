package com.lach.noted.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

/** EditBox without text shadow (1.20.1 has no setTextShadow). */
class NoteEditBox extends EditBox {
    NoteEditBox(Font font, int x, int y, int w, int h, Component msg) {
        super(font, x, y, w, h, msg);
    }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        Flat flat = new Flat(Minecraft.getInstance(), g.bufferSource());
        flat.pose().last().pose().set(g.pose().last().pose());
        super.renderWidget(flat, mouseX, mouseY, partialTick);
        flat.flush();
    }

    private static final class Flat extends GuiGraphics {
        Flat(Minecraft mc, net.minecraft.client.renderer.MultiBufferSource.BufferSource buffers) {
            super(mc, buffers);
        }

        @Override
        public int drawString(Font font, @Nullable String text, int x, int y, int color) {
            return drawString(font, text, x, y, color, false);
        }

        @Override
        public int drawString(Font font, FormattedCharSequence text, int x, int y, int color) {
            return drawString(font, text, x, y, color, false);
        }

        @Override
        public int drawString(Font font, Component text, int x, int y, int color) {
            return drawString(font, text, x, y, color, false);
        }
    }
}
