/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics;

import org.apache.logging.log4j.Logger;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import thaumicenergistics.proxy.CommonProxy;

@Mod(modid = Tags.MOD_ID, name = Tags.MOD_NAME, version = Tags.VERSION, useMetadata = true,
        dependencies = ThaumicEnergistics.DEPENDENCIES)
public class ThaumicEnergistics {

    /**
     * No version on AE2UD: the fork shares its mod id with AE2 and AE2UEL, and the version range that tells
     * them apart waits for AE2UD's first release.
     */
    static final String DEPENDENCIES = "required-after:appliedenergistics2;required-after:thaumcraft;after:jei;after:thaumicjei;after:theoneprobe";

    public static final String MODID = Tags.MOD_ID;

    @Mod.Instance(Tags.MOD_ID)
    public static ThaumicEnergistics INSTANCE;

    @SidedProxy(clientSide = "thaumicenergistics.proxy.ClientProxy", serverSide = "thaumicenergistics.proxy.CommonProxy")
    public static CommonProxy proxy;

    public static Logger log;

    @Mod.EventHandler
    public void onPreInit(FMLPreInitializationEvent event) {
        log = event.getModLog();
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void onInit(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void onPostInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MODID, path);
    }

}
