/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics.proxy;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import appeng.api.AEApi;
import appeng.api.behaviors.ContainerItemStrategy;
import appeng.api.behaviors.ExternalStorageStrategy;
import appeng.api.behaviors.GenericSlotCapacities;
import appeng.api.behaviors.StackExportStrategy;
import appeng.api.behaviors.StackImportStrategy;
import appeng.api.stacks.AEKeyType;

import thaumicenergistics.ThEItems;
import thaumicenergistics.ThERecipes;
import thaumicenergistics.me.EssentiaKeyType;
import thaumicenergistics.me.strategy.AspectContainerAdapter;
import thaumicenergistics.me.strategy.EssentiaContainerItemStrategy;
import thaumicenergistics.me.strategy.EssentiaExportStrategy;
import thaumicenergistics.me.strategy.EssentiaImportStrategy;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        ThEItems.init();
    }

    @SubscribeEvent
    public void onRegisterItems(final RegistryEvent.Register<Item> event) {
        ThEItems.ITEMS.values().forEach(event.getRegistry()::register);
    }

    @SubscribeEvent
    public void onRegisterRecipes(final RegistryEvent.Register<IRecipe> event) {
        ThERecipes.register();
    }

    public void init(FMLInitializationEvent event) {
        // With these three, the buses, storage buses and interfaces AE2UD already ships carry essentia.
        StackImportStrategy.register(EssentiaKeyType.INSTANCE, EssentiaImportStrategy::create);
        StackExportStrategy.register(EssentiaKeyType.INSTANCE, EssentiaExportStrategy::create);
        ExternalStorageStrategy.register(EssentiaKeyType.INSTANCE, AspectContainerAdapter.Strategy::new);

        // A phial or a jar item fills and empties against a terminal row or a filter slot like a bucket.
        ContainerItemStrategy.register(EssentiaKeyType.INSTANCE, new EssentiaContainerItemStrategy());

        // An interface's essentia slot holds one jar's worth; the interface multiplies it like any other.
        GenericSlotCapacities.register(EssentiaKeyType.INSTANCE, 250L);

        // After AE2's own cards, which it registers during initialisation too.
        AEApi.instance().registries().upgrades().registerCard(new ItemStack(ThEItems.ARCANE_CHARGING_CARD),
                ThEItems.ARCANE_CHARGING, 1);
    }

    public void postInit(FMLPostInitializationEvent event) {
    }

    @SubscribeEvent
    public void onRegisterKeyTypes(final RegistryEvent.Register<AEKeyType> event) {
        event.getRegistry().register(EssentiaKeyType.INSTANCE);
    }

}
