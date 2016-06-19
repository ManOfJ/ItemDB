package com.manofj.minecraft.moj_itemdb.util

import net.minecraft.item.ItemStack

import com.manofj.minecraft.moj_commons.util.ImplicitConversions.AnyExtension


object ItemHelper {

  def areItemStackTypeEqual( item1: ItemStack, item2: ItemStack ): Boolean =
    ItemStack.areItemsEqualIgnoreDurability( item1, item2 ) &&
    ItemStack.areItemStackTagsEqual( item1, item2 )

  def areItemsStackable( dist: ItemStack, src: ItemStack ): Boolean =
    dist.?.zip( src.? ).exists { case ( x, y ) =>
      ( x.stackSize + y.stackSize <= x.getMaxStackSize ) &&
      areItemStackTypeEqual( x, y )
    }

}
