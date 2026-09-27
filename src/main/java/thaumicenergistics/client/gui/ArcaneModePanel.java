/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics.client.gui;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.IArcaneRecipe;

import appeng.api.config.FluidSubstitution;
import appeng.api.config.ItemSubstitution;
import appeng.api.config.Settings;
import appeng.api.patterns.PatternEncodingMode;
import appeng.api.patterns.client.IPatternTerminalScreen;
import appeng.api.patterns.client.PatternModePanel;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.client.gui.widgets.GuiImgButton;

import thaumicenergistics.crafting.ArcaneEncodingMode;
import thaumicenergistics.crafting.ArcaneGrid;
import thaumicenergistics.crafting.ArcaneGridSlots;

/**
 * The arcane mode of a pattern terminal: Thaumcraft's own workbench on a plate beside the window, as the Arcane
 * Crafting Terminal has it, with the row a pattern is encoded along under it and the view cells on a plate of
 * their own above. What the grid makes and the crystals it takes show before anything is encoded.
 */
public class ArcaneModePanel extends PatternModePanel {

    /** The two tabs stay under the list's scrollbar, where every mode has them. */
    private static final int TAB_SIZE = 22;
    private static final int TAB_X = 173;

    private static final int WING_LEFT = 198;
    private static final int PAD = 4;
    private static final int GAP = 3;
    private static final int BENCH_WIDTH = 190;
    private static final int BENCH_HEIGHT = 143;
    private static final int BENCH_X = WING_LEFT + PAD;
    private static final int WING_WIDTH = PAD + BENCH_WIDTH + PAD;

    /** Under the workbench: a blank pattern, the arrow that spends it, the pattern, and what acts on it. */
    private static final int WELL_Y = PAD + BENCH_HEIGHT + 2;
    private static final int WELL_SIZE = 24;
    private static final int WELL_INSET = 4;
    private static final int SLOT_Y = WELL_Y + WELL_INSET;
    private static final int BLANK_X = WING_LEFT + 8;
    private static final int ENCODE_X = WING_LEFT + 29;
    private static final int UPLOAD_X = WING_LEFT + 49;
    private static final int WELL_X = WING_LEFT + 61;
    private static final int CLEAR_X = WING_LEFT + 88;
    private static final int CLEAR_Y = WELL_Y + 2;
    private static final int SUBSTITUTE_Y = WELL_Y + 12;
    private static final int FLUID_SUBSTITUTE_X = CLEAR_X + 10;
    private static final int COST_X = WING_LEFT + 110;
    private static final int WING_HEIGHT = WELL_Y + WELL_SIZE + PAD;

    /** The view cells' plate over the workbench. */
    private static final int CELLS_EDGE = 7;
    private static final int CELLS_HEIGHT = CELLS_EDGE + 18 + CELLS_EDGE;

    private static final int RESULT_X = 160;
    private static final int RESULT_Y = 64;
    private static final int GLOW_U = 192;
    private static final int GLOW_SIZE = 64;
    private static final int FABRICATED_SLOT_TINT = 0x8032CD32;
    private static final int TEXT_COLOR = 0x404040;

    private GuiImgButton substitutionsEnabledBtn;
    private GuiImgButton substitutionsDisabledBtn;
    private GuiImgButton fluidSubstitutionsEnabledBtn;
    private GuiImgButton fluidSubstitutionsDisabledBtn;

    private ItemStack result = ItemStack.EMPTY;
    private AspectList crystals = new AspectList();
    private int vis = -1;
    private boolean[] fabricated = new boolean[9];
    private int readFrom;

    public ArcaneModePanel(final PatternEncodingMode mode, final IPatternTerminalScreen screen) {
        super(mode, screen);
    }

    @Override
    public int getHeight() {
        return 0;
    }

    @Nullable
    @Override
    public String getBackground() {
        return null;
    }

    @Override
    public boolean showsViewCellColumn() {
        return false;
    }

    /** The workbench and the cells over it, together centred on the window's height. */
    private int cellsTop() {
        return (this.getScreen().getYSize() - CELLS_HEIGHT - GAP - WING_HEIGHT) / 2;
    }

    private int wingTop() {
        return this.cellsTop() + CELLS_HEIGHT + GAP;
    }

    private int cellsWidth() {
        return CELLS_EDGE * 2 + 18 * this.getScreen().getViewCellSlots().size();
    }

    @Override
    public List<Rectangle> getOutsideAreas() {
        final int top = Math.min(0, this.cellsTop());
        final int bottom = Math.max(Math.max(this.getScreen().getYSize(), this.wingTop() + WING_HEIGHT),
                this.tabsTop() + 2 * TAB_SIZE);
        return Arrays.asList(
                new Rectangle(WING_LEFT, this.wingTop(), WING_WIDTH, WING_HEIGHT),
                new Rectangle(WING_LEFT, this.cellsTop(), this.cellsWidth(), CELLS_HEIGHT),
                new Rectangle(TAB_X, top, WING_LEFT - TAB_X, bottom - top));
    }

    /** A wireless terminal's cards stand in a row beside the view cells'. */
    @Override
    public Point getWirelessCardRow() {
        return new Point(WING_LEFT + this.cellsWidth() + GAP, this.cellsTop());
    }

    @Override
    public void layOut() {
        final int bench = this.wingTop() + PAD;
        final List<Slot> grid = this.getScreen().getGridSlots(ArcaneEncodingMode.GRID);
        for (int square = 0; square < grid.size(); square++) {
            this.getScreen().placeSlot(grid.get(square), BENCH_X + ArcaneGridSlots.gridX(square),
                    bench + ArcaneGridSlots.gridY(square));
        }

        final List<Slot> cells = this.getScreen().getViewCellSlots();
        for (int index = 0; index < cells.size(); index++) {
            this.getScreen().placeSlot(cells.get(index), WING_LEFT + CELLS_EDGE + 1 + 18 * index,
                    this.cellsTop() + CELLS_EDGE + 1);
        }

        final int top = this.wingTop();
        this.getScreen().placeSlot(IPatternTerminalScreen.TerminalSlot.BLANK_PATTERN, BLANK_X, top + SLOT_Y);
        this.getScreen().placeButton(IPatternTerminalScreen.TerminalButton.ENCODE, ENCODE_X, top + SLOT_Y);
        this.getScreen().placeButton(IPatternTerminalScreen.TerminalButton.UPLOAD, UPLOAD_X, top + SLOT_Y + 4);
        this.getScreen().placeSlot(IPatternTerminalScreen.TerminalSlot.ENCODED_PATTERN, WELL_X + WELL_INSET,
                top + SLOT_Y);
        this.getScreen().placeButton(IPatternTerminalScreen.TerminalButton.CLEAR, CLEAR_X, top + CLEAR_Y);

        this.getScreen().placeButton(IPatternTerminalScreen.TerminalButton.MODE_TAB, TAB_X, this.tabsTop());
        this.getScreen().placeButton(IPatternTerminalScreen.TerminalButton.MODES, TAB_X, this.tabsTop() + TAB_SIZE);
    }

    /** Where the list stops, which is where every other mode's band starts and its tabs with it. */
    private int tabsTop() {
        return this.getScreen().getPanelTop();
    }

    @Override
    public void addButtons(final List<GuiButton> buttons) {
        this.substitutionsEnabledBtn = halfSize(new GuiImgButton(0, 0, Settings.ACTIONS, ItemSubstitution.ENABLED));
        this.substitutionsDisabledBtn = halfSize(new GuiImgButton(0, 0, Settings.ACTIONS,
                ItemSubstitution.DISABLED));
        this.fluidSubstitutionsEnabledBtn = halfSize(new GuiImgButton(0, 0, Settings.ACTIONS,
                FluidSubstitution.ENABLED));
        this.fluidSubstitutionsDisabledBtn = halfSize(new GuiImgButton(0, 0, Settings.ACTIONS,
                FluidSubstitution.DISABLED));
        buttons.add(this.substitutionsEnabledBtn);
        buttons.add(this.substitutionsDisabledBtn);
        buttons.add(this.fluidSubstitutionsEnabledBtn);
        buttons.add(this.fluidSubstitutionsDisabledBtn);
    }

    private static GuiImgButton halfSize(final GuiImgButton button) {
        button.setHalfSize(true);
        return button;
    }

    /** The plate moves with the window's height, so the buttons are placed again every frame. */
    @Override
    public void updateButtons() {
        final boolean substitute = this.getScreen().getHost().isSubstitution();
        this.substitutionsEnabledBtn.visible = substitute;
        this.substitutionsDisabledBtn.visible = !substitute;
        final boolean fluids = this.getScreen().getHost().isFluidSubstitution();
        this.fluidSubstitutionsEnabledBtn.visible = fluids;
        this.fluidSubstitutionsDisabledBtn.visible = !fluids;

        final int left = this.getScreen().getGuiLeft();
        final int y = this.getScreen().getGuiTop() + this.wingTop() + SUBSTITUTE_Y;
        for (final GuiImgButton button : new GuiImgButton[] { this.substitutionsEnabledBtn,
                this.substitutionsDisabledBtn }) {
            button.x = left + CLEAR_X;
            button.y = y;
        }
        for (final GuiImgButton button : new GuiImgButton[] { this.fluidSubstitutionsEnabledBtn,
                this.fluidSubstitutionsDisabledBtn }) {
            button.x = left + FLUID_SUBSTITUTE_X;
            button.y = y;
        }
    }

    @Override
    public boolean actionPerformed(final GuiButton button) {
        if (button == this.substitutionsEnabledBtn || button == this.substitutionsDisabledBtn) {
            this.getScreen().sendModeAction(ArcaneEncodingMode.TOGGLE_SUBSTITUTION);
            return true;
        }
        if (button == this.fluidSubstitutionsEnabledBtn || button == this.fluidSubstitutionsDisabledBtn) {
            this.getScreen().sendModeAction(ArcaneEncodingMode.TOGGLE_FLUID_SUBSTITUTION);
            return true;
        }
        return false;
    }

    @Override
    public void drawBackground(final int mouseX, final int mouseY) {
        final int top = this.wingTop();
        this.getScreen().drawPanelBackground(WING_LEFT, top, WING_WIDTH, WING_HEIGHT);
        this.getScreen().drawPanelBackground(WING_LEFT, this.cellsTop(), this.cellsWidth(), CELLS_HEIGHT);
        for (int index = 0; index < this.getScreen().getViewCellSlots().size(); index++) {
            this.getScreen().drawSlotBackground(WING_LEFT + CELLS_EDGE + 1 + 18 * index,
                    this.cellsTop() + CELLS_EDGE + 1);
        }
        this.getScreen().drawSlotBackground(BLANK_X, top + SLOT_Y);
        this.getScreen().drawWellBackground(WELL_X, top + WELL_Y, WELL_SIZE, WELL_SIZE);

        this.read();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();
        this.getScreen().bindTexture("thaumcraft", "gui/arcaneworkbench.png");
        this.getScreen().drawTexture(BENCH_X, top + PAD, 0, 0, BENCH_WIDTH, BENCH_HEIGHT);
        this.drawGlows(top + PAD);
        GlStateManager.disableBlend();
    }

    /** The workbench's slow turning glow behind each crystal the recipe on the grid takes. */
    private void drawGlows(final int benchTop) {
        final Minecraft mc = Minecraft.getMinecraft();
        final float ticks = mc.getRenderViewEntity() == null ? 0
                : mc.getRenderViewEntity().ticksExisted + mc.getRenderPartialTicks();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        for (int index = 0; index < ArcaneGrid.PRIMALS.length; index++) {
            if (this.crystals.getAmount(ArcaneGrid.PRIMALS[index]) <= 0) {
                continue;
            }
            final int color = ArcaneGrid.PRIMALS[index].getColor();
            GlStateManager.color((color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F,
                    (color & 0xFF) / 255.0F, 0.33F);
            GlStateManager.pushMatrix();
            GlStateManager.translate(BENCH_X + ArcaneGridSlots.crystalX(index) + 7.5F,
                    benchTop + ArcaneGridSlots.crystalY(index) + 8.0F, 0.0F);
            GlStateManager.rotate(index * 60 + ticks % 360, 0.0F, 0.0F, 1.0F);
            GlStateManager.scale(0.5F, 0.5F, 1.0F);
            this.getScreen().drawTexture(-GLOW_SIZE / 2, -GLOW_SIZE / 2, GLOW_U, 0, GLOW_SIZE, GLOW_SIZE);
            GlStateManager.popMatrix();
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    }

    @Override
    public void drawForeground(final int mouseX, final int mouseY) {
        final int bench = this.wingTop() + PAD;
        for (int index = 0; index < ArcaneGrid.PRIMALS.length; index++) {
            final int amount = this.crystals.getAmount(ArcaneGrid.PRIMALS[index]);
            if (amount > 0) {
                final int x = BENCH_X + ArcaneGridSlots.crystalX(index);
                final int y = bench + ArcaneGridSlots.crystalY(index);
                this.getScreen().drawItemStack(x, y, ThaumcraftApiHelper.makeCrystal(ArcaneGrid.PRIMALS[index]));
                this.getScreen().drawItemCount(x, y, amount);
            }
        }
        if (!this.result.isEmpty()) {
            this.getScreen().drawItemStack(BENCH_X + RESULT_X, bench + RESULT_Y, this.result);
        }
        if (this.vis >= 0) {
            this.getScreen().drawText(I18n.format("gui.thaumicenergistics.arcane_terminal.cost", this.vis), COST_X,
                    this.wingTop() + WELL_Y + 8, TEXT_COLOR);
        }
        this.drawFabricated();

        if (this.overResult(mouseX, mouseY)) {
            this.getScreen().drawSlotHighlight(BENCH_X + RESULT_X, bench + RESULT_Y);
            if (!this.result.isEmpty()) {
                this.getScreen().drawTooltip(Collections.singletonList(this.result.getDisplayName()), mouseX,
                        mouseY);
            }
        }
    }

    @Nullable
    @Override
    public AEKey getKeyUnderMouse(final int x, final int y) {
        return this.result.isEmpty() || !this.overResult(x, y) ? null : AEItemKey.of(this.result);
    }

    private boolean overResult(final int x, final int y) {
        final int left = BENCH_X + RESULT_X;
        final int top = this.wingTop() + PAD + RESULT_Y;
        return x >= left && x < left + 16 && y >= top && y < top + 16;
    }

    /** The squares the network would fill in for, while the fluid toggle is under the cursor. */
    private void drawFabricated() {
        final GuiImgButton button = this.fluidSubstitutionsEnabledBtn.visible ? this.fluidSubstitutionsEnabledBtn
                : this.fluidSubstitutionsDisabledBtn;
        if (!button.visible || !button.isMouseOver()) {
            return;
        }
        final List<Slot> grid = this.getScreen().getGridSlots(ArcaneEncodingMode.GRID);
        // Over the ghost items, which are drawn in front of anything flat.
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        for (int square = 0; square < grid.size() && square < this.fabricated.length; square++) {
            if (this.fabricated[square]) {
                final Slot slot = grid.get(square);
                this.getScreen().drawRectangle(slot.xPos, slot.yPos, 16, 16, FABRICATED_SLOT_TINT);
            }
        }
        GlStateManager.enableDepth();
        GlStateManager.enableLighting();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** The same question encoding asks, asked again only when the grid changed. */
    private void read() {
        final List<Slot> grid = this.getScreen().getGridSlots(ArcaneEncodingMode.GRID);
        final int contents = contentsOf(grid);
        if (contents == this.readFrom) {
            return;
        }
        this.readFrom = contents;
        final ArcaneGridSlots.Preview preview = ArcaneGridSlots.preview(
                this.getScreen().getHost().getEncodingGrid(this.getMode(), ArcaneEncodingMode.GRID),
                Minecraft.getMinecraft().player);
        final IArcaneRecipe recipe = preview.recipe();
        this.result = preview.result();
        this.crystals = recipe == null || recipe.getCrystals() == null ? new AspectList() : recipe.getCrystals();
        this.vis = recipe == null ? -1 : recipe.getVis();
        this.fabricated = preview.fabricated();
    }

    private static int contentsOf(final List<Slot> slots) {
        int hash = 1;
        for (final Slot slot : slots) {
            final ItemStack is = slot.getStack();
            hash = hash * 31 + (is.isEmpty() ? 0 : Item.getIdFromItem(is.getItem()) * 31 + is.getItemDamage());
        }
        return hash;
    }
}
