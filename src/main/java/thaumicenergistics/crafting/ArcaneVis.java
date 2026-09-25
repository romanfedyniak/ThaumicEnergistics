/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics.crafting;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import thaumcraft.api.aura.AuraHelper;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.common.items.casters.CasterManager;

/**
 * Vis for arcane crafting away from the workbench, drawn the way the workbench draws it: from the chunk it stands
 * in, or - with an Arcane Charging Card, as with a vis charger on the workbench - from that chunk and the eight
 * around it.
 */
public final class ArcaneVis {

    private static final int CHUNK = 16;
    private static final int CHUNKS = 9;

    private ArcaneVis() {
    }

    /** What the recipe costs this player, after what their gear takes off it. */
    public static int cost(final IArcaneRecipe recipe, final EntityPlayer player) {
        return (int) (recipe.getVis() * (1.0F - CasterManager.getTotalVisDiscount(player)));
    }

    public static int available(final World world, final BlockPos pos, final boolean charging) {
        if (!charging) {
            return (int) AuraHelper.getVis(world, pos);
        }
        float vis = 0;
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                vis += AuraHelper.getVis(world, pos.add(x * CHUNK, 0, z * CHUNK));
            }
        }
        return (int) vis;
    }

    /** Takes it evenly from all nine chunks when charging, so no one chunk is emptied first. */
    public static void drain(final World world, final BlockPos pos, final int vis, final boolean charging) {
        if (vis <= 0) {
            return;
        }
        if (!charging) {
            AuraHelper.drainVis(world, pos, vis, false);
            return;
        }
        float left = vis;
        final float share = Math.max(1, vis / CHUNKS);
        boolean drained = true;
        while (left > 0 && drained) {
            drained = false;
            for (int x = -1; x <= 1 && left > 0; x++) {
                for (int z = -1; z <= 1 && left > 0; z++) {
                    final float taken = AuraHelper.drainVis(world, pos.add(x * CHUNK, 0, z * CHUNK),
                            Math.min(share, left), false);
                    left -= taken;
                    drained |= taken > 0;
                }
            }
        }
    }
}
