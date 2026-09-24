/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics;

import java.util.Locale;
import java.util.function.Function;

import appeng.api.definitions.IItemDefinition;
import appeng.api.definitions.IItems;
import appeng.api.definitions.IMaterials;
import appeng.items.materials.MaterialType;

/**
 * The eight sizes of essentia storage, 1k to 16384k. The first four are the original mod's; the larger ones
 * follow AE2UD's high capacity switch, as its own cells do.
 */
public enum EssentiaTier {

    T1K("1k", 1, IItems::portableFluidCell1k, IMaterials::logicProcessor),
    T4K("4k", 4, IItems::portableFluidCell4k, IMaterials::calcProcessor),
    T16K("16k", 16, IItems::portableFluidCell16k, IMaterials::engProcessor),
    T64K("64k", 64, IItems::portableFluidCell64k, IMaterials::engProcessor),
    T256K("256k", 256, IItems::portableFluidCell256k, IMaterials::engProcessor),
    T1024K("1024k", 1024, IItems::portableFluidCell1024k, IMaterials::engProcessor),
    T4096K("4096k", 4096, IItems::portableFluidCell4096k, IMaterials::engProcessor),
    T16384K("16384k", 16384, IItems::portableFluidCell16384k, IMaterials::engProcessor);

    public final String name;
    public final int kilobytes;
    /** AE2UD's portable fluid cell of this size; the portable essentia cell is there when it is. */
    public final Function<IItems, IItemDefinition> portableFluidCell;
    public final Function<IMaterials, IItemDefinition> processor;

    EssentiaTier(final String name, final int kilobytes, final Function<IItems, IItemDefinition> portableFluidCell,
            final Function<IMaterials, IItemDefinition> processor) {
        this.name = name;
        this.kilobytes = kilobytes;
        this.portableFluidCell = portableFluidCell;
        this.processor = processor;
    }

    public boolean isHighCapacity() {
        return this.kilobytes >= 256;
    }

    /** AE2UD's universal component of the same size, which only fills the cell's constructor. */
    public MaterialType universalComponent() {
        return MaterialType.valueOf("CELL" + this.name.toUpperCase(Locale.ROOT) + "_PART");
    }

    /** The research that teaches this size, the original mod's key for the sizes it had. */
    public String research() {
        return "ESSENTIASTORAGE" + this.name;
    }
}
