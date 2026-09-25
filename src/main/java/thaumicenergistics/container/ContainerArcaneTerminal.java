/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics.container;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.PlayerInvWrapper;

import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.IArcaneRecipe;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.container.ContainerNull;
import appeng.container.guisync.GuiSync;
import appeng.container.implementations.ContainerMEMonitorable;
import appeng.container.slot.SlotCraftingMatrix;
import appeng.container.slot.SlotRestrictedInput;
import appeng.core.sync.GuiBridge;
import appeng.helpers.ICraftingGridContainer;
import appeng.helpers.IContainerCraftingPacket;
import appeng.me.GridAccessException;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.Platform;
import appeng.util.inv.IAEAppEngInventory;
import appeng.util.inv.InvOperation;
import appeng.util.inv.WrapperInvItemHandler;

import thaumicenergistics.ThEGuis;
import thaumicenergistics.ThEItems;
import thaumicenergistics.crafting.ArcaneGrid;
import thaumicenergistics.crafting.ArcaneTerminalRecipe;
import thaumicenergistics.crafting.ArcaneVis;
import thaumicenergistics.part.PartArcaneTerminal;

/**
 * AE2's crafting terminal with the arcane workbench's recipes on its grid. It crafts what the workbench crafts,
 * plain recipes included; an arcane one also needs its vis in the aura and its crystals in the network, and
 * shows no result until both are there.
 */
public class ContainerArcaneTerminal extends ContainerMEMonitorable
        implements IAEAppEngInventory, IContainerCraftingPacket, ICraftingGridContainer {

    /** Right of where AE2 moves a terminal's own slots, so they stay where the screen puts them. */
    private static final int OFF_WINDOW = 200;
    /** Head first, as the player's own screen stacks them. */
    public static final EntityEquipmentSlot[] ARMOUR = { EntityEquipmentSlot.HEAD, EntityEquipmentSlot.CHEST,
            EntityEquipmentSlot.LEGS, EntityEquipmentSlot.FEET };

    /** The aura changes on its own, so what the result slot shows is checked again this often. */
    private static final int RECHECK_TICKS = 10;
    private static final int CRYSTAL_BITS = 8;
    private static final int CRYSTAL_MASK = (1 << CRYSTAL_BITS) - 1;

    private final PartArcaneTerminal part;
    private final AppEngInternalInventory output = new AppEngInternalInventory(this, 1);
    private final SlotCraftingMatrix[] gridSlots = new SlotCraftingMatrix[9];
    private final SlotArmour[] armourSlots = new SlotArmour[ARMOUR.length];
    private final SlotArcaneResult outputSlot;
    private final SlotRestrictedInput cardSlot;

    @Nullable
    private IArcaneRecipe arcane;
    @Nullable
    private IRecipe plain;
    private int ticks;

    /** What the arcane recipe on the grid costs after the player's discount; -1 without one. */
    @GuiSync(20)
    public long visCost = -1;
    @GuiSync(21)
    public long visAvailable;
    /** How many of each primal crystal the recipe takes, eight bits each in {@link ArcaneGrid#PRIMALS} order. */
    @GuiSync(22)
    public long crystals;
    /** One bit for each of those the network has too few of. */
    @GuiSync(23)
    public long crystalsMissing;
    /** How many of each primal crystal the network holds, comma-separated in the same order. */
    @GuiSync(24)
    public String crystalsStored = "";

    public ContainerArcaneTerminal(final InventoryPlayer ip, final PartArcaneTerminal part) {
        super(ip, part, false);
        this.part = part;

        final IItemHandler crafting = part.getInventoryByName("crafting");
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                this.addSlotToContainer(this.gridSlots[x + y * 3] = new SlotCraftingMatrix(this, crafting,
                        x + y * 3, 37 + x * 18, -72 + y * 18));
            }
        }

        this.addSlotToContainer(this.outputSlot = new SlotArcaneResult(ip.player, this.getActionSource(),
                this.getPowerSource(), part, crafting, this.output, 131, -72 + 18, this));

        for (int slot = 0; slot < ARMOUR.length; slot++) {
            this.addSlotToContainer(this.armourSlots[slot] = new SlotArmour(ip.player, ARMOUR[slot], OFF_WINDOW, 0));
        }

        this.addSlotToContainer(this.cardSlot = new SlotRestrictedInput(
                SlotRestrictedInput.PlacableItemType.UPGRADES, part.getInventoryByName("upgrades"), 0, OFF_WINDOW, 0,
                ip));

        this.bindPlayerInventory(ip, 0, 0);

        this.onCraftMatrixChanged(new WrapperInvItemHandler(crafting));
    }

    @Override
    public GuiBridge getOriginGui() {
        return ThEGuis.arcaneTerminal();
    }

    public SlotCraftingMatrix[] getGridSlots() {
        return this.gridSlots;
    }

    public SlotArcaneResult getOutputSlot() {
        return this.outputSlot;
    }

    public SlotArmour[] getArmourSlots() {
        return this.armourSlots;
    }

    public SlotRestrictedInput getCardSlot() {
        return this.cardSlot;
    }

    /** How many crystals of the primal at {@code index} the recipe on the grid takes. */
    public int getCrystalsNeeded(final int index) {
        return (int) (this.crystals >>> (index * CRYSTAL_BITS)) & CRYSTAL_MASK;
    }

    public boolean isCrystalMissing(final int index) {
        return (this.crystalsMissing & (1L << index)) != 0;
    }

    @Override
    public void detectAndSendChanges() {
        if (Platform.isServer() && ++this.ticks >= RECHECK_TICKS) {
            this.ticks = 0;
            this.onCraftMatrixChanged(null);
        }
        super.detectAndSendChanges();
    }

    @Override
    public void onCraftMatrixChanged(final IInventory inventory) {
        final InventoryCrafting grid = new InventoryCrafting(new ContainerNull(), 3, 3);
        for (int slot = 0; slot < this.gridSlots.length; slot++) {
            grid.setInventorySlotContents(slot, this.gridSlots[slot].getStack());
        }
        final IRecipe recipe = this.findRecipe(grid, this.getPlayerInv().player.world);
        this.outputSlot.putStack(recipe == null ? ItemStack.EMPTY : recipe.getCraftingResult(grid));
    }

    @Override
    public int getGridWidth() {
        return 3;
    }

    @Override
    public int getGridHeight() {
        return 3;
    }

    /**
     * An arcane recipe first, as the workbench looks, and only if it can be paid for now; a plain one otherwise.
     */
    @Nullable
    @Override
    public IRecipe findRecipe(final InventoryCrafting grid, final World world) {
        final EntityPlayer player = this.getPlayerInv().player;
        this.arcane = ArcaneTerminalRecipe.find(grid, player, this.arcane);
        if (this.arcane != null) {
            return this.canPay(this.arcane) ? new ArcaneTerminalRecipe(this.arcane) : null;
        }
        this.updateCosts(-1, 0, 0);
        if (this.plain == null || !this.plain.matches(grid, world)) {
            this.plain = CraftingManager.findMatchingRecipe(grid, world);
        }
        return this.plain;
    }

    private boolean canPay(final IArcaneRecipe recipe) {
        if (!Platform.isServer()) {
            // Only the server can see the aura and the network; the client goes by what it was last sent.
            return this.visCost <= this.visAvailable && this.crystalsMissing == 0;
        }
        final int cost = ArcaneVis.cost(recipe, this.getPlayerInv().player);
        long needed = 0;
        long missing = 0;
        final AspectList crystals = recipe.getCrystals();
        if (crystals != null) {
            final MEStorage network = this.getCellInventory();
            for (int index = 0; index < ArcaneGrid.PRIMALS.length; index++) {
                final int amount = crystals.getAmount(ArcaneGrid.PRIMALS[index]);
                if (amount <= 0) {
                    continue;
                }
                needed |= (long) Math.min(amount, CRYSTAL_MASK) << (index * CRYSTAL_BITS);
                final AEItemKey crystal = AEItemKey.of(ThaumcraftApiHelper.makeCrystal(ArcaneGrid.PRIMALS[index]));
                if (network == null
                        || network.extract(crystal, amount, Actionable.SIMULATE, this.getActionSource()) < amount) {
                    missing |= 1L << index;
                }
            }
        }
        this.updateCosts(cost, needed, missing);
        return cost <= this.visAvailable && missing == 0;
    }

    private void updateCosts(final int cost, final long needed, final long missing) {
        if (Platform.isServer()) {
            this.visCost = cost;
            this.visAvailable = ArcaneVis.available(this.getWorld(), this.getPos(), this.isCharging());
            this.crystals = needed;
            this.crystalsMissing = missing;
            this.crystalsStored = this.countCrystals();
        }
    }

    private String countCrystals() {
        final StringBuilder counts = new StringBuilder();
        try {
            final KeyCounter stored = this.part.getProxy().getStorage().getCachedInventory();
            for (final Aspect primal : ArcaneGrid.PRIMALS) {
                if (counts.length() > 0) {
                    counts.append(',');
                }
                counts.append(stored.get(AEItemKey.of(ThaumcraftApiHelper.makeCrystal(primal))));
            }
        } catch (final GridAccessException e) {
            return "";
        }
        return counts.toString();
    }

    /** How many crystals of the primal at {@code index} the network holds, as last sent. */
    public long getCrystalsStored(final int index) {
        final String[] counts = this.crystalsStored.split(",");
        return index < counts.length && !counts[index].isEmpty() ? Long.parseLong(counts[index]) : 0;
    }

    /** Takes what the arcane recipe on {@code grid} costs, as the result is handed over. */
    void payFor(final InventoryCrafting grid, final World world) {
        if (!(this.findRecipe(grid, world) instanceof ArcaneTerminalRecipe recipe)) {
            return;
        }
        final IArcaneRecipe arcane = recipe.getArcane();
        ArcaneVis.drain(this.getWorld(), this.getPos(), ArcaneVis.cost(arcane, this.getPlayerInv().player),
                this.isCharging());
        final AspectList crystals = arcane.getCrystals();
        final MEStorage network = this.getCellInventory();
        if (crystals == null || network == null) {
            return;
        }
        for (final Aspect aspect : crystals.getAspects()) {
            final AEItemKey crystal = AEItemKey.of(ThaumcraftApiHelper.makeCrystal(aspect));
            Platform.poweredExtraction(this.getPowerSource(), network, crystal, crystals.getAmount(aspect),
                    this.getActionSource());
        }
    }

    private boolean isCharging() {
        return this.part.isInstalled(ThEItems.ARCANE_CHARGING);
    }

    private World getWorld() {
        return this.part.getTile().getWorld();
    }

    private BlockPos getPos() {
        return this.part.getTile().getPos();
    }

    @Override
    public void saveChanges() {
    }

    @Override
    public void onChangeInventory(final IItemHandler inv, final int slot, final InvOperation mc,
            final ItemStack removedStack, final ItemStack newStack) {
    }

    @Override
    public IItemHandler getInventoryByName(final String name) {
        if (name.equals("player")) {
            return new PlayerInvWrapper(this.getInventoryPlayer());
        }
        return this.part.getInventoryByName(name);
    }

    @Override
    public boolean useRealItems() {
        return true;
    }
}
