/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.network.IGuiHandler;

import appeng.api.features.IWirelessTerminalMode;

/** The arcane terminal as a face of AE2's wireless terminal, unlocked by crafting the part into it. */
public final class ArcaneTerminalMode implements IWirelessTerminalMode {

    @Override
    public ResourceLocation getId() {
        return ThEGuis.ARCANE_MODE;
    }

    @Override
    public ItemStack getIcon() {
        return new ItemStack(ThEItems.ARCANE_TERMINAL);
    }

    @Override
    public String getUnlocalizedName() {
        return "gui.thaumicenergistics.wireless_mode.arcane";
    }

    @Override
    public IGuiHandler getGuiHandler() {
        return ThEGuis.wirelessArcaneTerminal();
    }

    @Override
    public ItemStack getUnlockIngredient() {
        return this.getIcon();
    }
}
