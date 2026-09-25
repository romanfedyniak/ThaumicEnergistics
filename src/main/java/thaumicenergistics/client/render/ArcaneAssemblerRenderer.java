/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemStack;

import thaumicenergistics.tile.TileArcaneAssembler;

/**
 * What the assembler is crafting, turning slowly behind its glass, as the original showed its knowledge core.
 */
public class ArcaneAssemblerRenderer extends TileEntitySpecialRenderer<TileArcaneAssembler> {

    private static final double DEGREES_PER_MILLI = 0.05;

    @Override
    public void render(final TileArcaneAssembler tile, final double x, final double y, final double z,
            final float partialTicks, final int destroyStage, final float alpha) {
        final ItemStack crafting = tile.getCrafting();
        if (crafting.isEmpty()) {
            return;
        }
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5, y + 0.5, z + 0.5);
        GlStateManager.rotate((float) (Minecraft.getSystemTime() * DEGREES_PER_MILLI % 360), 0, 1, 0);
        GlStateManager.scale(0.5F, 0.5F, 0.5F);
        RenderHelper.enableStandardItemLighting();
        Minecraft.getMinecraft().getRenderItem().renderItem(crafting, ItemCameraTransforms.TransformType.FIXED);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.popMatrix();
    }
}
