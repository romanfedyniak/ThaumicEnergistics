/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics.item;

import java.util.Collections;
import java.util.Set;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import appeng.api.stacks.AEKeyType;
import appeng.items.storage.AbstractStorageCell;
import appeng.util.InventoryAdaptor;

import thaumicenergistics.EssentiaTier;
import thaumicenergistics.ThEItems;
import thaumicenergistics.me.EssentiaKeyType;

/**
 * An essentia cell: an essentia component in an essentia cell housing. Twelve aspects a cell, and the bytes per
 * aspect and idle drain the original mod's cells had, which are also AE2UD's fluid cells'.
 */
public class ItemEssentiaStorageCell extends AbstractStorageCell {

    private static final int TYPES = 12;

    private final EssentiaTier tier;

    public ItemEssentiaStorageCell(final EssentiaTier tier) {
        super(tier.universalComponent(), tier.kilobytes);
        this.tier = tier;
    }

    @Override
    public ItemStack getComponent() {
        return new ItemStack(ThEItems.COMPONENTS.get(this.tier));
    }

    @Override
    public int getBytesPerType(final ItemStack cellItem) {
        return this.tier.kilobytes * 8;
    }

    @Override
    public double getIdleDrain() {
        return 0.5 * (this.tier.ordinal() + 1);
    }

    @Override
    public Set<AEKeyType> getKeyTypes() {
        return Collections.singleton(EssentiaKeyType.INSTANCE);
    }

    @Override
    public int getTotalTypes(final ItemStack cellItem) {
        return TYPES;
    }

    @Override
    protected void dropEmptyStorageCellCase(final InventoryAdaptor ia, final EntityPlayer player) {
        final ItemStack extra = ia.addItems(new ItemStack(ThEItems.ESSENTIA_CELL_HOUSING));
        if (!extra.isEmpty()) {
            player.dropItem(extra, false);
        }
    }

    @Override
    public ItemStack getContainerItem(final ItemStack itemStack) {
        return new ItemStack(ThEItems.ESSENTIA_CELL_HOUSING);
    }
}
