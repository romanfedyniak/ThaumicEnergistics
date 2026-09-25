/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics.research;

import java.util.Random;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.translation.I18n;

import thaumcraft.api.research.theorycraft.ResearchTableData;
import thaumcraft.api.research.theorycraft.TheorycraftCard;

/**
 * The research table's card for the mod's tab: 10 to 24 points of it for one inspiration.
 */
public class CardTinkerAE extends TheorycraftCard {

    private static final Random RANDOM = new Random();

    @Override
    public int getInspirationCost() {
        return 1;
    }

    @Override
    @SuppressWarnings("deprecation")
    public String getLocalizedName() {
        return I18n.translateToLocal("card.tinkerae.name");
    }

    @Override
    @SuppressWarnings("deprecation")
    public String getLocalizedText() {
        return I18n.translateToLocal("card.tinkerae.text");
    }

    @Override
    public boolean activate(final EntityPlayer player, final ResearchTableData data) {
        data.addTotal(ThEResearch.CATEGORY, RANDOM.nextInt(15) + 10);
        return true;
    }
}
