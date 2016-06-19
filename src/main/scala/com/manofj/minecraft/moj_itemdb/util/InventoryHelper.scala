package com.manofj.minecraft.moj_itemdb.util

import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack


object InventoryHelper {

  def toStream( inventory: IInventory ): Stream[ ItemStack ] =
    Stream.range( 0, inventory.getSizeInventory ).map( inventory.getStackInSlot )

  // インベントリにアイテムをいくつ収納可能かを返す
  def capacityLimit( inventory: IInventory, item: ItemStack ): Int = {
    val maxSize = item.getMaxStackSize min inventory.getInventoryStackLimit
    ( 0 /: toStream( inventory ) ) { ( limit, slotItem ) =>
      if      ( slotItem eq null )                                   limit + maxSize
      else if ( ItemHelper.areItemStackTypeEqual( slotItem, item ) ) limit + ( maxSize - slotItem.stackSize )
      else                                                           limit
  } }

  def capacityLimit( inventory: InventoryPlayer, item: ItemStack ): Int = {
    val maxSize = item.getMaxStackSize min inventory.getInventoryStackLimit
    ( 0 /: inventory.mainInventory ) { ( limit, slotItem ) =>
      if      ( slotItem eq null )                                   limit + maxSize
      else if ( ItemHelper.areItemStackTypeEqual( slotItem, item ) ) limit + ( maxSize - slotItem.stackSize )
      else                                                           limit
  } }

}
