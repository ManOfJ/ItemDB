package com.manofj.minecraft.moj_itemdb

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }

import org.apache.logging.log4j.{ LogManager, Logger }

import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.client.registry.ClientRegistry
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event._
import net.minecraftforge.fml.common.network.NetworkRegistry
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.relauncher.Side

import com.manofj.minecraft.moj_itemdb.ItemDBMessageHandler.alias._
import com.manofj.minecraft.moj_itemdb.network.ItemDBMessage.alias._


@Mod( modid       = ItemDB.ID,
      name        = ItemDB.NAME,
      version     = ItemDB.VERSION,
      guiFactory  = "com.manofj.minecraft.moj_itemdb.ItemDBConfigGuiFactory",
      modLanguage = "scala" )
object ItemDB {
  import com.manofj.minecraft.moj_itemdb.dao.alias.Inventory
  import com.manofj.minecraft.moj_itemdb.dao.alias.NameMap


  final val ID      = "@modid@"
  final val NAME    = "ItemDB"
  final val VERSION = "@version@"


  private[ this ] var dbRestore: Future[ Unit ] = null
  private[ this ] var dbInit: Future[ Unit ] = null


  private[ moj_itemdb ] lazy val network = NetworkRegistry.INSTANCE.newSimpleChannel( ID )


  lazy val log: Logger = LogManager.getLogger( ID )


  @Mod.EventHandler
  def preInit( event: FMLPreInitializationEvent ): Unit = {
    import com.manofj.minecraft.moj_commons.config.javaFile2ForgeConfig

    MinecraftForge.EVENT_BUS.register( ItemDBConfigHandler )
    MinecraftForge.EVENT_BUS.register( ItemDBEventHandler )
    MinecraftForge.EVENT_BUS.register( ItemDBGuiHandler )

    NetworkRegistry.INSTANCE.registerGuiHandler( this, ItemDBGuiHandler )
    network.registerMessage[ SearchwordUpdateMessage, IMessage ]( SearchwordUpdateHandler, classOf[ SearchwordUpdateMessage ], 0, Side.SERVER )
    network.registerMessage[ DatabaseChangedMessage, IMessage ]( DatabaseChangedHandler, classOf[ DatabaseChangedMessage ], 1, Side.CLIENT )

    ItemDBConfigHandler.captureConfig( event.getSuggestedConfigurationFile )

    ClientRegistry.registerKeyBinding( ItemDBConfigHandler.guiKey )

    dbRestore = Future { ItemDBConfigHandler.dbFilePath.foreach( Inventory.restoreFromFile ) }
  }

  @Mod.EventHandler
  def postInit( event: FMLPostInitializationEvent ): Unit = {
    Await.ready( dbRestore, Duration.Inf )
    dbInit = Future { NameMap.initialize() }
  }

  @Mod.EventHandler
  def serverAboutToStart( event: FMLServerAboutToStartEvent ): Unit = {
    Await.ready( dbInit, Duration.Inf )
  }

  @Mod.EventHandler
  def serverStopping( event: FMLServerStoppingEvent ): Unit = {
    ItemDBConfigHandler.dbFilePath.foreach( Inventory.dumpToFile )
  }

}
