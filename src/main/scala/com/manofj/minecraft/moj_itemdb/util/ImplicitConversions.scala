package com.manofj.minecraft.moj_itemdb.util

import java.io.{ ByteArrayInputStream, ByteArrayOutputStream }

import scala.language.implicitConversions

import net.minecraft.nbt.{ CompressedStreamTools, NBTTagCompound }


object ImplicitConversions {

  object UnsignedLong {
    import com.google.common.primitives.{ UnsignedLong => UnsLong }


    final val INT_MAX = UnsLong.valueOf( Int.MaxValue )


    implicit def int2UnsignedLong( i: Int ): UnsLong = UnsLong.valueOf( i )
    implicit def unsignedLong2int( l: UnsLong ): Int = if ( ( l compareTo INT_MAX ) >= 0 ) Int.MaxValue else l.intValue

  }

  object ItemStack {
    import net.minecraft.item.{ ItemStack => MCItemStack }

    import com.manofj.minecraft.moj_itemdb.dao.types.Binary


    implicit class ItemStackExtension( val item: MCItemStack )
      extends AnyVal
    {

      def toBinary( implicit converter: MCItemStack => NBTTagCompound ): Binary = item2Binary( item )( converter )

    }


    implicit def item2NBT( item: MCItemStack ): NBTTagCompound = item.writeToNBT( new NBTTagCompound )

    implicit def binary2Item( binary: Binary ): MCItemStack = {
      val in = new ByteArrayInputStream( binary.array )
      MCItemStack.loadItemStackFromNBT( CompressedStreamTools.readCompressed( in ) )
    }

    implicit def item2Binary( item: MCItemStack )
                            ( implicit converter: MCItemStack => NBTTagCompound ): Binary =
    {
      val out = new ByteArrayOutputStream()
      CompressedStreamTools.writeCompressed( converter( item ), out )
      out.toByteArray
    }

  }

}
