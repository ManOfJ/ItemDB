package com.manofj.minecraft.moj_itemdb

import net.minecraftforge.fml.client.FMLClientHandler
import net.minecraftforge.fml.common.FMLCommonHandler
import net.minecraftforge.fml.common.network.simpleimpl.{IMessage, IMessageHandler, MessageContext}

import com.manofj.minecraft.moj_commons.util.ImplicitConversions.AnyExtension

import com.manofj.minecraft.moj_itemdb.inventory.ItemDBInventoryContainer
import com.manofj.minecraft.moj_itemdb.network.ItemDBMessage.alias._


object ItemDBMessageHandler {

  object alias {

    val SearchwordUpdateHandler = SearchwordUpdate
    val DatabaseChangedHandler  = DatabaseChanged

  }

  object SearchwordUpdate
    extends IMessageHandler[ SearchwordUpdateMessage, IMessage ]
  {
    override def onMessage( message: SearchwordUpdateMessage, ctx: MessageContext ): IMessage = {
      ctx.getServerHandler.playerEntity.?.foreach {
        _.openContainer match {
          case dbContainer: ItemDBInventoryContainer =>
            dbContainer.dbInventory.searchAndUpdateInventory( message.word )
          case _ =>
      } }
      null
    }
  }

  object DatabaseChanged
    extends IMessageHandler[ DatabaseChangedMessage, IMessage ]
  {
    override def onMessage( message: DatabaseChangedMessage, ctx: MessageContext ): IMessage = {
      FMLClientHandler.instance.getClientPlayerEntity.?.foreach {
        _.openContainer match {
          case dbContainer: ItemDBInventoryContainer =>
            dbContainer.dbInventory.refreshInventory()
          case _ =>
      } }
      null
    }
  }

}
