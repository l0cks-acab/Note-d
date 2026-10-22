package com.lach.noted.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;

public final class ClientHooks {
    private ClientHooks() {}

    public static void openNotes(BlockPos pos, int index) {
        Minecraft.getInstance().setScreen(new NotesScreen(pos, index));
    }

    public static int textColor(DyeColor bg) {
        int c = bg.getTextureDiffuseColor();
        double lum = (0.299 * (c >> 16 & 255) + 0.587 * (c >> 8 & 255) + 0.114 * (c & 255)) / 255.0;
        return lum < 0.45 ? 0xFFF0F0F0 : 0xFF202020;
    }
}
