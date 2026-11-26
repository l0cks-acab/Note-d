# Changelog

## Note'd 0.1.1+forge-1.20.1

**Forge 1.20.1**
- Ported to Minecraft 1.20.1 / Forge 47. Needs Java 17.
- Same features as 0.1.1: sticky notes on any block face, up to 9 per face, read/edit screen with paging, Tear Off, dye to recolor, glow ink for glowing text, notes drop with their text when broken.
- Recipes use `forge:slimeballs` and `forge:dyes/*` tags. Note color and text are stored in the item's `noted` NBT tag.

## Note'd 0.1.1 / 0.1.1+26.1.2

**Fixes (both versions)**
- The note screen, edits and Tear Off now track a note by its grid slot instead of its list position. Before, if someone else added or removed a note on the same face while you had the screen open, your edit or tear could hit the wrong note, and a freshly added note could open the wrong page.
- Tear Off keeps any text you typed but hadn't saved yet.
- The note screen closes if someone else tears your note off.
- The first note on a face now lands in the clicked slot when placed by a deployer or other fake player (was always the centre).

**26.1.2**
- Fixed the Gradle build (toolchain plugin too old for Gradle 9).

## Note'd 0.1.0+26.1.2

**Minecraft 26.1.2**
- Ported to Minecraft 26.1.2 / NeoForge 26.1.2. Needs Java 25.
- Same features as 0.1.0: sticky notes on any block face, up to 9 per face, read/edit screen with paging, Tear Off, dye to recolor, glow ink for glowing text, notes drop with their text when broken.
- The 1.21.1 build stays at 0.1.0.

## Note'd 0.1.0

**First release (Minecraft 1.21.1)**
- Craft: 2 paper + 1 slimeball -> 6 notes. Note + dye -> recolor.
- Up to 9 notes per block face in a 3x3 grid, placed where you click.
- Right-click a note to read/edit it (4 lines, shows author). Page through the face's notes, or Tear Off to take one back.
- Dye a placed note to recolor it; glow ink sac makes the text glow, ink sac removes it.
- Breaking the block drops every note with its text.
- Long lines shrink to fit the note instead of being cut off.
