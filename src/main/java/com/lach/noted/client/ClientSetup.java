package com.lach.noted.client;

import com.lach.noted.Noted;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

@EventBusSubscriber(modid = Noted.MODID, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Noted.NOTE_BE.get(), StickyNoteRenderer::new);
    }

    @SubscribeEvent
    static void tintSources(RegisterColorHandlersEvent.ItemTintSources event) {
        event.register(Noted.id("note_color"), NoteTint.MAP_CODEC);
    }
}
