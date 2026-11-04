package com.lach.noted.client;

import com.lach.noted.Noted;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** Tints the note item by its {@code noted:color} component. */
public record NoteTint() implements ItemTintSource {
    public static final MapCodec<NoteTint> MAP_CODEC = MapCodec.unit(new NoteTint());

    @Override
    public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        return 0xFF000000 | stack.getOrDefault(Noted.COLOR.get(), DyeColor.YELLOW).getTextureDiffuseColor();
    }

    @Override
    public MapCodec<NoteTint> type() {
        return MAP_CODEC;
    }
}
