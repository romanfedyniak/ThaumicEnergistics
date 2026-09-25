/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics.crafting;

/**
 * What an arcane craft on a terminal's grid costs and what there is to pay it with, as the screen shows it. The
 * server works it out and sends it as one line of text.
 */
public final class ArcaneCosts {

    public static final ArcaneCosts NONE = new ArcaneCosts(-1, 0, new int[6], new boolean[6], new long[6]);

    /** After the player's discount; -1 without an arcane recipe on the grid. */
    private final int visCost;
    private final int visAvailable;
    /** Each in {@link ArcaneGrid#PRIMALS} order. */
    private final int[] crystalsNeeded;
    private final boolean[] crystalsMissing;
    private final long[] crystalsStored;

    ArcaneCosts(final int visCost, final int visAvailable, final int[] crystalsNeeded,
            final boolean[] crystalsMissing, final long[] crystalsStored) {
        this.visCost = visCost;
        this.visAvailable = visAvailable;
        this.crystalsNeeded = crystalsNeeded;
        this.crystalsMissing = crystalsMissing;
        this.crystalsStored = crystalsStored;
    }

    public int getVisCost() {
        return this.visCost;
    }

    public int getVisAvailable() {
        return this.visAvailable;
    }

    public boolean isShortOfVis() {
        return this.visCost > this.visAvailable;
    }

    public int getCrystalsNeeded(final int index) {
        return this.crystalsNeeded[index];
    }

    public boolean isCrystalMissing(final int index) {
        return this.crystalsMissing[index];
    }

    public long getCrystalsStored(final int index) {
        return this.crystalsStored[index];
    }

    boolean canPay() {
        if (this.visCost > this.visAvailable) {
            return false;
        }
        for (final boolean missing : this.crystalsMissing) {
            if (missing) {
                return false;
            }
        }
        return true;
    }

    public String encode() {
        final StringBuilder line = new StringBuilder().append(this.visCost).append(';').append(this.visAvailable);
        for (int index = 0; index < ArcaneGrid.PRIMALS.length; index++) {
            line.append(';').append(this.crystalsNeeded[index]).append(',').append(this.crystalsMissing[index] ? 1 : 0)
                    .append(',').append(this.crystalsStored[index]);
        }
        return line.toString();
    }

    public static ArcaneCosts decode(final String line) {
        final String[] parts = line.split(";");
        if (parts.length != 2 + ArcaneGrid.PRIMALS.length) {
            return NONE;
        }
        final int[] needed = new int[ArcaneGrid.PRIMALS.length];
        final boolean[] missing = new boolean[ArcaneGrid.PRIMALS.length];
        final long[] stored = new long[ArcaneGrid.PRIMALS.length];
        try {
            for (int index = 0; index < ArcaneGrid.PRIMALS.length; index++) {
                final String[] crystal = parts[2 + index].split(",");
                needed[index] = Integer.parseInt(crystal[0]);
                missing[index] = crystal[1].equals("1");
                stored[index] = Long.parseLong(crystal[2]);
            }
            return new ArcaneCosts(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), needed, missing, stored);
        } catch (final NumberFormatException | ArrayIndexOutOfBoundsException e) {
            return NONE;
        }
    }
}
