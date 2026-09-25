/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.network.IGuiHandler;

import baubles.api.BaublesApi;

import appeng.api.AEApi;
import appeng.api.features.IWirelessTermHandler;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.util.AEPartLocation;
import appeng.container.AEBaseContainer;
import appeng.container.ContainerOpenContext;
import appeng.core.sync.GuiBridge;
import appeng.core.sync.GuiWrapper;
import appeng.helpers.WirelessTerminalGuiObject;

import thaumicenergistics.client.gui.GuiArcaneAssembler;
import thaumicenergistics.client.gui.GuiArcaneTerminal;
import thaumicenergistics.client.gui.GuiWirelessArcaneTerminal;
import thaumicenergistics.container.ContainerArcaneAssembler;
import thaumicenergistics.container.ContainerArcaneTerminal;
import thaumicenergistics.container.ContainerWirelessArcaneTerminal;
import thaumicenergistics.part.PartArcaneTerminal;
import thaumicenergistics.tile.TileArcaneAssembler;

/**
 * The mod's windows. AE2's own list of them is fixed, so each is opened by Forge's handler here and known to AE2
 * through a wrapper, which is how a screen of AE2's opened over one - confirming a craft - finds its way back.
 */
public final class ThEGuis implements IGuiHandler {

    public static final ThEGuis INSTANCE = new ThEGuis();

    /** The arcane mode of AE2's wireless terminal, and the key its grid is kept under in the terminal. */
    public static final ResourceLocation ARCANE_MODE = ThaumicEnergistics.id("arcane_terminal");

    /** An arcane terminal: one id for each face it can sit on. */
    private static final int ARCANE_TERMINAL = 0;
    /** The same carried: x is the slot the wireless terminal sits in, y says whether it is a bauble slot. */
    private static final int WIRELESS_ARCANE_TERMINAL = 6;
    /** An arcane assembler, at x, y, z. */
    public static final int ARCANE_ASSEMBLER = 7;

    private static GuiBridge arcaneTerminal;
    private static GuiBridge wirelessArcaneTerminal;

    private ThEGuis() {
    }

    public static GuiBridge arcaneTerminal() {
        return arcaneTerminal;
    }

    public static GuiBridge wirelessArcaneTerminal() {
        return wirelessArcaneTerminal;
    }

    public static void registerBridges() {
        final ResourceLocation id = ThaumicEnergistics.id("arcane_terminal");
        // Written out rather than as a lambda: the method it implements is generic.
        GuiWrapper.INSTANCE.registerExternalGuiHandler(id, new GuiWrapper.Opener() {
            @Override
            public <T extends GuiWrapper.IExternalGui> void open(final T window, final GuiWrapper.GuiContext context) {
                if (context.pos != null && context.facing != null) {
                    context.player.openGui(ThaumicEnergistics.INSTANCE, ARCANE_TERMINAL + context.facing.ordinal(),
                            context.world, context.pos.getX(), context.pos.getY(), context.pos.getZ());
                }
            }
        });
        arcaneTerminal = GuiWrapper.INSTANCE.wrap(() -> id);

        // A carried terminal is found by the slot it sits in; one used by hand is the one being held.
        final ResourceLocation wirelessId = ThaumicEnergistics.id("wireless_arcane_terminal");
        GuiWrapper.INSTANCE.registerExternalGuiHandler(wirelessId, new GuiWrapper.Opener() {
            @Override
            public <T extends GuiWrapper.IExternalGui> void open(final T window, final GuiWrapper.GuiContext context) {
                final int slot = context.extra == null ? context.player.inventory.currentItem
                        : context.extra.getInteger("slot");
                final boolean bauble = context.extra != null && context.extra.getBoolean("isBauble");
                context.player.openGui(ThaumicEnergistics.INSTANCE, WIRELESS_ARCANE_TERMINAL, context.world, slot,
                        bauble ? 1 : 0, Integer.MIN_VALUE);
            }
        });
        wirelessArcaneTerminal = GuiWrapper.INSTANCE.wrap(() -> wirelessId);
    }

    @Nullable
    private static WirelessTerminalGuiObject carriedTerminal(final EntityPlayer player, final int slot,
            final boolean bauble) {
        final ItemStack terminal;
        if (bauble) {
            terminal = Loader.isModLoaded("baubles") ? BaublesApi.getBaublesHandler(player).getStackInSlot(slot)
                    : ItemStack.EMPTY;
        } else {
            terminal = player.inventory.getStackInSlot(slot);
        }
        if (terminal.isEmpty()) {
            return null;
        }
        final IWirelessTermHandler handler = AEApi.instance().registries().wireless()
                .getWirelessTerminalHandler(terminal);
        return handler == null ? null
                : new WirelessTerminalGuiObject(handler, terminal, player, player.world, slot, bauble ? 1 : 0,
                        Integer.MIN_VALUE);
    }

    @Nullable
    private static PartArcaneTerminal arcaneTerminalAt(final int id, final World world, final int x, final int y,
            final int z) {
        final int face = id - ARCANE_TERMINAL;
        if (face < 0 || face >= EnumFacing.VALUES.length) {
            return null;
        }
        final TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
        if (!(tile instanceof IPartHost host)) {
            return null;
        }
        final IPart part = host.getPart(AEPartLocation.fromFacing(EnumFacing.byIndex(face)));
        return part instanceof PartArcaneTerminal terminal ? terminal : null;
    }

    /** Reopening the window - which going back from a screen of AE2's does - finds the part on this face. */
    private static <T extends AEBaseContainer> T withContext(final T container, final World world, final int x,
            final int y, final int z, final AEPartLocation side) {
        final ContainerOpenContext context = new ContainerOpenContext(container.getTarget());
        context.setSide(side);
        context.setWorld(world);
        context.setX(x);
        context.setY(y);
        context.setZ(z);
        container.setOpenContext(context);
        return container;
    }

    @Nullable
    @Override
    public Object getServerGuiElement(final int id, final EntityPlayer player, final World world, final int x,
            final int y, final int z) {
        if (id == ARCANE_ASSEMBLER) {
            return world.getTileEntity(new BlockPos(x, y, z)) instanceof TileArcaneAssembler assembler
                    ? new ContainerArcaneAssembler(player.inventory, assembler)
                    : null;
        }
        if (id == WIRELESS_ARCANE_TERMINAL) {
            final WirelessTerminalGuiObject carried = carriedTerminal(player, x, y == 1);
            return carried == null ? null
                    : withContext(new ContainerWirelessArcaneTerminal(player.inventory, carried), world, x, y, z,
                            AEPartLocation.INTERNAL);
        }
        final PartArcaneTerminal terminal = arcaneTerminalAt(id, world, x, y, z);
        return terminal == null ? null
                : withContext(new ContainerArcaneTerminal(player.inventory, terminal), world, x, y, z,
                        terminal.getSide());
    }

    @Nullable
    @Override
    public Object getClientGuiElement(final int id, final EntityPlayer player, final World world, final int x,
            final int y, final int z) {
        if (id == ARCANE_ASSEMBLER) {
            return world.getTileEntity(new BlockPos(x, y, z)) instanceof TileArcaneAssembler assembler
                    ? new GuiArcaneAssembler(player.inventory, assembler)
                    : null;
        }
        if (id == WIRELESS_ARCANE_TERMINAL) {
            final WirelessTerminalGuiObject carried = carriedTerminal(player, x, y == 1);
            return carried == null ? null : new GuiWirelessArcaneTerminal(player.inventory, carried);
        }
        final PartArcaneTerminal terminal = arcaneTerminalAt(id, world, x, y, z);
        return terminal == null ? null : new GuiArcaneTerminal(player.inventory, terminal);
    }
}
