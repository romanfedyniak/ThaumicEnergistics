/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.GameRegistry;

import appeng.api.AEApi;
import appeng.block.AEBaseItemBlock;
import appeng.core.features.ActivityState;
import appeng.core.features.BlockStackSrc;
import appeng.tile.AEBaseTile;
import appeng.api.upgrades.CardTrait;
import appeng.items.tools.powered.ToolPortableCell;

import thaumicenergistics.block.BlockInfusionProvider;
import thaumicenergistics.item.ItemArcaneChargingCard;
import thaumicenergistics.item.ItemArcaneTerminal;
import thaumicenergistics.item.ItemCreativeEssentiaCell;
import thaumicenergistics.item.ItemEssentiaStorageCell;
import thaumicenergistics.me.EssentiaKeyType;
import thaumicenergistics.tile.TileInfusionProvider;

/**
 * Everything the mod registers, by registry name. The names are the original mod's, so a world it saved finds
 * its items again.
 */
public final class ThEItems {

    public static final CreativeTabs TAB = new CreativeTabs(ThaumicEnergistics.MODID) {
        @Nonnull
        @Override
        public ItemStack createIcon() {
            return new ItemStack(COALESCENCE_CORE);
        }
    };

    /** Lets a machine draw vis from the chunks around its own, not only the one it stands in. */
    public static final CardTrait ARCANE_CHARGING = CardTrait.of(ThaumicEnergistics.id("arcane_charging"));

    public static final Map<String, Item> ITEMS = new LinkedHashMap<>();
    /** Where each item's model lives, when it is not under its own name. */
    public static final Map<Item, ResourceLocation> MODELS = new LinkedHashMap<>();

    public static Item COALESCENCE_CORE;
    public static Item DIFFUSION_CORE;
    public static Item ARCANE_CHARGING_CARD;
    public static Item ARCANE_TERMINAL;
    public static final Map<EssentiaTier, Item> COMPONENTS = new EnumMap<>(EssentiaTier.class);
    public static final Map<EssentiaTier, Item> CELLS = new EnumMap<>(EssentiaTier.class);
    public static final Map<EssentiaTier, Item> PORTABLE_CELLS = new EnumMap<>(EssentiaTier.class);
    public static Item ESSENTIA_CELL_HOUSING;
    public static Item CREATIVE_ESSENTIA_CELL;

    public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
    public static BlockInfusionProvider INFUSION_PROVIDER;

    private ThEItems() {
    }

    public static void init() {
        INFUSION_PROVIDER = block("infusion_provider", new BlockInfusionProvider());

        COALESCENCE_CORE = material("coalescence_core", new Item());
        DIFFUSION_CORE = material("diffusion_core", new Item());
        ARCANE_CHARGING_CARD = material("upgrade_arcane", new ItemArcaneChargingCard());
        ARCANE_TERMINAL = part("arcane_terminal", new ItemArcaneTerminal());

        final boolean highCapacity = AEApi.instance().definitions().items().cell256k().isEnabled();
        for (final EssentiaTier tier : EssentiaTier.values()) {
            if (!tier.isHighCapacity() || highCapacity) {
                COMPONENTS.put(tier, material("essentia_component_" + tier.name, new Item()));
            }
        }
        ESSENTIA_CELL_HOUSING = material("essentia_cell_housing", new Item());
        for (final EssentiaTier tier : COMPONENTS.keySet()) {
            CELLS.put(tier, cell("essentia_cell_" + tier.name, new ItemEssentiaStorageCell(tier)));
        }
        for (final EssentiaTier tier : COMPONENTS.keySet()) {
            if (tier.portableFluidCell.apply(AEApi.instance().definitions().items()).isEnabled()) {
                PORTABLE_CELLS.put(tier, cell("portable_essentia_cell_" + tier.name,
                        new ToolPortableCell(tier.kilobytes, () -> EssentiaKeyType.INSTANCE)));
            }
        }
        // No recipe: a pack that wants it as a reward gives it one.
        CREATIVE_ESSENTIA_CELL = cell("essentia_cell_creative", new ItemCreativeEssentiaCell());
    }

    private static <T extends Block> T block(final String name, final T block) {
        block.setRegistryName(ThaumicEnergistics.id(name));
        block.setTranslationKey(ThaumicEnergistics.MODID + "." + name);
        block.setCreativeTab(TAB);
        BLOCKS.put(name, block);
        return block;
    }

    /** Called while blocks are registered, and before the items that stand for them are. */
    public static void registerTiles() {
        registerTile(TileInfusionProvider.class, INFUSION_PROVIDER, "infusion_provider");
        for (final Block block : BLOCKS.values()) {
            ITEMS.put(block.getRegistryName().getPath(), new AEBaseItemBlock(block).setRegistryName(block.getRegistryName()));
        }
    }

    private static void registerTile(final Class<? extends TileEntity> tile, final Block block, final String name) {
        GameRegistry.registerTileEntity(tile, ThaumicEnergistics.id(name));
        AEBaseTile.registerTileItem(tile, new BlockStackSrc(block, 0, ActivityState.Enabled));
    }

    private static Item cell(final String name, final Item item) {
        item(name, item);
        MODELS.put(item, ThaumicEnergistics.id("cell/" + name));
        return item;
    }

    private static Item part(final String name, final Item item) {
        item(name, item);
        MODELS.put(item, ThaumicEnergistics.id("part/" + name));
        return item;
    }

    private static Item material(final String name, final Item item) {
        item(name, item);
        MODELS.put(item, ThaumicEnergistics.id("material/" + name));
        return item;
    }

    private static Item item(final String name, final Item item) {
        item.setRegistryName(ThaumicEnergistics.id(name));
        item.setTranslationKey(ThaumicEnergistics.MODID + "." + name);
        item.setCreativeTab(TAB);
        ITEMS.put(name, item);
        return item;
    }
}
