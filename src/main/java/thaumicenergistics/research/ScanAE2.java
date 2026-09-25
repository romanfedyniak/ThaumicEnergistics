/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics.research;

import javax.annotation.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

import thaumcraft.api.research.IScanThing;

/**
 * Scanning anything of AE2UD's - an item, a block, a part on a cable, an entity - teaches what opens the mod's
 * research tab.
 */
public final class ScanAE2 implements IScanThing {

    private static final String AE2 = "appliedenergistics2";

    @Override
    public boolean checkThing(final EntityPlayer player, final Object thing) {
        final ResourceLocation id = idOf(player, thing);
        return id != null && AE2.equals(id.getNamespace());
    }

    @Nullable
    private static ResourceLocation idOf(final EntityPlayer player, final Object thing) {
        if (thing instanceof ItemStack stack) {
            return stack.isEmpty() ? null : stack.getItem().getRegistryName();
        }
        if (thing instanceof EntityItem item) {
            return item.getItem().isEmpty() ? null : item.getItem().getItem().getRegistryName();
        }
        if (thing instanceof BlockPos pos) {
            return player.world.getBlockState(pos).getBlock().getRegistryName();
        }
        if (thing instanceof Entity entity) {
            return EntityList.getKey(entity);
        }
        return null;
    }

    @Override
    public String getResearchKey(final EntityPlayer player, final Object thing) {
        return ThEResearch.SCAN_KEY;
    }
}
