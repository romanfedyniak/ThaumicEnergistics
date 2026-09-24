/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics.block;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import appeng.block.AEBaseTileBlock;

import thaumicenergistics.tile.TileInfusionProvider;

/**
 * The Infusion Provider, whose screen shows whether it has power and a channel.
 */
public class BlockInfusionProvider extends AEBaseTileBlock {

    private static final PropertyBool ACTIVE = PropertyBool.create("active");
    private static final PropertyBool POWERED = PropertyBool.create("powered");

    public BlockInfusionProvider() {
        super(Material.IRON);
        this.setTileEntity(TileInfusionProvider.class);
        this.setSoundType(SoundType.METAL);
        this.setDefaultState(this.getDefaultState().withProperty(ACTIVE, false).withProperty(POWERED, false));
    }

    @Override
    protected IProperty[] getAEStates() {
        return new IProperty[] { ACTIVE, POWERED };
    }

    @Override
    public IBlockState getActualState(final IBlockState state, final IBlockAccess world, final BlockPos pos) {
        final TileInfusionProvider tile = this.getTileEntity(world, pos);
        return super.getActualState(state, world, pos)
                .withProperty(ACTIVE, tile != null && tile.isActive())
                .withProperty(POWERED, tile != null && tile.isPowered());
    }
}
