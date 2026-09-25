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
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import appeng.block.AEBaseTileBlock;

import thaumicenergistics.ThEGuis;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.tile.TileArcaneAssembler;

/**
 * The Arcane Assembler: a frame round glass that lights while it has power and glows while it crafts.
 */
public class BlockArcaneAssembler extends AEBaseTileBlock {

    private static final PropertyBool ACTIVE = PropertyBool.create("active");
    private static final PropertyBool POWERED = PropertyBool.create("powered");

    public BlockArcaneAssembler() {
        super(Material.IRON);
        this.setTileEntity(TileArcaneAssembler.class);
        this.setSoundType(SoundType.GLASS);
        this.setOpaque(false);
        this.lightOpacity = 1;
        this.setDefaultState(this.getDefaultState().withProperty(ACTIVE, false).withProperty(POWERED, false));
    }

    @Override
    protected IProperty[] getAEStates() {
        return new IProperty[] { ACTIVE, POWERED };
    }

    @Override
    public IBlockState getActualState(final IBlockState state, final IBlockAccess world, final BlockPos pos) {
        final TileArcaneAssembler tile = this.getTileEntity(world, pos);
        return super.getActualState(state, world, pos)
                .withProperty(ACTIVE, tile != null && !tile.getCrafting().isEmpty())
                .withProperty(POWERED, tile != null && tile.isPowered());
    }

    @Override
    public boolean onActivated(final World world, final BlockPos pos, final EntityPlayer player, final EnumHand hand,
            final ItemStack heldItem, final EnumFacing side, final float hitX, final float hitY,
            final float hitZ) {
        if (player.isSneaking() || this.getTileEntity(world, pos) == null) {
            return false;
        }
        if (!world.isRemote) {
            player.openGui(ThaumicEnergistics.INSTANCE, ThEGuis.ARCANE_ASSEMBLER, world, pos.getX(), pos.getY(),
                    pos.getZ());
        }
        return true;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.TRANSLUCENT;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public boolean canRenderInLayer(final IBlockState state, final BlockRenderLayer layer) {
        return layer == BlockRenderLayer.SOLID || layer == BlockRenderLayer.TRANSLUCENT;
    }

    @Override
    public boolean isFullCube(final IBlockState state) {
        return false;
    }
}
