/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics.crafting;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.registries.IForgeRegistryEntry;

import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.crafting.IArcaneRecipe;

/**
 * An arcane recipe as a terminal's three-by-three grid sees it. Thaumcraft's recipes match only the workbench's
 * own fifteen slots, so the grid is set on one, crystals and all, each time the recipe is asked about.
 */
public final class ArcaneTerminalRecipe extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {

    private static final int GRID = 9;

    private final IArcaneRecipe recipe;

    public ArcaneTerminalRecipe(final IArcaneRecipe recipe) {
        this.recipe = recipe;
    }

    public IArcaneRecipe getArcane() {
        return this.recipe;
    }

    /**
     * The arcane recipe laid out in {@code grid} that {@code player} has researched, trying {@code last} first.
     */
    @Nullable
    public static IArcaneRecipe find(final InventoryCrafting grid, final EntityPlayer player,
            @Nullable final IArcaneRecipe last) {
        if (ArcaneGrid.isEmpty(grid)) {
            return null;
        }
        final ArcaneGrid arcane = ArcaneGrid.of(grid);
        if (last != null && last.matches(arcane, player.world) && knows(player, last)) {
            return last;
        }
        for (final IRecipe recipe : CraftingManager.REGISTRY) {
            if (recipe instanceof IArcaneRecipe candidate && candidate.matches(arcane, player.world)
                    && knows(player, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean knows(final EntityPlayer player, final IArcaneRecipe recipe) {
        return ThaumcraftCapabilities.getKnowledge(player).isResearchKnown(recipe.getResearch());
    }

    @Override
    public boolean matches(final InventoryCrafting inv, final World world) {
        return this.recipe.matches(ArcaneGrid.of(inv), world);
    }

    @Override
    public ItemStack getCraftingResult(final InventoryCrafting inv) {
        return this.recipe.getCraftingResult(ArcaneGrid.of(inv));
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(final InventoryCrafting inv) {
        final NonNullList<ItemStack> all = this.recipe.getRemainingItems(ArcaneGrid.of(inv));
        final NonNullList<ItemStack> grid = NonNullList.withSize(inv.getSizeInventory(), ItemStack.EMPTY);
        for (int slot = 0; slot < Math.min(GRID, grid.size()); slot++) {
            grid.set(slot, all.get(slot));
        }
        return grid;
    }

    @Override
    public boolean canFit(final int width, final int height) {
        return this.recipe.canFit(width, height);
    }

    @Override
    public ItemStack getRecipeOutput() {
        return this.recipe.getRecipeOutput();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return this.recipe.getIngredients();
    }

    @Override
    public boolean isDynamic() {
        return true;
    }
}
