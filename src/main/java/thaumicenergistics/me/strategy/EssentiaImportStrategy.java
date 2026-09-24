/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics.me.strategy;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IAspectContainer;

import appeng.api.behaviors.StackImportStrategy;
import appeng.api.behaviors.StackTransferContext;
import appeng.api.config.Actionable;

import thaumicenergistics.me.EssentiaKey;

/**
 * What an import bus pulls out of a jar, an alembic or any other essentia container next to it: one aspect per
 * call, within the bus's operations.
 */
public class EssentiaImportStrategy implements StackImportStrategy {

    private final World world;
    private final BlockPos fromPos;

    EssentiaImportStrategy(final World world, final BlockPos fromPos) {
        this.world = world;
        this.fromPos = fromPos;
    }

    @Override
    public boolean transfer(final StackTransferContext context) {
        if (!context.hasOperationsLeft()) {
            return false;
        }

        final IAspectContainer container = AspectContainers.get(this.world, this.fromPos);
        if (container == null) {
            return false;
        }

        final int maxDraw = (int) Math.min(context.getOperationsRemaining(), Integer.MAX_VALUE);
        final var internal = context.getInternalStorage();
        final var source = context.getActionSource();

        for (final Aspect aspect : AspectContainers.contents(container).getAspects()) {
            if (aspect == null) {
                continue;
            }
            final EssentiaKey what = EssentiaKey.of(aspect);
            if (!context.getFilter().matches(what)) {
                continue;
            }

            final int candidate = AspectContainers.simulateExtract(container, aspect, maxDraw);
            if (candidate <= 0) {
                continue;
            }
            final long acceptable = internal.insert(what, candidate, Actionable.SIMULATE, source);
            if (acceptable <= 0) {
                continue;
            }

            final int taken = AspectContainers.extract(container, aspect, (int) acceptable);
            if (taken <= 0) {
                continue;
            }

            final long inserted = internal.insert(what, taken, Actionable.MODULATE, source);
            if (inserted < taken) {
                // Hand back what did not fit rather than void it.
                AspectContainers.insert(container, aspect, (int) (taken - inserted));
            }

            context.reduceOperationsRemaining(Math.max(1, inserted));
            return inserted > 0;
        }

        return false;
    }

    public static StackImportStrategy create(final World world, final BlockPos fromPos, final EnumFacing fromSide) {
        return new EssentiaImportStrategy(world, fromPos);
    }
}
