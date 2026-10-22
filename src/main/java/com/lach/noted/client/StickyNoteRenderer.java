package com.lach.noted.client;

import com.lach.noted.Note;
import com.lach.noted.StickyNoteBlock;
import com.lach.noted.StickyNoteBlockEntity;
import com.lach.noted.Noted;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;

public class StickyNoteRenderer implements BlockEntityRenderer<StickyNoteBlockEntity> {
    private static final float CELL = 1 / 3f;
    private static final float HALF = 2.5f / 16f;
    private static final float LAYER = 0.003f;
    private static final float TEXT_SCALE = 0.0036f;
    private static final int MAX_WIDTH = 72;
    private final Font font;

    public StickyNoteRenderer(BlockEntityRendererProvider.Context ctx) {
        this.font = ctx.getFont();
    }

    @Override
    public void render(StickyNoteBlockEntity be, float partialTick, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        List<Note> notes = be.getNotes();
        if (notes.isEmpty()) return;
        BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof StickyNoteBlock)) return;

        ps.pushPose();
        ps.translate(0.5, 0.5, 0.5);
        ps.mulPose(StickyNoteBlock.faceRotation(state));
        ps.translate(0, 0, -0.5 + LAYER);

        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(Noted.id("block/sticky_note"));
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS));
        int textLight = be.isGlowing() ? LightTexture.FULL_BRIGHT : light;
        for (Note note : notes) {
            int slot = note.slot();
            ps.pushPose();
            ps.translate((slot % 3 - 1) * CELL, (1 - slot / 3) * CELL, 0);
            jitter(ps, slot, be.getBlockPos().hashCode());
            quad(ps, vc, sprite, note.color().getTextureDiffuseColor(), light, overlay);
            text(ps, buffers, note, textLight);
            ps.popPose();
        }
        ps.popPose();
    }

    private static void jitter(PoseStack ps, int slot, int seed) {
        long h = ((slot + 1) * 0x9E3779B97F4A7C15L ^ seed * 0xC2B2AE3D27D4EB4FL) >>> 40;
        float angle = ((h & 0xFF) / 255f - 0.5f) * 14f;
        float dx = (((h >> 8) & 0xFF) / 255f - 0.5f) * 0.03f;
        float dy = (((h >> 16) & 0xFF) / 255f - 0.5f) * 0.03f;
        ps.translate(dx, dy, 0);
        ps.mulPose(Axis.ZP.rotationDegrees(angle));
    }

    private static void quad(PoseStack ps, VertexConsumer vc, TextureAtlasSprite s, int argb, int light, int overlay) {
        PoseStack.Pose pose = ps.last();
        float u0 = s.getU(2 / 16f), u1 = s.getU(14 / 16f), v0 = s.getV(2 / 16f), v1 = s.getV(14 / 16f);
        int c = 0xFF000000 | argb;
        vc.addVertex(pose, -HALF, -HALF, 0).setColor(c).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, 1);
        vc.addVertex(pose, HALF, -HALF, 0).setColor(c).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, 1);
        vc.addVertex(pose, HALF, HALF, 0).setColor(c).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, 1);
        vc.addVertex(pose, -HALF, HALF, 0).setColor(c).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, 1);
    }

    private void text(PoseStack ps, MultiBufferSource buffers, Note note, int light) {
        ps.translate(0, 0, LAYER / 2);
        ps.scale(TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);
        int color = ClientHooks.textColor(note.color());
        int lineH = 10;
        float y = -note.lines().size() * lineH / 2f + 4;
        for (String line : note.lines()) {
            String l = font.plainSubstrByWidth(line, MAX_WIDTH);
            font.drawInBatch(l, -font.width(l) / 2f, y, color, false, ps.last().pose(), buffers, Font.DisplayMode.POLYGON_OFFSET, 0, light);
            y += lineH;
        }
    }
}
