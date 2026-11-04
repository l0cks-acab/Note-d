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
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

public class StickyNoteRenderer implements BlockEntityRenderer<StickyNoteBlockEntity, StickyNoteRenderer.State> {
    private static final float CELL = 1 / 3f;
    private static final float HALF = 2.5f / 16f;
    private static final float LAYER = 0.003f;
    private static final float TEXT_SCALE = 0.0036f;
    private static final int MAX_WIDTH = 76;
    private final Font font;

    public static class State extends BlockEntityRenderState {
        List<Note> notes = List.of();
        boolean glowing;
        @Nullable Quaternionf rotation;
        int seed;
    }

    public StickyNoteRenderer(BlockEntityRendererProvider.Context ctx) {
        this.font = ctx.font();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(StickyNoteBlockEntity be, State state, float partialTicks, Vec3 camera,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, camera, breakProgress);
        BlockState bs = be.getBlockState();
        state.notes = List.copyOf(be.getNotes());
        state.glowing = be.isGlowing();
        state.rotation = bs.getBlock() instanceof StickyNoteBlock ? StickyNoteBlock.faceRotation(bs) : null;
        state.seed = be.getBlockPos().hashCode();
    }

    @Override
    public void submit(State state, PoseStack ps, SubmitNodeCollector out, CameraRenderState camera) {
        if (state.notes.isEmpty() || state.rotation == null) return;
        ps.pushPose();
        ps.translate(0.5, 0.5, 0.5);
        ps.mulPose(state.rotation);
        ps.translate(0, 0, -0.5 + LAYER);

        TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(Noted.id("block/sticky_note"));
        int light = state.lightCoords;
        int textLight = state.glowing ? LightCoordsUtil.FULL_BRIGHT : light;
        for (Note note : state.notes) {
            int slot = note.slot();
            ps.pushPose();
            ps.translate((slot % 3 - 1) * CELL, (1 - slot / 3) * CELL, 0);
            jitter(ps, slot, state.seed);
            int argb = note.color().getTextureDiffuseColor();
            out.submitCustomGeometry(ps, Sheets.cutoutBlockSheet(), (pose, vc) -> quad(pose, vc, sprite, argb, light));
            text(ps, out, note, textLight);
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

    private static void quad(PoseStack.Pose pose, VertexConsumer vc, TextureAtlasSprite s, int argb, int light) {
        float u0 = s.getU(2 / 16f), u1 = s.getU(14 / 16f), v0 = s.getV(2 / 16f), v1 = s.getV(14 / 16f);
        int c = 0xFF000000 | argb, ov = OverlayTexture.NO_OVERLAY;
        vc.addVertex(pose, -HALF, -HALF, 0).setColor(c).setUv(u0, v1).setOverlay(ov).setLight(light).setNormal(pose, 0, 0, 1);
        vc.addVertex(pose, HALF, -HALF, 0).setColor(c).setUv(u1, v1).setOverlay(ov).setLight(light).setNormal(pose, 0, 0, 1);
        vc.addVertex(pose, HALF, HALF, 0).setColor(c).setUv(u1, v0).setOverlay(ov).setLight(light).setNormal(pose, 0, 0, 1);
        vc.addVertex(pose, -HALF, HALF, 0).setColor(c).setUv(u0, v0).setOverlay(ov).setLight(light).setNormal(pose, 0, 0, 1);
    }

    private void text(PoseStack ps, SubmitNodeCollector out, Note note, int light) {
        ps.translate(0, 0, LAYER / 2);
        ps.scale(TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);
        int color = ClientHooks.textColor(note.color());
        int lineH = 10;
        float y = -note.lines().size() * lineH / 2f + 4;
        for (String line : note.lines()) {
            int w = font.width(line);
            float squeeze = w > MAX_WIDTH ? (float) MAX_WIDTH / w : 1f;
            ps.pushPose();
            ps.translate(0, y + 4, 0);
            ps.scale(squeeze, squeeze, 1f);
            out.submitText(ps, -w / 2f, -4, FormattedCharSequence.forward(line, net.minecraft.network.chat.Style.EMPTY), false,
                    Font.DisplayMode.POLYGON_OFFSET, light, color, 0, 0);
            ps.popPose();
            y += lineH;
        }
    }
}
