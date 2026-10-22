package com.lach.noted.net;

import com.lach.noted.Note;
import com.lach.noted.Noted;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record EditNotePayload(BlockPos pos, int index, List<String> lines) implements CustomPacketPayload {
    public static final Type<EditNotePayload> TYPE = new Type<>(Noted.id("edit_note"));
    public static final StreamCodec<ByteBuf, EditNotePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, EditNotePayload::pos,
            ByteBufCodecs.VAR_INT, EditNotePayload::index,
            ByteBufCodecs.stringUtf8(Noted.MAX_LINE * 4).apply(ByteBufCodecs.list(Note.LINES)), EditNotePayload::lines,
            EditNotePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
