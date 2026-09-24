/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics.me;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaTransport;

import appeng.api.behaviors.GenericInternalInventory;
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.parts.IPartHost;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.helpers.IInterfaceHost;
import appeng.tile.inventory.AppEngInternalAEInventory;

/**
 * What an ME interface is to Thaumcraft's tubes. With essentia stocked in its slots it is a source, as a jar is,
 * and tubes draw that essentia; with none it takes whatever a tube brings straight into the network, pulling
 * harder than any tube so nothing flows back out. A slot set to keep essentia makes it a source even while
 * that slot is empty: otherwise a tube drawing the last of it would turn the interface into a sink that takes
 * the essentia straight back.
 * <p>
 * Built by AE2UD for each interface, over the interface's own slots, and asked for by the mixin that makes the
 * interface block and the cable bus tubes' neighbours.
 */
public final class InterfaceEssentia implements IEssentiaTransport {

    @CapabilityInject(InterfaceEssentia.class)
    public static Capability<InterfaceEssentia> CAPABILITY;

    private static final int SINK_SUCTION = 128;
    private static final int SOURCE_SUCTION = -1;
    private static final int MINIMUM_SUCTION = 1;

    private final GenericInternalInventory inventory;
    private final Supplier<IStorageService> network;
    private final IActionSource source;
    @Nullable
    private AppEngInternalAEInventory config;

    public InterfaceEssentia(final GenericInternalInventory inventory, final Supplier<IStorageService> network,
            final IActionSource source) {
        this.inventory = inventory;
        this.network = network;
        this.source = source;
    }

    /** The interface on that face of the block, told what its slots are set to keep. */
    @Nullable
    public static InterfaceEssentia of(final TileEntity tile, final EnumFacing face) {
        if (CAPABILITY == null) {
            return null;
        }
        final InterfaceEssentia on = tile.getCapability(CAPABILITY, face);
        if (on != null && on.config == null) {
            final Object host = tile instanceof IPartHost parts ? parts.getPart(face) : tile;
            if (host instanceof IInterfaceHost iface
                    && iface.getInterfaceDuality().getConfig() instanceof AppEngInternalAEInventory config) {
                on.config = config;
            }
        }
        return on;
    }

    private boolean isConfiguredForEssentia() {
        if (this.config == null) {
            return false;
        }
        for (int slot = 0; slot < this.config.getSlots(); slot++) {
            final GenericStack wanted = this.config.getAEStackInSlot(slot);
            if (wanted != null && wanted.what() instanceof EssentiaKey) {
                return true;
            }
        }
        return false;
    }

    /** The first slot that holds essentia, or -1. */
    private int stockedSlot() {
        for (int slot = 0; slot < this.inventory.size(); slot++) {
            if (this.inventory.getKey(slot) instanceof EssentiaKey && this.inventory.getAmount(slot) > 0) {
                return slot;
            }
        }
        return -1;
    }

    private boolean isSource() {
        return this.isConfiguredForEssentia() || this.stockedSlot() >= 0;
    }

    @Override
    public boolean isConnectable(final EnumFacing face) {
        return true;
    }

    @Override
    public boolean canInputFrom(final EnumFacing face) {
        return !this.isSource();
    }

    @Override
    public boolean canOutputTo(final EnumFacing face) {
        return this.isSource();
    }

    @Override
    public void setSuction(final Aspect aspect, final int amount) {
        // An endpoint, not a hop: its suction is its own, as a jar's is.
    }

    @Nullable
    @Override
    public Aspect getSuctionType(final EnumFacing face) {
        return null;
    }

    @Override
    public int getSuctionAmount(final EnumFacing face) {
        return this.isSource() ? SOURCE_SUCTION : SINK_SUCTION;
    }

    @Override
    public int getMinimumSuction() {
        return MINIMUM_SUCTION;
    }

    @Nullable
    @Override
    public Aspect getEssentiaType(final EnumFacing face) {
        final int slot = this.stockedSlot();
        return slot < 0 ? null : ((EssentiaKey) this.inventory.getKey(slot)).getAspect();
    }

    @Override
    public int getEssentiaAmount(final EnumFacing face) {
        final int slot = this.stockedSlot();
        return slot < 0 ? 0 : (int) Math.min(this.inventory.getAmount(slot), Integer.MAX_VALUE);
    }

    @Override
    public int takeEssentia(final Aspect aspect, final int amount, final EnumFacing face) {
        if (aspect == null || amount <= 0) {
            return 0;
        }
        return (int) this.inventory.extract(EssentiaKey.of(aspect), amount, Actionable.MODULATE);
    }

    /**
     * The pull a jar makes on the tube above it, made on a tube beside an interface that takes essentia in: one
     * essentia, if the tube draws less hard and the network has room for it.
     */
    void pullFrom(final IEssentiaTransport tube, final EnumFacing tubeFace) {
        if (this.isSource() || !tube.canOutputTo(tubeFace) || tube.getSuctionAmount(tubeFace) >= SINK_SUCTION
                || SINK_SUCTION < tube.getMinimumSuction() || tube.getEssentiaAmount(tubeFace) <= 0) {
            return;
        }
        final Aspect aspect = tube.getEssentiaType(tubeFace);
        final IStorageService storage = this.network.get();
        if (aspect == null || storage == null) {
            return;
        }
        final AEKey key = EssentiaKey.of(aspect);
        if (storage.getInventory().insert(key, 1, Actionable.SIMULATE, this.source) < 1) {
            return;
        }
        final int taken = tube.takeEssentia(aspect, 1, tubeFace);
        if (taken > 0) {
            storage.getInventory().insert(key, taken, Actionable.MODULATE, this.source);
        }
    }

    @Override
    public int addEssentia(final Aspect aspect, final int amount, final EnumFacing face) {
        if (aspect == null || amount <= 0 || this.isSource()) {
            return 0;
        }
        final IStorageService storage = this.network.get();
        if (storage == null) {
            return 0;
        }
        final AEKey key = EssentiaKey.of(aspect);
        return (int) storage.getInventory().insert(key, amount, Actionable.MODULATE, this.source);
    }
}
