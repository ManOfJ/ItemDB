package com.manofj.minecraft.moj_itemdb.inventory

import net.minecraft.entity.player.{ EntityPlayer, InventoryPlayer }
import net.minecraft.inventory.{ ClickType, Container, Slot }
import net.minecraft.item.ItemStack

import net.minecraftforge.client.settings.KeyModifier

import com.manofj.minecraft.moj_commons.util.ImplicitConversions.AnyExtension

import com.manofj.minecraft.moj_itemdb.util.{ InventoryHelper, ItemHelper }


class ItemDBInventoryContainer(                           playerInventory: InventoryPlayer,
                                private[ moj_itemdb ] val dbInventory:     ItemDBInventory )
  extends Container
{
  // 初期化ブロック
  {
    for { row    <- 0 until 5
          column <- 0 until 12
    } addSlotToContainer( new Slot( dbInventory, ( row * 12 ) + column, ( column * 18 ) + 8, ( row * 18 ) + 42 ) )

    for { row    <- 0 until 3
          column <- 0 until 9
    } addSlotToContainer( new Slot( playerInventory, ( ( row * 9 ) + column ) + 9, ( column * 18 ) + 8, ( row * 18 ) + 146 ) )

    for ( column <- 0 until 9 ) addSlotToContainer( new Slot( playerInventory, column, ( column * 18 ) + 8, 204 ) )
  }


  private[ this ] def isPlayerInventory( index: Int ): Boolean =
    index >= dbInventory.getSizeInventory && index < inventorySlots.size

  private[ this ] def isDBInventory( index: Int ): Boolean =
    index >= 0 && index < dbInventory.getSizeInventory


  override def canInteractWith( playerIn: EntityPlayer ): Boolean = dbInventory.isUseableByPlayer( playerIn )

  override def slotClick( slotId: Int, dragType: Int, clickTypeIn: ClickType, player: EntityPlayer ): ItemStack = {
    def slotItem( index: Int ): Option[ ItemStack ] = getSlot( index ).getStack.?

    var result: ItemStack = null

    if ( isDBInventory( slotId ) ) {
      val itemOpt = slotItem( slotId )

      clickTypeIn match {
        // スロットのアイテムをDBから､まとめてピックアップする
        // Ctrl+Altキーを押下しているとスロットのアイテムを削除する
        case ClickType.CLONE =>
          itemOpt.foreach { slotItem =>
            if ( KeyModifier.CONTROL.isActive && KeyModifier.ALT.isActive ) {
              dbInventory.deleteFromDatabase( player, slotItem )
            }
            else if ( player.inventory.getItemStack.?.isEmpty ) {
              result = dbInventory.takeFromDatabase( player, slotItem, slotItem.getMaxStackSize )
              player.inventory.setItemStack( result )
          } }

        // スロットのアイテムをDBから､一つずつピックアップする
        // ピックアップしているアイテムが存在し､スロットのアイテムと違う種類である場合は収納する
        case ClickType.PICKUP =>
          def sizeOfPickup( i: ItemStack ): Int =
            if ( dragType == 0 )                         i.getMaxStackSize - i.stackSize
            else if ( i.getMaxStackSize == i.stackSize ) 0
            else                                         1

          def sizeOfStore( i: ItemStack ): Int = if ( dragType == 0 ) i.stackSize else 1

          player.inventory.getItemStack.? match {
            case Some( grab ) => itemOpt match {
              // ピックアップしているアイテムが存在するがスロットのアイテムが存在しない
              // 右クリックなら一つ､左クリックなら可能な限り､データベースに収納
              case None =>
                dbInventory.storeToDatabase( player, grab, sizeOfStore( grab ) ) match {
                  case item: ItemStack => grab.stackSize = item.stackSize
                  case _ => player.inventory.setItemStack( null )
                }

              // ピックアップしているアイテム､スロットのアイテム両方が存在
              case Some( slotItem ) =>
                // 二つのアイテムが同一種類である場合
                // 右クリックなら一つ､左クリックなら可能な限り､ピックアップしているアイテムに追加
                if ( ItemHelper.areItemStackTypeEqual( grab, slotItem ) ) {
                  dbInventory.takeFromDatabase( player, grab, sizeOfPickup( grab ) ).?
                    .foreach( grab.stackSize += _.stackSize )
                }
                // 二つのアイテムが違う種類の場合
                // 右クリックなら一つ､左クリックなら可能な限り､データベースに収納
                else {
                  dbInventory.storeToDatabase( player, grab, sizeOfStore( grab ) ) match {
                    case item: ItemStack => grab.stackSize = item.stackSize
                    case _ => player.inventory.setItemStack( null )
                  }

            } }

            // スロットのアイテムが存在するがピックアップしているアイテムが存在しない
            // 右クリックなら一つ､左クリックなら可能な限りピックアップ
            case None => itemOpt.foreach { slotItem =>
              val item = slotItem.copy(); item.stackSize = 0
              result = dbInventory.takeFromDatabase( player, slotItem, sizeOfPickup( item ) )
              player.inventory.setItemStack( result )
          } }

        // スロットのアイテムをDBからプレイヤーのインベントリにまとめて移動する
        case ClickType.QUICK_MOVE =>
          itemOpt.foreach { slotItem =>
            val size = if ( KeyModifier.CONTROL.isActive ) InventoryHelper.capacityLimit( player.inventory, slotItem )
                       else                                slotItem.getMaxStackSize

            val item = dbInventory.takeFromDatabase( player, slotItem, size )

            if ( !player.inventory.addItemStackToInventory( item ) )
              dbInventory.storeToDatabase( player, item )

          }

        // スロットのアイテムをDBから排出する
        case ClickType.THROW =>
          itemOpt.foreach { slotItem =>
            val size = if ( KeyModifier.CONTROL.isActive ) slotItem.getMaxStackSize else 1
            player.dropItem( dbInventory.takeFromDatabase( player, slotItem, size ), false, false )
          }

        // 上記以外の場合は何もしない
        case _ =>
    } }
    else if ( isPlayerInventory( slotId ) ) {
      val slot = getSlot( slotId )

      clickTypeIn match {
        // 見本のアイテムも統合してしまうため一時使用不可
        case ClickType.PICKUP_ALL =>

        // スロットのアイテムをまとめてDBから取り出す
        case ClickType.CLONE =>
          slot.getStack.?.foreach { slotItem =>
            if ( KeyModifier.SHIFT.isActive && KeyModifier.CONTROL.isActive ) {
              val size = InventoryHelper.capacityLimit( player.inventory, slotItem )
              val item = dbInventory.takeFromDatabase( player, slotItem, size )

              if ( !player.inventory.addItemStackToInventory( item ) )
                dbInventory.storeToDatabase( player, item )

            }
            else {
              val size = slotItem.getMaxStackSize - slotItem.stackSize
              val item = dbInventory.takeFromDatabase( player, slotItem, size )

              slotItem.stackSize += item.stackSize
              slot.onSlotChanged()
          } }

        // スロットのアイテムをまとめてDBに収納する
        case ClickType.QUICK_MOVE =>
          slot.getStack.?.foreach { slotItem =>
            if ( KeyModifier.CONTROL.isActive ) {
              import scala.collection.convert.WrapAsScala._
              inventorySlots
                .withFilter( _.inventory == player.inventory )
                .map( x => ( x, x.getStack ) )
                .withFilter( x => ItemHelper.areItemStackTypeEqual( x._2, slotItem ) )
                .foreach { case ( x, y ) =>
                  x.putStack( dbInventory.storeToDatabase( player, y ) )
                  x.onSlotChanged()
            } }
            else {
              slot.putStack( dbInventory.storeToDatabase( player, slotItem ) )
              slot.onSlotChanged()
          } }

        // 上記以外の場合は本来の処理を走らせる
        case _ => result = super.slotClick( slotId, dragType, clickTypeIn, player )
    } }
    else {
      // スロットIDが範囲外( ドラッグ処理など )の場合
      result = super.slotClick( slotId, dragType, clickTypeIn, player )
    }

    result
  }

  override def canDragIntoSlot( slotIn: Slot ): Boolean = {
    slotIn.?.fold( true )( _.inventory == playerInventory )
  }

  override def transferStackInSlot( playerIn: EntityPlayer, index: Int ): ItemStack = { null }
  override def mergeItemStack( stack: ItemStack, startIndex: Int, endIndex: Int, reverseDirection: Boolean ): Boolean = { false }

}
