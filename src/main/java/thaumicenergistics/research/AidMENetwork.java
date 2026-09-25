/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics.research;

import net.minecraft.block.Block;

import thaumcraft.api.research.theorycraft.ITheorycraftAid;
import thaumcraft.api.research.theorycraft.TheorycraftCard;

/**
 * An ME block near the research table, which offers the mod's card: the controller, or the drive where a pack has
 * switched the controller off.
 */
public class AidMENetwork implements ITheorycraftAid {

    private final Block block;

    public AidMENetwork(final Block block) {
        this.block = block;
    }

    @Override
    public Object getAidObject() {
        return this.block;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<TheorycraftCard>[] getCards() {
        return new Class[] { CardTinkerAE.class };
    }
}
