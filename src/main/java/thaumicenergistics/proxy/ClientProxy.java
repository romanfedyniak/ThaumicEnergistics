/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics.proxy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import appeng.api.client.AEKeyRendering;
import appeng.api.integrations.hei.IngredientConverters;
import appeng.api.patterns.client.PatternModePanels;

import thaumicenergistics.ThEItems;
import thaumicenergistics.client.EssentiaKeyRenderHandler;
import thaumicenergistics.client.gui.ArcaneModePanel;
import thaumicenergistics.crafting.ArcaneEncodingMode;
import thaumicenergistics.client.hei.AspectIngredientConverter;
import thaumicenergistics.item.ItemArcaneTerminal;
import thaumicenergistics.me.EssentiaKeyType;

public class ClientProxy extends CommonProxy {

    @SubscribeEvent
    public void onRegisterModels(final ModelRegistryEvent event) {
        ThEItems.ITEMS.values().forEach(item -> ModelLoader.setCustomModelResourceLocation(item, 0,
                new ModelResourceLocation(ThEItems.MODELS.getOrDefault(item, item.getRegistryName()), "inventory")));
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        AEKeyRendering.register(EssentiaKeyType.INSTANCE, new EssentiaKeyRenderHandler());
        IngredientConverters.register(new AspectIngredientConverter());
        PatternModePanels.register(ArcaneEncodingMode.ID, ArcaneModePanel::new);
        Minecraft.getMinecraft().getItemColors().registerItemColorHandler(ItemArcaneTerminal::getColor,
                ThEItems.ARCANE_TERMINAL);
    }

}
