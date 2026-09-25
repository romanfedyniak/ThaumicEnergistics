/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics.item;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

import appeng.api.AEApi;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.helpers.ItemStackHelper;

import thaumicenergistics.crafting.ArcanePatternDetails;

/**
 * A pattern for an arcane workbench recipe, kept apart from AE2's own so that only an Arcane Assembler takes it.
 * It holds the recipe's nine squares, what it makes and the crystals it takes, read off the recipe when it was
 * written; the vis is paid by whatever makes it.
 */
public class ItemArcanePattern extends Item implements ICraftingPatternItem {

    private static final String INPUTS = "in";
    private static final String OUTPUT = "out";
    private static final String CRYSTALS = "crystals";
    private static final String SUBSTITUTE = "sub";
    private static final String FLUID_SUBSTITUTE = "subfl";
    /** The key AE2 reads its own patterns' author from, so its tooltip line reads this one's too. */
    private static final String AUTHOR = "author";
    private static final int GRID = 9;

    /** No creative tab: one of these only ever exists encoded. */
    public ItemArcanePattern() {
        this.setMaxStackSize(64);
    }

    public static ItemStack encode(final Item item, final ItemStack[] grid, final ItemStack result,
            @Nullable final AspectList crystals, final boolean substitute, final boolean substituteFluids,
            final String author) {
        final NBTTagList inputs = new NBTTagList();
        for (int slot = 0; slot < GRID; slot++) {
            final NBTTagCompound entry = new NBTTagCompound();
            if (slot < grid.length && !grid[slot].isEmpty()) {
                final ItemStack one = grid[slot].copy();
                one.setCount(1);
                one.writeToNBT(entry);
            }
            inputs.appendTag(entry);
        }

        final NBTTagCompound encoded = new NBTTagCompound();
        encoded.setTag(INPUTS, inputs);
        encoded.setTag(OUTPUT, ItemStackHelper.stackToNBT(result));
        final NBTTagCompound aspects = new NBTTagCompound();
        if (crystals != null) {
            for (final Aspect aspect : crystals.getAspects()) {
                if (aspect != null && crystals.getAmount(aspect) > 0) {
                    aspects.setInteger(aspect.getTag(), crystals.getAmount(aspect));
                }
            }
        }
        encoded.setTag(CRYSTALS, aspects);
        if (substitute) {
            encoded.setBoolean(SUBSTITUTE, true);
        }
        if (substituteFluids) {
            encoded.setBoolean(FLUID_SUBSTITUTE, true);
        }
        encoded.setString(AUTHOR, author);

        final ItemStack pattern = new ItemStack(item);
        pattern.setTagCompound(encoded);
        return pattern;
    }

    public static boolean isEncoded(final ItemStack stack) {
        return stack.getItem() instanceof ItemArcanePattern && stack.getTagCompound() != null
                && stack.getTagCompound().hasKey(OUTPUT);
    }

    /** The nine squares, empty where nothing was drawn. */
    public static ItemStack[] gridOf(final ItemStack stack) {
        final ItemStack[] grid = new ItemStack[GRID];
        final NBTTagCompound tag = stack.getTagCompound();
        final NBTTagList inputs = tag == null ? new NBTTagList()
                : tag.getTagList(INPUTS, Constants.NBT.TAG_COMPOUND);
        for (int slot = 0; slot < GRID; slot++) {
            grid[slot] = slot < inputs.tagCount() ? new ItemStack(inputs.getCompoundTagAt(slot)) : ItemStack.EMPTY;
        }
        return grid;
    }

    public static ItemStack resultOf(final ItemStack stack) {
        final NBTTagCompound tag = stack.getTagCompound();
        return tag == null ? ItemStack.EMPTY : ItemStackHelper.stackFromNBT(tag.getCompoundTag(OUTPUT));
    }

    public static AspectList crystalsOf(final ItemStack stack) {
        final AspectList crystals = new AspectList();
        final NBTTagCompound tag = stack.getTagCompound();
        if (tag != null) {
            final NBTTagCompound aspects = tag.getCompoundTag(CRYSTALS);
            for (final String key : aspects.getKeySet()) {
                final Aspect aspect = Aspect.getAspect(key);
                if (aspect != null) {
                    crystals.add(aspect, aspects.getInteger(key));
                }
            }
        }
        return crystals;
    }

    public static boolean substitutesOf(final ItemStack stack) {
        return stack.getTagCompound() != null && stack.getTagCompound().getBoolean(SUBSTITUTE);
    }

    public static boolean fluidSubstitutesOf(final ItemStack stack) {
        return stack.getTagCompound() != null && stack.getTagCompound().getBoolean(FLUID_SUBSTITUTE);
    }

    @Nullable
    @Override
    public ICraftingPatternDetails getPatternForItem(final ItemStack is, final World w) {
        try {
            return new ArcanePatternDetails(is, w);
        } catch (final IllegalArgumentException e) {
            return null;
        }
    }

    /** AE2's own lines, so an arcane pattern reads as a crafting one does, in every language. */
    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(final ItemStack stack, @Nullable final World world, final List<String> lines,
            final ITooltipFlag flag) {
        AEApi.instance().client().addPatternInformation(this.getPatternForItem(stack, world), stack, lines);
    }
}
