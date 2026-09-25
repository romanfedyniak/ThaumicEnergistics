/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import appeng.api.AEApi;
import appeng.api.parts.IPartItem;
import appeng.api.util.AEColor;

import thaumicenergistics.part.PartArcaneTerminal;

public class ItemArcaneTerminal extends Item implements IPartItem<PartArcaneTerminal> {

    /** AE2's display model tints these two layers: the rim and the screen's corner marks. */
    private static final int DARK = 1;
    private static final int RIM = 4;

    @Override
    public PartArcaneTerminal createPartFromItemStack(final ItemStack is) {
        return new PartArcaneTerminal(is);
    }

    @Override
    public EnumActionResult onItemUse(final EntityPlayer player, final World world, final BlockPos pos,
            final EnumHand hand, final EnumFacing side, final float hitX, final float hitY, final float hitZ) {
        return AEApi.instance().partHelper().placeBus(player.getHeldItem(hand), pos, side, player, hand, world);
    }

    /** The grid on the screen is coloured already; only what a plain cable's colour shows on is tinted. */
    public static int getColor(final ItemStack stack, final int tintIndex) {
        return tintIndex == DARK || tintIndex == RIM ? AEColor.TRANSPARENT.getVariantByTintIndex(tintIndex)
                : 0xFFFFFF;
    }
}
