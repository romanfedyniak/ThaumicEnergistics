/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics.proxy;

import net.minecraftforge.fml.common.event.FMLInitializationEvent;

import appeng.api.client.AEKeyRendering;

import thaumicenergistics.client.EssentiaKeyRenderHandler;
import thaumicenergistics.me.EssentiaKeyType;

public class ClientProxy extends CommonProxy {

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        AEKeyRendering.register(EssentiaKeyType.INSTANCE, new EssentiaKeyRenderHandler());
    }

}
