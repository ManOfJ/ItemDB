package com.manofj.minecraft.moj_itemdb.inventory

import net.minecraft.client.resources.I18n
import net.minecraft.entity.player.{ EntityPlayer, EntityPlayerMP }
import net.minecraft.inventory.{ IInventory, InventoryBasic }
import net.minecraft.item.ItemStack
import net.minecraft.util.text.{ ITextComponent, TextComponentTranslation }

import com.manofj.minecraft.moj_commons.collection.java.alias.JavaList

import com.manofj.minecraft.moj_itemdb.ItemDB
import com.manofj.minecraft.moj_itemdb.models.InventoryItem
import com.manofj.minecraft.moj_itemdb.network.ItemDBMessage.alias.DatabaseChangedMessage


class ItemDBInventory
  extends IInventory
{
  import com.manofj.minecraft.moj_itemdb.dao.H2DatabaseDriver._
  import com.manofj.minecraft.moj_itemdb.dao.alias._
  import com.manofj.minecraft.moj_itemdb.dao.mappers._
  import com.manofj.minecraft.moj_itemdb.util.ImplicitConversions.ItemStack.binary2Item
  import com.manofj.minecraft.moj_itemdb.util.ImplicitConversions.UnsignedLong._


  private[ this ] val dummyInventory = new InventoryBasic( "dummy", false, 60 )


  private[ this ] var keywordCache = ""


  // 初期化ブロック
  {
    searchAndUpdateInventory( "" )
  }


  def addHoveringText( textLines: JavaList[ String ], index: Int ): Unit =
    textLines.add( 1, I18n.format( "moj_itemdb.gui.stock_quantity", {
      val item = dummyInventory.getStackInSlot( index )
      Inventory.firstOption( Inventory.findByDataQuery( item ) )
        .map( _.size.toString )
        .getOrElse( "0" )
    } ) )

  def searchAndUpdateInventory( text: String ): Unit = {

    val keyword = if ( text.isEmpty ) "%" else text
    keywordCache = keyword

    val query = NameMap.sortedQuery
      .filter( _.text like keyword )
      .take( dummyInventory.getSizeInventory )

    dummyInventory.clear()
    NameMap.seq( query )
      .zipWithIndex
      .foreach { case ( record, index ) =>
        Inventory.foreach( _.filter( _.slot === record.slot ) ) {
          x => dummyInventory.setInventorySlotContents( index, x.data )

      } }
  }

  def refreshInventory(): Unit = searchAndUpdateInventory( keywordCache )

  def storeToDatabase( player: EntityPlayer, item: ItemStack, count: Int ): ItemStack = {
    if ( count == 0 ) return null

    val result = item.copy()
    val data = Inventory.item2Binary( result )

    Inventory.firstOption( _.filter( _.data === data ) ) match {
      case Some( record ) =>
        result.stackSize = Inventory.guardOverflow( record.size, count )

        if ( !player.worldObj.isRemote )
          Inventory.update( record.copy( size = record.size plus result.stackSize ) )

      case None =>
        result.stackSize = count

        if ( !player.worldObj.isRemote ) {
          Inventory.insert( InventoryItem( None, Inventory.item2Name( item ), data, result.stackSize ) )
          player match {
            case playerMp: EntityPlayerMP =>
              refreshInventory( )
              ItemDB.network.sendTo( new DatabaseChangedMessage, playerMp )
            case _ => // Illegal player
        } }
    }

    result.stackSize = item.stackSize - result.stackSize
    if ( result.stackSize > 0 ) result else null
  }

  def storeToDatabase( player: EntityPlayer, item: ItemStack ): ItemStack =
    storeToDatabase( player, item, item.stackSize )

  def takeFromDatabase( player: EntityPlayer, item: ItemStack, count: Int ): ItemStack = {
    if ( count == 0 ) return null

    var result: ItemStack = null

    val data = Inventory.item2Binary( item )
    Inventory.foreach( _.filter( _.data === data ) )
      { record =>
        result = record.data
        result.stackSize = count min record.size
      }

    if ( !player.worldObj.isRemote ) {
      Inventory.take( result, result.stackSize )
      if ( Inventory.count( _.filter( _.data === data ) ) == 0 )
        player match {
          case playerMp: EntityPlayerMP =>
            refreshInventory()
            ItemDB.network.sendTo( new DatabaseChangedMessage, playerMp )
          case _ => // Illegal player

    } }

    result
  }

  def deleteFromDatabase( player: EntityPlayer, item: ItemStack ): Unit =
    if ( !player.worldObj.isRemote ) {
      Inventory.foreach( _.filter( _.data === Inventory.item2Binary( item ) ) )( Inventory.delete )
      player match {
        case playerMp: EntityPlayerMP =>
          refreshInventory()
          ItemDB.network.sendTo( new DatabaseChangedMessage, playerMp )
        case _ => // Illegal player

    } }


  override def clear(): Unit = dummyInventory.clear()
  override def closeInventory( player: EntityPlayer ): Unit = dummyInventory.closeInventory( player )
  override def decrStackSize( index: Int, count: Int ): ItemStack = dummyInventory.decrStackSize( index, count )
  override def getField( id: Int ): Int = dummyInventory.getField( id )
  override def getFieldCount: Int = dummyInventory.getFieldCount
  override def getInventoryStackLimit: Int = dummyInventory.getInventoryStackLimit
  override def getSizeInventory: Int = dummyInventory.getSizeInventory
  override def getStackInSlot( index: Int ): ItemStack = dummyInventory.getStackInSlot( index )
  override def isItemValidForSlot( index: Int, stack: ItemStack ): Boolean = dummyInventory.isItemValidForSlot( index, stack )
  override def markDirty(): Unit = dummyInventory.markDirty()
  override def openInventory( player: EntityPlayer ): Unit = dummyInventory.openInventory( player )
  override def removeStackFromSlot( index: Int ): ItemStack = dummyInventory.removeStackFromSlot( index )
  override def setField( id: Int, value: Int ): Unit = dummyInventory.setField( id, value )
  override def setInventorySlotContents( index: Int, stack: ItemStack ): Unit = dummyInventory.setInventorySlotContents( index, stack )


  override def getDisplayName: ITextComponent = new TextComponentTranslation( this.getName )
  override def getName: String = "moj_itemdb.inventory"
  override def hasCustomName: Boolean = false
  override def isUseableByPlayer( player: EntityPlayer ): Boolean = true

}
