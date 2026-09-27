/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics.crafting;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import thaumcraft.api.crafting.IArcaneRecipe;

import appeng.api.networking.crafting.PatternRecipesChangedEvent;
import appeng.api.stacks.AEItemKey;

/**
 * The recipe each arcane pattern's grid makes, as AE2UD keeps it for its crafting patterns: a pattern is decoded
 * every time an interface loads or a tooltip is drawn, and walking the recipe registry is the costly part. Misses
 * are kept too. Everything is dropped when AE2UD says recipes have changed.
 */
public final class ArcanePatternRecipes {

    private static final Map<AEItemKey, Optional<IArcaneRecipe>> KNOWN = new ConcurrentHashMap<>();

    private ArcanePatternRecipes() {
    }

    @Nullable
    static IArcaneRecipe get(final ItemStack pattern, final Supplier<IArcaneRecipe> lookup) {
        final AEItemKey key = AEItemKey.of(pattern);
        if (key == null) {
            return lookup.get();
        }
        return KNOWN.computeIfAbsent(key, k -> Optional.ofNullable(lookup.get())).orElse(null);
    }

    @SubscribeEvent
    public static void onRecipesChanged(final PatternRecipesChangedEvent event) {
        KNOWN.clear();
    }
}
