package com.manofj.minecraft.moj_itemdb

import net.minecraft.inventory.{ IInventory, InventoryCrafting }
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.CraftingManager
import net.minecraft.nbt.NBTTagCompound

import net.minecraftforge.client.settings.KeyModifier
import net.minecraftforge.event.entity.item.ItemTossEvent
import net.minecraftforge.event.entity.living.LivingDeathEvent
import net.minecraftforge.event.entity.player.{ EntityItemPickupEvent, PlayerDestroyItemEvent }
import net.minecraftforge.fml.common.FMLCommonHandler
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.PlayerEvent.ItemCraftedEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent

import com.manofj.minecraft.moj_commons.util.ImplicitConversions.AnyExtension

import com.manofj.minecraft.moj_itemdb.util.{ InventoryHelper, ItemHelper }


object ItemDBEventHandler {
  import com.manofj.minecraft.moj_itemdb.dao.H2DatabaseDriver._
  import com.manofj.minecraft.moj_itemdb.dao.mappers._
  import com.manofj.minecraft.moj_itemdb.util.ImplicitConversions.UnsignedLong._
  import com.manofj.minecraft.moj_itemdb.util.ImplicitConversions.ItemStack._
  import com.manofj.minecraft.moj_itemdb.dao.alias.Inventory
  import com.manofj.minecraft.moj_itemdb.dao.alias.NameMap


  private[ this ] final val TAG_MARKDIRTY = "moj_itemdb.MarkDirty"


  private[ this ] var currentLanguage = FMLCommonHandler.instance.getCurrentLanguage


  private[ this ] def calcStackSize( item: ItemStack, inv: IInventory, quantity: Int = Int.MaxValue ): Int =
    item.getMaxStackSize min inv.getInventoryStackLimit min quantity


  // 使用言語が変更された際､アイテム名のマッピングを再実行する
  @SubscribeEvent
  def tick( event: ClientTickEvent ): Unit =
    FMLCommonHandler.instance.getCurrentLanguage.?
      .filter( _ != currentLanguage )
      .foreach { language =>
        currentLanguage = language

        NameMap.foreach( x => x ) { nameMap =>
          Inventory.foreach( _.filter( _.slot === nameMap.slot ) ) { inventory =>
            NameMap.update( nameMap.copy( text = inventory.data.getDisplayName ) )

        } }
      }


//  @SubscribeEvent
//  def livingDeath( event: LivingDeathEvent ): Unit = {}


  // プレイヤーがアイテムを投げた(捨てた)際､ショートカットキーが押されていればDBへ格納する
  @SubscribeEvent
  def itemToss( event: ItemTossEvent ): Unit =
    if ( ItemDBConfigHandler.specialKey.isActive ) {
      event.getEntityItem.?
        .filter( _.isEntityAlive )
        .foreach { itemEntity =>
          val original = itemEntity.getEntityItem
          val item = original.copy()

          Inventory.foreach( Inventory.findByDataQuery( item ) )
            { record =>
              item.stackSize = Inventory.guardOverflow( record.size, item.stackSize )
            }
          original.stackSize -= item.stackSize

          if ( !event.getPlayer.worldObj.isRemote )
            Inventory.store( item )

          if ( original.stackSize <= 0 ) itemEntity.setDead()
          event.setCanceled( itemEntity.isDead )
    } }


  // 接触しているアイテムがインベントリに入りきらない場合､DBへ格納する
  @SubscribeEvent
  def entityItemPickup( event: EntityItemPickupEvent ): Unit = {
    val itemEntity = event.getItem
    val original = itemEntity.getEntityItem
    val player = event.getEntityPlayer
    val playerInv = player.inventory

    InventoryHelper.toStream( playerInv )
      .indexWhere( ItemHelper.areItemsStackable( _, original ) ).?
      .collect { case -1 => playerInv.getFirstEmptyStack }
      .foreach {
        case -1 =>
          val item = original.copy()

          Inventory.foreach( Inventory.findByDataQuery( item ) )
            { record =>
              item.stackSize = Inventory.guardOverflow( record.size, item.stackSize )
            }
          original.stackSize -= item.stackSize

          if ( !player.worldObj.isRemote )
            Inventory.store( item )

          player.onItemPickup( itemEntity, 0 )

          if ( original.stackSize <= 0 ) itemEntity.setDead()
          event.setCanceled( itemEntity.isDead )
        case _ =>
  } }


  // プレイヤーがアイテムを使い切った際､自動でDBから充填する
  @SubscribeEvent
  def destroyItem( event: PlayerDestroyItemEvent ): Unit = {
    val original = event.getOriginal
    val player = event.getEntityPlayer

    Inventory.seq( _.filter( _.name === Inventory.item2Name( original ) ) )
      .map( x => ( x, x.data: ItemStack ) )
      .filter( x => ItemHelper.areItemStackTypeEqual( x._2, original ) )
      .sortBy( _._2.getItemDamage )
      .foreach { case ( record, item ) =>
          val stackSize = calcStackSize( item, player.inventory, record.size )

          val supply = original.copy()
          supply.stackSize = stackSize

          if ( stackSize == 1 )
            supply.setTagCompound( ( supply.getTagCompound.? getOrElse new NBTTagCompound ) << {
              _.setBoolean( TAG_MARKDIRTY, true )
            } )

          player.setHeldItem( event.getHand, supply )

          if ( !player.worldObj.isRemote )
            Inventory.take( original, stackSize )

          player.inventoryContainer.detectAndSendChanges()

          if ( stackSize == 1 ) {
            val tag = supply.getTagCompound.? getOrElse new NBTTagCompound
            tag.removeTag( TAG_MARKDIRTY )
            supply.setTagCompound( tag.?.filterNot( _.hasNoTags ).orNull )

      } }

  }


  // アイテムクラフト時､ショートカットキーが押されていればDBのアイテムを使用してクラフトを行う
  @SubscribeEvent
  def itemCrafted( event: ItemCraftedEvent ): Unit =
    if ( ItemDBConfigHandler.specialKey.isActive ) {
      val inventory = event.craftMatrix
      val player = event.player

      val materials = Range( 0, inventory.getSizeInventory )
        .map( inventory.getStackInSlot )
        .filter( _ ne null )

      materials.?
        .map( _.groupBy( Inventory.item2Binary ) )
        .map( _.map { case ( x, y ) => Inventory.firstOption( _.filter( _.data === x ) ) -> y } )
        .find( _.forall { case ( x, y ) => x.exists( z => ( z.size compareTo y.size ) >= 0 ) } )
        .foreach { x =>
          if ( !KeyModifier.SHIFT.isActive ) {
            x.foreach {
              case ( None, _ ) => assert( false )
              case ( Some( record ), items ) =>
                items.foreach( _.stackSize += 1 )

                if ( !player.worldObj.isRemote ) {
                  Inventory.take( items.head, items.size )

            } }
          }
          else {
            val manager = CraftingManager.getInstance
            val playerInv = player.inventory
            val crafting = event.crafting

            inventory.?
              .collect { case x: InventoryCrafting => manager.findMatchingRecipe( x, player.worldObj ) }
              .find( ItemHelper.areItemStackTypeEqual( _, crafting ) )
              .foreach { recipe =>
                val map = x.keys.flatten.zip( x.values )

                val item = recipe.copy()
                val amount = recipe.stackSize

                val stackSize = {
                  val craftable = ( Int.MaxValue /: map ) { ( x, y ) => x min ( y._1.size dividedBy y._2.size ) }
                  val capacity = InventoryHelper.capacityLimit( playerInv, item )
                  ( craftable * amount ) min ( ( capacity / amount ) * amount )
                }

                item.stackSize = stackSize
                playerInv.addItemStackToInventory( item )

                if ( !player.worldObj.isRemote ) {
                  val sizeBase = stackSize / amount

                  map.foreach { case ( record, items ) =>
                    Inventory.take( items.head, sizeBase * items.size )

              } } }

      } }
    }

}
