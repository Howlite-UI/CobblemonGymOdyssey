package com.howlite.client.screen

import com.howlite.CobblemonGymOdyssey
import com.howlite.wallet.ClientWalletCache
import com.howlite.wallet.WalletNetwork
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

/**
 * Bouton wallet affiché dans l'inventaire du joueur.
 *
 * Positionné entre le slot résultat du craft (2×2) et l'inventaire,
 * sur la droite. Cliqué → toggle [WalletOverlay.isOpen].
 *
 * Textures :
 *  - Normale  : cobblemongymodyssey:textures/gui/coin/inventory_wallet_ico.png
 *  - Hover    : cobblemongymodyssey:textures/gui/coin/inventory_wallet_ico_hover.png
 */
class InventoryWalletButton(
    x: Int, y: Int, width: Int, height: Int
) : AbstractWidget(x, y, width, height, Component.empty()) {

    companion object {
        val BUTTON_TEX: ResourceLocation = ResourceLocation.fromNamespaceAndPath(
            CobblemonGymOdyssey.MOD_ID, "textures/gui/coin/inventory_wallet_button.png"
        )
    }

    override fun renderWidget(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val textureV = if (this.isHovered || WalletOverlay.isOpen) 20f else 0f
        graphics.blit(BUTTON_TEX, x, y, 0f, textureV, width, height, width, height * 2)
    }

    override fun onClick(mouseX: Double, mouseY: Double) {
        WalletOverlay.isOpen = !WalletOverlay.isOpen
    }

    override fun updateWidgetNarration(output: NarrationElementOutput) {
        output.add(
            net.minecraft.client.gui.narration.NarratedElementType.TITLE,
            Component.translatable("gui.cobblemongymodyssey.wallet.title")
        )
    }
}
