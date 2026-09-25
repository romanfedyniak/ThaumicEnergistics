/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics.part;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.IItemHandler;

import appeng.api.implementations.IUpgradeableHost;
import appeng.api.parts.IPartModel;
import appeng.api.upgrades.CardTrait;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import appeng.core.sync.GuiBridge;
import appeng.parts.PartModel;
import appeng.parts.reporting.AbstractPartTerminal;
import appeng.tile.inventory.AppEngInternalInventory;

import thaumicenergistics.ThEGuis;
import thaumicenergistics.ThaumicEnergistics;

/**
 * A crafting terminal for arcane recipes. Its grid is the workbench's; the crystals come out of the network and
 * the vis out of the aura where it stands.
 */
public class PartArcaneTerminal extends AbstractPartTerminal implements IUpgradeableHost {

    public static final PartModel MODELS_OFF = new PartModel(MODEL_BASE,
            ThaumicEnergistics.id("part/arcane_terminal/off"), MODEL_STATUS_OFF);
    public static final PartModel MODELS_ON = new PartModel(MODEL_BASE,
            ThaumicEnergistics.id("part/arcane_terminal/on"), MODEL_STATUS_ON);
    public static final PartModel MODELS_HAS_CHANNEL = new PartModel(MODEL_BASE,
            ThaumicEnergistics.id("part/arcane_terminal/on"), MODEL_STATUS_HAS_CHANNEL);

    private final AppEngInternalInventory craftingGrid = new AppEngInternalInventory(this, 9);
    private final IUpgradeInventory upgrades;

    public PartArcaneTerminal(final ItemStack is) {
        super(is);
        this.upgrades = UpgradeInventories.forMachine(is, 1, changed -> this.getHost().markForSave());
    }

    @Override
    public void getDrops(final List<ItemStack> drops, final boolean wrenched) {
        super.getDrops(drops, wrenched);
        for (final ItemStack is : this.craftingGrid) {
            if (!is.isEmpty()) {
                drops.add(is);
            }
        }
        for (int slot = 0; slot < this.upgrades.getSlots(); slot++) {
            final ItemStack card = this.upgrades.getStackInSlot(slot);
            if (!card.isEmpty()) {
                drops.add(card);
            }
        }
    }

    @Override
    public void readFromNBT(final NBTTagCompound data) {
        super.readFromNBT(data);
        this.craftingGrid.readFromNBT(data, "craftingGrid");
        this.upgrades.readFromNBT(data, "upgrades");
    }

    @Override
    public void writeToNBT(final NBTTagCompound data) {
        super.writeToNBT(data);
        this.craftingGrid.writeToNBT(data, "craftingGrid");
        this.upgrades.writeToNBT(data, "upgrades");
    }

    @Override
    public GuiBridge getGui(final EntityPlayer player) {
        return this.getGuiBridge();
    }

    @Override
    public GuiBridge getGuiBridge() {
        return ThEGuis.arcaneTerminal();
    }

    @Override
    public IItemHandler getInventoryByName(final String name) {
        if (name.equals("crafting")) {
            return this.craftingGrid;
        }
        if (name.equals("upgrades")) {
            return this.upgrades;
        }
        return super.getInventoryByName(name);
    }

    @Override
    public int getInstalledUpgrades(final ItemStack upgradeCard) {
        return this.upgrades.getInstalledUpgrades(upgradeCard);
    }

    @Override
    public int getInstalledPoints(final CardTrait trait) {
        return this.upgrades.getInstalledPoints(trait);
    }

    @Override
    public IPartModel getStaticModels() {
        return this.selectModel(MODELS_OFF, MODELS_ON, MODELS_HAS_CHANNEL);
    }
}
