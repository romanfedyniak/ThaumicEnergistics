/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics.mixin;

import javax.annotation.Nullable;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

import org.spongepowered.asm.mixin.Mixin;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaTransport;

import appeng.tile.misc.TileInterface;
import appeng.tile.networking.TileCableBus;

import thaumicenergistics.me.InterfaceEssentia;
import thaumicenergistics.me.InterfaceEssentiaPulls;

/**
 * Thaumcraft's tubes look for {@link IEssentiaTransport} on the block entity itself, not for a capability, so the
 * interface block and the cable bus have to be one. Each call goes to the interface on that face: the block's own,
 * or the part a cable bus has there. A face without one is not a tube's neighbour at all.
 */
@Mixin(value = { TileInterface.class, TileCableBus.class }, remap = false)
public abstract class MixinInterfaceEssentia implements IEssentiaTransport {

    @Nullable
    private InterfaceEssentia thaumicenergistics$on(final EnumFacing face) {
        return InterfaceEssentia.of((TileEntity) (Object) this, face);
    }

    @Override
    public boolean isConnectable(final EnumFacing face) {
        final InterfaceEssentia on = this.thaumicenergistics$on(face);
        if (on == null) {
            return false;
        }
        InterfaceEssentiaPulls.watch((TileEntity) (Object) this);
        return on.isConnectable(face);
    }

    @Override
    public boolean canInputFrom(final EnumFacing face) {
        final InterfaceEssentia on = this.thaumicenergistics$on(face);
        return on != null && on.canInputFrom(face);
    }

    @Override
    public boolean canOutputTo(final EnumFacing face) {
        final InterfaceEssentia on = this.thaumicenergistics$on(face);
        return on != null && on.canOutputTo(face);
    }

    @Override
    public void setSuction(final Aspect aspect, final int amount) {
    }

    @Override
    public Aspect getSuctionType(final EnumFacing face) {
        return null;
    }

    @Override
    public int getSuctionAmount(final EnumFacing face) {
        final InterfaceEssentia on = this.thaumicenergistics$on(face);
        return on == null ? 0 : on.getSuctionAmount(face);
    }

    @Override
    public int getMinimumSuction() {
        return 1;
    }

    @Override
    public Aspect getEssentiaType(final EnumFacing face) {
        final InterfaceEssentia on = this.thaumicenergistics$on(face);
        return on == null ? null : on.getEssentiaType(face);
    }

    @Override
    public int getEssentiaAmount(final EnumFacing face) {
        final InterfaceEssentia on = this.thaumicenergistics$on(face);
        return on == null ? 0 : on.getEssentiaAmount(face);
    }

    @Override
    public int takeEssentia(final Aspect aspect, final int amount, final EnumFacing face) {
        final InterfaceEssentia on = this.thaumicenergistics$on(face);
        return on == null ? 0 : on.takeEssentia(aspect, amount, face);
    }

    @Override
    public int addEssentia(final Aspect aspect, final int amount, final EnumFacing face) {
        final InterfaceEssentia on = this.thaumicenergistics$on(face);
        return on == null ? 0 : on.addEssentia(aspect, amount, face);
    }
}
