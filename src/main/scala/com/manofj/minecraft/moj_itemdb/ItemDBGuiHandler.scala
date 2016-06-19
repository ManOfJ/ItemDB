package com.manofj.minecraft.moj_itemdb

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.World

import net.minecraftforge.fml.client.FMLClientHandler
import net.minecraftforge.fml.common.FMLCommonHandler
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent
import net.minecraftforge.fml.common.network.IGuiHandler

import com.manofj.minecraft.moj_commons.util.ImplicitConversions.AnyExtension

import com.manofj.minecraft.moj_itemdb.gui.ItemDBInventoryGui
import com.manofj.minecraft.moj_itemdb.inventory.{ ItemDBInventory, ItemDBInventoryContainer }


object ItemDBGuiHandler
  extends IGuiHandler
{

  private[ this ] final val GUI_INVENTORY = 0


  @SubscribeEvent
  def keyInput( event: KeyInputEvent ): Unit =
    if ( ItemDBConfigHandler.guiKey.isKeyDown )
      FMLClientHandler.instance.getClientPlayerEntity.?
        .foreach { playerSp =>
          FMLCommonHandler.instance.getMinecraftServerInstance.?
            .flatMap( _.getPlayerList.? )
            .flatMap( _.getPlayerByUUID( playerSp.getUniqueID ).? )
            .foreach { playerMp =>
              playerMp.openGui( ItemDB, GUI_INVENTORY, playerMp.worldObj, 0, 0, 0 )

      } }


  override def getClientGuiElement( ID: Int, player: EntityPlayer, world: World, x: Int, y: Int, z: Int ): AnyRef =
    ID match {
      case GUI_INVENTORY => new ItemDBInventoryGui( player.inventory, new ItemDBInventory )
    }

  override def getServerGuiElement( ID: Int, player: EntityPlayer, world: World, x: Int, y: Int, z: Int ): AnyRef =
    ID match {
      case GUI_INVENTORY => new ItemDBInventoryContainer( player.inventory, new ItemDBInventory )
    }

}
