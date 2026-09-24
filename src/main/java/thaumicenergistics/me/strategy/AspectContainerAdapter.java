/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics.me.strategy;

import javax.annotation.Nullable;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;

import appeng.api.behaviors.ExternalStorageStrategy;
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.IStorageChangeSource;
import appeng.api.storage.MEStorage;
import appeng.me.storage.ITickingMonitor;
import appeng.me.storage.StorageChangeListeners;

import thaumicenergistics.me.EssentiaKey;

/**
 * An essentia container mounted on a network by a storage bus, seen as storage. Tubes and golems fill and empty
 * jars behind the network's back, so the contents are polled.
 */
public class AspectContainerAdapter implements MEStorage, ITickingMonitor, IStorageChangeSource {

    private final IAspectContainer container;
    @Nullable
    private final Runnable changeListener;

    private KeyCounter currentlyCached = new KeyCounter();
    private final StorageChangeListeners listeners = new StorageChangeListeners();

    AspectContainerAdapter(final IAspectContainer container, @Nullable final Runnable changeListener) {
        this.container = container;
        this.changeListener = changeListener;
        this.updateCache();
    }

    @Override
    public void addChangeListener(final Listener listener) {
        this.listeners.addChangeListener(listener);
    }

    @Override
    public void removeChangeListener(final Listener listener) {
        this.listeners.removeChangeListener(listener);
    }

    @Override
    public long insert(final AEKey what, final long amount, final Actionable mode, final IActionSource source) {
        if (!(what instanceof EssentiaKey essentia) || amount <= 0) {
            return 0;
        }

        final int toInsert = (int) Math.min(amount, Integer.MAX_VALUE);
        if (mode == Actionable.SIMULATE) {
            return AspectContainers.simulateInsert(this.container, essentia.getAspect(), toInsert);
        }

        final int added = AspectContainers.insert(this.container, essentia.getAspect(), toInsert);
        if (added > 0) {
            this.updateCache();
            this.notifyChange();
        }
        return added;
    }

    @Override
    public long extract(final AEKey what, final long amount, final Actionable mode, final IActionSource source) {
        if (!(what instanceof EssentiaKey essentia) || amount <= 0) {
            return 0;
        }

        final int toTake = (int) Math.min(amount, Integer.MAX_VALUE);
        if (mode == Actionable.SIMULATE) {
            return AspectContainers.simulateExtract(this.container, essentia.getAspect(), toTake);
        }

        final int taken = AspectContainers.extract(this.container, essentia.getAspect(), toTake);
        if (taken > 0) {
            this.updateCache();
            this.notifyChange();
        }
        return taken;
    }

    @Override
    public void getAvailableStacks(final KeyCounter out) {
        for (final var entry : this.currentlyCached) {
            out.add(entry.getKey(), entry.getLongValue());
        }
    }

    @Override
    public TickRateModulation onTick() {
        final KeyCounter before = this.currentlyCached;
        this.updateCache();
        return keyCountersEqual(before, this.currentlyCached) ? TickRateModulation.SLOWER : TickRateModulation.URGENT;
    }

    private void notifyChange() {
        if (this.changeListener != null) {
            this.changeListener.run();
        }
    }

    private void updateCache() {
        final KeyCounter fresh = new KeyCounter();
        final AspectList contents = AspectContainers.contents(this.container);
        for (final Aspect aspect : contents.getAspects()) {
            final int amount = aspect == null ? 0 : contents.getAmount(aspect);
            if (amount > 0) {
                fresh.add(EssentiaKey.of(aspect), amount);
            }
        }

        // Everything this adapter can see passes through here, its own inserts as much as a tube filling the
        // jar, so this is the one place that says what moved.
        this.listeners.postDiff(this.currentlyCached, fresh);
        this.currentlyCached = fresh;
    }

    private static boolean keyCountersEqual(final KeyCounter a, final KeyCounter b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (final var entry : a) {
            if (b.get(entry.getKey()) != entry.getLongValue()) {
                return false;
            }
        }
        return true;
    }

    /**
     * The {@link ExternalStorageStrategy} for essentia; a storage bus reaches it through
     * {@code StackWorldBehaviors}, never this class directly.
     */
    public static final class Strategy implements ExternalStorageStrategy {

        private final World world;
        private final BlockPos fromPos;

        public Strategy(final World world, final BlockPos fromPos, final EnumFacing fromSide) {
            this.world = world;
            this.fromPos = fromPos;
        }

        @Nullable
        @Override
        public MEStorage createWrapper(final boolean extractableOnly, final Runnable injectOrExtractCallback) {
            final IAspectContainer container = AspectContainers.get(this.world, this.fromPos);
            return container == null ? null : new AspectContainerAdapter(container, injectOrExtractCallback);
        }
    }
}
