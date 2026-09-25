/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics;

import net.minecraftforge.common.config.Config;

/**
 * config/thaumicenergistics.cfg. A feature switched off registers nothing - no item, block, recipe or mode - so a
 * world that already holds its blocks loses them, and a server and its players need the same switches.
 */
@Config(modid = ThaumicEnergistics.MODID)
public final class ThEConfig {

    @Config.Comment("Each of these takes effect on the next start.")
    public static final Features features = new Features();

    private ThEConfig() {
    }

    public static final class Features {

        @Config.Comment("The Arcane Crafting Terminal, and the Arcane Charging Card that works in it.")
        @Config.RequiresMcRestart
        public boolean arcaneTerminal = true;

        @Config.Comment("The arcane mode of the wireless terminal. Needs arcaneTerminal.")
        @Config.RequiresMcRestart
        public boolean wirelessArcaneMode = true;

        @Config.Comment("Arcane patterns, the arcane mode of the pattern terminals, and the Arcane Assembler that runs "
                + "them. Needs arcaneTerminal.")
        @Config.RequiresMcRestart
        public boolean arcaneAutocrafting = true;

        @Config.Comment("The Infusion Provider.")
        @Config.RequiresMcRestart
        public boolean infusionProvider = true;

        @Config.Comment("Thaumcraft's tubes connecting to ME interfaces.")
        @Config.RequiresMcRestart
        public boolean interfaceTubes = true;

        @Config.Comment("Essentia storage cells, their components and their housing.")
        @Config.RequiresMcRestart
        public boolean essentiaCells = true;

        @Config.Comment("Portable essentia cells. Needs essentiaCells.")
        @Config.RequiresMcRestart
        public boolean portableEssentiaCells = true;

        @Config.Comment("The creative essentia cell.")
        @Config.RequiresMcRestart
        public boolean creativeEssentiaCell = true;

        private Features() {
        }
    }

    public static boolean arcaneTerminal() {
        return features.arcaneTerminal;
    }

    public static boolean wirelessArcaneMode() {
        return features.wirelessArcaneMode && arcaneTerminal();
    }

    public static boolean arcaneAutocrafting() {
        return features.arcaneAutocrafting && arcaneTerminal();
    }

    public static boolean infusionProvider() {
        return features.infusionProvider;
    }

    public static boolean interfaceTubes() {
        return features.interfaceTubes;
    }

    public static boolean essentiaCells() {
        return features.essentiaCells;
    }

    public static boolean portableEssentiaCells() {
        return features.portableEssentiaCells && essentiaCells();
    }

    public static boolean creativeEssentiaCell() {
        return features.creativeEssentiaCell;
    }

    /** Says which switches left on are off anyway, because what they need is off. */
    public static void warnAboutDependencies() {
        warnIf(features.wirelessArcaneMode && !wirelessArcaneMode(), "wirelessArcaneMode", "arcaneTerminal");
        warnIf(features.arcaneAutocrafting && !arcaneAutocrafting(), "arcaneAutocrafting", "arcaneTerminal");
        warnIf(features.portableEssentiaCells && !portableEssentiaCells(), "portableEssentiaCells", "essentiaCells");
    }

    private static void warnIf(final boolean dropped, final String feature, final String needs) {
        if (dropped) {
            ThaumicEnergistics.log.warn("{} is on in the config but stays off, because {} is off", feature, needs);
        }
    }
}
