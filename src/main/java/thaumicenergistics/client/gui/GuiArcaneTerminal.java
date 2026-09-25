/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics.client.gui;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;

import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.common.items.casters.CasterManager;

import appeng.api.config.ActionItems;
import appeng.api.config.Settings;
import appeng.api.storage.ITerminalHost;
import appeng.client.gui.implementations.GuiMEMonitorable;
import appeng.container.implementations.ContainerMEMonitorable;
import appeng.client.gui.widgets.GuiImgButton;
import appeng.container.slot.SlotRestrictedInput;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketInventoryAction;
import appeng.helpers.InventoryAction;

import thaumicenergistics.container.ContainerArcaneTerminal;
import thaumicenergistics.container.IArcaneTerminal;
import thaumicenergistics.crafting.ArcaneCosts;
import thaumicenergistics.crafting.ArcaneGrid;
import thaumicenergistics.part.PartArcaneTerminal;

/**
 * An ME terminal with Thaumcraft's own arcane workbench on a panel beside it, at its own size and from its own
 * texture: the grid inside the ring of crystal sockets, which the network fills, and the result box. The vis is
 * written under it and the Arcane Charging Card sits over the result. The view cells hang in a row on a plate of
 * their own above it.
 */
public class GuiArcaneTerminal extends GuiMEMonitorable {

    private static final ResourceLocation WORKBENCH = new ResourceLocation("thaumcraft",
            "textures/gui/arcaneworkbench.png");

    /** As far from the window as AE2's plates beside it stand. */
    private static final int PANEL_X = 198;
    private static final int PAD = 4;
    private static final int GAP = 3;
    /** The view cells' plate, laid across over the workbench rather than down beside the window. */
    private static final int CELLS_EDGE = 7;
    private static final int CELLS_HEIGHT = CELLS_EDGE + 18 + CELLS_EDGE;
    /** Thaumcraft's workbench screen down to the result box, as its texture has it. */
    private static final int BENCH_WIDTH = 190;
    private static final int BENCH_HEIGHT = 143;
    private static final int PANEL_WIDTH = PAD + BENCH_WIDTH + PAD;
    private static final int VIS_Y = PAD + BENCH_HEIGHT + 2;
    private static final int PANEL_HEIGHT = VIS_Y + 9 + PAD;

    /** Where Thaumcraft's own workbench screen puts things, from its corner. */
    private static final int GRID = 40;
    private static final int GRID_STEP = 24;
    private static final int[] CRYSTAL_X = { 64, 17, 112, 17, 112, 64 };
    private static final int[] CRYSTAL_Y = { 13, 35, 35, 93, 93, 115 };
    private static final int RESULT_X = 160;
    private static final int RESULT_Y = 64;
    private static final int RESULT_BOX_BOTTOM = 93;
    /** Over the result box, where the workbench is bare. */
    private static final int CARD_Y = 8;
    private static final int GLOW_U = 192;
    private static final int GLOW_SIZE = 64;


    private static final int TEXT_COLOR = 0x404040;
    private static final int SHORT_COLOR = 0xAA0000;
    private static final int MISSING_GLOW = 0xFF2020;

    private final ContainerMEMonitorable monitor;
    private final IArcaneTerminal terminal;
    private GuiImgButton clearBtn;
    private GuiImgButton clearToPlayerBtn;

    public GuiArcaneTerminal(final InventoryPlayer inventoryPlayer, final PartArcaneTerminal part) {
        this(inventoryPlayer, part, new ContainerArcaneTerminal(inventoryPlayer, part));
    }

    protected <C extends ContainerMEMonitorable & IArcaneTerminal> GuiArcaneTerminal(
            final InventoryPlayer inventoryPlayer, final ITerminalHost host, final C container) {
        super(inventoryPlayer, host, container);
        this.monitor = container;
        this.terminal = container;
        this.setViewCellColumnShown(false);
    }

    private static int benchX() {
        return PANEL_X + PAD;
    }

    /** The cells' plate and the workbench under it, centred together on the window's height. */
    private int cellsY() {
        return (this.ySize - CELLS_HEIGHT - GAP - PANEL_HEIGHT) / 2;
    }

    private int panelY() {
        return this.cellsY() + CELLS_HEIGHT + GAP;
    }

    private int benchY() {
        return this.panelY() + PAD;
    }

    private int cellsWidth() {
        return rowWidth(this.monitor.getViewCells().length);
    }

    private static int cellX(final int index) {
        return PANEL_X + CELLS_EDGE + 1 + 18 * index;
    }

    private static int crystalX(final int index) {
        return benchX() + CRYSTAL_X[index];
    }

    private int crystalY(final int index) {
        return this.benchY() + CRYSTAL_Y[index];
    }

    @Override
    public void initGui() {
        super.initGui();

        final Slot[] grid = this.terminal.getGridSlots();
        for (int index = 0; index < grid.length; index++) {
            grid[index].xPos = benchX() + GRID + GRID_STEP * (index % 3);
            grid[index].yPos = this.benchY() + GRID + GRID_STEP * (index / 3);
        }
        final Slot result = this.terminal.getOutputSlot();
        result.xPos = benchX() + RESULT_X;
        result.yPos = this.benchY() + RESULT_Y;

        final Slot card = this.terminal.getCardSlot();
        card.xPos = benchX() + RESULT_X;
        card.yPos = this.benchY() + CARD_Y;

        for (int index = 0; index < this.monitor.getViewCells().length; index++) {
            final Slot cell = this.monitor.getCellViewSlot(index);
            if (cell != null) {
                cell.xPos = cellX(index);
                cell.yPos = this.cellsY() + CELLS_EDGE + 1;
            }
        }

        final List<Slot> cards = this.terminalCards();
        for (int index = 0; index < cards.size(); index++) {
            cards.get(index).xPos = this.cardsX() + CELLS_EDGE + 1 + 18 * index;
            cards.get(index).yPos = this.cellsY() + CELLS_EDGE + 1;
        }

        // Under the result box, where the workbench has nothing.
        final int buttonX = this.guiLeft + benchX() + RESULT_X - 1;
        final int buttonY = this.guiTop + this.benchY() + RESULT_BOX_BOTTOM + 3;
        this.buttonList.add(this.clearBtn = new GuiImgButton(buttonX, buttonY, Settings.ACTIONS,
                ActionItems.STASH));
        this.clearBtn.setHalfSize(true);
        this.buttonList.add(this.clearToPlayerBtn = new GuiImgButton(buttonX + 10, buttonY, Settings.ACTIONS,
                ActionItems.STASH_TO_PLAYER_INV));
        this.clearToPlayerBtn.setHalfSize(true);
    }

    @Override
    protected void actionPerformed(final GuiButton btn) {
        super.actionPerformed(btn);

        if (btn == this.clearBtn || btn == this.clearToPlayerBtn) {
            final InventoryAction action = btn == this.clearBtn
                    ? InventoryAction.MOVE_REGION
                    : InventoryAction.MOVE_REGION_TO_PLAYER;
            NetworkHandler.instance().sendToServer(
                    new PacketInventoryAction(action, this.terminal.getGridSlots()[0].slotNumber, 0));
        }
    }

    /** A wireless terminal's own cards, which sit in a row of their own beside the view cells'. */
    private List<Slot> terminalCards() {
        final List<Slot> cards = new ArrayList<>();
        for (final Slot slot : this.inventorySlots.inventorySlots) {
            if (slot instanceof SlotRestrictedInput upgrade && upgrade != this.terminal.getCardSlot()
                    && upgrade.getPlaceableItemType() == SlotRestrictedInput.PlacableItemType.UPGRADES) {
                cards.add(slot);
            }
        }
        return cards;
    }

    private int cardsX() {
        return PANEL_X + this.cellsWidth() + GAP;
    }

    private static int rowWidth(final int slots) {
        return CELLS_EDGE * 2 + 18 * slots;
    }

    @Override
    protected boolean drawsWirelessUpgradePlate() {
        return false;
    }

    @Override
    protected int getHorizontalShift() {
        return (Math.max(this.xSize, PANEL_X + PANEL_WIDTH) - this.xSize) / 2;
    }

    @Override
    public List<Rectangle> getJEIExclusionArea() {
        final List<Rectangle> areas = new ArrayList<>(super.getJEIExclusionArea());
        areas.add(new Rectangle(this.guiLeft + PANEL_X, this.guiTop + this.panelY(), PANEL_WIDTH, PANEL_HEIGHT));
        areas.add(new Rectangle(this.guiLeft + PANEL_X, this.guiTop + this.cellsY(), this.cellsWidth(),
                CELLS_HEIGHT));
        final int cards = this.terminalCards().size();
        if (cards > 0) {
            areas.add(new Rectangle(this.guiLeft + this.cardsX(), this.guiTop + this.cellsY(), rowWidth(cards),
                    CELLS_HEIGHT));
        }
        return areas;
    }

    @Override
    public void drawBG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        super.drawBG(offsetX, offsetY, mouseX, mouseY);


        drawPanel(offsetX + PANEL_X, offsetY + this.panelY(), PANEL_WIDTH, PANEL_HEIGHT);

        this.mc.getTextureManager().bindTexture(WORKBENCH);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
        this.drawTexturedModalRect(offsetX + benchX(), offsetY + this.benchY(), 0, 0, BENCH_WIDTH, BENCH_HEIGHT);
        this.drawGlows(offsetX, offsetY);
        GlStateManager.disableBlend();

        drawSlotWell(offsetX + benchX() + RESULT_X, offsetY + this.benchY() + CARD_Y);

        drawPanel(offsetX + PANEL_X, offsetY + this.cellsY(), this.cellsWidth(), CELLS_HEIGHT);
        final List<Slot> cards = this.terminalCards();
        if (!cards.isEmpty()) {
            drawPanel(offsetX + this.cardsX(), offsetY + this.cellsY(), rowWidth(cards.size()), CELLS_HEIGHT);
            for (final Slot card : cards) {
                drawSlotWell(offsetX + card.xPos, offsetY + card.yPos);
            }
        }
        for (int index = 0; index < this.monitor.getViewCells().length; index++) {
            if (this.monitor.getCellViewSlot(index) != null) {
                drawSlotWell(offsetX + cellX(index), offsetY + this.cellsY() + CELLS_EDGE + 1);
            }
        }
    }

    /**
     * The workbench's slow turning glow behind each crystal the recipe takes, in the aspect's colour, or red for
     * one the network is short of.
     */
    private void drawGlows(final int offsetX, final int offsetY) {
        final float ticks = this.mc.getRenderViewEntity() == null ? 0
                : this.mc.getRenderViewEntity().ticksExisted + this.mc.getRenderPartialTicks();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        for (int index = 0; index < ArcaneGrid.PRIMALS.length; index++) {
            if (this.costs().getCrystalsNeeded(index) == 0) {
                continue;
            }
            final boolean missing = this.costs().isCrystalMissing(index);
            final int color = missing ? MISSING_GLOW : ArcaneGrid.PRIMALS[index].getColor();
            GlStateManager.color((color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F,
                    (color & 0xFF) / 255.0F, missing ? 0.8F : 0.33F);
            GlStateManager.pushMatrix();
            GlStateManager.translate(offsetX + crystalX(index) + 7.5F, offsetY + this.crystalY(index) + 8.0F, 0.0F);
            GlStateManager.rotate(index * 60 + ticks % 360, 0.0F, 0.0F, 1.0F);
            GlStateManager.scale(0.5F, 0.5F, 1.0F);
            this.drawTexturedModalRect(-GLOW_SIZE / 2, -GLOW_SIZE / 2, GLOW_U, 0, GLOW_SIZE, GLOW_SIZE);
            GlStateManager.popMatrix();
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    }

    @Override
    public void drawFG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        super.drawFG(offsetX, offsetY, mouseX, mouseY);

        final String cost = this.costs().getVisCost() < 0 ? ""
                : I18n.format("gui.thaumicenergistics.arcane_terminal.cost", this.costs().getVisCost()) + "   ";
        final String available = I18n.format("gui.thaumicenergistics.arcane_terminal.available",
                this.costs().getVisAvailable());
        final int left = PANEL_X + (PANEL_WIDTH - this.fontRenderer.getStringWidth(cost + available)) / 2;
        this.fontRenderer.drawString(cost, left, this.panelY() + VIS_Y, this.costs().isShortOfVis() ? SHORT_COLOR : TEXT_COLOR);
        this.fontRenderer.drawString(available, left + this.fontRenderer.getStringWidth(cost), this.panelY() + VIS_Y,
                TEXT_COLOR);

        RenderHelper.enableGUIStandardItemLighting();
        for (int index = 0; index < ArcaneGrid.PRIMALS.length; index++) {
            final int needed = this.costs().getCrystalsNeeded(index);
            if (needed > 0) {
                final ItemStack crystal = this.crystal(index);
                final String count = this.costs().isCrystalMissing(index)
                        ? TextFormatting.RED.toString() + needed
                        : null;
                this.itemRender.renderItemAndEffectIntoGUI(crystal, crystalX(index), this.crystalY(index));
                this.itemRender.renderItemOverlayIntoGUI(this.fontRenderer, crystal, crystalX(index),
                        this.crystalY(index), count);
            }
        }
        RenderHelper.disableStandardItemLighting();
    }

    private ArcaneCosts costs() {
        return this.terminal.getCosts();
    }

    private ItemStack crystal(final int index) {
        return ThaumcraftApiHelper.makeCrystal(ArcaneGrid.PRIMALS[index],
                Math.max(1, this.costs().getCrystalsNeeded(index)));
    }

    @Override
    public void drawScreen(final int mouseX, final int mouseY, final float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);

        final int x = mouseX - this.guiLeft;
        final int y = mouseY - this.guiTop;
        for (int index = 0; index < ArcaneGrid.PRIMALS.length; index++) {
            if (within(x, y, crystalX(index), this.crystalY(index), 16, 16)) {
                this.drawHoveringText(this.crystalTooltip(index), mouseX, mouseY);
                return;
            }
        }
        if (within(x, y, PANEL_X, this.panelY() + VIS_Y, PANEL_WIDTH, this.fontRenderer.FONT_HEIGHT)) {
            this.drawHoveringText(this.visTooltip(), mouseX, mouseY);
        }
    }

    private List<String> crystalTooltip(final int index) {
        final List<String> lines = new ArrayList<>();
        lines.add(this.crystal(index).getDisplayName());
        final int needed = this.costs().getCrystalsNeeded(index);
        if (needed > 0) {
            lines.add(TextFormatting.GRAY + I18n.format("gui.thaumicenergistics.arcane_terminal.crystal_needed",
                    needed));
            lines.add(TextFormatting.GRAY + I18n.format("gui.thaumicenergistics.arcane_terminal.crystal_stored",
                    this.costs().getCrystalsStored(index)));
            if (this.costs().isCrystalMissing(index)) {
                lines.add(TextFormatting.RED
                        + I18n.format("gui.thaumicenergistics.arcane_terminal.crystal_missing"));
            }
        }
        return lines;
    }

    private List<String> visTooltip() {
        final List<String> lines = new ArrayList<>();
        if (this.costs().getVisCost() >= 0) {
            lines.add(I18n.format("gui.thaumicenergistics.vis_required", this.costs().getVisCost()));
        }
        lines.add(I18n.format("gui.thaumicenergistics.vis_available", this.costs().getVisAvailable()));
        final int discount = Math.round(CasterManager.getTotalVisDiscount(this.mc.player) * 100);
        if (discount > 0) {
            lines.add(I18n.format("gui.thaumicenergistics.vis_discount", discount));
        }
        return lines;
    }

    private static boolean within(final int x, final int y, final int left, final int top, final int width,
            final int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }
}
