package com.manofj.minecraft.moj_itemdb.network

import com.google.common.base.Charsets
import io.netty.buffer.ByteBuf

import net.minecraftforge.fml.common.network.simpleimpl.IMessage


object ItemDBMessage {

  object alias {

    type SearchwordUpdateMessage = SearchwordUpdate
    type DatabaseChangedMessage  = DatabaseChanged
  }

  class SearchwordUpdate()
    extends IMessage
  {
    var word: String = ""

    def this( word: String ) = {
      this()
      this.word = word
    }

    override def fromBytes( buf: ByteBuf ): Unit = { word = buf.toString( Charsets.UTF_8 ) }
    override def toBytes( buf: ByteBuf ): Unit = { buf.writeBytes( word.getBytes( Charsets.UTF_8 ) ) }
  }

  class DatabaseChanged()
    extends IMessage
  {
    override def fromBytes( buf: ByteBuf ): Unit = {}
    override def toBytes( buf: ByteBuf ): Unit = {}
  }

}
