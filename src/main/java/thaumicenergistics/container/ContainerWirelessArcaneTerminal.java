/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics.container;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.PlayerInvWrapper;

import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.MEStorage;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import appeng.container.ContainerNull;
import appeng.container.guisync.GuiSync;
import appeng.container.implementations.ContainerMEPortableTerminal;
import appeng.container.slot.SlotCraftingMatrix;
import appeng.container.slot.SlotRestrictedInput;
import appeng.core.sync.GuiBridge;
import appeng.helpers.ICraftingGridContainer;
import appeng.helpers.IContainerCraftingPacket;
import appeng.helpers.WirelessTerminalGuiObject;
import appeng.helpers.WirelessTerminalModes;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.Platform;
import appeng.util.inv.InvOperation;
import appeng.util.inv.WrapperInvItemHandler;

import thaumicenergistics.ThEGuis;
import thaumicenergistics.ThEItems;
import thaumicenergistics.crafting.ArcaneCosts;
import thaumicenergistics.crafting.ArcaneCrafting;

/**
 * The arcane terminal as a mode of AE2's wireless terminal. The vis comes from the aura where the player stands
 * when the craft is made, and the grid and the card are kept in the terminal item, with this mode's own data.
 */
public class ContainerWirelessArcaneTerminal extends ContainerMEPortableTerminal implements IContainerCraftingPacket,
        ICraftingGridContainer, IArcaneTerminal, ArcaneCrafting.Host {

    private static final int OFF_WINDOW = 200;

    private final ArcaneCrafting crafting = new ArcaneCrafting(this);
    private final AppEngInternalInventory output = new AppEngInternalInventory(this, 1);
    private final AppEngInternalInventory craftingGrid;
    private final SlotCraftingMatrix[] gridSlots = new SlotCraftingMatrix[9];
    private final SlotArcaneResult outputSlot;
    /** The Arcane Charging Card, kept with this mode's grid rather than among the terminal's own cards. */
    private final IUpgradeInventory card;
    private final SlotRestrictedInput cardSlot;

    @GuiSync(20)
    public String costs = "";
    private ArcaneCosts decoded = ArcaneCosts.NONE;
    private String decodedFrom = "";

    public ContainerWirelessArcaneTerminal(final InventoryPlayer ip, final WirelessTerminalGuiObject gui) {
        super(ip, gui, false);

        final NBTTagCompound modeData = WirelessTerminalModes.getModeData(gui.getItemStack(), ThEGuis.ARCANE_MODE);
        this.craftingGrid = new AppEngInternalInventory(this, 9);
        this.craftingGrid.readFromNBT(modeData, "craftingGrid");
        this.card = UpgradeInventories.forMachine(new ItemStack(ThEItems.ARCANE_TERMINAL), 1,
                changed -> this.saveChanges());
        this.card.readFromNBT(modeData, "upgrades");

        for (int slot = 0; slot < this.gridSlots.length; slot++) {
            this.addSlotToContainer(this.gridSlots[slot] = new SlotCraftingMatrix(this, this.craftingGrid, slot,
                    OFF_WINDOW, 0));
        }
        this.addSlotToContainer(this.outputSlot = new SlotArcaneResult(ip.player, this.getActionSource(),
                this.getPowerSource(), gui, this.craftingGrid, this.output, this, this.crafting));
        this.addSlotToContainer(this.cardSlot = new SlotRestrictedInput(
                SlotRestrictedInput.PlacableItemType.UPGRADES, this.card, 0, OFF_WINDOW, 0, ip));

        this.onCraftMatrixChanged(new WrapperInvItemHandler(this.craftingGrid));
    }

    @Override
    public GuiBridge getOriginGui() {
        return ThEGuis.wirelessArcaneTerminal();
    }

    @Override
    public SlotCraftingMatrix[] getGridSlots() {
        return this.gridSlots;
    }

    @Override
    public SlotArcaneResult getOutputSlot() {
        return this.outputSlot;
    }

    @Override
    public SlotRestrictedInput getCardSlot() {
        return this.cardSlot;
    }

    @Override
    public ArcaneCosts getCosts() {
        if (!this.decodedFrom.equals(this.costs)) {
            this.decodedFrom = this.costs;
            this.decoded = ArcaneCosts.decode(this.costs);
        }
        return this.decoded;
    }

    @Override
    public void detectAndSendChanges() {
        if (Platform.isServer() && this.craftingGrid != null && this.crafting.recheck()) {
            this.onCraftMatrixChanged(null);
        }
        super.detectAndSendChanges();
    }

    @Override
    public void onCraftMatrixChanged(final IInventory inventory) {
        // The terminal's own constructor gets here before this one has built the grid.
        if (this.outputSlot == null) {
            return;
        }
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

    @Nullable
    @Override
    public IRecipe findRecipe(final InventoryCrafting grid, final World world) {
        final IRecipe recipe = this.crafting.findRecipe(grid, world, this.getCosts());
        if (Platform.isServer()) {
            this.costs = this.crafting.getCosts().encode();
        }
        return recipe;
    }

    @Override
    public void saveChanges() {
        super.saveChanges();
        if (Platform.isServer() && this.craftingGrid != null && this.card != null) {
            final NBTTagCompound modeTag = new NBTTagCompound();
            this.craftingGrid.writeToNBT(modeTag, "craftingGrid");
            this.card.writeToNBT(modeTag, "upgrades");
            WirelessTerminalModes.setModeData(this.wirelessTerminalGUIObject.getItemStack(), ThEGuis.ARCANE_MODE,
                    modeTag);
        }
    }

    @Override
    public void onChangeInventory(final IItemHandler inv, final int slot, final InvOperation mc,
            final ItemStack removedStack, final ItemStack newStack) {
        if (inv == this.craftingGrid) {
            this.saveChanges();
        }
    }

    @Override
    public EntityPlayer getPlayer() {
        return this.getPlayerInv().player;
    }

    @Override
    public World getWorld() {
        return this.getPlayer().world;
    }

    @Override
    public BlockPos getPos() {
        return this.getPlayer().getPosition();
    }

    @Override
    public boolean isCharging() {
        return this.card.isInstalled(ThEItems.ARCANE_CHARGING);
    }

    @Nullable
    @Override
    public MEStorage getNetwork() {
        return this.getCellInventory();
    }

    @Override
    public IActionSource getSource() {
        return this.getActionSource();
    }

    @Override
    public IEnergySource getEnergy() {
        return this.getPowerSource();
    }

    @Override
    public IItemHandler getInventoryByName(final String name) {
        if (name.equals("player")) {
            return new PlayerInvWrapper(this.getInventoryPlayer());
        } else if (name.equals("crafting")) {
            return this.craftingGrid;
        }
        return null;
    }

    @Override
    public boolean useRealItems() {
        return true;
    }
}
