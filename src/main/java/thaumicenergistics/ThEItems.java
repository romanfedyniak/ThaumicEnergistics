/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import appeng.api.upgrades.CardTrait;

import thaumicenergistics.item.ItemArcaneChargingCard;

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

    private ThEItems() {
    }

    public static void init() {
        COALESCENCE_CORE = material("coalescence_core", new Item());
        DIFFUSION_CORE = material("diffusion_core", new Item());
        ARCANE_CHARGING_CARD = material("upgrade_arcane", new ItemArcaneChargingCard());
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
