/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics.client.hei;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;

import appeng.integration.modules.jei.RecipeTransferHandler;

import thaumicenergistics.ThEItems;
import thaumicenergistics.container.ContainerArcaneTerminal;
import thaumicenergistics.container.ContainerWirelessArcaneTerminal;

@JEIPlugin
public class ThEJeiPlugin implements IModPlugin {

    /** Thaumic JEI's screen of arcane workbench recipes. */
    private static final String ARCANE_WORKBENCH = "THAUMCRAFT_ARCANE_WORKBENCH";

    @Override
    public void register(final IModRegistry registry) {
        final ItemStack terminal = new ItemStack(ThEItems.ARCANE_TERMINAL);
        registry.getRecipeTransferRegistry().addRecipeTransferHandler(
                new RecipeTransferHandler<>(ContainerArcaneTerminal.class), VanillaRecipeCategoryUid.CRAFTING);
        registry.getRecipeTransferRegistry().addRecipeTransferHandler(
                new RecipeTransferHandler<>(ContainerWirelessArcaneTerminal.class), VanillaRecipeCategoryUid.CRAFTING);
        registry.addRecipeCatalyst(terminal, VanillaRecipeCategoryUid.CRAFTING);

        // Its screen lists the grid's nine squares first, as the terminal numbers them.
        if (Loader.isModLoaded("thaumicjei")) {
            registry.getRecipeTransferRegistry().addRecipeTransferHandler(
                    new RecipeTransferHandler<>(ContainerArcaneTerminal.class), ARCANE_WORKBENCH);
            registry.getRecipeTransferRegistry().addRecipeTransferHandler(
                    new RecipeTransferHandler<>(ContainerWirelessArcaneTerminal.class), ARCANE_WORKBENCH);
            registry.addRecipeCatalyst(terminal, ARCANE_WORKBENCH);
        }
    }
}
