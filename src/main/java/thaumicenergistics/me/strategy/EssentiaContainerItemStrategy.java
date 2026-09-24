/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics.me.strategy;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.blocks.essentia.BlockJarItem;
import thaumcraft.common.items.consumables.ItemPhial;

import appeng.api.behaviors.ContainerItemStrategy;
import appeng.api.config.Actionable;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;

import thaumicenergistics.me.EssentiaKey;

/**
 * A phial or a jar in item form, filled and emptied against a terminal row or a filter slot like a bucket.
 * <p>
 * A phial holds ten of one aspect or nothing, the only two states Thaumcraft makes of it, so it fills and empties
 * whole. A jar holds up to 250 of one aspect and honours its label. A crystal also answers Thaumcraft's container
 * interface, but it is an item in its own right and is left alone.
 */
public final class EssentiaContainerItemStrategy implements ContainerItemStrategy {

    private static final int PHIAL_CAPACITY = 10;
    private static final int JAR_CAPACITY = 250;
    private static final int PHIAL_EMPTY = 0;
    private static final int PHIAL_FILLED = 1;

    @Nullable
    @Override
    public GenericStack getContainedStack(final ItemStack stack) {
        final Context context = this.openContext(stack);
        return context == null ? null : context.getExtractableContent();
    }

    @Nullable
    @Override
    public Context openContext(final ItemStack container) {
        if (container.isEmpty()) {
            return null;
        }
        final ItemStack one = container.copy();
        one.setCount(1);
        if (one.getItem() instanceof ItemPhial) {
            return new PhialContext(one);
        }
        if (one.getItem() instanceof BlockJarItem) {
            return new JarContext(one);
        }
        return null;
    }

    @Override
    public ItemStack getEmptyContainerFor(final AEKey what) {
        return what instanceof EssentiaKey ? new ItemStack(ItemsTC.phial, 1, PHIAL_EMPTY) : ItemStack.EMPTY;
    }

    @Nullable
    private static Aspect storedAspect(@Nullable final AspectList aspects) {
        if (aspects == null || aspects.size() == 0) {
            return null;
        }
        final Aspect aspect = aspects.getAspects()[0];
        return aspect != null && aspects.getAmount(aspect) > 0 ? aspect : null;
    }

    private static final class PhialContext implements Context {

        private final ItemStack stack;

        private PhialContext(final ItemStack stack) {
            this.stack = stack;
        }

        @Nullable
        private Aspect stored() {
            return this.stack.getItemDamage() == PHIAL_FILLED
                    ? storedAspect(((ItemPhial) this.stack.getItem()).getAspects(this.stack)) : null;
        }

        @Override
        public long insert(final AEKey what, final long amount, final Actionable mode) {
            if (!(what instanceof EssentiaKey essentia) || amount < PHIAL_CAPACITY
                    || this.stack.getItemDamage() != PHIAL_EMPTY) {
                return 0;
            }
            if (mode == Actionable.MODULATE) {
                this.stack.setItemDamage(PHIAL_FILLED);
                ((ItemPhial) this.stack.getItem()).setAspects(this.stack,
                        new AspectList().add(essentia.getAspect(), PHIAL_CAPACITY));
            }
            return PHIAL_CAPACITY;
        }

        @Override
        public long extract(final AEKey what, final long amount, final Actionable mode) {
            final Aspect stored = this.stored();
            if (!(what instanceof EssentiaKey essentia) || stored != essentia.getAspect() || amount < PHIAL_CAPACITY) {
                return 0;
            }
            if (mode == Actionable.MODULATE) {
                this.stack.setItemDamage(PHIAL_EMPTY);
                this.stack.setTagCompound(null);
            }
            return PHIAL_CAPACITY;
        }

        @Nullable
        @Override
        public GenericStack getExtractableContent() {
            final Aspect stored = this.stored();
            return stored == null ? null : new GenericStack(EssentiaKey.of(stored), PHIAL_CAPACITY);
        }

        @Override
        public ItemStack getContainer() {
            return this.stack;
        }
    }

    private static final class JarContext implements Context {

        private static final String ASPECTS_TAG = "Aspects";

        private final ItemStack stack;
        private final BlockJarItem item;

        private JarContext(final ItemStack stack) {
            this.stack = stack;
            this.item = (BlockJarItem) stack.getItem();
        }

        private int held(final Aspect aspect) {
            final AspectList aspects = this.item.getAspects(this.stack);
            return aspects == null ? 0 : aspects.getAmount(aspect);
        }

        @Override
        public long insert(final AEKey what, final long amount, final Actionable mode) {
            if (!(what instanceof EssentiaKey essentia) || amount <= 0) {
                return 0;
            }
            final Aspect aspect = essentia.getAspect();
            final Aspect stored = storedAspect(this.item.getAspects(this.stack));
            final Aspect label = this.item.getFilter(this.stack);
            if (stored != null && stored != aspect || label != null && label != aspect) {
                return 0;
            }
            final int held = this.held(aspect);
            final int toAdd = (int) Math.min(amount, JAR_CAPACITY - held);
            if (toAdd <= 0) {
                return 0;
            }
            if (mode == Actionable.MODULATE) {
                this.item.setAspects(this.stack, new AspectList().add(aspect, held + toAdd));
            }
            return toAdd;
        }

        @Override
        public long extract(final AEKey what, final long amount, final Actionable mode) {
            if (!(what instanceof EssentiaKey essentia) || amount <= 0) {
                return 0;
            }
            final Aspect aspect = essentia.getAspect();
            if (storedAspect(this.item.getAspects(this.stack)) != aspect) {
                return 0;
            }
            final int held = this.held(aspect);
            final int toRemove = (int) Math.min(amount, held);
            if (mode == Actionable.MODULATE) {
                if (toRemove == held) {
                    this.clearAspects();
                } else {
                    this.item.setAspects(this.stack, new AspectList().add(aspect, held - toRemove));
                }
            }
            return toRemove;
        }

        /**
         * An emptied jar keeps only its label, so it stacks with jars that never held anything.
         */
        private void clearAspects() {
            final NBTTagCompound tag = this.stack.getTagCompound();
            if (tag == null) {
                return;
            }
            tag.removeTag(ASPECTS_TAG);
            if (tag.isEmpty()) {
                this.stack.setTagCompound(null);
            }
        }

        @Nullable
        @Override
        public GenericStack getExtractableContent() {
            final Aspect stored = storedAspect(this.item.getAspects(this.stack));
            return stored == null ? null : new GenericStack(EssentiaKey.of(stored), this.held(stored));
        }

        @Override
        public ItemStack getContainer() {
            return this.stack;
        }
    }
}
