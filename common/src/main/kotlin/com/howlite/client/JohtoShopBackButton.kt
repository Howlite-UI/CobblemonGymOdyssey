package com.howlite.client

import net.minecraft.client.gui.components.AbstractButton
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

class JohtoShopBackButton(
    x: Int,
    y: Int,
    private val onPress: Runnable
) : AbstractButton(x, y, 26, 13, Component.empty()) {

    private val TEXTURE = ResourceLocation.fromNamespaceAndPath(
        "cobblemongymodyssey",
        "textures/gui/back_button.png"
    )
    private val ICON_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        "cobblemongymodyssey",
        "textures/gui/back_button_icon.png"
    )

    override fun onPress() {
        onPress.run()
    }

    override fun updateWidgetNarration(narrationElementOutput: NarrationElementOutput) {
    }

    override fun renderWidget(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val isHovered = isHoveredOrFocused
        val vOffset = if (isHovered) 13f else 0f
        
        // Draw the tab button background (26x13)
        graphics.blit(TEXTURE, getX(), getY(), 0f, vOffset, 26, 13, 26, 26)
        
        // Draw the icon centered inside the tab
        // Tab is 26 wide, 13 high. Icon is 21 wide, 11 high.
        graphics.blit(ICON_TEXTURE, getX() + 2, getY() + 1, 0f, 0f, 21, 11, 21, 11)
    }
}
