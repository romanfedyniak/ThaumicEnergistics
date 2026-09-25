/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics.client.gui;

import java.awt.Rectangle;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import appeng.client.gui.AEBaseGui;

import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.container.ContainerArcaneAssembler;
import thaumicenergistics.tile.TileArcaneAssembler;

/**
 * The original's window for the assembler: the ring that lights while it crafts, what it is crafting at its
 * centre, a dot for each crystal the craft takes, and the card column beside it.
 */
public class GuiArcaneAssembler extends AEBaseGui {

    private static final ResourceLocation INACTIVE = ThaumicEnergistics.id("textures/gui/arcane_assembler/inactive.png");
    private static final ResourceLocation ACTIVE = ThaumicEnergistics.id("textures/gui/arcane_assembler/active.png");
    private static final ResourceLocation ASPECTS = ThaumicEnergistics.id("textures/gui/arcane_assembler/aspects.png");

    private static final int WIDTH = 210;
    private static final int HEIGHT = 231;
    /** The window proper, left of the card column. */
    private static final int WINDOW_WIDTH = 176;

    /**
     * The original's card column holds five; the sixth slot is its first slot again, drawn under the fifth, and
     * the column's lower edge under that.
     */
    private static final int COLUMN_X = 178;
    private static final int COLUMN_WIDTH = 32;
    private static final int SLOT_V = 7;
    private static final int SLOT_ROW = 18;
    private static final int COLUMN_BOTTOM_V = 97;
    private static final int COLUMN_BOTTOM = 7;

    /** Each primal's dot in the aspects texture, in {@code ArcaneGrid.PRIMALS} order, as the original put them. */
    private static final int[][] DOTS = { { 69, 2 }, { 21, 25 }, { 117, 25 }, { 21, 82 }, { 117, 82 }, { 69, 106 } };
    private static final int DOT_SIZE = 40;

    /** The network tool's plate: AE2UD's panel, its edge, and the slot wells inside it. */
    private static final int TOOLBOX_EDGE = 7;
    private static final int TOOLBOX_INSET = TOOLBOX_EDGE + 1;
    private static final int TOOLBOX_SIZE = TOOLBOX_EDGE * 2 + 3 * 18;

    private static final int CRAFTING_X = 81;
    private static final int CRAFTING_Y = 66;

    private static final int TITLE_Y = 3;
    private static final int LABEL_Y = HEIGHT - 92;
    private static final int TEXT_COLOR = 0x404040;
    private static final int SHORT_COLOR = 0xC02020;

    private final ContainerArcaneAssembler container;
    private final TileArcaneAssembler assembler;
    /** How far the ring has lit, easing in and out as crafts start and end. */
    private float lit;

    public GuiArcaneAssembler(final InventoryPlayer inventory, final TileArcaneAssembler assembler) {
        super(new ContainerArcaneAssembler(inventory, assembler));
        this.container = (ContainerArcaneAssembler) this.inventorySlots;
        this.assembler = assembler;
        this.xSize = WIDTH;
        this.ySize = HEIGHT;
    }

    @Override
    public void drawBG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        final boolean crafting = !this.assembler.getCrafting().isEmpty();
        final float step = 0.05F * this.mc.getRenderPartialTicks();
        this.lit = Math.max(0.0F, Math.min(1.0F, this.lit + (crafting ? step : -step)));

        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        if (this.lit < 1.0F) {
            this.mc.getTextureManager().bindTexture(INACTIVE);
            drawModalRectWithCustomSizedTexture(offsetX, offsetY, 0, 0, WIDTH, HEIGHT, WIDTH, HEIGHT);
        }
        if (this.lit > 0.0F) {
            GlStateManager.color(1.0F, 1.0F, 1.0F, this.lit);
            this.mc.getTextureManager().bindTexture(ACTIVE);
            drawModalRectWithCustomSizedTexture(offsetX, offsetY, 0, 0, WIDTH, HEIGHT, WIDTH, HEIGHT);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }

        this.mc.getTextureManager().bindTexture(INACTIVE);
        final int sixth = offsetY + COLUMN_BOTTOM_V;
        drawModalRectWithCustomSizedTexture(offsetX + COLUMN_X, sixth, COLUMN_X, SLOT_V, COLUMN_WIDTH, SLOT_ROW,
                WIDTH, HEIGHT);
        drawModalRectWithCustomSizedTexture(offsetX + COLUMN_X, sixth + SLOT_ROW, COLUMN_X, COLUMN_BOTTOM_V,
                COLUMN_WIDTH, COLUMN_BOTTOM, WIDTH, HEIGHT);

        if (this.container.hasToolbox()) {
            drawPanel(offsetX + toolboxX(), offsetY + toolboxY(), TOOLBOX_SIZE, TOOLBOX_SIZE);
            for (int row = 0; row < 3; row++) {
                for (int column = 0; column < 3; column++) {
                    drawSlotWell(offsetX + ContainerArcaneAssembler.TOOLBOX_X + column * 18,
                            offsetY + ContainerArcaneAssembler.TOOLBOX_Y + row * 18);
                }
            }
        }

        this.mc.getTextureManager().bindTexture(ASPECTS);
        for (int index = 0; index < DOTS.length; index++) {
            final boolean taken = (this.container.crystals & 1L << index) != 0;
            GlStateManager.color(1.0F, 1.0F, 1.0F, taken ? 0.2F + this.lit * 0.8F : 0.2F + this.lit * 0.3F);
            final int x = DOTS[index][0];
            final int y = DOTS[index][1];
            drawModalRectWithCustomSizedTexture(offsetX + x, offsetY + y, x, y, DOT_SIZE, DOT_SIZE, WIDTH, HEIGHT);
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static int toolboxX() {
        return ContainerArcaneAssembler.TOOLBOX_X - TOOLBOX_INSET;
    }

    private static int toolboxY() {
        return ContainerArcaneAssembler.TOOLBOX_Y - TOOLBOX_INSET;
    }

    /** The toolbox stands outside the window, so a recipe viewer has to be told to keep off it. */
    @Override
    public List<Rectangle> getJEIExclusionArea() {
        return this.container.hasToolbox()
                ? Collections.singletonList(new Rectangle(this.guiLeft + toolboxX(), this.guiTop + toolboxY(),
                        TOOLBOX_SIZE, TOOLBOX_SIZE))
                : Collections.emptyList();
    }

    @Override
    public void drawFG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        this.fontRenderer.drawString(this.getGuiDisplayName(I18n.format("tile.thaumicenergistics.arcane_assembler.name")),
                8, TITLE_Y, TEXT_COLOR);
        this.fontRenderer.drawString(I18n.format("container.inventory"), 8, LABEL_Y, TEXT_COLOR);

        final ItemStack crafting = this.assembler.getCrafting();
        if (!crafting.isEmpty()) {
            this.drawItem(CRAFTING_X, CRAFTING_Y, crafting);
        }

        this.drawVis();

        if (!crafting.isEmpty() && this.isPointInRegion(CRAFTING_X, CRAFTING_Y, 16, 16, mouseX, mouseY)) {
            this.renderToolTip(crafting, mouseX - offsetX, mouseY - offsetY);
        }
    }

    /** What the craft costs and what the aura holds, right of the inventory's name; the cost red while it waits. */
    private void drawVis() {
        final boolean planned = !this.assembler.getCrafting().isEmpty();
        final String cost = planned
                ? I18n.format("gui.thaumicenergistics.arcane_terminal.cost", this.container.visCost) + "   "
                : "";
        final String available = I18n.format("gui.thaumicenergistics.arcane_terminal.available",
                this.container.visAvailable);
        final int right = WINDOW_WIDTH - 8;
        final int left = right - this.fontRenderer.getStringWidth(cost + available);
        final boolean waiting = planned && !this.container.paid && this.container.visCost > this.container.visAvailable;
        this.fontRenderer.drawString(cost, left, LABEL_Y, waiting ? SHORT_COLOR : TEXT_COLOR);
        this.fontRenderer.drawString(available, left + this.fontRenderer.getStringWidth(cost), LABEL_Y, TEXT_COLOR);
    }
}
