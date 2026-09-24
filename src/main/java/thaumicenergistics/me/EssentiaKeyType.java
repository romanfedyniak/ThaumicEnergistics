/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics.me;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.netty.buffer.ByteBuf;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

import thaumcraft.api.blocks.BlocksTC;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;

import thaumicenergistics.ThaumicEnergistics;

/**
 * Essentia as a kind of content a network can hold. The amounts are AE2UD's defaults, which are also the
 * original mod's: eight essentia to a byte, one to a machine operation.
 */
public final class EssentiaKeyType extends AEKeyType {

    public static final ResourceLocation ID = ThaumicEnergistics.id("essentia");

    public static final EssentiaKeyType INSTANCE = new EssentiaKeyType();

    private EssentiaKeyType() {
        super(ID, EssentiaKey.class, new TextComponentTranslation("thaumicenergistics.key_type.essentia"));
    }

    @Override
    public AEKey readFromPacket(@Nonnull final ByteBuf input) throws IOException {
        return EssentiaKey.fromPacket(input);
    }

    @Nullable
    @Override
    public AEKey loadKeyFromTag(@Nonnull final NBTTagCompound tag) {
        return EssentiaKey.fromTag(tag);
    }

    @Override
    public TextFormatting getDisplayColour() {
        return TextFormatting.LIGHT_PURPLE;
    }

    @Override
    public ResourceLocation getButtonTexture() {
        return new ResourceLocation("appliedenergistics2", "textures/guis/states.png");
    }

    @Override
    public ItemStack getButtonIcon() {
        return new ItemStack(BlocksTC.jarNormal);
    }
}
