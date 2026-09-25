/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics.crafting;

import net.minecraft.inventory.InventoryCrafting;

import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.crafting.ContainerDummy;
import thaumcraft.api.crafting.IArcaneWorkbench;

/**
 * The arcane workbench's fifteen slots as Thaumcraft's recipes read them: the grid, then one crystal slot for each
 * primal aspect. The crystals are never real here - every slot holds more than any recipe asks - since a terminal
 * takes them from its network and says itself which are missing.
 */
public final class ArcaneGrid extends InventoryCrafting implements IArcaneWorkbench {

    /** In the order Thaumcraft's own workbench lays its crystal slots out. */
    public static final Aspect[] PRIMALS = { Aspect.AIR, Aspect.FIRE, Aspect.WATER, Aspect.EARTH, Aspect.ORDER,
            Aspect.ENTROPY };

    private static final int GRID = 9;
    private static final int PLENTY = 64;

    private ArcaneGrid() {
        super(new ContainerDummy(), 5, 3);
    }

    /** The workbench as it would stand with {@code grid}'s nine squares on it. */
    public static ArcaneGrid of(final InventoryCrafting grid) {
        final ArcaneGrid arcane = new ArcaneGrid();
        for (int slot = 0; slot < GRID; slot++) {
            arcane.setInventorySlotContents(slot, grid.getStackInSlot(slot));
        }
        for (int crystal = 0; crystal < PRIMALS.length; crystal++) {
            arcane.setInventorySlotContents(GRID + crystal, ThaumcraftApiHelper.makeCrystal(PRIMALS[crystal], PLENTY));
        }
        return arcane;
    }

    public static boolean isEmpty(final InventoryCrafting grid) {
        for (int slot = 0; slot < GRID; slot++) {
            if (!grid.getStackInSlot(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
