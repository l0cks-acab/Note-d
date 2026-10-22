package com.lach.noted;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

public class StickyNoteItem extends BlockItem {
    public StickyNoteItem(Block block, Properties props) {
        super(block, props);
    }

    @Override
    public Component getName(ItemStack stack) {
        DyeColor c = stack.getOrDefault(Noted.COLOR.get(), DyeColor.YELLOW);
        return Component.translatable("item.noted.sticky_note.colored", Component.translatable("color.minecraft." + c.getName()));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        for (String line : stack.getOrDefault(Noted.TEXT.get(), List.<String>of()))
            if (!line.isBlank()) tooltip.add(Component.literal(line).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
