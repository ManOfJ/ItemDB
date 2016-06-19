package com.manofj.minecraft.moj_itemdb.gui

import org.lwjgl.input.Keyboard

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.gui.{ FontRenderer, GuiButton, GuiTextField }
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.resources.I18n
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.util.ResourceLocation

import com.manofj.minecraft.moj_commons.collection.java.alias.JavaList
import com.manofj.minecraft.moj_commons.util.ImplicitConversions.AnyExtension

import com.manofj.minecraft.moj_itemdb.inventory.{ ItemDBInventory, ItemDBInventoryContainer }
import com.manofj.minecraft.moj_itemdb.network.ItemDBMessage
import com.manofj.minecraft.moj_itemdb.{ ItemDB, ItemDBConfigHandler }


class ItemDBInventoryGui( playerInventory: InventoryPlayer, dbInventory: ItemDBInventory )
  extends GuiContainer( new ItemDBInventoryContainer( playerInventory, dbInventory ) )
{

  private[ this ] final val FRAME_TEXTURE = new ResourceLocation( "moj_itemdb:textures/gui/container/inventory_gui.png" )
  private[ this ] final val FRAME_WIDTH   = 248
  private[ this ] final val FRAME_HEIGHT  = 228


  private[ this ] var searchword: GuiTextField = null
  private[ this ] var previousSearchword: String = ""

  this.xSize = FRAME_WIDTH
  this.ySize = FRAME_HEIGHT

  var xCenter = xSize / 2
  var yCenter = ySize / 2

  override def initGui(): Unit = {
    super.initGui()

    xCenter = xSize / 2
    yCenter = ySize / 2

    Keyboard.enableRepeatEvents( true )

    searchword = new GuiTextField( 0, fontRendererObj, guiLeft + 10, guiTop + 35 - fontRendererObj.FONT_HEIGHT, 144, fontRendererObj.FONT_HEIGHT )
    searchword.setText( "" )
    searchword.setMaxStringLength( 24 )
    searchword.setEnableBackgroundDrawing( false )
    searchword.setFocused( false )
    searchword.setVisible( true )

    buttonList.add( new GuiButton( 101, guiLeft + 156, guiTop + 24, 12, 12, "x" ) {
      override def drawButton( mc: Minecraft, mouseX: Int, mouseY: Int ): Unit = {
        if ( this.visible ) {
          this.hovered = mouseX >= this.xPosition &&
                         mouseY >= this.yPosition &&
                         mouseX < this.xPosition + this.width &&
                         mouseY < this.yPosition + this.height

          this.mouseDragged( mc, mouseX, mouseY )

          if ( ItemDBInventoryGui.this.searchword.getText.? exists( !_.isEmpty ) )
            this.drawCenteredString( mc.fontRendererObj, this.displayString,
              this.xPosition + this.width / 2,
              this.yPosition + ( this.height - 10 ) / 2,
              {
                if ( this.packedFGColour != 0 ) this.packedFGColour
                else this.getHoverState( this.hovered ) match {
                  case 0 => 0xa0a0a0
                  case 1 => 0xe0e0e0
                  case 2 => 0xff0033
                }
              } )
        }
      }
    } )

  }

  override def onGuiClosed(): Unit = {
    super.onGuiClosed()
    Keyboard.enableRepeatEvents( false )
  }

  override def updateScreen(): Unit = {
    super.updateScreen()

    searchword.getText match {
      case text if text != previousSearchword =>
        previousSearchword = text
        dbInventory.searchAndUpdateInventory( text )
        ItemDB.network.sendToServer( new ItemDBMessage.SearchwordUpdate( text ) )

      case _ =>
    }

  }

  override def drawGuiContainerForegroundLayer( mouseX: Int, mouseY: Int ): Unit = {
    val title = I18n.format( "moj_itemdb.gui.inventory.title" )
    fontRendererObj.drawString( title, xCenter - fontRendererObj.getStringWidth( title ) / 2, 7 + fontRendererObj.FONT_HEIGHT / 2, 4210752 )
  }

  override def drawGuiContainerBackgroundLayer( partialTicks: Float, mouseX: Int, mouseY: Int ): Unit = {
    GlStateManager.color( 1F, 1F, 1F, 1F )
    mc.getTextureManager.bindTexture( FRAME_TEXTURE )
    drawTexturedModalRect( guiLeft, guiTop, 0, 0, xSize, ySize )

    searchword.drawTextBox()
  }

  override def drawHoveringText( textLines: JavaList[ String ], x: Int, y: Int, font: FontRenderer ): Unit = {
    getSlotUnderMouse.?
      .filter( _.inventory eq dbInventory )
      .foreach { x => dbInventory.addHoveringText( textLines, x.slotNumber ) }

    super.drawHoveringText( textLines, x, y, font )
  }

  override def actionPerformed( button: GuiButton ): Unit = {
    button.id match {
      case 101 => searchword.setText( "" )
    }
  }

  override def mouseClicked( mouseX: Int, mouseY: Int, mouseButton: Int ): Unit = {
    super.mouseClicked( mouseX, mouseY, mouseButton )
    searchword.mouseClicked( mouseX, mouseY, mouseButton )
  }

  override def keyTyped( typedChar: Char, keyCode: Int ): Unit =
    keyCode match {
      case 1 => mc.thePlayer.closeScreen()
      case any =>
        if ( Keyboard.isKeyDown( Keyboard.KEY_RETURN ) )
          searchword.setFocused( !searchword.isFocused )

        else if ( searchword.isFocused )
          searchword.textboxKeyTyped( typedChar, keyCode )

        else if ( mc.gameSettings.keyBindInventory.isActiveAndMatches( keyCode ) ||
                  ItemDBConfigHandler.guiKey.isActiveAndMatches( keyCode ) )
          mc.thePlayer.closeScreen()

        else
          super.keyTyped( typedChar, keyCode )

    }

}
