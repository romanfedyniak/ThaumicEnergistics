/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics.crafting;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.IArcaneRecipe;

import appeng.api.config.Actionable;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.MEStorage;
import appeng.util.Platform;

/**
 * An arcane terminal's crafting, wherever the terminal is: a part on a cable or a mode of the wireless terminal.
 * It crafts what the arcane workbench crafts, plain recipes included; an arcane one also needs its vis in the aura
 * and its crystals in the network, and gives no result until both are there. The aura is read where the terminal
 * is at the moment it is asked, the craft itself included.
 */
public final class ArcaneCrafting {

    /** What the terminal the crafting happens at has to say for itself. */
    public interface Host {

        EntityPlayer getPlayer();

        World getWorld();

        BlockPos getPos();

        /** With an Arcane Charging Card, the eight chunks around too. */
        boolean isCharging();

        @Nullable
        MEStorage getNetwork();

        IActionSource getSource();

        IEnergySource getEnergy();
    }

    /** The aura changes on its own, so what the result slot shows is checked again this often. */
    private static final int RECHECK_TICKS = 10;

    private final Host host;
    @Nullable
    private IArcaneRecipe arcane;
    @Nullable
    private IRecipe plain;
    private ArcaneCosts costs = ArcaneCosts.NONE;
    private int ticks;

    public ArcaneCrafting(final Host host) {
        this.host = host;
    }

    /** What the server last worked out, for it to send on. */
    public ArcaneCosts getCosts() {
        return this.costs;
    }

    /** True once every so many ticks, when the result slot is to be looked at again. */
    public boolean recheck() {
        if (++this.ticks < RECHECK_TICKS) {
            return false;
        }
        this.ticks = 0;
        return true;
    }

    /**
     * An arcane recipe first, as the workbench looks, and only if it can be paid for now; a plain one otherwise. The
     * client cannot see the aura or the network and goes by {@code known}, what it was last sent.
     */
    @Nullable
    public IRecipe findRecipe(final InventoryCrafting grid, final World world, final ArcaneCosts known) {
        this.arcane = ArcaneTerminalRecipe.find(grid, this.host.getPlayer(), this.arcane);
        if (this.arcane != null) {
            final boolean payable = Platform.isServer() ? this.work(this.arcane).canPay() : known.canPay();
            return payable ? new ArcaneTerminalRecipe(this.arcane) : null;
        }
        if (Platform.isServer()) {
            this.costs = new ArcaneCosts(-1, this.visAvailable(), new int[6], new boolean[6], this.stored());
        }
        if (this.plain == null || !this.plain.matches(grid, world)) {
            this.plain = CraftingManager.findMatchingRecipe(grid, world);
        }
        return this.plain;
    }

    private ArcaneCosts work(final IArcaneRecipe recipe) {
        final int[] needed = new int[ArcaneGrid.PRIMALS.length];
        final boolean[] missing = new boolean[ArcaneGrid.PRIMALS.length];
        final long[] stored = this.stored();
        final AspectList crystals = recipe.getCrystals();
        if (crystals != null) {
            for (int index = 0; index < ArcaneGrid.PRIMALS.length; index++) {
                needed[index] = crystals.getAmount(ArcaneGrid.PRIMALS[index]);
                missing[index] = needed[index] > stored[index];
            }
        }
        this.costs = new ArcaneCosts(ArcaneVis.cost(recipe, this.host.getPlayer()), this.visAvailable(), needed,
                missing, stored);
        return this.costs;
    }

    private int visAvailable() {
        return ArcaneVis.available(this.host.getWorld(), this.host.getPos(), this.host.isCharging());
    }

    /** How many of each primal crystal the network would give up. */
    private long[] stored() {
        final long[] stored = new long[ArcaneGrid.PRIMALS.length];
        final MEStorage network = this.host.getNetwork();
        if (network != null) {
            for (int index = 0; index < ArcaneGrid.PRIMALS.length; index++) {
                stored[index] = network.extract(crystal(ArcaneGrid.PRIMALS[index]), Long.MAX_VALUE,
                        Actionable.SIMULATE, this.host.getSource());
            }
        }
        return stored;
    }

    private static AEItemKey crystal(final Aspect aspect) {
        return AEItemKey.of(ThaumcraftApiHelper.makeCrystal(aspect));
    }

    /** Takes what the arcane recipe on {@code grid} costs, as the result is handed over. */
    public void payFor(final InventoryCrafting grid, final World world) {
        if (!(this.findRecipe(grid, world, ArcaneCosts.NONE) instanceof ArcaneTerminalRecipe recipe)) {
            return;
        }
        final IArcaneRecipe paid = recipe.getArcane();
        ArcaneVis.drain(this.host.getWorld(), this.host.getPos(), ArcaneVis.cost(paid, this.host.getPlayer()),
                this.host.isCharging());
        final AspectList crystals = paid.getCrystals();
        final MEStorage network = this.host.getNetwork();
        if (crystals == null || network == null) {
            return;
        }
        for (final Aspect aspect : crystals.getAspects()) {
            Platform.poweredExtraction(this.host.getEnergy(), network, crystal(aspect), crystals.getAmount(aspect),
                    this.host.getSource());
        }
    }
}
