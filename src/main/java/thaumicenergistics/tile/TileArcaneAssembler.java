/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics.tile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.netty.buffer.ByteBuf;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import thaumcraft.api.crafting.ContainerDummy;
import thaumcraft.api.crafting.IArcaneRecipe;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.implementations.IPowerChannelState;
import appeng.api.implementations.IUpgradeableHost;
import appeng.api.implementations.tiles.ICraftingMachine;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.FabricatedSlots;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.events.MENetworkEventSubscribe;
import appeng.api.networking.events.MENetworkPowerStatusChange;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.IStorageMonitorableAccessor;
import appeng.api.storage.MEStorage;
import appeng.api.upgrades.CardTrait;
import appeng.api.upgrades.CardTraits;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import appeng.api.util.AECableType;
import appeng.api.util.AEPartLocation;
import appeng.api.util.IConfigManager;
import appeng.capabilities.Capabilities;
import appeng.me.GridAccessException;
import appeng.me.helpers.MachineSource;
import appeng.tile.grid.AENetworkTile;
import appeng.util.ConfigManager;
import appeng.util.IConfigManagerHost;
import appeng.util.Platform;
import appeng.util.UpgradeSpeedCalculations;

import thaumicenergistics.ThEItems;
import thaumicenergistics.crafting.ArcanePatternDetails;
import thaumicenergistics.crafting.ArcaneVis;

/**
 * The Arcane Assembler: what a molecular assembler is to crafting patterns, it is to arcane ones. An ME interface
 * beside it pushes a pattern's ingredients and crystals in; it waits until the aura holds the recipe's whole vis,
 * takes it all at once and then works the craft, faster for each acceleration card, and hands what it made and
 * what the recipe left back to that interface.
 * <p>
 * The vis is the recipe's full price - no one stands at the assembler wearing anything that lowers it - drawn
 * from the chunk it stands in, or with an Arcane Charging Card from that chunk and the eight around it.
 */
public class TileArcaneAssembler extends AENetworkTile implements ICraftingMachine, IGridTickable, IUpgradeableHost,
        IConfigManagerHost, IPowerChannelState {

    public static final int SPEED_SLOTS = 5;
    /** The acceleration cards and the charging card, in one column. */
    public static final int CARD_SLOTS = SPEED_SLOTS + 1;
    public static final int MAX_PROGRESS = 100;

    private static final int TABLE = ArcanePatternDetails.WIDTH * ArcanePatternDetails.HEIGHT;
    /** How often an assembler waiting for vis looks at the aura again. */
    private static final int VIS_WAIT_TICKS = 20;

    private final IActionSource source = new MachineSource(this);
    private final IUpgradeInventory upgrades;
    /** Empty: nothing about the assembler is set from its window, but an upgradeable machine answers for one. */
    private final IConfigManager settings = new ConfigManager(this);
    private final ItemStack[] table = new ItemStack[TABLE];
    /** What the last craft made and left, on its way back to the interface. */
    private final List<ItemStack> outbox = new ArrayList<>();

    @Nullable
    private ArcanePatternDetails plan;
    @Nullable
    private EnumFacing returnSide;
    /** Whether this craft's vis has been taken; the craft only runs once it has. */
    private boolean paid;
    private double progress;
    private boolean awake;

    /** What the craft running now makes, on both sides: the window and the model inside the block show it. */
    private ItemStack crafting = ItemStack.EMPTY;
    private boolean clientPowered;

    public TileArcaneAssembler() {
        this.getProxy().setIdlePowerUsage(0.0);
        this.upgrades = UpgradeInventories.forMachine(new ItemStack(ThEItems.ARCANE_ASSEMBLER), CARD_SLOTS,
                inventory -> this.saveChanges());
        clear(this.table);
    }

    private static void clear(final ItemStack[] stacks) {
        for (int slot = 0; slot < stacks.length; slot++) {
            stacks[slot] = ItemStack.EMPTY;
        }
    }

    public IUpgradeInventory getUpgrades() {
        return this.upgrades;
    }

    @Override
    public int getInstalledUpgrades(final ItemStack upgradeCard) {
        return this.upgrades.getInstalledUpgrades(upgradeCard);
    }

    @Override
    public int getInstalledPoints(final CardTrait trait) {
        return this.upgrades.getInstalledPoints(trait);
    }

    @Override
    public IConfigManager getConfigManager() {
        return this.settings;
    }

    @Override
    public void updateSetting(final IConfigManager manager, final Enum settingName, final Enum newValue) {
    }

    @Nullable
    @Override
    public IItemHandler getInventoryByName(final String name) {
        return "upgrades".equals(name) ? this.upgrades : null;
    }

    public ItemStack getCrafting() {
        return this.crafting;
    }

    @Nullable
    public ArcanePatternDetails getPlan() {
        return this.plan;
    }

    public boolean isCharging() {
        return this.upgrades.getInstalledPoints(ThEItems.ARCANE_CHARGING) > 0;
    }

    /** The vis the craft waiting or running now costs, or 0. */
    public int getVisCost() {
        final IArcaneRecipe recipe = this.plan == null ? null : this.plan.recipe(this.world);
        return recipe == null ? 0 : recipe.getVis();
    }

    public int getVisAvailable() {
        return ArcaneVis.available(this.world, this.pos, this.isCharging());
    }

    public boolean isPaid() {
        return this.paid;
    }

    @Override
    public boolean isPowered() {
        return Platform.isClient() ? this.clientPowered : this.getProxy().isActive();
    }

    /** Needs no channel, so it is working whenever it has power. */
    @Override
    public boolean isActive() {
        return this.isPowered();
    }

    @Override
    public boolean canRun(final ICraftingPatternDetails patternDetails) {
        return patternDetails instanceof ArcanePatternDetails;
    }

    @Override
    public boolean acceptsFabricatedContainers() {
        return true;
    }

    @Override
    public boolean acceptsPlans() {
        return this.plan == null && this.outbox.isEmpty() && this.getProxy().isActive();
    }

    @Override
    public boolean pushPattern(final ICraftingPatternDetails patternDetails, final InventoryCrafting table,
            final EnumFacing ejectionDirection) {
        if (!(patternDetails instanceof ArcanePatternDetails details) || !this.acceptsPlans()
                || table.getSizeInventory() != TABLE) {
            return false;
        }
        for (int slot = 0; slot < TABLE; slot++) {
            this.table[slot] = table.getStackInSlot(slot).copy();
        }
        this.plan = details;
        this.returnSide = ejectionDirection;
        this.paid = false;
        this.progress = 0;
        this.show(details.getResult());
        this.wake();
        this.saveChanges();
        return true;
    }

    private void show(final ItemStack stack) {
        if (!ItemStack.areItemStacksEqual(stack, this.crafting)) {
            this.crafting = stack;
            this.markForUpdate();
        }
    }

    private void wake() {
        if (this.awake) {
            return;
        }
        this.awake = true;
        try {
            this.getProxy().getTick().wakeDevice(this.getProxy().getNode());
        } catch (final GridAccessException e) {
            // Not on a grid yet: it wakes when it joins one.
        }
    }

    @Nonnull
    @Override
    public TickingRequest getTickingRequest(@Nonnull final IGridNode node) {
        this.awake = this.plan != null || !this.outbox.isEmpty();
        return new TickingRequest(1, VIS_WAIT_TICKS, !this.awake, false);
    }

    @Nonnull
    @Override
    public TickRateModulation tickingRequest(@Nonnull final IGridNode node, final int ticksSinceLastCall) {
        if (!this.outbox.isEmpty()) {
            this.sendBack();
            if (!this.outbox.isEmpty()) {
                return TickRateModulation.SLOWER;
            }
        }
        if (this.plan == null) {
            return this.sleep();
        }
        if (!this.paid && !this.pay()) {
            return this.plan == null && this.outbox.isEmpty() ? this.sleep() : TickRateModulation.SLOWER;
        }

        final int speed = UpgradeSpeedCalculations.molecularAssemblerSpeed(
                this.upgrades.getInstalledPoints(CardTraits.SPEED));
        final double wanted = Math.min((double) speed * ticksSinceLastCall, MAX_PROGRESS - this.progress);
        this.progress += this.powered(wanted, Math.min(speed, MAX_PROGRESS) / 10.0);

        if (this.progress < MAX_PROGRESS) {
            return TickRateModulation.URGENT;
        }
        this.finish();
        return this.outbox.isEmpty() ? this.sleep() : TickRateModulation.SLOWER;
    }

    private TickRateModulation sleep() {
        this.awake = false;
        return TickRateModulation.SLEEP;
    }

    /**
     * Takes the recipe's vis if the aura holds all of it. A recipe that is gone - removed by a script since the
     * pattern was written - sends its ingredients back instead.
     */
    private boolean pay() {
        final IArcaneRecipe recipe = this.plan.recipe(this.world);
        if (recipe == null) {
            this.giveUp();
            return false;
        }
        final int cost = recipe.getVis();
        final boolean charging = this.isCharging();
        if (ArcaneVis.available(this.world, this.pos, charging) < cost) {
            return false;
        }
        ArcaneVis.drain(this.world, this.pos, cost, charging);
        this.paid = true;
        this.saveChanges();
        return true;
    }

    /** The share of {@code progress} the network pays for, at {@code tax} AE a point. */
    private double powered(final double progress, final double tax) {
        if (progress <= 0) {
            return 0;
        }
        try {
            final double wanted = progress * tax;
            final double paid = this.getProxy().getEnergy().extractAEPower(wanted, Actionable.MODULATE,
                    PowerMultiplier.CONFIG);
            return paid >= wanted - 0.0001 ? progress : paid / tax;
        } catch (final GridAccessException e) {
            return 0;
        }
    }

    private InventoryCrafting tableGrid() {
        final InventoryCrafting grid = new InventoryCrafting(new ContainerDummy(), ArcanePatternDetails.WIDTH,
                ArcanePatternDetails.HEIGHT);
        for (int slot = 0; slot < TABLE; slot++) {
            grid.setInventorySlotContents(slot, this.table[slot].copy());
        }
        return grid;
    }

    private void finish() {
        final InventoryCrafting grid = this.tableGrid();
        final ItemStack output = this.plan.getOutput(grid, this.world);
        if (output.isEmpty()) {
            this.giveUp();
            return;
        }
        this.outbox.add(output);
        final NonNullList<ItemStack> left = Platform.getRemainingItems(this.plan, grid, this.world, true);
        for (final ItemStack stack : left) {
            if (!stack.isEmpty()) {
                this.outbox.add(stack);
            }
        }
        this.endCraft();
        this.sendBack();
    }

    /**
     * Sends what was pushed back unused. A container the network filled for the craft is not the machine's to
     * give back: it goes, as the contents that paid for it went.
     */
    private void giveUp() {
        for (int slot = 0; slot < TABLE; slot++) {
            if (!this.table[slot].isEmpty() && !FabricatedSlots.isConjured(this.plan, true, slot)) {
                this.outbox.add(this.table[slot]);
            }
        }
        this.endCraft();
        this.sendBack();
    }

    private void endCraft() {
        clear(this.table);
        this.plan = null;
        this.paid = false;
        this.progress = 0;
        this.show(ItemStack.EMPTY);
        this.saveChanges();
    }

    /** Into the network behind the interface if it offers that, else into its slots. */
    private void sendBack() {
        final EnumFacing side = this.returnSide;
        final TileEntity target = side == null ? null : this.world.getTileEntity(this.pos.offset(side));
        if (target == null) {
            return;
        }
        final IStorageMonitorableAccessor accessor = target.getCapability(Capabilities.STORAGE_MONITORABLE_ACCESSOR,
                side.getOpposite());
        final MEStorage storage = accessor == null ? null : accessor.getInventory(this.source);
        final IItemHandler handler = target.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY,
                side.getOpposite());

        boolean moved = false;
        for (int index = this.outbox.size() - 1; index >= 0; index--) {
            final ItemStack stack = this.outbox.get(index);
            ItemStack left = stack;
            if (storage != null) {
                final long inserted = storage.insert(AEItemKey.of(left), left.getCount(), Actionable.MODULATE,
                        this.source);
                left = inserted >= left.getCount() ? ItemStack.EMPTY
                        : ItemHandlerHelper.copyStackWithSize(left, left.getCount() - (int) inserted);
            }
            if (!left.isEmpty() && handler != null) {
                left = ItemHandlerHelper.insertItem(handler, left, false);
            }
            if (left.isEmpty()) {
                this.outbox.remove(index);
                moved = true;
            } else if (left.getCount() != stack.getCount()) {
                this.outbox.set(index, left);
                moved = true;
            }
        }
        if (this.outbox.isEmpty()) {
            this.returnSide = null;
        }
        if (moved) {
            this.saveChanges();
        }
    }

    @MENetworkEventSubscribe
    public void powerChanged(final MENetworkPowerStatusChange changed) {
        this.markForUpdate();
    }

    @Override
    public AECableType getCableConnectionType(final AEPartLocation dir) {
        return AECableType.COVERED;
    }

    @Override
    public void getDrops(final World world, final BlockPos pos, final List<ItemStack> drops) {
        for (int slot = 0; slot < TABLE; slot++) {
            if (!this.table[slot].isEmpty() && !FabricatedSlots.isConjured(this.plan, true, slot)) {
                drops.add(this.table[slot]);
            }
        }
        drops.addAll(this.outbox);
        for (int slot = 0; slot < this.upgrades.getSlots(); slot++) {
            final ItemStack card = this.upgrades.getStackInSlot(slot);
            if (!card.isEmpty()) {
                drops.add(card);
            }
        }
    }

    @Override
    protected void writeToStream(final ByteBuf data) throws IOException {
        super.writeToStream(data);
        data.writeBoolean(this.getProxy().isActive());
        new PacketBuffer(data).writeItemStack(this.crafting);
    }

    @Override
    protected boolean readFromStream(final ByteBuf data) throws IOException {
        final boolean changed = super.readFromStream(data);
        final boolean powered = data.readBoolean();
        final ItemStack crafting = new PacketBuffer(data).readItemStack();
        final boolean flipped = powered != this.clientPowered
                || !ItemStack.areItemStacksEqual(crafting, this.crafting);
        this.clientPowered = powered;
        this.crafting = crafting;
        return changed || flipped;
    }

    @Override
    public NBTTagCompound writeToNBT(final NBTTagCompound data) {
        super.writeToNBT(data);
        this.upgrades.writeToNBT(data, "upgrades");
        if (this.plan != null) {
            data.setTag("plan", this.plan.getPattern().writeToNBT(new NBTTagCompound()));
            data.setTag("table", writeStacks(this.table));
            data.setBoolean("paid", this.paid);
            data.setDouble("progress", this.progress);
        }
        if (!this.outbox.isEmpty()) {
            data.setTag("outbox", writeStacks(this.outbox.toArray(new ItemStack[0])));
        }
        if (this.returnSide != null) {
            data.setByte("returnSide", (byte) this.returnSide.getIndex());
        }
        return data;
    }

    @Override
    public void readFromNBT(final NBTTagCompound data) {
        super.readFromNBT(data);
        this.upgrades.readFromNBT(data, "upgrades");
        clear(this.table);
        this.outbox.clear();
        this.plan = null;
        this.paid = false;
        this.progress = 0;
        this.crafting = ItemStack.EMPTY;
        if (data.hasKey("plan")) {
            final NBTTagList stacks = data.getTagList("table", Constants.NBT.TAG_COMPOUND);
            for (int slot = 0; slot < TABLE && slot < stacks.tagCount(); slot++) {
                this.table[slot] = new ItemStack(stacks.getCompoundTagAt(slot));
            }
            this.plan = readPlan(new ItemStack(data.getCompoundTag("plan")));
            if (this.plan != null) {
                this.paid = data.getBoolean("paid");
                this.progress = data.getDouble("progress");
                this.crafting = this.plan.getResult();
            } else {
                // Its pattern is unreadable now, so what it was given goes back.
                for (final ItemStack stack : this.table) {
                    if (!stack.isEmpty()) {
                        this.outbox.add(stack);
                    }
                }
                clear(this.table);
            }
        }
        final NBTTagList outbox = data.getTagList("outbox", Constants.NBT.TAG_COMPOUND);
        for (int index = 0; index < outbox.tagCount(); index++) {
            final ItemStack stack = new ItemStack(outbox.getCompoundTagAt(index));
            if (!stack.isEmpty()) {
                this.outbox.add(stack);
            }
        }
        this.returnSide = data.hasKey("returnSide") ? EnumFacing.byIndex(data.getByte("returnSide")) : null;
    }

    @Nullable
    private ArcanePatternDetails readPlan(final ItemStack pattern) {
        try {
            return new ArcanePatternDetails(pattern, this.world);
        } catch (final IllegalArgumentException e) {
            return null;
        }
    }

    private static NBTTagList writeStacks(final ItemStack[] stacks) {
        final NBTTagList list = new NBTTagList();
        for (final ItemStack stack : stacks) {
            list.appendTag(stack.writeToNBT(new NBTTagCompound()));
        }
        return list;
    }
}
