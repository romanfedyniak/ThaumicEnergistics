/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.oredict.OreDictionary;

import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.crafting.ShapedArcaneRecipe;
import thaumcraft.api.crafting.ShapelessArcaneRecipe;
import thaumcraft.api.items.ItemsTC;

import appeng.api.AEApi;
import appeng.api.definitions.IItemDefinition;
import appeng.api.definitions.IMaterials;

/**
 * The arcane recipes, the original mod's own. Thaumcraft writes them straight into the recipe registry, so they
 * are added while it is open.
 */
public final class ThERecipes {

    private static final ResourceLocation GROUP = ThaumicEnergistics.id("arcane");
    private static final int QUICKSILVER_NUGGET = 5;

    private ThERecipes() {
    }

    public static void register() {
        final IMaterials materials = AEApi.instance().definitions().materials();
        final ItemStack quicksilver = new ItemStack(ItemsTC.nuggets, 1, QUICKSILVER_NUGGET);

        ThaumcraftApi.addArcaneCraftingRecipe(ThaumicEnergistics.id("coalescence_core"),
                new ShapedArcaneRecipe(GROUP, "DIGISENTIA@2", 10, new AspectList(),
                        new ItemStack(ThEItems.COALESCENCE_CORE, 2),
                        "SSS", "QFL", "SSS",
                        'S', quicksilver,
                        'Q', anyOf("crystalCertusQuartz", materials.purifiedCertusQuartzCrystal()),
                        'F', stack(materials.fluixDust()),
                        'L', stack(materials.logicProcessor())));

        ThaumcraftApi.addArcaneCraftingRecipe(ThaumicEnergistics.id("diffusion_core"),
                new ShapedArcaneRecipe(GROUP, "DIGISENTIA@2", 10, new AspectList(),
                        new ItemStack(ThEItems.DIFFUSION_CORE, 2),
                        "SSS", "QFL", "SSS",
                        'S', quicksilver,
                        'Q', anyOf("gemQuartz", materials.purifiedNetherQuartzCrystal()),
                        'F', stack(materials.fluixDust()),
                        'L', stack(materials.logicProcessor())));

        ThaumcraftApi.addArcaneCraftingRecipe(ThaumicEnergistics.id("upgrade_arcane"),
                new ShapelessArcaneRecipe(GROUP, "ARCANETERMINAL@2&&WORKBENCHCHARGER", 25, new AspectList(),
                        new ItemStack(ThEItems.ARCANE_CHARGING_CARD),
                        new Object[] { stack(materials.advCard()), BlocksTC.arcaneWorkbenchCharger }));
    }

    private static ItemStack stack(final IItemDefinition definition) {
        return definition.maybeStack(1).orElse(ItemStack.EMPTY);
    }

    /** An ore dictionary name, and AE2's purified crystal alongside it. */
    private static Ingredient anyOf(final String ore, final IItemDefinition purified) {
        final List<ItemStack> stacks = new ArrayList<>(OreDictionary.getOres(ore));
        purified.maybeStack(1).ifPresent(stacks::add);
        return Ingredient.fromStacks(stacks.toArray(new ItemStack[0]));
    }
}
