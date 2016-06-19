package com.manofj.minecraft.moj_itemdb.dao

import scala.slick.jdbc.StaticQuery
import scala.slick.lifted.ProvenShape

import com.google.common.primitives.UnsignedLong
import org.apache.commons.io.FileUtils

import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

import com.manofj.minecraft.moj_commons.io.java.alias.JavaFile
import com.manofj.minecraft.moj_commons.util.ImplicitConversions.AnyExtension

import com.manofj.minecraft.moj_itemdb.ItemDB
import com.manofj.minecraft.moj_itemdb.models.InventoryItem


object InventoryDAO {
  import H2DatabaseDriver._
  import com.manofj.minecraft.moj_itemdb.dao.mappers._
  import com.manofj.minecraft.moj_itemdb.dao.types.Binary
  import com.manofj.minecraft.moj_itemdb.util.ImplicitConversions.ItemStack.ItemStackExtension
  import com.manofj.minecraft.moj_itemdb.util.ImplicitConversions.UnsignedLong._


  class InventoryTable( tag: Tag )
    extends Table[ InventoryItem ]( tag, "INVENTORY" )
  {
    def slot = column[ UnsignedLong ]( "SLOT", O.PrimaryKey, O.AutoInc )
    def name = column[ String ]( "NAME", O.NotNull )
    def data = column[ Binary ]( "DATA", O.NotNull )
    def size = column[ UnsignedLong ]( "SIZE", O.NotNull )

    override def * : ProvenShape[ InventoryItem ] =
      ( slot.?, name, data, size ) <> ( InventoryItem.tupled, InventoryItem.unapply )

  }


  type InventoryQuery = Query[ InventoryTable, InventoryItem, Seq ]
  type InventoryQueryChain = InventoryQuery => InventoryQuery

  private[ this ] final lazy val db = Database.forConfig( "moj_itemdb.inventory" )


  private[ moj_itemdb ] val tableQuery = TableQuery[ InventoryTable ]


  private[ this ] def item2NBT( is: ItemStack ): NBTTagCompound =
    is.writeToNBT( new NBTTagCompound ) << { _.setByte( "Count", 1 ) }


  private[ moj_itemdb ] def item2Binary( is: ItemStack ): Binary = is.toBinary( item2NBT )

  private[ moj_itemdb ] def item2Name( is: ItemStack ): String =
    is.getItem.?
      .flatMap( _.getRegistryName.? )
      .map( _.toString )
      .getOrElse( "minecraft:air" )

  private[ moj_itemdb ] def guardOverflow( current: UnsignedLong, amount: UnsignedLong ): UnsignedLong =
    if ( current == UnsignedLong.MAX_VALUE )
      UnsignedLong.ZERO
    else if ( ( ( current plus amount ) compareTo current ) < 0 )
      UnsignedLong.MAX_VALUE minus current
    else
      amount


  private[ moj_itemdb ] def findByDataQuery( item: ItemStack ): InventoryQuery =
    tableQuery.filter( _.data === item.toBinary( item2NBT ) )


  private[ moj_itemdb ] def seq( query: InventoryQuery ): Seq[ InventoryItem ] =
    db.withSession( implicit s => query.list )

  private[ moj_itemdb ] def seq( queryChain: InventoryQueryChain ): Seq[ InventoryItem ] =
    seq( queryChain( tableQuery ) )

  private[ moj_itemdb ] def firstOption( query: InventoryQuery ): Option[ InventoryItem ] =
    db.withSession { implicit s => query.firstOption }

  private[ moj_itemdb ] def firstOption( queryChain: InventoryQueryChain ): Option[ InventoryItem ] =
    firstOption( queryChain( tableQuery ) )

  private[ moj_itemdb ] def foreach( query: InventoryQuery )
                                   ( function: InventoryItem => Unit ): Unit =
    db.withSession { implicit s => query.foreach( function ) }

  private[ moj_itemdb ] def foreach( queryChain: InventoryQueryChain )
                                   ( function: InventoryItem => Unit ): Unit =
    foreach( queryChain( tableQuery ) )( function )

  private[ moj_itemdb ] def count( query: InventoryQuery ): Int =
    db.withSession { implicit s => query.length.run }

  private[ moj_itemdb ] def count( queryChain: InventoryQueryChain ): Int =
    count( queryChain( tableQuery ) )


  private[ moj_itemdb ] def dumpToFile( file: JavaFile ): Unit =
    file.? foreach { x =>
      file.?.filterNot( _.isFile ).foreach { y =>
        FileUtils.touch( y )
        ItemDB.log.info( s"Make new database file: ${ y.getAbsolutePath }" )
      }

      db.withSession { implicit s =>
        val dumpQuery = StaticQuery.update[ String ]( "SCRIPT TO ? COMPRESSION GZIP" )
        dumpQuery( x.getCanonicalPath ).execute

    } }

  private[ moj_itemdb ] def restoreFromFile( file: JavaFile ): Unit =
    db.withSession { implicit s => file.? foreach {
      case x if x.isFile =>
        val restoreQuery = StaticQuery.update[ String ]( "RUNSCRIPT FROM ? COMPRESSION GZIP" )
        restoreQuery( x.getCanonicalPath ).execute
      case _ =>
        StaticQuery.updateNA( "RUNSCRIPT FROM 'classpath:sql/init_inventory.sql'" ).execute
    } }




  def store( item: ItemStack ): Unit =
    item2Name( item ) match {
      case "minecraft:air" =>
      // 無効なリソースロケーション

      case registryName =>
        val data = item2Binary( item )

        db.withSession { implicit s =>
          tableQuery.filter( _.data === data ).firstOption match {
            case Some( record ) =>
              tableQuery.filter( _.slot === record.slot )
                .update( record.copy( size = record.size plus item.stackSize ) )

            case None =>
              tableQuery += InventoryItem( None, registryName, data, item.stackSize )
              tableQuery
                .filter( _.data === data )
                .foreach( NameMapDAO.insert )

    } } }


  def take( item: ItemStack, size: Int ): Unit =
    db.withSession { implicit s =>
      tableQuery.filter( _.data === item2Binary( item ) ).foreach { record =>
        val newSize = record.size minus size
        val query = tableQuery.filter( _.slot === record.slot )

        if ( newSize == UnsignedLong.ZERO )
          {
            query.delete
            NameMapDAO.cascadeDelete( record )
          }
        else
          query.update( record.copy( size = newSize ) )

    } }


  def insert( inventoryItem: InventoryItem ): Unit =
    db.withSession { implicit s =>
      tableQuery += inventoryItem
      tableQuery
        .filter( _.data === inventoryItem.data )
        .foreach( NameMapDAO.insert )
    }

  def delete( inventoryItem: InventoryItem ): Unit =
    db.withSession { implicit s =>
      tableQuery.filter( _.slot === inventoryItem.slot ).delete
      NameMapDAO.cascadeDelete( inventoryItem )
    }

  def update( inventoryItem: InventoryItem ): Unit =
    db.withSession { implicit s => tableQuery.filter( _.slot === inventoryItem.slot ).update( inventoryItem ) }

}
