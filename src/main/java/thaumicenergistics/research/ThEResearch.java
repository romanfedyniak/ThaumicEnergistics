/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics.research;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.util.ResourceLocation;

import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ResearchAddendum;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategory;
import thaumcraft.api.research.ResearchEntry;
import thaumcraft.api.research.ScanningManager;
import thaumcraft.api.research.theorycraft.TheorycraftManager;

import appeng.api.AEApi;
import appeng.api.definitions.IBlocks;

import thaumicenergistics.EssentiaTier;
import thaumicenergistics.ThEConfig;
import thaumicenergistics.ThEItems;
import thaumicenergistics.ThaumicEnergistics;

/**
 * The mod's tab in the Thaumonomicon, as the original had it: opened by scanning anything of AE2's, with a card of
 * its own at the research table. Each feature's entries are a file of their own, and only the files of the features
 * switched on are handed to Thaumcraft, so the tab never teaches what is not there.
 */
public final class ThEResearch {

    public static final String CATEGORY = "THAUMICENERGISTICS";
    public static final String SCAN_KEY = "f_AECORE";

    /** Addenda that tell of a feature other than their entry's own, dropped with that feature. */
    private static final String PORTABLE_CELLS = "research.essentiastorage.addenda.portable";
    private static final String INTERFACE_TUBES = "research.essentiabuses.addenda.tubes";
    private static final String WIRELESS_MODE = "research.arcaneterminal.addenda.wireless";

    private ThEResearch() {
    }

    /** During initialisation, after Thaumcraft's own tabs exist and before it reads the research files. */
    public static void register() {
        final ResearchCategory basics = ResearchCategories.getResearchCategory("BASICS");
        ResearchCategories.registerCategory(CATEGORY, SCAN_KEY,
                new AspectList().add(Aspect.MECHANISM, 15).add(Aspect.CRAFT, 15).add(Aspect.ENERGY, 20)
                        .add(Aspect.EXCHANGE, 20).add(Aspect.MAGIC, 15).add(Aspect.METAL, 5),
                ThaumicEnergistics.id("textures/research/tab_icon.png"), basics.background, basics.background2);

        file("base");
        if (ThEConfig.essentiaCells()) {
            file("essentia_cells");
            if (ThEItems.COMPONENTS.containsKey(EssentiaTier.T256K)) {
                file("essentia_cells_large");
            }
        }
        if (ThEConfig.infusionProvider()) {
            file("infusion_provider");
        }
        if (ThEConfig.arcaneTerminal()) {
            file("arcane_terminal");
        }
        if (ThEConfig.arcaneAutocrafting()) {
            file("arcane_autocrafting");
        }

        ScanningManager.addScannableThing(new ScanAE2());

        TheorycraftManager.registerCard(CardTinkerAE.class);
        final IBlocks blocks = AEApi.instance().definitions().blocks();
        final Optional<Block> controller = blocks.controller().maybeBlock();
        (controller.isPresent() ? controller : blocks.drive().maybeBlock())
                .ifPresent(block -> TheorycraftManager.registerAid(new AidMENetwork(block)));
    }

    private static void file(final String name) {
        ThaumcraftApi.registerResearchLocation(new ResourceLocation(ThaumicEnergistics.MODID, "research/" + name));
    }

    /**
     * After Thaumcraft has read the files. An addendum cannot live in a file of its own feature, so the ones for a
     * feature switched off are taken back out of the entries they were read into.
     */
    public static void dropAddendaOfDisabledFeatures() {
        final Set<String> dropped = new HashSet<>();
        if (!ThEConfig.portableEssentiaCells()) {
            dropped.add(PORTABLE_CELLS);
        }
        if (!ThEConfig.interfaceTubes()) {
            dropped.add(INTERFACE_TUBES);
        }
        if (!ThEConfig.wirelessArcaneMode()) {
            dropped.add(WIRELESS_MODE);
        }
        final ResearchCategory category = ResearchCategories.getResearchCategory(CATEGORY);
        if (dropped.isEmpty() || category == null) {
            return;
        }
        for (final ResearchEntry entry : category.research.values()) {
            final ResearchAddendum[] addenda = entry.getAddenda();
            if (addenda != null) {
                entry.setAddenda(Arrays.stream(addenda).filter(addendum -> !dropped.contains(addendum.getText()))
                        .toArray(ResearchAddendum[]::new));
            }
        }
    }
}
