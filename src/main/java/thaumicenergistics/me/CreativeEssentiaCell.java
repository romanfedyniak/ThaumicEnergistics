/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics.me;

import java.util.Collections;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

import thaumcraft.api.aspects.Aspect;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.IStorageSink;
import appeng.api.storage.cells.CellState;
import appeng.api.storage.cells.ICellHandler;
import appeng.api.storage.cells.ISaveProvider;
import appeng.api.storage.cells.StorageCell;

import thaumicenergistics.ThEItems;

/**
 * The creative essentia cell's contents: every aspect without end, anything put in gone, and never a change to
 * report.
 */
public final class CreativeEssentiaCell implements StorageCell, IStorageSink {

    /** What AE2UD's creative cell shows, large enough to read as endless and small enough to add up. */
    private static final long STORED_AMOUNT = (1L << 52) - 1;

    private final ItemStack stack;

    private CreativeEssentiaCell(final ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public long insert(final AEKey what, final long amount, final Actionable mode, final IActionSource source) {
        return EssentiaKey.is(what) ? amount : 0;
    }

    @Override
    public long extract(final AEKey what, final long amount, final Actionable mode, final IActionSource source) {
        return EssentiaKey.is(what) ? amount : 0;
    }

    @Override
    public void getAvailableStacks(final KeyCounter out) {
        for (final Aspect aspect : Aspect.aspects.values()) {
            out.add(EssentiaKey.of(aspect), STORED_AMOUNT);
        }
    }

    @Override
    public boolean isPreferredStorageFor(final AEKey what, final IActionSource source) {
        return EssentiaKey.is(what);
    }

    @Override
    public Set<AEKeyType> getSupportedKeyTypes() {
        return Collections.singleton(EssentiaKeyType.INSTANCE);
    }

    @Override
    public CellState getStatus() {
        return CellState.TYPES_FULL;
    }

    @Override
    public double getIdleDrain() {
        return 0;
    }

    @Override
    public boolean canFitInsideCell() {
        return false;
    }

    @Override
    public void persist() {
        // Nothing changes, so nothing to save.
    }

    @Override
    public ITextComponent getDescription() {
        return new TextComponentString(this.stack.getDisplayName());
    }

    public static final class Handler implements ICellHandler {

        @Override
        public boolean isCell(final ItemStack is) {
            return !is.isEmpty() && is.getItem() == ThEItems.CREATIVE_ESSENTIA_CELL;
        }

        @Nullable
        @Override
        public StorageCell getCellInventory(final ItemStack is, @Nullable final ISaveProvider host) {
            return this.isCell(is) ? new CreativeEssentiaCell(is) : null;
        }
    }
}
