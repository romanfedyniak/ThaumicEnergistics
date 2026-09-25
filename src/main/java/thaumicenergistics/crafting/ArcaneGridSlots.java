/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics.crafting;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import thaumcraft.api.crafting.IArcaneRecipe;

import appeng.api.networking.crafting.FabricatedSlots;
import appeng.api.stacks.GenericStack;

/**
 * Where Thaumcraft's own workbench screen puts its grid and its crystals, measured from the corner of its
 * texture, and what a grid laid out there would make.
 */
public final class ArcaneGridSlots {

    private static final int GRID = 40;
    private static final int GRID_STEP = 24;
    /** In {@link ArcaneGrid#PRIMALS} order. */
    private static final int[] CRYSTAL_X = { 64, 17, 112, 17, 112, 64 };
    private static final int[] CRYSTAL_Y = { 13, 35, 35, 93, 93, 115 };

    private ArcaneGridSlots() {
    }

    public static int gridX(final int square) {
        return GRID + GRID_STEP * (square % 3);
    }

    public static int gridY(final int square) {
        return GRID + GRID_STEP * (square / 3);
    }

    public static int crystalX(final int index) {
        return CRYSTAL_X[index];
    }

    public static int crystalY(final int index) {
        return CRYSTAL_Y[index];
    }

    /** What encoding the grid would write for this player: the recipe, what it makes, the squares it fills. */
    public static Preview preview(final IItemHandler grid, final EntityPlayer player) {
        final InventoryCrafting squares = ArcaneEncodingMode.squaresOf(grid);
        final IArcaneRecipe recipe = ArcaneTerminalRecipe.find(squares, player, null);
        final boolean[] fabricated = new boolean[9];
        if (recipe == null) {
            return new Preview(null, ItemStack.EMPTY, fabricated);
        }
        final GenericStack[] filled = FabricatedSlots.find(squares, new ArcaneTerminalRecipe(recipe));
        for (int square = 0; square < fabricated.length && square < filled.length; square++) {
            fabricated[square] = filled[square] != null;
        }
        return new Preview(recipe, recipe.getCraftingResult(ArcaneGrid.of(squares)), fabricated);
    }

    public static final class Preview {

        @Nullable
        private final IArcaneRecipe recipe;
        private final ItemStack result;
        private final boolean[] fabricated;

        Preview(@Nullable final IArcaneRecipe recipe, final ItemStack result, final boolean[] fabricated) {
            this.recipe = recipe;
            this.result = result;
            this.fabricated = fabricated;
        }

        @Nullable
        public IArcaneRecipe recipe() {
            return this.recipe;
        }

        public ItemStack result() {
            return this.result;
        }

        /** Per square: whether the network would fill it with the container's contents. */
        public boolean[] fabricated() {
            return this.fabricated;
        }
    }
}
