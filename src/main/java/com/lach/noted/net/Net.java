package com.lach.noted.net;

import com.lach.noted.Note;
import com.lach.noted.Noted;
import com.lach.noted.StickyNoteBlockEntity;
import com.lach.noted.client.ClientHooks;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.jetbrains.annotations.Nullable;

public final class Net {
    private static final String VERSION = "2";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(Noted.id("main"), () -> VERSION, VERSION::equals, VERSION::equals);

    private Net() {}

    public record OpenNotes(BlockPos pos, int slot) {}
    public record EditNote(BlockPos pos, int slot, List<String> lines) {}
    public record TearNote(BlockPos pos, int slot) {}

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(OpenNotes.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((m, b) -> b.writeBlockPos(m.pos()).writeVarInt(m.slot()))
                .decoder(b -> new OpenNotes(b.readBlockPos(), b.readVarInt()))
                .consumerMainThread(Net::onOpen).add();
        CHANNEL.messageBuilder(EditNote.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder((m, b) -> {
                    b.writeBlockPos(m.pos()).writeVarInt(m.slot());
                    b.writeCollection(m.lines(), (bb, s) -> bb.writeUtf(s, Noted.MAX_LINE * 4));
                })
                .decoder(b -> new EditNote(b.readBlockPos(), b.readVarInt(), readLines(b)))
                .consumerMainThread(Net::onEdit).add();
        CHANNEL.messageBuilder(TearNote.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder((m, b) -> b.writeBlockPos(m.pos()).writeVarInt(m.slot()))
                .decoder(b -> new TearNote(b.readBlockPos(), b.readVarInt()))
                .consumerMainThread(Net::onTear).add();
    }

    private static List<String> readLines(FriendlyByteBuf b) {
        int n = b.readVarInt();
        if (n < 0 || n > Note.LINES) throw new IllegalArgumentException("Too many note lines: " + n);
        List<String> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) out.add(b.readUtf(Noted.MAX_LINE * 4));
        return out;
    }

    public static void openNotes(ServerPlayer sp, BlockPos pos, int slot) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), new OpenNotes(pos, slot));
    }

    public static void sendToServer(Object msg) {
        CHANNEL.sendToServer(msg);
    }

    private static void onOpen(OpenNotes m, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openNotes(m.pos(), m.slot()));
    }

    private static void onEdit(EditNote m, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        StickyNoteBlockEntity be = reachable(player, m.pos());
        int i = be == null ? -1 : be.indexAt(m.slot());
        if (i < 0) return;
        Note n = be.getNotes().get(i);
        if (!n.lines().equals(Note.sanitize(m.lines()))) be.set(i, n.withLines(m.lines()));
    }

    private static void onTear(TearNote m, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        StickyNoteBlockEntity be = reachable(player, m.pos());
        int i = be == null ? -1 : be.indexAt(m.slot());
        if (i < 0) return;
        ItemStack stack = be.remove(i).toStack();
        if (!player.getInventory().add(stack)) player.drop(stack, false);
        player.level().playSound(null, m.pos(), SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1f, 1.2f);
        if (be.getNotes().isEmpty()) player.level().removeBlock(m.pos(), false);
    }

    @Nullable
    private static StickyNoteBlockEntity reachable(@Nullable ServerPlayer sp, BlockPos pos) {
        if (sp == null || !sp.mayBuild()) return null;
        var level = sp.level();
        if (!level.isLoaded(pos) || sp.distanceToSqr(Vec3.atCenterOf(pos)) > 64) return null;
        return level.getBlockEntity(pos) instanceof StickyNoteBlockEntity be ? be : null;
    }
}
