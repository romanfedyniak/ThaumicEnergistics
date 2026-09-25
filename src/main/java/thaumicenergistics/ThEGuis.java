/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.util.AEPartLocation;
import appeng.container.AEBaseContainer;
import appeng.container.ContainerOpenContext;
import appeng.core.sync.GuiBridge;
import appeng.core.sync.GuiWrapper;

import thaumicenergistics.client.gui.GuiArcaneTerminal;
import thaumicenergistics.container.ContainerArcaneTerminal;
import thaumicenergistics.part.PartArcaneTerminal;

/**
 * The mod's windows. AE2's own list of them is fixed, so each is opened by Forge's handler here and known to AE2
 * through a wrapper, which is how a screen of AE2's opened over one - confirming a craft - finds its way back.
 */
public final class ThEGuis implements IGuiHandler {

    public static final ThEGuis INSTANCE = new ThEGuis();

    /** An arcane terminal: one id for each face it can sit on. */
    private static final int ARCANE_TERMINAL = 0;

    private static GuiBridge arcaneTerminal;

    private ThEGuis() {
    }

    public static GuiBridge arcaneTerminal() {
        return arcaneTerminal;
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
        final PartArcaneTerminal terminal = arcaneTerminalAt(id, world, x, y, z);
        return terminal == null ? null
                : withContext(new ContainerArcaneTerminal(player.inventory, terminal), world, x, y, z,
                        terminal.getSide());
    }

    @Nullable
    @Override
    public Object getClientGuiElement(final int id, final EntityPlayer player, final World world, final int x,
            final int y, final int z) {
        final PartArcaneTerminal terminal = arcaneTerminalAt(id, world, x, y, z);
        return terminal == null ? null : new GuiArcaneTerminal(player.inventory, terminal);
    }
}
