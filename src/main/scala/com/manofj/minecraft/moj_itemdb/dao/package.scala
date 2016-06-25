package com.manofj.minecraft.moj_itemdb

import java.sql.Blob
import javax.sql.rowset.serial.SerialBlob

import com.google.common.primitives.UnsignedLong
import org.apache.commons.io.IOUtils


package object dao {

  // DAO のクエリ作成に必要なドライバ
  final val H2DatabaseDriver = slick.driver.H2Driver.simple
  import H2DatabaseDriver._


  // このパッケージに属する DAO オブジェクトのエイリアス
  object alias {

    final val Inventory = InventoryDAO
    final val NameMap   = NameMapDAO

  }


  // このパッケージに属する DAO オブジェクトが使用する型エイリアス
  object types {

    type Binary = scala.collection.mutable.WrappedArray[ Byte ]

  }


  // DAO のカラムを変換するマッパーを定義する
  private[ moj_itemdb ] object mappers {
    import com.manofj.minecraft.moj_itemdb.dao.types.Binary


    implicit val unsignedLongMapper =
      MappedColumnType.base[ UnsignedLong, Long ](
        { x => x.longValue },
        { x => UnsignedLong.fromLongBits( x ) }
      )

    implicit val binaryMapper =
      MappedColumnType.base[ Binary, Blob ](
        { x => new SerialBlob( x.array ) },
        { x => IOUtils.toByteArray( x.getBinaryStream ) }
      )

  }

}
