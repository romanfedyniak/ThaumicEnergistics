/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics.container;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraftforge.items.IItemHandler;

import thaumcraft.api.aspects.AspectList;

import appeng.api.config.SecurityPermissions;
import appeng.container.guisync.GuiSync;
import appeng.container.implementations.ContainerUpgradeable;
import appeng.container.slot.SlotRestrictedInput;
import appeng.util.Platform;

import thaumicenergistics.crafting.ArcaneGrid;
import thaumicenergistics.crafting.ArcanePatternDetails;
import thaumicenergistics.tile.TileArcaneAssembler;

/**
 * The assembler's window: its cards, the network tool's own cards when the player carries one, and what the craft
 * it is on costs.
 */
public class ContainerArcaneAssembler extends ContainerUpgradeable {

    /** Where the original's texture draws the column of card slots. */
    public static final int CARDS_X = 186;
    public static final int CARDS_Y = 8;
    /** The network tool's three by three, right of the player's inventory, where the original put it. */
    public static final int TOOLBOX_X = 187;
    public static final int TOOLBOX_Y = 150;
    private static final int HEIGHT = 231;

    @GuiSync(21)
    public long visCost;
    @GuiSync(22)
    public long visAvailable;
    /** One bit per primal, in {@link ArcaneGrid#PRIMALS} order, set for each crystal the craft takes. */
    @GuiSync(23)
    public long crystals;
    /** Whether the craft's vis is already taken, after which the aura no longer matters to it. */
    @GuiSync(24)
    public boolean paid;

    public ContainerArcaneAssembler(final InventoryPlayer inventory, final TileArcaneAssembler assembler) {
        super(inventory, assembler);
    }

    @Override
    protected int getHeight() {
        return HEIGHT;
    }

    @Override
    protected int getToolboxX() {
        return TOOLBOX_X;
    }

    @Override
    protected int getToolboxY() {
        return TOOLBOX_Y;
    }

    @Override
    public int availableUpgrades() {
        return TileArcaneAssembler.CARD_SLOTS;
    }

    @Override
    protected void setupConfig() {
        final IItemHandler upgrades = this.getUpgradeable().getInventoryByName("upgrades");
        for (int slot = 0; slot < this.availableUpgrades(); slot++) {
            this.addSlotToContainer(new SlotRestrictedInput(SlotRestrictedInput.PlacableItemType.UPGRADES,
                    upgrades, slot, CARDS_X, CARDS_Y + slot * 18, this.getInventoryPlayer()).setNotDraggable());
        }
    }

    /** The assembler has no settings to read back, only what the craft costs. */
    @Override
    public void detectAndSendChanges() {
        this.verifyPermissions(SecurityPermissions.BUILD, false);
        if (Platform.isServer()) {
            final TileArcaneAssembler assembler = (TileArcaneAssembler) this.getUpgradeable();
            this.visCost = assembler.getVisCost();
            this.visAvailable = assembler.getVisAvailable();
            this.crystals = crystalsOf(assembler.getPlan());
            this.paid = assembler.isPaid();
        }
        this.checkToolbox();
        this.standardDetectAndSendChanges();
    }

    private static long crystalsOf(final ArcanePatternDetails plan) {
        if (plan == null) {
            return 0;
        }
        final AspectList crystals = plan.getCrystals();
        long mask = 0;
        for (int index = 0; index < ArcaneGrid.PRIMALS.length; index++) {
            if (crystals.getAmount(ArcaneGrid.PRIMALS[index]) > 0) {
                mask |= 1L << index;
            }
        }
        return mask;
    }
}
