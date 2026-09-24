/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics.client;

import javax.annotation.Nullable;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import thaumcraft.api.aspects.Aspect;

import appeng.api.client.AEKeyModelContext;
import appeng.api.client.AEKeyRenderHandler;
import appeng.api.stacks.AEKey;

import thaumicenergistics.me.EssentiaKey;

/**
 * An aspect is drawn as Thaumcraft draws it in its own screens: its loose icon texture, tinted with the aspect's
 * colour and blended by alpha, unlit. Baked into the block atlas the icon's faint dark fill turns solid.
 */
@SideOnly(Side.CLIENT)
public class EssentiaKeyRenderHandler implements AEKeyRenderHandler {

    @Nullable
    @Override
    public IBakedModel getModel(final AEKey what, final AEKeyModelContext context) {
        return null;
    }

    @Override
    public boolean drawsItself(final AEKey what) {
        return what instanceof EssentiaKey;
    }

    @Override
    public void draw(final AEKey what) {
        if (!(what instanceof EssentiaKey essentia)) {
            return;
        }
        final Aspect aspect = essentia.getAspect();
        final int color = aspect.getColor();
        final float r = (color >> 16 & 0xFF) / 255.0F;
        final float g = (color >> 8 & 0xFF) / 255.0F;
        final float b = (color & 0xFF) / 255.0F;

        Minecraft.getMinecraft().getTextureManager().bindTexture(aspect.getImage());
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        // A slot draws flat items unlit and a world view lights them; leave it as it was found.
        final boolean lit = GL11.glIsEnabled(GL11.GL_LIGHTING);
        GlStateManager.disableLighting();

        final Tessellator tessellator = Tessellator.getInstance();
        final BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        buffer.pos(0, 0, 0.5).tex(0, 1).color(r, g, b, 1.0F).endVertex();
        buffer.pos(1, 0, 0.5).tex(1, 1).color(r, g, b, 1.0F).endVertex();
        buffer.pos(1, 1, 0.5).tex(1, 0).color(r, g, b, 1.0F).endVertex();
        buffer.pos(0, 1, 0.5).tex(0, 0).color(r, g, b, 1.0F).endVertex();
        tessellator.draw();

        if (lit) {
            GlStateManager.enableLighting();
        }
    }
}
