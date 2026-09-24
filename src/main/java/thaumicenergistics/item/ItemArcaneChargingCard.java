/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import appeng.api.upgrades.UpgradeCards;

/**
 * The Arcane Charging Card. It does nothing on its own; a machine that takes it draws vis from the chunks around
 * the one it stands in.
 */
public class ItemArcaneChargingCard extends Item {

    /** Sneak-clicking a machine puts the card in, the way AE2's own cards do. */
    @Override
    public EnumActionResult onItemUseFirst(final EntityPlayer player, final World world, final BlockPos pos,
            final EnumFacing side, final float hitX, final float hitY, final float hitZ, final EnumHand hand) {
        if (player.isSneaking()
                && UpgradeCards.installHeldCard(player, hand, world, pos, new Vec3d(hitX, hitY, hitZ))) {
            return world.isRemote ? EnumActionResult.PASS : EnumActionResult.SUCCESS;
        }
        return super.onItemUseFirst(player, world, pos, side, hitX, hitY, hitZ, hand);
    }
}
