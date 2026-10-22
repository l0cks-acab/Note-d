package com.lach.noted;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

public record Note(String author, DyeColor color, List<String> lines, int slot) {
    public static final int LINES = 4;
    public static final Codec<Note> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("author", "").forGetter(Note::author),
            DyeColor.CODEC.optionalFieldOf("color", DyeColor.YELLOW).forGetter(Note::color),
            Codec.STRING.listOf().optionalFieldOf("lines", List.of()).forGetter(Note::lines),
            Codec.INT.optionalFieldOf("slot", -1).forGetter(Note::slot)
    ).apply(i, Note::new));

    public Note {
        lines = sanitize(lines);
    }

    public static Note fromStack(ItemStack stack, String author, int slot) {
        return new Note(author, stack.getOrDefault(Noted.COLOR.get(), DyeColor.YELLOW),
                stack.getOrDefault(Noted.TEXT.get(), List.of()), slot);
    }

    public ItemStack toStack() {
        ItemStack s = Noted.noteStack(color);
        if (!isBlank()) s.set(Noted.TEXT.get(), lines);
        return s;
    }

    public boolean isBlank() {
        return lines.stream().allMatch(String::isBlank);
    }

    public Note withLines(List<String> l) {
        return new Note(author, color, l, slot);
    }

    public Note withColor(DyeColor c) {
        return new Note(author, c, lines, slot);
    }

    public Note withSlot(int s) {
        return new Note(author, color, lines, s);
    }

    public Note withAuthor(String a) {
        return new Note(a, color, lines, slot);
    }

    public static List<String> sanitize(List<String> in) {
        List<String> out = new ArrayList<>(LINES);
        for (int i = 0; i < LINES; i++) {
            String s = i < in.size() ? ChatFormatting.stripFormatting(in.get(i)) : "";
            if (s == null) s = "";
            out.add(s.length() > Noted.MAX_LINE ? s.substring(0, Noted.MAX_LINE) : s);
        }
        return List.copyOf(out);
    }
}
