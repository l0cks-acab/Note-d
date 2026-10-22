package com.lach.noted.net;

import com.lach.noted.Noted;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record OpenNotesPayload(BlockPos pos, int index) implements CustomPacketPayload {
    public static final Type<OpenNotesPayload> TYPE = new Type<>(Noted.id("open_notes"));
    public static final StreamCodec<ByteBuf, OpenNotesPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, OpenNotesPayload::pos, ByteBufCodecs.VAR_INT, OpenNotesPayload::index, OpenNotesPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
