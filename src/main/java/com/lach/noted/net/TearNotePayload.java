package com.lach.noted.net;

import com.lach.noted.Noted;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record TearNotePayload(BlockPos pos, int slot) implements CustomPacketPayload {
    public static final Type<TearNotePayload> TYPE = new Type<>(Noted.id("tear_note"));
    public static final StreamCodec<ByteBuf, TearNotePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, TearNotePayload::pos, ByteBufCodecs.VAR_INT, TearNotePayload::slot, TearNotePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
