package com.lach.noted;

import com.lach.noted.client.ClientHooks;
import com.lach.noted.net.EditNotePayload;
import com.lach.noted.net.OpenNotesPayload;
import com.lach.noted.net.TearNotePayload;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

@Mod(Noted.MODID)
public class Noted {
    public static final String MODID = "noted";
    public static final int MAX_LINE = 20;
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<DyeColor>> COLOR = COMPONENTS.registerComponentType("color",
            b -> b.persistent(DyeColor.CODEC).networkSynchronized(DyeColor.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<String>>> TEXT = COMPONENTS.registerComponentType("text",
            b -> b.persistent(Codec.STRING.listOf()).networkSynchronized(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list())));

    public static final DeferredBlock<StickyNoteBlock> NOTE = BLOCKS.registerBlock("sticky_note", StickyNoteBlock::new, StickyNoteBlock.PROPERTIES);
    public static final DeferredItem<StickyNoteItem> NOTE_ITEM = ITEMS.registerItem("sticky_note", p -> new StickyNoteItem(NOTE.get(), p));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StickyNoteBlockEntity>> NOTE_BE = BLOCK_ENTITIES.register("sticky_note",
            () -> BlockEntityType.Builder.of(StickyNoteBlockEntity::new, NOTE.get()).build(null));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + MODID))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> NOTE_ITEM.get().getDefaultInstance())
            .displayItems((params, out) -> {
                for (DyeColor c : DyeColor.values()) out.accept(noteStack(c));
            })
            .build());

    public Noted(IEventBus modBus, ModContainer container) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        COMPONENTS.register(modBus);
        TABS.register(modBus);
        modBus.addListener(Noted::registerPayloads);
        LOGGER.info("{} loaded", MODID);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    public static ItemStack noteStack(DyeColor color) {
        ItemStack s = new ItemStack(NOTE_ITEM.get());
        s.set(COLOR.get(), color);
        return s;
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1");
        r.playToClient(OpenNotesPayload.TYPE, OpenNotesPayload.STREAM_CODEC, (p, ctx) -> ClientHooks.openNotes(p.pos(), p.index()));
        r.playToServer(EditNotePayload.TYPE, EditNotePayload.STREAM_CODEC, (p, ctx) -> {
            StickyNoteBlockEntity be = reachable(ctx.player(), p.pos());
            if (be == null || p.index() < 0 || p.index() >= be.getNotes().size()) return;
            Note n = be.getNotes().get(p.index());
            if (!n.lines().equals(Note.sanitize(p.lines()))) be.set(p.index(), n.withLines(p.lines()));
        });
        r.playToServer(TearNotePayload.TYPE, TearNotePayload.STREAM_CODEC, (p, ctx) -> {
            StickyNoteBlockEntity be = reachable(ctx.player(), p.pos());
            if (be == null || p.index() < 0 || p.index() >= be.getNotes().size()) return;
            Player player = ctx.player();
            ItemStack stack = be.remove(p.index()).toStack();
            if (!player.getInventory().add(stack)) player.drop(stack, false);
            player.level().playSound(null, p.pos(), SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1f, 1.2f);
            if (be.getNotes().isEmpty()) player.level().removeBlock(p.pos(), false);
        });
    }

    @Nullable
    private static StickyNoteBlockEntity reachable(Player player, BlockPos pos) {
        if (!(player instanceof ServerPlayer sp) || !sp.mayBuild()) return null;
        var level = sp.level();
        if (!level.isLoaded(pos) || sp.distanceToSqr(Vec3.atCenterOf(pos)) > 64) return null;
        return level.getBlockEntity(pos) instanceof StickyNoteBlockEntity be ? be : null;
    }
}
