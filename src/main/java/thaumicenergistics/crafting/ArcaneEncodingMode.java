/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics.crafting;

import java.util.Collections;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.items.IItemHandler;

import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.crafting.ContainerDummy;
import thaumcraft.api.crafting.IArcaneRecipe;

import appeng.api.patterns.IPatternEncodingHost;
import appeng.api.patterns.PatternEncodingMode;
import appeng.api.patterns.PatternGrid;
import appeng.api.patterns.RecipePlacement;
import appeng.api.patterns.TransferredRecipe;
import appeng.api.stacks.GenericStack;
import appeng.util.helpers.ItemHandlerUtil;

import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.item.ItemArcanePattern;

/**
 * Arcane patterns, written in any of AE2's pattern terminals. The player draws the recipe's nine squares; the
 * crystals are read off the recipe, and only a recipe the player has researched is written.
 */
public final class ArcaneEncodingMode extends PatternEncodingMode {

    public static final ResourceLocation ID = ThaumicEnergistics.id("arcane");
    public static final String GRID = "arcane";

    /** What the panel's two toggles send; the terminal keeps the answers, as it does for AE2's own. */
    public static final String TOGGLE_SUBSTITUTION = "substitute";
    public static final String TOGGLE_FLUID_SUBSTITUTION = "substitute_fluids";

    /** Thaumic JEI's screen of arcane workbench recipes. */
    private static final String CATEGORY = "THAUMCRAFT_ARCANE_WORKBENCH";

    private final Item pattern;
    private ItemStack icon = ItemStack.EMPTY;

    public ArcaneEncodingMode(final Item pattern) {
        super(ID, Collections.singletonList(PatternGrid.itemsOnly(GRID, 9, PatternGrid.Role.INPUT)));
        this.pattern = pattern;
    }

    @Override
    public ItemStack getIcon() {
        if (this.icon.isEmpty()) {
            this.icon = new ItemStack(BlocksTC.arcaneWorkbench);
        }
        return this.icon;
    }

    @Override
    public String getTranslationKey() {
        return "gui.thaumicenergistics.pattern_mode.arcane";
    }

    @Override
    public boolean claimsRecipeCategory(final String categoryUid) {
        return CATEGORY.equals(categoryUid);
    }

    /** Thaumic JEI lists the nine squares first, a shaped recipe padded out to three by three. */
    @Override
    public void placeRecipe(final TransferredRecipe recipe, final RecipePlacement placement) {
        final List<List<GenericStack>> inputs = recipe.getItemInputs();
        for (int square = 0; square < inputs.size() && square < 9; square++) {
            if (!inputs.get(square).isEmpty()) {
                placement.put(GRID, square, inputs.get(square));
            }
        }
    }

    @Override
    public void onAction(final IPatternEncodingHost host, final String action) {
        if (TOGGLE_SUBSTITUTION.equals(action)) {
            host.setSubstitution(!host.isSubstitution());
        } else if (TOGGLE_FLUID_SUBSTITUTION.equals(action)) {
            host.setFluidSubstitution(!host.isFluidSubstitution());
        }
    }

    @Override
    public boolean isPattern(final ItemStack stack) {
        return ItemArcanePattern.isEncoded(stack);
    }

    @Override
    public boolean load(final IPatternEncodingHost host, final ItemStack pattern) {
        if (!ItemArcanePattern.isEncoded(pattern)) {
            return false;
        }
        final ItemStack[] squares = ItemArcanePattern.gridOf(pattern);
        final IItemHandler grid = host.getEncodingGrid(this, GRID);
        for (int square = 0; square < grid.getSlots(); square++) {
            ItemHandlerUtil.setStackInSlot(grid, square, square < squares.length ? squares[square] : ItemStack.EMPTY);
        }
        // A pattern read back sets the toggles it was written with, so encoding it again writes the same ones.
        host.setSubstitution(ItemArcanePattern.substitutesOf(pattern));
        host.setFluidSubstitution(ItemArcanePattern.fluidSubstitutesOf(pattern));
        return true;
    }

    @Override
    public ItemStack encode(final IPatternEncodingHost host, final EntityPlayer player) {
        final InventoryCrafting squares = squaresOf(host.getEncodingGrid(this, GRID));
        final IArcaneRecipe recipe = ArcaneTerminalRecipe.find(squares, player, null);
        if (recipe == null) {
            return ItemStack.EMPTY;
        }
        final ItemStack[] grid = new ItemStack[9];
        for (int square = 0; square < grid.length; square++) {
            grid[square] = squares.getStackInSlot(square);
        }
        return ItemArcanePattern.encode(this.pattern, grid, recipe.getCraftingResult(ArcaneGrid.of(squares)),
                recipe.getCrystals(), host.isSubstitution(), host.isFluidSubstitution(), player.getName());
    }

    public static InventoryCrafting squaresOf(final IItemHandler grid) {
        final InventoryCrafting squares = new InventoryCrafting(new ContainerDummy(), 3, 3);
        for (int square = 0; square < 9 && square < grid.getSlots(); square++) {
            squares.setInventorySlotContents(square, grid.getStackInSlot(square).copy());
        }
        return squares;
    }
}
