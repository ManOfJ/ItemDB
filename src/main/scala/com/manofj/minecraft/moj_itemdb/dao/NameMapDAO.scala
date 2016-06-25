package com.manofj.minecraft.moj_itemdb.dao

import scala.slick.jdbc.StaticQuery
import scala.slick.lifted.ProvenShape

import com.google.common.primitives.UnsignedLong

import net.minecraft.item.ItemStack

import com.manofj.minecraft.moj_itemdb.ItemDB
import com.manofj.minecraft.moj_itemdb.models.{DisplayName, InventoryItem}


object NameMapDAO {
  import com.manofj.minecraft.moj_itemdb.dao.H2DatabaseDriver._
  import com.manofj.minecraft.moj_itemdb.dao.mappers.unsignedLongMapper
  import com.manofj.minecraft.moj_itemdb.util.ImplicitConversions.ItemStack.binary2Item


  class NameMapTable( tag: Tag )
    extends Table[ DisplayName ]( tag, "NAME_MAP" )
  {
    def slot = column[ UnsignedLong ]( "SLOT", O.PrimaryKey )
    def text = column[ String ]( "TEXT", O.NotNull )

    override def * : ProvenShape[ DisplayName ] =
      ( slot, text ) <> ( DisplayName.tupled, DisplayName.unapply )

  }


  type NameMapQuery = Query[ NameMapTable, DisplayName, Seq ]
  type NameMapQueryChain = NameMapQuery => NameMapQuery


  private[ this ] final lazy val db = Database.forConfig( "moj_itemdb.namemap" )


  private[ moj_itemdb ] val tableQuery = TableQuery[ NameMapTable ]


  private[ dao ] def insert( invItem: InventoryItem ): Unit =
    db.withSession { implicit s => tableQuery += DisplayName( invItem.slot.get, invItem.data.getDisplayName ) }

  private[ dao ] def cascadeDelete( invItem: InventoryItem ): Unit =
    db.withSession { implicit s => tableQuery.filter( _.slot === invItem.slot ).delete }


  private[ moj_itemdb ] def sortedQuery: NameMapQuery = tableQuery.sortBy( _.text )

  private[ moj_itemdb ] def seq( query: NameMapQuery ): Seq[ DisplayName ] =
    db.withSession { implicit s => query.list }

  private[ moj_itemdb ] def seq( queryChain: NameMapQueryChain ): Seq[ DisplayName ] =
    seq( queryChain( tableQuery ) )

  private[ moj_itemdb ] def firstOption( query: NameMapQuery ): Option[ DisplayName ] =
    db.withSession { implicit s => query.firstOption }

  private[ moj_itemdb ] def firstOption( queryChain: NameMapQueryChain ): Option[ DisplayName ] =
    firstOption( queryChain( tableQuery ) )

  private[ moj_itemdb ] def foreach( query: NameMapQuery )
                                   ( function: DisplayName => Unit ): Unit =
    db.withSession { implicit s => query.foreach( function ) }

  private[ moj_itemdb ] def foreach( queryChain: NameMapQueryChain )
                                   ( function: DisplayName => Unit ): Unit =
    foreach( queryChain( tableQuery ) )( function )

  private[ moj_itemdb ] def count( query: NameMapQuery ): Int =
    db.withSession { implicit s => query.length.run }

  private[ moj_itemdb ] def count( queryChain: NameMapQueryChain ): Int =
    count( queryChain( tableQuery ) )


  private[ moj_itemdb ] def printAll(): Unit =
    db.withSession{ implicit s =>
      val slots = tableQuery.sortBy( _.slot ).map( _.slot ).run
      InventoryDAO.foreach( _.filter( _.slot inSet slots ) ) { record =>
        ItemDB.log.info( s"= Slot ${ record.slot.get }. ${ record.data.getDisplayName } = ${ record.size }" )
    } }

  private[ moj_itemdb ] def initialize(): Unit = {
    db.withSession { implicit s =>
      StaticQuery.updateNA( "RUNSCRIPT FROM 'classpath:sql/init_namemap.sql'" ).execute

      InventoryDAO.foreach( x => x ) { record =>
        val item = record.data: ItemStack
        if ( item ne null )
          tableQuery += DisplayName( record.slot.get, item.getDisplayName )
      }

      ItemDB.log.debug( "== Stored items in ItemDB ================================" )
      tableQuery.sortBy( _.slot ).foreach { record =>
        InventoryDAO.foreach( _.filter( _.slot === record.slot ) ) { record =>
          ItemDB.log.debug( s"= Slot ${ record.slot.get }. ${ record.data.getDisplayName } = ${ record.size }" )
      } }
      ItemDB.log.debug( "================================================================" )
    }
  }


  def insert( displayName: DisplayName ): Unit =
    db.withSession { implicit s => tableQuery += displayName }

  def delete( displayName: DisplayName ): Unit =
    db.withSession { implicit s => tableQuery.filter( _.slot === displayName.slot ).delete }

  def update( displayName: DisplayName ): Unit =
    db.withSession { implicit s => tableQuery.filter( _.slot === displayName.slot ).update( displayName ) }

}
