package com.manofj.minecraft.moj_itemdb

import java.util.Locale

import com.google.common.collect.Lists
import org.lwjgl.input.Keyboard

import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.resources.I18n
import net.minecraft.client.settings.KeyBinding

import net.minecraftforge.client.settings.{ KeyConflictContext, KeyModifier }
import net.minecraftforge.common.config.Property

import com.manofj.minecraft.moj_commons.config.{ ConfigGui, ConfigGuiFactory, ConfigGuiHandler }
import com.manofj.minecraft.moj_commons.io.java.alias.JavaFile
import com.manofj.minecraft.moj_commons.util.ImplicitConversions.AnyExtension


object ItemDBConfigHandler
  extends ConfigGuiHandler
{
  override final val modId: String = ItemDB.ID


  private[ this ] var dbFilePathOpt = Option.empty[ String ]
  private[ moj_itemdb ] def dbFilePath: Option[ JavaFile ] =
    dbFilePathOpt.map( new JavaFile( _ ).getCanonicalFile )

  private[ this ] var specialKeyOpt = Option.empty[ String ]
  private[ moj_itemdb ] def specialKey: KeyModifier =
    specialKeyOpt
      .fold( KeyModifier.NONE ) {
        _.toLowerCase( Locale.ENGLISH ) match {
          case "shift"                        => KeyModifier.SHIFT
          case "alt"  | "option"              => KeyModifier.ALT
          case "ctrl" | "control" | "command" => KeyModifier.CONTROL
          case _                              => KeyModifier.NONE
      } }

  private[ moj_itemdb ] val guiKey: KeyBinding =
    new KeyBinding( "moj_itemdb.key.gui", KeyConflictContext.IN_GAME, Keyboard.KEY_COMMA, "key.categories.inventory" )


  override def title: String = I18n.format( "moj_itemdb.gui.cfg.title", ItemDB.NAME )

  override def syncConfig( load: Boolean ): Unit = {
    val cfg = config
    if ( !cfg.isChild && load ) cfg.load()

    cfg.setCategoryPropertyOrder( configId, Lists.newArrayList() )

    var prop = null: Property

    prop = cfg.get( configId, "db_filepath", "data/moj_itemdb.zip" )
    prop.setComment( I18n.format( "moj_itemdb.cfg.db_filepath.tooltip" ) )
    prop.setLanguageKey( "moj_itemdb.cfg.db_filepath" )
    prop.setRequiresMcRestart( true )
    dbFilePathOpt = prop.getString.?

    prop = cfg.get( configId, "db_special_key", "alt" )
    prop.setComment( I18n.format( "moj_itemdb.cfg.db_special_key.tooltip" ) )
    prop.setLanguageKey( "moj_itemdb.cfg.db_special_key" )
    specialKeyOpt = prop.getString.?

    if ( cfg.hasChanged ) cfg.save()
  }

}

class ItemDBConfigGui( parent: GuiScreen ) extends ConfigGui( parent, ItemDBConfigHandler )
class ItemDBConfigGuiFactory extends ConfigGuiFactory[ ItemDBConfigGui ]
