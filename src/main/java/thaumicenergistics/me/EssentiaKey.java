/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics.me;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.netty.buffer.ByteBuf;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

import thaumcraft.api.aspects.Aspect;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.storage.AEKeyFilter;

/**
 * One aspect of essentia, as a network names it. An aspect is nothing but its tag, so there is no secondary
 * part to drop.
 */
public final class EssentiaKey extends AEKey {

    private final Aspect aspect;
    private final int hash;

    private EssentiaKey(final Aspect aspect) {
        this.aspect = aspect;
        this.hash = aspect.getTag().hashCode();
    }

    public static EssentiaKey of(final Aspect aspect) {
        return new EssentiaKey(Objects.requireNonNull(aspect, "aspect"));
    }

    public static boolean is(@Nullable final AEKey what) {
        return what instanceof EssentiaKey;
    }

    public static AEKeyFilter filter() {
        return EssentiaKey::is;
    }

    @Override
    public AEKeyType getType() {
        return EssentiaKeyType.INSTANCE;
    }

    @Override
    public ResourceLocation getId() {
        return new ResourceLocation("aspect", this.aspect.getTag());
    }

    @Override
    public Object getPrimaryKey() {
        return this.aspect;
    }

    @Override
    public EssentiaKey dropSecondary() {
        return this;
    }

    public Aspect getAspect() {
        return this.aspect;
    }

    @Override
    public void toTag(final NBTTagCompound out) {
        out.setString("aspect", this.aspect.getTag());
    }

    @Nullable
    public static EssentiaKey fromTag(final NBTTagCompound tag) {
        final Aspect aspect = Aspect.getAspect(tag.getString("aspect"));
        return aspect == null ? null : new EssentiaKey(aspect);
    }

    @Override
    public void writeToPacket(final ByteBuf data) throws IOException {
        new PacketBuffer(data).writeString(this.aspect.getTag());
    }

    public static EssentiaKey fromPacket(final ByteBuf data) throws IOException {
        final String tag = new PacketBuffer(data).readString(Short.MAX_VALUE);
        final Aspect aspect = Aspect.getAspect(tag);
        if (aspect == null) {
            throw new IOException("Received an essentia key with an unknown aspect: " + tag);
        }
        return new EssentiaKey(aspect);
    }

    @Override
    protected ITextComponent computeDisplayName() {
        return new TextComponentString(this.aspect.getName());
    }

    @Override
    public String getModId() {
        return this.aspect.getImage().getNamespace();
    }

    @Override
    public void addDrops(final long amount, final List<ItemStack> drops, @Nonnull final World world,
            @Nonnull final BlockPos pos) {
        // Essentia has no item form of its own, so a machine that breaks voids what it held, as fluids do.
    }

    @Override
    public boolean equals(final Object o) {
        return this == o || o instanceof EssentiaKey other && this.aspect == other.aspect;
    }

    @Override
    public int hashCode() {
        return this.hash;
    }

    @Override
    public String toString() {
        return this.aspect.getTag();
    }
}
