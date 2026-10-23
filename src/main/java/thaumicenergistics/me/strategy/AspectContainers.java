/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics.me.strategy;

import java.lang.reflect.Field;

import javax.annotation.Nullable;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.common.tiles.crafting.TileCrucible;
import thaumcraft.common.tiles.crafting.TileThaumatorium;
import thaumcraft.common.tiles.crafting.TileThaumatoriumTop;
import thaumcraft.common.tiles.essentia.TileCentrifuge;
import thaumcraft.common.tiles.essentia.TileJarFillable;
import thaumcraft.common.tiles.essentia.TileJarFillableVoid;
import thaumcraft.common.tiles.essentia.TileTubeBuffer;

import thaumicenergistics.tile.TileInfusionProvider;

/**
 * Moving essentia in and out of Thaumcraft's {@link IAspectContainer}, which has no way to ask what an insert
 * would do without doing it and differs from block to block in what it takes per call.
 */
final class AspectContainers {

    private static final int JAR_CAPACITY = 250;
    private static final int TUBE_BUFFER_CAPACITY = 10;
    private static final Field CURRENT_RECIPE = ReflectionHelper.findField(TileThaumatorium.class, "currentRecipe");

    private AspectContainers() {
    }

    @Nullable
    static IAspectContainer get(final World world, final BlockPos pos) {
        if (world.getChunkProvider().getLoadedChunk(pos.getX() >> 4, pos.getZ() >> 4) == null) {
            return null;
        }
        final TileEntity target = world.getTileEntity(pos);
        // The provider is this network's essentia, not a container: a storage bus on it would count it twice.
        if (target instanceof TileInfusionProvider) {
            return null;
        }
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
        if (isThaumatorium(container)) {
            return Math.min(amount, thaumatoriumRoom(container, aspect));
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
        if (isThaumatorium(container) && thaumatoriumRoom(container, aspect) <= 0) {
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

    private static boolean isThaumatorium(final IAspectContainer container) {
        return container instanceof TileThaumatorium || container instanceof TileThaumatoriumTop;
    }

    /**
     * A thaumatorium, through either of its halves, takes what the recipe it is on still needs. Before it has
     * picked one, Thaumcraft's own insert throws, so it is not asked then.
     */
    private static int thaumatoriumRoom(final IAspectContainer container, final Aspect aspect) {
        final TileThaumatorium thaumatorium = container instanceof TileThaumatoriumTop
                ? ((TileThaumatoriumTop) container).thaumatorium
                : (TileThaumatorium) container;
        if (thaumatorium == null) {
            return 0;
        }
        final CrucibleRecipe recipe = currentRecipe(thaumatorium);
        if (recipe == null || recipe.getAspects() == null) {
            return 0;
        }
        return Math.max(0, recipe.getAspects().getAmount(aspect) - thaumatorium.essentia.getAmount(aspect));
    }

    @Nullable
    private static CrucibleRecipe currentRecipe(final TileThaumatorium thaumatorium) {
        try {
            return (CrucibleRecipe) CURRENT_RECIPE.get(thaumatorium);
        } catch (final IllegalAccessException e) {
            return null;
        }
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
