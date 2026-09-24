/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics.me.strategy;

import javax.annotation.Nullable;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.common.tiles.crafting.TileCrucible;
import thaumcraft.common.tiles.essentia.TileCentrifuge;
import thaumcraft.common.tiles.essentia.TileJarFillable;
import thaumcraft.common.tiles.essentia.TileJarFillableVoid;
import thaumcraft.common.tiles.essentia.TileTubeBuffer;

/**
 * Moving essentia in and out of Thaumcraft's {@link IAspectContainer}, which has no way to ask what an insert
 * would do without doing it and differs from block to block in what it takes per call.
 */
final class AspectContainers {

    private static final int JAR_CAPACITY = 250;
    private static final int TUBE_BUFFER_CAPACITY = 10;

    private AspectContainers() {
    }

    @Nullable
    static IAspectContainer get(final World world, final BlockPos pos) {
        if (world.getChunkProvider().getLoadedChunk(pos.getX() >> 4, pos.getZ() >> 4) == null) {
            return null;
        }
        final TileEntity target = world.getTileEntity(pos);
        return target instanceof IAspectContainer container ? container : null;
    }

    /**
     * The crucible answers every insert as taken and keeps none of it; the centrifuge takes one aspect as the
     * one it is splitting out. Neither is storage.
     */
    static boolean acceptsInserts(final IAspectContainer container) {
        return !(container instanceof TileCrucible) && !(container instanceof TileCentrifuge);
    }

    static AspectList contents(final IAspectContainer container) {
        final AspectList aspects = container.getAspects();
        return aspects == null ? new AspectList() : aspects;
    }

    /**
     * How much an insert would take, worked out from what the container is instead of trying it. A container
     * this does not know is taken at its word that it accepts the aspect.
     */
    static int simulateInsert(final IAspectContainer container, final Aspect aspect, final int amount) {
        if (amount <= 0 || !acceptsInserts(container) || !container.doesContainerAccept(aspect)) {
            return 0;
        }
        if (container instanceof TileJarFillable jar) {
            if (jar.amount > 0 && jar.aspect != aspect) {
                return 0;
            }
            return jar instanceof TileJarFillableVoid ? amount : Math.min(amount, JAR_CAPACITY - jar.amount);
        }
        if (container instanceof TileTubeBuffer) {
            return Math.max(0, Math.min(amount, TUBE_BUFFER_CAPACITY - contents(container).visSize()));
        }
        return amount;
    }

    /**
     * Some containers take one essentia per call and refuse more, so a refused batch is retried one at a time.
     */
    static int insert(final IAspectContainer container, final Aspect aspect, final int amount) {
        if (amount <= 0 || !acceptsInserts(container) || !container.doesContainerAccept(aspect)) {
            return 0;
        }
        int added = amount - container.addToContainer(aspect, amount);
        if (added == 0 && amount > 1) {
            while (added < amount && container.addToContainer(aspect, 1) == 0) {
                added++;
            }
        }
        return added;
    }

    static int simulateExtract(final IAspectContainer container, final Aspect aspect, final int amount) {
        return Math.max(0, Math.min(amount, container.containerContains(aspect)));
    }

    /**
     * A take is all or nothing, so only what the container holds is asked for.
     */
    static int extract(final IAspectContainer container, final Aspect aspect, final int amount) {
        final int available = simulateExtract(container, aspect, amount);
        if (available <= 0) {
            return 0;
        }
        if (container.takeFromContainer(aspect, available)) {
            return available;
        }
        int taken = 0;
        while (taken < available && container.takeFromContainer(aspect, 1)) {
            taken++;
        }
        return taken;
    }
}
