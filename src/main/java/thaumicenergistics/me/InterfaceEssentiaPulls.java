/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics.me;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.IEssentiaTransport;

/**
 * An ME interface taking essentia in has to pull it out of the tube, as a jar does: tubes only carry essentia
 * toward whatever draws on them. AE2UD's interfaces do not tick in the world, so an interface a tube has asked
 * about is remembered here and pulls on the tubes around it every few ticks, at a jar's pace. One with no tube
 * left beside it is dropped until a tube asks again.
 */
public final class InterfaceEssentiaPulls {

    /** A jar fills every fifth tick. */
    private static final int INTERVAL = 5;

    private static final Set<TileEntity> WATCHED = Collections.newSetFromMap(new WeakHashMap<>());

    private int ticks;

    private InterfaceEssentiaPulls() {
    }

    public static final InterfaceEssentiaPulls INSTANCE = new InterfaceEssentiaPulls();

    /** Called whenever a tube asks an interface whether it connects. */
    public static void watch(final TileEntity tile) {
        if (tile.hasWorld() && !tile.getWorld().isRemote) {
            WATCHED.add(tile);
        }
    }

    @SubscribeEvent
    public void onServerTick(final TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ++this.ticks % INTERVAL != 0 || WATCHED.isEmpty()) {
            return;
        }
        for (final TileEntity tile : new ArrayList<>(WATCHED)) {
            if (tile.isInvalid() || !tile.hasWorld() || !this.pullAround(tile)) {
                WATCHED.remove(tile);
            }
        }
    }

    /**
     * @return whether any face still has a tube beside it.
     */
    private boolean pullAround(final TileEntity tile) {
        boolean tubes = false;
        for (final EnumFacing face : EnumFacing.values()) {
            final InterfaceEssentia on = InterfaceEssentia.of(tile, face);
            if (on == null) {
                continue;
            }
            final TileEntity neighbour = ThaumcraftApiHelper.getConnectableTile(tile.getWorld(), tile.getPos(), face);
            if (neighbour instanceof IEssentiaTransport transport) {
                tubes = true;
                on.pullFrom(transport, face.getOpposite());
            }
        }
        return tubes;
    }
}
