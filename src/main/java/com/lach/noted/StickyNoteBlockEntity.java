package com.lach.noted;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class StickyNoteBlockEntity extends BlockEntity {
    public static final int MAX_NOTES = 9;

    private final List<Note> notes = new ArrayList<>();
    private boolean glowing;

    public StickyNoteBlockEntity(BlockPos pos, BlockState state) {
        super(Noted.NOTE_BE.get(), pos, state);
    }

    public List<Note> getNotes() {
        return Collections.unmodifiableList(notes);
    }

    public boolean isFull() {
        return notes.size() >= MAX_NOTES;
    }

    public boolean isGlowing() {
        return glowing;
    }

    /** Index of the note in the given grid slot, or -1. */
    public int indexAt(int slot) {
        for (int i = 0; i < notes.size(); i++) if (notes.get(i).slot() == slot) return i;
        return -1;
    }

    /** The requested slot if free, else the nearest free one, or -1 when full. */
    public int freeSlot(int preferred) {
        int best = -1, bestDist = Integer.MAX_VALUE;
        for (int s = 0; s < MAX_NOTES; s++) {
            if (indexAt(s) >= 0) continue;
            int dist = Math.abs(s / 3 - preferred / 3) + Math.abs(s % 3 - preferred % 3);
            if (dist < bestDist) {
                best = s;
                bestDist = dist;
            }
        }
        return best;
    }

    /** Adds a note in (or near) its slot; returns its index, or -1 when full. */
    public int add(Note note) {
        int slot = freeSlot(note.slot() < 0 ? 4 : note.slot());
        if (slot < 0) return -1;
        Note placed = note.withSlot(slot);
        notes.add(placed);
        sortNotes();
        changed();
        return notes.indexOf(placed);
    }

    private void sortNotes() {
        notes.sort(Comparator.comparingInt(Note::slot));
    }

    public void set(int index, Note note) {
        if (index < 0 || index >= notes.size()) return;
        notes.set(index, note);
        sortNotes();
        changed();
    }

    public Note remove(int index) {
        Note n = notes.remove(index);
        changed();
        return n;
    }

    public void setGlowing(boolean glow) {
        glowing = glow;
        changed();
    }

    private void changed() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        Note.CODEC.listOf().encodeStart(NbtOps.INSTANCE, notes).ifSuccess(t -> tag.put("notes", t));
        tag.putBoolean("glowing", glowing);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        notes.clear();
        if (tag.contains("notes"))
            Note.CODEC.listOf().parse(NbtOps.INSTANCE, tag.get("notes")).ifSuccess(l -> {
                for (Note n : l) {
                    int slot = n.slot() >= 0 && n.slot() < MAX_NOTES && indexAt(n.slot()) < 0 ? n.slot() : freeSlot(4);
                    if (slot >= 0) notes.add(n.withSlot(slot));
                }
            });
        sortNotes();
        glowing = tag.getBoolean("glowing");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput input) {
        notes.clear();
        notes.add(new Note("", input.getOrDefault(Noted.COLOR.get(), net.minecraft.world.item.DyeColor.YELLOW),
                input.getOrDefault(Noted.TEXT.get(), List.of()), 4));
    }
}
