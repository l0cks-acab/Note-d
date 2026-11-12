package com.lach.noted;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

public class StickyNoteItem extends BlockItem {
    /** Where the player clicked while placing, so the first note lands in the clicked grid slot. */
    static final ThreadLocal<Vec3> CLICK = new ThreadLocal<>();

    public StickyNoteItem(Block block, Properties props) {
        super(block, props);
    }

    @Override
    public InteractionResult place(BlockPlaceContext ctx) {
        CLICK.set(ctx.getClickLocation());
        try {
            return super.place(ctx);
        } finally {
            CLICK.remove();
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        DyeColor c = stack.getOrDefault(Noted.COLOR.get(), DyeColor.YELLOW);
        return Component.translatable("item.noted.sticky_note.colored", Component.translatable("color.minecraft." + c.getName()));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        for (String line : stack.getOrDefault(Noted.TEXT.get(), List.<String>of()))
            if (!line.isBlank()) tooltip.accept(Component.literal(line).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
