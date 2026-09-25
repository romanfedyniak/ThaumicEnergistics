/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics.crafting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.common.crafting.IShapedRecipe;

import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.ContainerDummy;
import thaumcraft.api.crafting.IArcaneRecipe;

import appeng.api.networking.crafting.FabricatedSlots;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.IPatternInput;
import appeng.api.networking.crafting.IPatternInputs;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;

import thaumicenergistics.item.ItemArcanePattern;

/**
 * An encoded arcane pattern, as the network reads it.
 * <p>
 * A crafting pattern on a table five across and three down, the arcane workbench's own shape: the recipe's nine
 * squares in the middle three columns and the crystals it takes down the two outer ones, the first three primals
 * on the left and the other three on the right. The network lays a craft out that way, and the pattern view draws
 * it that way too.
 */
public final class ArcanePatternDetails implements ICraftingPatternDetails {

    public static final int WIDTH = 5;
    public static final int HEIGHT = 3;
    private static final int GRID = 9;

    private final ItemStack pattern;
    private final ItemStack result;
    /** The recipe's nine squares, in its own three-across order. */
    private final ItemStack[] grid;
    private final AspectList crystals;
    private final GenericStack[] inputs;
    private final GenericStack[] outputs;
    private final IPatternInputs patternInputs;
    /** Null unless the pattern substitutes, in which case one entry per slot of the table. */
    @Nullable
    private final Ingredient[] ingredients;
    private final boolean canSubstituteFluids;
    @Nullable
    private IArcaneRecipe recipe;
    private boolean recipeLookedUp;
    private int priority;

    /** @throws IllegalArgumentException when the stack is not an arcane pattern this can read. */
    public ArcanePatternDetails(final ItemStack stack, @Nullable final World world) {
        if (!ItemArcanePattern.isEncoded(stack)) {
            throw new IllegalArgumentException("No arcane pattern here!");
        }
        this.pattern = stack;
        this.result = ItemArcanePattern.resultOf(stack);
        this.grid = ItemArcanePattern.gridOf(stack);
        this.crystals = ItemArcanePattern.crystalsOf(stack);
        if (this.result.isEmpty()) {
            throw new IllegalArgumentException("No arcane pattern here!");
        }

        this.inputs = new GenericStack[WIDTH * HEIGHT];
        boolean any = false;
        for (int square = 0; square < GRID; square++) {
            this.inputs[tableSlotOfSquare(square)] = GenericStack.fromItemStack(this.grid[square]);
            any |= !this.grid[square].isEmpty();
        }
        if (!any) {
            throw new IllegalArgumentException("No arcane pattern here!");
        }
        for (int index = 0; index < ArcaneGrid.PRIMALS.length; index++) {
            final int amount = this.crystals.getAmount(ArcaneGrid.PRIMALS[index]);
            if (amount > 0) {
                this.inputs[tableSlotOfCrystal(index)] = new GenericStack(crystal(index), amount);
            }
        }
        this.outputs = new GenericStack[] { GenericStack.fromItemStack(this.result) };

        final boolean wantsItems = ItemArcanePattern.substitutesOf(stack);
        final boolean wantsFluids = ItemArcanePattern.fluidSubstitutesOf(stack);
        final IArcaneRecipe found = (wantsItems || wantsFluids) && world != null ? this.recipe(world) : null;

        this.ingredients = found == null || !wantsItems ? null : this.ingredientsFor(found);
        final GenericStack[] fabricated = new GenericStack[WIDTH * HEIGHT];
        if (found != null && wantsFluids) {
            final GenericStack[] filled = FabricatedSlots.find(this.squares(), new ArcaneTerminalRecipe(found));
            for (int square = 0; square < GRID; square++) {
                fabricated[tableSlotOfSquare(square)] = filled[square];
            }
        }
        boolean anyFabricated = false;
        for (final GenericStack slot : fabricated) {
            anyFabricated |= slot != null;
        }
        this.canSubstituteFluids = anyFabricated;
        this.patternInputs = this.ingredients == null && !anyFabricated
                ? IPatternInputs.of(this.inputs)
                : substitutesOf(this.inputs, this.ingredients, fabricated);
    }

    /** Where square {@code square} of the recipe's three-by-three sits on the five-across table. */
    public static int tableSlotOfSquare(final int square) {
        return square % 3 + 1 + square / 3 * WIDTH;
    }

    /** Where the crystal of primal {@code index} sits: down the left, then down the right. */
    public static int tableSlotOfCrystal(final int index) {
        return index < 3 ? index * WIDTH : WIDTH - 1 + (index - 3) * WIDTH;
    }

    private static AEItemKey crystal(final int index) {
        return AEItemKey.of(ThaumcraftApiHelper.makeCrystal(ArcaneGrid.PRIMALS[index]));
    }

    /** What one craft makes. */
    public ItemStack getResult() {
        return this.result.copy();
    }

    public AspectList getCrystals() {
        return this.crystals;
    }

    /** The recipe the nine squares make, looked up once, the first time it is wanted. */
    @Nullable
    public IArcaneRecipe recipe(final World world) {
        if (!this.recipeLookedUp) {
            this.recipeLookedUp = true;
            this.recipe = ArcaneTerminalRecipe.match(this.squares(), world);
        }
        return this.recipe;
    }

    private InventoryCrafting squares() {
        final InventoryCrafting squares = new InventoryCrafting(new ContainerDummy(), 3, 3);
        for (int square = 0; square < GRID; square++) {
            squares.setInventorySlotContents(square, this.grid[square].copy());
        }
        return squares;
    }

    /** The recipe's nine squares out of a table laid out as this pattern lays it. */
    public static InventoryCrafting squaresOf(final InventoryCrafting table) {
        final InventoryCrafting squares = new InventoryCrafting(new ContainerDummy(), 3, 3);
        for (int square = 0; square < GRID; square++) {
            squares.setInventorySlotContents(square, table.getStackInSlot(tableSlotOfSquare(square)).copy());
        }
        return squares;
    }

    @Override
    public ItemStack getPattern() {
        return this.pattern;
    }

    @Override
    public boolean isCraftable() {
        return true;
    }

    @Override
    public String getTypeTranslationKey() {
        return "gui.thaumicenergistics.arcane_pattern";
    }

    @Override
    public int getTableWidth() {
        return WIDTH;
    }

    @Override
    public int getTableHeight() {
        return HEIGHT;
    }

    @Override
    public boolean isValidItemForSlot(final int slotIndex, final ItemStack itemStack, final World world) {
        if (slotIndex < 0 || slotIndex >= this.inputs.length) {
            return false;
        }
        if (this.ingredients != null && this.ingredients[slotIndex] != null) {
            return this.ingredients[slotIndex].apply(itemStack);
        }
        final GenericStack wanted = this.inputs[slotIndex];
        return wanted == null ? itemStack.isEmpty() : AEItemKey.matches(wanted.what(), itemStack);
    }

    @Override
    public GenericStack[] getInputs() {
        return this.inputs;
    }

    @Override
    public IPatternInputs getPatternInputs() {
        return this.patternInputs;
    }

    @Override
    public GenericStack[] getOutputs() {
        return this.outputs;
    }

    @Override
    public GenericStack[] getCondensedOutputs() {
        return this.outputs;
    }

    @Override
    public boolean canSubstitute() {
        return this.ingredients != null;
    }

    @Override
    public boolean canSubstituteFluids() {
        return this.canSubstituteFluids;
    }

    /** What the table in front of the machine makes, which has to be what the pattern promised. */
    @Override
    public ItemStack getOutput(final InventoryCrafting craftingInv, final World world) {
        final IArcaneRecipe made = this.recipe(world);
        if (made == null) {
            return ItemStack.EMPTY;
        }
        final InventoryCrafting squares = squaresOf(craftingInv);
        if (!made.matches(ArcaneGrid.of(squares), world)) {
            return ItemStack.EMPTY;
        }
        final ItemStack output = made.getCraftingResult(ArcaneGrid.of(squares));
        return ItemStack.areItemsEqual(output, this.result) && ItemStack.areItemStackTagsEqual(output, this.result)
                ? output
                : ItemStack.EMPTY;
    }

    /** What the recipe leaves in its squares; the crystals are used up. */
    @Nullable
    @Override
    public NonNullList<ItemStack> getRemainingItems(final InventoryCrafting craftingInv, final World world) {
        final IArcaneRecipe made = this.recipe(world);
        if (made == null) {
            return null;
        }
        final NonNullList<ItemStack> left = new ArcaneTerminalRecipe(made).getRemainingItems(squaresOf(craftingInv));
        final NonNullList<ItemStack> remaining = NonNullList.withSize(craftingInv.getSizeInventory(),
                ItemStack.EMPTY);
        for (int square = 0; square < GRID && square < left.size(); square++) {
            remaining.set(tableSlotOfSquare(square), left.get(square));
        }
        return remaining;
    }

    /**
     * What else the recipe accepts in each square that was drawn: read off a shaped recipe by where the drawing
     * sits, or taken from the first of a shapeless recipe's ingredients that accepts what was drawn.
     */
    @Nullable
    private Ingredient[] ingredientsFor(final IArcaneRecipe found) {
        final Ingredient[] byTable = new Ingredient[WIDTH * HEIGHT];
        final List<Ingredient> ingredients = found.getIngredients();
        int minX = 3;
        int minY = 3;
        for (int square = 0; square < GRID; square++) {
            if (!this.grid[square].isEmpty()) {
                minX = Math.min(minX, square % 3);
                minY = Math.min(minY, square / 3);
            }
        }
        for (int square = 0; square < GRID; square++) {
            final ItemStack drawn = this.grid[square];
            if (drawn.isEmpty()) {
                continue;
            }
            Ingredient match = null;
            if (found instanceof IShapedRecipe shaped) {
                final int width = shaped.getRecipeWidth();
                final int x = square % 3 - minX;
                final int y = square / 3 - minY;
                match = at(ingredients, x + y * width, drawn);
                if (match == null) {
                    match = at(ingredients, width - 1 - x + y * width, drawn);
                }
            } else {
                for (final Ingredient ingredient : ingredients) {
                    if (ingredient.apply(drawn)) {
                        match = ingredient;
                        break;
                    }
                }
            }
            byTable[tableSlotOfSquare(square)] = match;
        }
        return byTable;
    }

    @Nullable
    private static Ingredient at(final List<Ingredient> ingredients, final int index, final ItemStack drawn) {
        return index >= 0 && index < ingredients.size() && ingredients.get(index).apply(drawn)
                ? ingredients.get(index)
                : null;
    }

    @Override
    public int getPriority() {
        return this.priority;
    }

    @Override
    public void setPriority(final int priority) {
        this.priority = priority;
    }

    @Override
    public int hashCode() {
        return AEItemKey.of(this.pattern).hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof ArcanePatternDetails other && ItemStack.areItemStacksEqual(this.pattern, other.pattern);
    }

    /**
     * The inputs of a substituting pattern: each square, and everything else its ingredient takes - or, for a
     * square the network fills itself, what it puts there. The crystals are always exactly what the recipe asks.
     */
    private static IPatternInputs substitutesOf(final GenericStack[] inputs, @Nullable final Ingredient[] ingredients,
            final GenericStack[] fabricated) {
        final IPatternInput[] slots = new IPatternInput[inputs.length];
        final Map<AEKey, GenericStack> totals = new LinkedHashMap<>();
        for (int slot = 0; slot < inputs.length; slot++) {
            final GenericStack input = inputs[slot];
            if (input == null) {
                slots[slot] = IPatternInput.NOTHING;
                continue;
            }
            slots[slot] = fabricated[slot] != null
                    ? new Filled(fabricated[slot])
                    : new Substitutes(input, ingredients == null ? null : ingredients[slot]);
            totals.merge(input.what(), input, GenericStack::sum);
        }
        return new IPatternInputs.Fixed(slots, totals.values().toArray(new GenericStack[0]));
    }

    /** One square of a substituting pattern: what was encoded there, and then what else would do. */
    private static final class Substitutes implements IPatternInput {

        private final List<GenericStack> options;

        Substitutes(final GenericStack encoded, @Nullable final Ingredient ingredient) {
            final ItemStack[] accepted = ingredient == null ? new ItemStack[0] : ingredient.getMatchingStacks();
            this.options = new ArrayList<>(accepted.length + 1);
            // The encoded item first, so that what the player chose is what the network reaches for.
            this.options.add(new GenericStack(encoded.what(), 1));
            for (final ItemStack stack : accepted) {
                final AEItemKey key = AEItemKey.of(stack);
                if (key != null && !key.equals(encoded.what())) {
                    this.options.add(new GenericStack(key, 1));
                }
            }
        }

        @Override
        public List<GenericStack> getOptions() {
            return this.options;
        }

        @Override
        public boolean isFabricated() {
            return false;
        }
    }

    /** One square the network fills for itself: the contents out of storage, and no container handed back. */
    private static final class Filled implements IPatternInput {

        private final List<GenericStack> supplied;

        Filled(final GenericStack contents) {
            this.supplied = Collections.singletonList(contents);
        }

        @Override
        public List<GenericStack> getOptions() {
            return this.supplied;
        }

        @Override
        public boolean isFabricated() {
            return true;
        }
    }
}
