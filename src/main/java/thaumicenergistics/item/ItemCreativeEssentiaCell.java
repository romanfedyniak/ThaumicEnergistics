/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics.item;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The creative essentia cell, an item of its own so a pack can hand it out as a reward, though AE2UD's creative
 * cell given essentia does the same.
 */
public class ItemCreativeEssentiaCell extends Item {

    public ItemCreativeEssentiaCell() {
        this.setMaxStackSize(1);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(final ItemStack stack, @Nullable final World world, final List<String> tooltip,
            final ITooltipFlag flag) {
        tooltip.add(I18n.format("tooltip.thaumicenergistics.essentia_cell_creative.description"));
    }
}
