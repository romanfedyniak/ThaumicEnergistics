/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics.mixin;

import java.util.Collections;
import java.util.List;

import zone.rong.mixinbooter.ILateMixinLoader;

/**
 * The mixins target AE2UD's classes, which are there only once mods load.
 */
public class ThELateMixinLoader implements ILateMixinLoader {

    @Override
    public List<String> getMixinConfigs() {
        return Collections.singletonList("mixins.thaumicenergistics.json");
    }
}
