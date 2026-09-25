/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;

import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.ITerminalHost;
import appeng.container.slot.SlotCraftingTerm;

/**
 * The crafting terminal's result slot, which also pays for an arcane craft: AE2 asks it once per item crafted
 * what the recipe leaves behind, and that is when the vis and the crystals go.
 */
public class SlotArcaneResult extends SlotCraftingTerm {

    private final ContainerArcaneTerminal terminal;

    public SlotArcaneResult(final EntityPlayer player, final IActionSource source, final IEnergySource energy,
            final ITerminalHost host, final IItemHandler grid, final IItemHandler output, final int x, final int y,
            final ContainerArcaneTerminal terminal) {
        super(player, source, energy, host, grid, grid, output, x, y, terminal);
        this.terminal = terminal;
    }

    @Override
    protected NonNullList<ItemStack> getRemainingItems(final InventoryCrafting grid, final World world) {
        final NonNullList<ItemStack> remaining = super.getRemainingItems(grid, world);
        if (!world.isRemote) {
            this.terminal.payFor(grid, world);
        }
        return remaining;
    }
}
