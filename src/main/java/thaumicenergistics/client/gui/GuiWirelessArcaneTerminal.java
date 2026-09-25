/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics.client.gui;

import net.minecraft.entity.player.InventoryPlayer;

import appeng.helpers.WirelessTerminalGuiObject;

import thaumicenergistics.container.ContainerWirelessArcaneTerminal;

public class GuiWirelessArcaneTerminal extends GuiArcaneTerminal {

    public GuiWirelessArcaneTerminal(final InventoryPlayer inventoryPlayer, final WirelessTerminalGuiObject terminal) {
        super(inventoryPlayer, terminal, new ContainerWirelessArcaneTerminal(inventoryPlayer, terminal));
    }

    @Override
    protected boolean isWirelessTerminal() {
        return true;
    }
}
