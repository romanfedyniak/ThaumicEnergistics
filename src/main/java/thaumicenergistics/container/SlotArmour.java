/*
 * Copyright (c) 2026 Thaumic Energistics UD contributors
 * Licensed under the MIT License; see LICENSE.
 */

package thaumicenergistics.container;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.wrapper.PlayerArmorInvWrapper;

import appeng.container.slot.AppEngSlot;

/**
 * One of the player's armour slots, as the player's own inventory screen has them. Kept on the terminal because
 * what the player wears lowers what arcane crafting costs.
 */
public class SlotArmour extends AppEngSlot {

    private final EntityPlayer player;
    private final EntityEquipmentSlot type;

    public SlotArmour(final EntityPlayer player, final EntityEquipmentSlot type, final int x, final int y) {
        super(new PlayerArmorInvWrapper(player.inventory), type.getIndex(), x, y);
        this.player = player;
        this.type = type;
    }

    @Override
    public boolean isItemValid(@Nonnull final ItemStack stack) {
        return stack.getItem().isValidArmor(stack, this.type, this.player);
    }

    @Override
    public int getSlotStackLimit() {
        return 1;
    }

    @Override
    public boolean canTakeStack(final EntityPlayer player) {
        final ItemStack worn = this.getStack();
        return (worn.isEmpty() || player.isCreative() || !EnchantmentHelper.hasBindingCurse(worn))
                && super.canTakeStack(player);
    }

    @Nullable
    @Override
    @SideOnly(Side.CLIENT)
    public String getSlotTexture() {
        return ItemArmor.EMPTY_SLOT_NAMES[this.type.getIndex()];
    }
}
