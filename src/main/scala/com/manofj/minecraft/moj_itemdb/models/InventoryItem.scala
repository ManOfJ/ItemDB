package com.manofj.minecraft.moj_itemdb.models

import com.google.common.primitives.UnsignedLong

import com.manofj.minecraft.moj_itemdb.dao.types.Binary


case class InventoryItem( slot: Option[ UnsignedLong ], name: String, data: Binary, size: UnsignedLong )
