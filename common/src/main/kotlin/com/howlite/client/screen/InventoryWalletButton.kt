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
        val ICO_NORMAL: ResourceLocation = ResourceLocation.fromNamespaceAndPath(
            CobblemonGymOdyssey.MOD_ID, "textures/gui/coin/inventory_wallet_ico.png"
        )
        val ICO_HOVER: ResourceLocation = ResourceLocation.fromNamespaceAndPath(
            CobblemonGymOdyssey.MOD_ID, "textures/gui/coin/inventory_wallet_ico_hover.png"
        )
    }

    override fun renderWidget(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val texture = if (isHovered) ICO_HOVER else ICO_NORMAL
        graphics.blit(texture, x, y, 0f, 0f, width, height, width, height)
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
