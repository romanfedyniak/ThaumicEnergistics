/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics.proxy;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTBase;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import appeng.api.AEApi;
import appeng.api.behaviors.ContainerItemStrategy;
import appeng.api.behaviors.ExternalStorageStrategy;
import appeng.api.behaviors.GenericInventoryAdapters;
import appeng.api.behaviors.GenericSlotCapacities;
import appeng.api.behaviors.StackExportStrategy;
import appeng.api.behaviors.StackImportStrategy;
import appeng.api.stacks.AEKeyType;
import appeng.api.storage.StorageCells;
import appeng.api.upgrades.CardTraits;
import appeng.api.upgrades.IUpgradeRegistry;

import thaumicenergistics.ThEGuis;
import thaumicenergistics.ThEItems;
import thaumicenergistics.ThERecipes;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.me.CreativeEssentiaCell;
import thaumicenergistics.me.EssentiaKeyType;
import thaumicenergistics.me.InterfaceEssentia;
import thaumicenergistics.me.InterfaceEssentiaPulls;
import thaumicenergistics.me.strategy.AspectContainerAdapter;
import thaumicenergistics.me.strategy.EssentiaContainerItemStrategy;
import thaumicenergistics.me.strategy.EssentiaExportStrategy;
import thaumicenergistics.me.strategy.EssentiaImportStrategy;
import thaumicenergistics.part.PartArcaneTerminal;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(InterfaceEssentiaPulls.INSTANCE);
        CapabilityManager.INSTANCE.register(InterfaceEssentia.class, new NoStorage<>(), () -> {
            throw new UnsupportedOperationException("Only an ME interface offers essentia this way");
        });
        ThEItems.init();
        AEApi.instance().registries().partModels().registerModels(PartArcaneTerminal.MODELS_OFF.getModels());
        AEApi.instance().registries().partModels().registerModels(PartArcaneTerminal.MODELS_ON.getModels());
    }

    @SubscribeEvent
    public void onRegisterBlocks(final RegistryEvent.Register<Block> event) {
        ThEItems.BLOCKS.values().forEach(event.getRegistry()::register);
        ThEItems.registerTiles();
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
        // What an ME interface offers Thaumcraft's tubes, which reach it through MixinInterfaceEssentia.
        GenericInventoryAdapters.register(InterfaceEssentia.CAPABILITY, InterfaceEssentia::new);

        // A phial or a jar item fills and empties against a terminal row or a filter slot like a bucket.
        ContainerItemStrategy.register(EssentiaKeyType.INSTANCE, new EssentiaContainerItemStrategy());

        // An interface's essentia slot holds one jar's worth; the interface multiplies it like any other.
        GenericSlotCapacities.register(EssentiaKeyType.INSTANCE, 250L);

        // After AE2's own cards, which it registers during initialisation too.
        ThERecipes.registerInfusion();

        final IUpgradeRegistry upgrades = AEApi.instance().registries().upgrades();
        upgrades.registerCard(new ItemStack(ThEItems.ARCANE_CHARGING_CARD), ThEItems.ARCANE_CHARGING, 1);
        upgrades.addTraitSupport(ThEItems.ARCANE_CHARGING, new ItemStack(ThEItems.ARCANE_TERMINAL), 1);

        NetworkRegistry.INSTANCE.registerGuiHandler(ThaumicEnergistics.INSTANCE, ThEGuis.INSTANCE);
        ThEGuis.registerBridges();

        StorageCells.addCellHandler(new CreativeEssentiaCell.Handler());
        // The cards AE2UD gives its own fluid cells and portable fluid cells.
        for (final Item cell : ThEItems.CELLS.values()) {
            final ItemStack stack = new ItemStack(cell);
            upgrades.addTraitSupport(CardTraits.INVERTER, stack, 1);
            upgrades.addTraitSupport(CardTraits.STICKY, stack, 1);
            upgrades.addTraitSupport(CardTraits.EQUAL_DISTRIBUTION, stack, 1);
            upgrades.addTraitSupport(CardTraits.VOID, stack, 1);
        }
        for (final Item cell : ThEItems.PORTABLE_CELLS.values()) {
            final ItemStack stack = new ItemStack(cell);
            upgrades.addTraitSupport(CardTraits.ENERGY, stack, 2);
            upgrades.addTraitSupport(CardTraits.VOID, stack, 1);
            AEApi.instance().registries().charger().addChargeRate(cell, 800d);
        }
    }

    public void postInit(FMLPostInitializationEvent event) {
    }

    /** A capability AE2UD builds fresh for each interface; there is nothing of it to save. */
    private static final class NoStorage<T> implements Capability.IStorage<T> {

        @Override
        public NBTBase writeNBT(final Capability<T> capability, final T instance, final EnumFacing side) {
            return null;
        }

        @Override
        public void readNBT(final Capability<T> capability, final T instance, final EnumFacing side, final NBTBase nbt) {
        }
    }

    @SubscribeEvent
    public void onRegisterKeyTypes(final RegistryEvent.Register<AEKeyType> event) {
        event.getRegistry().register(EssentiaKeyType.INSTANCE);
    }

}
