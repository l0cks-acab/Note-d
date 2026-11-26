package com.lach.noted;

import com.lach.noted.net.Net;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

@Mod(Noted.MODID)
public class Noted {
    public static final String MODID = "noted";
    public static final int MAX_LINE = 20;
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<net.minecraft.world.level.block.Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final RegistryObject<StickyNoteBlock> NOTE = BLOCKS.register("sticky_note", () -> new StickyNoteBlock(StickyNoteBlock.PROPERTIES));
    public static final RegistryObject<StickyNoteItem> NOTE_ITEM = ITEMS.register("sticky_note", () -> new StickyNoteItem(NOTE.get(), new Item.Properties()));
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<StickyNoteBlockEntity>> NOTE_BE = BLOCK_ENTITIES.register("sticky_note",
            () -> BlockEntityType.Builder.of(StickyNoteBlockEntity::new, NOTE.get()).build(null));

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + MODID))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> new ItemStack(NOTE_ITEM.get()))
            .displayItems((params, out) -> {
                for (DyeColor c : DyeColor.values()) out.accept(noteStack(c));
            })
            .build());

    /** Item NBT: {noted: {color: "red", text: ["..", ..]}} (stands in for the 1.21 data components). */
    public static final String TAG = "noted";

    public Noted() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        TABS.register(modBus);
        Net.register();
        LOGGER.info("{} loaded", MODID);
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MODID, path);
    }

    public static ItemStack noteStack(DyeColor color) {
        ItemStack s = new ItemStack(NOTE_ITEM.get());
        setColor(s, color);
        return s;
    }

    public static DyeColor getColor(ItemStack stack) {
        CompoundTag t = stack.getTagElement(TAG);
        return t == null ? DyeColor.YELLOW : DyeColor.byName(t.getString("color"), DyeColor.YELLOW);
    }

    public static void setColor(ItemStack stack, DyeColor color) {
        stack.getOrCreateTagElement(TAG).putString("color", color.getName());
    }

    public static List<String> getText(ItemStack stack) {
        CompoundTag t = stack.getTagElement(TAG);
        List<String> out = new ArrayList<>();
        if (t != null) for (Tag e : t.getList("text", Tag.TAG_STRING)) out.add(e.getAsString());
        return out;
    }

    public static void setText(ItemStack stack, List<String> lines) {
        ListTag l = new ListTag();
        for (String s : lines) l.add(StringTag.valueOf(s));
        stack.getOrCreateTagElement(TAG).put("text", l);
    }

    /** Opaque RGB of a dye's texture tint (1.21's getTextureDiffuseColor). */
    public static int rgb(DyeColor c) {
        float[] f = c.getTextureDiffuseColors();
        return (int) (f[0] * 255) << 16 | (int) (f[1] * 255) << 8 | (int) (f[2] * 255);
    }
}
