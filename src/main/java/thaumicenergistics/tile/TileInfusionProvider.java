/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics.tile;

import java.io.IOException;

import javax.annotation.Nonnull;

import io.netty.buffer.ByteBuf;

import it.unimi.dsi.fastutil.objects.Object2LongMap;

import net.minecraft.network.PacketBuffer;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectSource;

import appeng.api.config.Actionable;
import appeng.api.implementations.IPowerChannelState;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.networking.events.MENetworkChannelsChanged;
import appeng.api.networking.events.MENetworkEventSubscribe;
import appeng.api.networking.events.MENetworkPowerStatusChange;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.stacks.AEKey;
import appeng.api.util.AECableType;
import appeng.api.util.AEPartLocation;
import appeng.me.GridAccessException;
import appeng.me.helpers.MachineSource;
import appeng.tile.grid.AENetworkTile;
import appeng.util.Platform;

import thaumicenergistics.me.EssentiaKey;

/**
 * The Infusion Provider: the whole network's essentia offered to whatever draws on essentia sources around it -
 * a runic matrix during infusion, an essentia mirror, an essentia output. It gives and never takes.
 * <p>
 * Those ask what a source holds far more often than they take, so what it holds is read from the network's own
 * running count rather than asked of its storage.
 */
public class TileInfusionProvider extends AENetworkTile implements IAspectSource, IGridTickable, IPowerChannelState {

    /** How often what goggles show above it is brought up to date. */
    private static final int SYNC_TICKS = 20;

    private final IActionSource source = new MachineSource(this);

    private boolean clientActive;
    private boolean clientPowered;
    /** What was last sent, on the server; what goggles show, on the client. */
    private AspectList shown = new AspectList();

    public TileInfusionProvider() {
        this.getProxy().setFlags(GridFlags.REQUIRE_CHANNEL);
        this.getProxy().setIdlePowerUsage(1.0);
    }

    @Override
    public boolean isPowered() {
        return Platform.isClient() ? this.clientPowered : this.getProxy().isPowered();
    }

    @Override
    public boolean isActive() {
        return Platform.isClient() ? this.clientActive : this.getProxy().isActive();
    }

    @MENetworkEventSubscribe
    public void channelsChanged(final MENetworkChannelsChanged changed) {
        this.markForUpdate();
    }

    @MENetworkEventSubscribe
    public void powerChanged(final MENetworkPowerStatusChange changed) {
        this.markForUpdate();
    }

    @Override
    public AECableType getCableConnectionType(final AEPartLocation dir) {
        return AECableType.SMART;
    }

    @Nonnull
    @Override
    public TickingRequest getTickingRequest(@Nonnull final IGridNode node) {
        return new TickingRequest(SYNC_TICKS, SYNC_TICKS, false, false);
    }

    /**
     * Goggles of revealing show a container's aspects above it, read on the client, so the client is sent the
     * network's essentia whenever it has changed.
     */
    @Nonnull
    @Override
    public TickRateModulation tickingRequest(@Nonnull final IGridNode node, final int ticksSinceLastCall) {
        final AspectList now = this.getAspects();
        if (!sameAspects(now, this.shown)) {
            this.shown = now;
            this.markForUpdate();
        }
        return TickRateModulation.SAME;
    }

    private static boolean sameAspects(final AspectList a, final AspectList b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (final Aspect aspect : a.getAspects()) {
            if (aspect != null && a.getAmount(aspect) != b.getAmount(aspect)) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void writeToStream(final ByteBuf data) throws IOException {
        super.writeToStream(data);
        data.writeBoolean(this.getProxy().isActive());
        data.writeBoolean(this.getProxy().isPowered());
        final PacketBuffer buffer = new PacketBuffer(data);
        final Aspect[] aspects = this.shown.getAspects();
        buffer.writeVarInt(aspects.length);
        for (final Aspect aspect : aspects) {
            buffer.writeString(aspect == null ? "" : aspect.getTag());
            buffer.writeVarInt(aspect == null ? 0 : this.shown.getAmount(aspect));
        }
    }

    @Override
    protected boolean readFromStream(final ByteBuf data) throws IOException {
        final boolean changed = super.readFromStream(data);
        final boolean active = data.readBoolean();
        final boolean powered = data.readBoolean();
        final boolean flipped = active != this.clientActive || powered != this.clientPowered;
        this.clientActive = active;
        this.clientPowered = powered;

        final PacketBuffer buffer = new PacketBuffer(data);
        final AspectList aspects = new AspectList();
        for (int i = buffer.readVarInt(); i > 0; i--) {
            final Aspect aspect = Aspect.getAspect(buffer.readString(Short.MAX_VALUE));
            final int amount = buffer.readVarInt();
            if (aspect != null && amount > 0) {
                aspects.add(aspect, amount);
            }
        }
        this.shown = aspects;
        return changed || flipped;
    }

    private long stored(final Aspect aspect) {
        if (aspect == null || Platform.isClient() || !this.getProxy().isActive()) {
            return 0;
        }
        try {
            return this.getProxy().getStorage().getCachedInventory().get(EssentiaKey.of(aspect));
        } catch (final GridAccessException e) {
            return 0;
        }
    }

    @Override
    public AspectList getAspects() {
        if (Platform.isClient()) {
            return this.shown;
        }
        final AspectList list = new AspectList();
        if (!this.getProxy().isActive()) {
            return list;
        }
        try {
            for (final Object2LongMap.Entry<AEKey> entry : this.getProxy().getStorage().getCachedInventory()) {
                if (entry.getKey() instanceof EssentiaKey essentia && entry.getLongValue() > 0) {
                    list.add(essentia.getAspect(), (int) Math.min(entry.getLongValue(), Integer.MAX_VALUE));
                }
            }
        } catch (final GridAccessException e) {
            // Not on a grid: nothing to offer.
        }
        return list;
    }

    @Override
    public void setAspects(final AspectList aspects) {
    }

    @Override
    public boolean doesContainerAccept(final Aspect aspect) {
        return false;
    }

    @Override
    public int addToContainer(final Aspect aspect, final int amount) {
        return amount;
    }

    /**
     * All or nothing, as Thaumcraft asks it.
     */
    @Override
    public boolean takeFromContainer(final Aspect aspect, final int amount) {
        if (amount <= 0 || this.stored(aspect) < amount) {
            return false;
        }
        try {
            final EssentiaKey key = EssentiaKey.of(aspect);
            final long taken = Platform.poweredExtraction(this.getProxy().getEnergy(),
                    this.getProxy().getStorage().getInventory(), key, amount, this.source);
            if (taken < amount) {
                if (taken > 0) {
                    Platform.poweredInsert(this.getProxy().getEnergy(), this.getProxy().getStorage().getInventory(),
                            key, taken, this.source, Actionable.MODULATE);
                }
                return false;
            }
            return true;
        } catch (final GridAccessException e) {
            return false;
        }
    }

    @Override
    public boolean takeFromContainer(final AspectList aspects) {
        return false;
    }

    @Override
    public boolean doesContainerContainAmount(final Aspect aspect, final int amount) {
        return this.stored(aspect) >= amount;
    }

    @Override
    public boolean doesContainerContain(final AspectList aspects) {
        return false;
    }

    @Override
    public int containerContains(final Aspect aspect) {
        return (int) Math.min(this.stored(aspect), Integer.MAX_VALUE);
    }

    @Override
    public boolean isBlocked() {
        return false;
    }
}
