/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics.migration;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.common.util.Constants;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaContainerItem;

import appeng.api.migration.LegacyRewrites;
import appeng.api.migration.LegacyStacks;
import appeng.api.stacks.GenericStack;

import thaumicenergistics.ThEItems;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.crafting.ArcaneEncodingMode;
import thaumicenergistics.item.ItemArcanePattern;
import thaumicenergistics.me.EssentiaKey;
import thaumicenergistics.me.EssentiaKeyType;

/**
 * What Thaumic Energistics left in a world saved on AE2 UEL, from the Extended Life line and the original: essentia
 * entries and placeholders, essentia cells, the essentia parts that AE2UD's own parts replace, and knowledge cores,
 * which become arcane patterns.
 */
public final class UelEssentiaMigration {

    /**
     * Patterns that came out of one knowledge core beyond the first, handed to whoever carries it.
     */
    public static final String UNPACK = ThaumicEnergistics.MODID + ":unpack";

    private static final String AE = "appliedenergistics2:";
    private static final String MEMORY_CARD_PART = "item.appliedenergistics2.multi_part.";
    private static final String BLANK_PATTERN = AE + "material";
    private static final int BLANK_PATTERN_META = 52;

    private UelEssentiaMigration() {
    }

    public static void register() {
        LegacyStacks.addReader(stack -> stack.hasKey("Aspect", Constants.NBT.TAG_STRING)
                ? essentia(stack.getString("Aspect"), stack.hasKey("Cnt") ? stack.getLong("Cnt") : stack.getLong("Amount"))
                : null);
        LegacyStacks.addWrapper(id("dummy_aspect"), stack -> essentia(stack.getCompoundTag("tag").getString("aspect"), 1));

        for (final String tier : new String[] { "1k", "4k", "16k", "64k" }) {
            // The cell's partition was "filter"; AE2UD's is "list".
            LegacyRewrites.addStackRewriter(id("essentia_cell_" + tier), stack -> {
                final NBTTagCompound tag = stack.getCompoundTag("tag");
                if (tag.hasKey("filter") && !tag.hasKey("list")) {
                    tag.setTag("list", tag.getTag("filter"));
                    tag.removeTag("filter");
                }
            });
            LegacyStacks.addCell(id("essentia_cell_" + tier));
        }

        part("essentia_storage", 220, part -> {
            filter(part, "config");
            upgrades(part, "upgrades");
            part.setTag("enabledKeyTypes", onlyEssentia());
        });
        part("essentia_import", 240, part -> {
            filter(part, "config");
            upgrades(part, "upgrades");
            part.setTag("enabledKeyTypes", onlyEssentia());
        });
        part("essentia_export", 260, part -> {
            filter(part, "config");
            upgrades(part, "upgrades");
        });
        part("essentia_level_emitter", 280, part -> filter(part, "config"));
        part("essentia_terminal", 380, part -> part.setTag("enabledKeyTypes", onlyEssentia()));
        part("arcane_inscriber", 340, UelEssentiaMigration::inscriber);
        LegacyRewrites.addPartRewriter(id("arcane_terminal"), (item, part) -> arcaneTerminal(part));

        LegacyRewrites.remapItem(id("wireless_essentia_terminal"), AE + "wireless_terminal");
        LegacyRewrites.addStackRewriter(id("wireless_essentia_terminal"), stack -> {
            final NBTTagCompound tag = stack.getCompoundTag("tag");
            tag.setTag("enabledKeyTypes", onlyEssentia());
            stack.setTag("tag", tag);
        });

        LegacyRewrites.addStackRewriter(id("knowledge_core"), UelEssentiaMigration::knowledgeCore);
        LegacyRewrites.remapItem(id("blank_knowledge_core"), BLANK_PATTERN);
        LegacyRewrites.addStackRewriter(id("blank_knowledge_core"),
                stack -> stack.setShort("Damage", (short) BLANK_PATTERN_META));

        LegacyRewrites.remove(id("essentia_interface"));
        LegacyRewrites.addTileRewriter(id("essentia_interface"), tile -> {
            LegacyRewrites.log().lost("essentia interface" + at(tile) + ", with its side settings");
            return tile;
        });
        LegacyRewrites.addTileRewriter(id("arcane_assembler"), UelEssentiaMigration::assembler);
        LegacyRewrites.addStackRewriter(AE + "memory_card", UelEssentiaMigration::memoryCard);
    }

    @Nullable
    private static GenericStack essentia(final String tag, final long amount) {
        final Aspect aspect = Aspect.getAspect(tag);
        return aspect == null ? null : new GenericStack(EssentiaKey.of(aspect), amount);
    }

    /**
     * An essentia part becomes the AE2UD part at {@code meta}, its item when held and its data on a cable bus.
     */
    private static void part(final String oldId, final int meta, final PartData rewrite) {
        LegacyRewrites.remapItem(id(oldId), AE + "part");
        LegacyRewrites.addStackRewriter(id(oldId), stack -> stack.setShort("Damage", (short) meta));
        LegacyRewrites.addPartRewriter(id(oldId), (item, part) -> rewrite.rewrite(part));
    }

    /**
     * {@code {count, "aspect#0": tag, ...}}, an empty slot without its key, into AE2UD's filter slots.
     */
    private static void filter(final NBTTagCompound data, final String key) {
        final NBTTagCompound filter = data.getCompoundTag(key);
        if (!filter.hasKey("count")) {
            return;
        }
        final NBTTagCompound slots = new NBTTagCompound();
        for (final String name : filter.getKeySet()) {
            if (!name.startsWith("aspect#")) {
                continue;
            }
            final GenericStack read = essentia(filter.getString(name), 1);
            if (read == null) {
                LegacyRewrites.log().lost("essentia filter, unknown aspect " + filter.getString(name));
                continue;
            }
            final NBTTagCompound slot = new NBTTagCompound();
            GenericStack.writeTag(slot, read);
            slots.setTag("#" + name.substring("aspect#".length()), slot);
        }
        data.setTag(key, slots);
    }

    /**
     * Thaumic Energistics' inventories were a list by position, an empty slot as air; AE2UD's are
     * {@code {Items: [{Slot, ...}], Size}}.
     */
    private static void upgrades(final NBTTagCompound data, final String key) {
        if (data.hasKey(key, Constants.NBT.TAG_LIST)) {
            data.setTag(key, inventory(stacks(data.getTagList(key, Constants.NBT.TAG_COMPOUND)), 0, Integer.MAX_VALUE));
        }
    }

    private static NBTTagCompound inventory(final List<NBTTagCompound> stacks, final int from, final int to) {
        final NBTTagList items = new NBTTagList();
        for (int slot = from; slot < Math.min(to, stacks.size()); slot++) {
            final NBTTagCompound stack = stacks.get(slot);
            if (!isAir(stack)) {
                final NBTTagCompound item = stack.copy();
                item.setInteger("Slot", slot - from);
                items.appendTag(item);
            }
        }
        final NBTTagCompound inventory = new NBTTagCompound();
        inventory.setTag("Items", items);
        inventory.setInteger("Size", Math.max(0, Math.min(to, stacks.size()) - from));
        return inventory;
    }

    private static List<NBTTagCompound> stacks(final NBTTagList list) {
        final List<NBTTagCompound> stacks = new ArrayList<>();
        for (int i = 0; i < list.tagCount(); i++) {
            stacks.add(list.getCompoundTagAt(i));
        }
        return stacks;
    }

    private static boolean isAir(final NBTTagCompound stack) {
        return stack.isEmpty() || "minecraft:air".equals(stack.getString("id")) || stack.getInteger("Count") <= 0;
    }

    /**
     * Squares 0-8 are the grid; 9-14 held real crystals, which the terminal now takes from the network.
     */
    private static void arcaneTerminal(final NBTTagCompound part) {
        if (part.hasKey("crafting", Constants.NBT.TAG_LIST)) {
            final List<NBTTagCompound> crafting = stacks(part.getTagList("crafting", Constants.NBT.TAG_COMPOUND));
            part.setTag("craftingGrid", inventory(crafting, 0, 9));
            for (int slot = 9; slot < crafting.size(); slot++) {
                if (!isAir(crafting.get(slot))) {
                    LegacyRewrites.spill(crafting.get(slot));
                }
            }
            part.removeTag("crafting");
        }
        upgrades(part, "upgrades");
    }

    /**
     * The inscriber becomes AE2UD's pattern terminal writing arcane patterns. Its grid was all ghosts; the core in
     * its card slot comes out as patterns.
     */
    private static void inscriber(final NBTTagCompound part) {
        if (part.hasKey("crafting", Constants.NBT.TAG_LIST)) {
            final NBTTagCompound modeGrids = new NBTTagCompound();
            final NBTTagCompound arcane = new NBTTagCompound();
            arcane.setTag(ArcaneEncodingMode.GRID,
                    inventory(stacks(part.getTagList("crafting", Constants.NBT.TAG_COMPOUND)), 0, 9));
            modeGrids.setTag(ArcaneEncodingMode.ID.toString(), arcane);
            part.setTag("modeGrids", modeGrids);
            part.removeTag("crafting");
        }
        for (final NBTTagCompound core : stacks(part.getTagList("upgrades", Constants.NBT.TAG_COMPOUND))) {
            spillCore(core);
        }
        part.removeTag("upgrades");
        part.setString("encodingMode", ArcaneEncodingMode.ID.toString());
    }

    /**
     * The assembler holds no patterns now, an interface beside it does: its core and a finished result it had not
     * handed back go into a chest.
     */
    private static NBTTagCompound assembler(final NBTTagCompound tile) {
        for (final NBTTagCompound core : stacks(tile.getTagList("cores", Constants.NBT.TAG_COMPOUND))) {
            spillCore(core);
        }
        tile.removeTag("cores");
        for (final NBTTagCompound result : stacks(tile.getTagList("crafting", Constants.NBT.TAG_COMPOUND))) {
            if (!isAir(result)) {
                LegacyRewrites.spill(result);
            }
        }
        tile.removeTag("crafting");
        upgrades(tile, "upgrades");
        return tile;
    }

    private static void spillCore(final NBTTagCompound core) {
        if (isAir(core)) {
            return;
        }
        if (core.getString("id").equals(id("blank_knowledge_core"))) {
            LegacyRewrites.spill(blankPattern());
            return;
        }
        final List<NBTTagCompound> patterns = patterns(core);
        if (patterns.isEmpty()) {
            LegacyRewrites.spill(blankPattern());
        }
        for (final NBTTagCompound pattern : patterns) {
            LegacyRewrites.spill(pattern);
        }
    }

    /**
     * A core held up to nine recipes; the first becomes this pattern and the rest ride along in it, until a player
     * carries it and gets them.
     */
    private static void knowledgeCore(final NBTTagCompound stack) {
        final List<NBTTagCompound> patterns = patterns(stack);
        final NBTTagCompound first = patterns.isEmpty() ? blankPattern() : patterns.get(0);
        if (patterns.size() > 1) {
            final NBTTagList rest = new NBTTagList();
            for (final NBTTagCompound pattern : patterns.subList(1, patterns.size())) {
                rest.appendTag(pattern);
            }
            first.getCompoundTag("tag").setTag(UNPACK, rest);
        }
        for (final String key : new ArrayList<>(stack.getKeySet())) {
            stack.removeTag(key);
        }
        stack.merge(first);
    }

    /**
     * Recipes {@code "0"} to {@code "8"}: nine squares, five crystals and what it makes.
     */
    private static List<NBTTagCompound> patterns(final NBTTagCompound core) {
        final List<NBTTagCompound> patterns = new ArrayList<>();
        if (ThEItems.ARCANE_PATTERN == null) {
            if (!core.getCompoundTag("tag").isEmpty()) {
                LegacyRewrites.log().lost("knowledge core, arcane autocrafting is switched off: " + core);
            }
            return patterns;
        }
        final NBTTagCompound recipes = core.getCompoundTag("tag");
        for (int n = 0; n < 9; n++) {
            if (!recipes.hasKey(String.valueOf(n), Constants.NBT.TAG_COMPOUND)) {
                continue;
            }
            final NBTTagCompound recipe = recipes.getCompoundTag(String.valueOf(n));
            final NBTTagList ingredients = recipe.getTagList("ingredients", Constants.NBT.TAG_COMPOUND);
            final ItemStack[] grid = new ItemStack[9];
            for (int slot = 0; slot < 9; slot++) {
                grid[slot] = slot < ingredients.tagCount() ? new ItemStack(ingredients.getCompoundTagAt(slot)) : ItemStack.EMPTY;
            }
            final AspectList crystals = new AspectList();
            for (int slot = 9; slot < ingredients.tagCount(); slot++) {
                final ItemStack crystal = new ItemStack(ingredients.getCompoundTagAt(slot));
                if (crystal.getItem() instanceof IEssentiaContainerItem) {
                    final AspectList aspects = ((IEssentiaContainerItem) crystal.getItem()).getAspects(crystal);
                    if (aspects != null) {
                        for (final Aspect aspect : aspects.getAspects()) {
                            crystals.add(aspect, crystal.getCount());
                        }
                    }
                }
            }
            final ItemStack pattern = ItemArcanePattern.encode(ThEItems.ARCANE_PATTERN, grid,
                    new ItemStack(recipe.getCompoundTag("result")), crystals, false, false, "");
            pattern.getTagCompound().removeTag("author");
            patterns.add(pattern.writeToNBT(new NBTTagCompound()));
        }
        return patterns;
    }

    private static NBTTagCompound blankPattern() {
        final NBTTagCompound blank = new NBTTagCompound();
        blank.setString("id", BLANK_PATTERN);
        blank.setByte("Count", (byte) 1);
        blank.setShort("Damage", (short) BLANK_PATTERN_META);
        return blank;
    }

    /**
     * The card belongs to AE2UD, which rewrites its own part of it; this rewrites what an essentia part put there.
     */
    private static void memoryCard(final NBTTagCompound stack) {
        final NBTTagCompound tag = stack.getCompoundTag("tag");
        final String source = tag.getString("Config");
        if (!source.contains("." + ThaumicEnergistics.MODID + ".")) {
            return;
        }
        final String name = source.substring(source.lastIndexOf('.') + 1);
        final String part;
        switch (name) {
            case "essentia_import":
                part = "import_bus";
                break;
            case "essentia_export":
                part = "export_bus";
                break;
            case "essentia_storage":
                part = "storage_bus";
                break;
            case "essentia_level_emitter":
                part = "level_emitter";
                break;
            default:
                part = null;
                break;
        }
        if (part == null) {
            if (name.equals("essentia_interface")) {
                LegacyRewrites.log().lost("memory card holding an essentia interface's settings");
                tag.removeTag("Config");
                tag.removeTag("Data");
            }
            return;
        }
        tag.setString("Config", MEMORY_CARD_PART + part);
        filter(tag.getCompoundTag("Data"), "config");
    }

    private static NBTTagList onlyEssentia() {
        final NBTTagList types = new NBTTagList();
        types.appendTag(new NBTTagString(EssentiaKeyType.INSTANCE.getId().toString()));
        return types;
    }

    private static String at(final NBTTagCompound tile) {
        return " at " + tile.getInteger("x") + " " + tile.getInteger("y") + " " + tile.getInteger("z");
    }

    private static String id(final String name) {
        return ThaumicEnergistics.MODID + ":" + name;
    }

    @FunctionalInterface
    private interface PartData {
        void rewrite(NBTTagCompound part);
    }
}
