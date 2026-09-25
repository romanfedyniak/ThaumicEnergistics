/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics.container;

import appeng.container.slot.SlotCraftingMatrix;
import appeng.container.slot.SlotRestrictedInput;

import thaumicenergistics.crafting.ArcaneCosts;

/** What an arcane terminal's screen lays out, whether the terminal is a part or a wireless one. */
public interface IArcaneTerminal {

    SlotCraftingMatrix[] getGridSlots();

    SlotArcaneResult getOutputSlot();

    /** The Arcane Charging Card's own slot. */
    SlotRestrictedInput getCardSlot();

    /** As last sent by the server. */
    ArcaneCosts getCosts();
}
