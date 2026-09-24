/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics.client.hei;

import javax.annotation.Nullable;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

import appeng.api.integrations.hei.IngredientConverter;
import appeng.api.stacks.GenericStack;

import thaumicenergistics.me.EssentiaKey;

/**
 * Thaumic JEI shows an aspect in the recipe viewer as an {@link AspectList} of one aspect; this tells AE2UD how
 * that maps to essentia, so an aspect can be dragged into a filter slot, looked up from a terminal and carried
 * into a pattern. Without Thaumic JEI nothing registers the ingredient and this is never asked.
 */
public final class AspectIngredientConverter implements IngredientConverter<AspectList> {

    @Override
    public Class<AspectList> getIngredientClass() {
        return AspectList.class;
    }

    @Nullable
    @Override
    public AspectList getIngredientFromStack(final GenericStack stack) {
        if (!(stack.what() instanceof EssentiaKey essentia)) {
            return null;
        }
        // At least one: the viewer drops an ingredient of nothing.
        return new AspectList().add(essentia.getAspect(), (int) Math.max(1, Math.min(stack.amount(), Integer.MAX_VALUE)));
    }

    @Nullable
    @Override
    public GenericStack getStackFromIngredient(final AspectList ingredient) {
        if (ingredient.size() != 1) {
            return null;
        }
        final Aspect aspect = ingredient.getAspects()[0];
        final int amount = aspect == null ? 0 : ingredient.getAmount(aspect);
        return amount <= 0 ? null : new GenericStack(EssentiaKey.of(aspect), amount);
    }
}
