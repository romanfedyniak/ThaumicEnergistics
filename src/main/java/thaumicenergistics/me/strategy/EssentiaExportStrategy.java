/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics.me.strategy;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import thaumcraft.api.aspects.IAspectContainer;

import appeng.api.behaviors.StackExportStrategy;
import appeng.api.behaviors.StackTransferContext;
import appeng.api.config.Actionable;
import appeng.api.stacks.AEKey;

import thaumicenergistics.me.EssentiaKey;

/**
 * What an export bus, an interface or a machine pushes into an essentia container next to it.
 */
public class EssentiaExportStrategy implements StackExportStrategy {

    private final World world;
    private final BlockPos fromPos;

    EssentiaExportStrategy(final World world, final BlockPos fromPos) {
        this.world = world;
        this.fromPos = fromPos;
    }

    @Override
    public long transfer(final StackTransferContext context, final AEKey what, final long maxAmount) {
        if (!(what instanceof EssentiaKey essentia) || maxAmount <= 0) {
            return 0;
        }

        final IAspectContainer container = AspectContainers.get(this.world, this.fromPos);
        if (container == null) {
            return 0;
        }

        final int room = AspectContainers.simulateInsert(container, essentia.getAspect(),
                (int) Math.min(maxAmount, Integer.MAX_VALUE));
        if (room <= 0) {
            return 0;
        }

        final var internal = context.getInternalStorage();
        final var source = context.getActionSource();
        final long extracted = internal.extract(what, room, Actionable.MODULATE, source);
        if (extracted <= 0) {
            return 0;
        }

        final int added = AspectContainers.insert(container, essentia.getAspect(), (int) extracted);
        if (added < extracted) {
            // Put back whatever the container would not take after all, rather than voiding it.
            internal.insert(what, extracted - added, Actionable.MODULATE, source);
        }
        return added;
    }

    @Override
    public long push(final AEKey what, final long maxAmount, final Actionable mode) {
        if (!(what instanceof EssentiaKey essentia) || maxAmount <= 0) {
            return 0;
        }

        final IAspectContainer container = AspectContainers.get(this.world, this.fromPos);
        if (container == null) {
            return 0;
        }

        final int amount = (int) Math.min(maxAmount, Integer.MAX_VALUE);
        return mode == Actionable.MODULATE
                ? AspectContainers.insert(container, essentia.getAspect(), amount)
                : AspectContainers.simulateInsert(container, essentia.getAspect(), amount);
    }

    public static StackExportStrategy create(final World world, final BlockPos fromPos, final EnumFacing fromSide) {
        return new EssentiaExportStrategy(world, fromPos);
    }
}
