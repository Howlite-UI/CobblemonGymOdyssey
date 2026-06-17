package com.howlite.client.screen

import com.howlite.CobblemonGymOdyssey
import com.howlite.wallet.ClientWalletCache
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.resources.ResourceLocation

/**
 * HUD de balance affiché en coin d'écran quand [ClientWalletCache.hudEnabled] est true.
 *
 * Affichage : Le count total en copper dans un cadre de fond minimaliste (wallet_background_ingame.png)
 * positionné sur le bord droit de l'écran.
 */
object WalletHudOverlay {

    private val BACKGROUND: ResourceLocation = ResourceLocation.fromNamespaceAndPath(
        CobblemonGymOdyssey.MOD_ID, "textures/gui/coin/wallet_background_ingame.png"
    )

    private const val MARGIN = 4
    private const val WIDTH = 60
    private const val HEIGHT = 14

    /**
     * Appelé chaque frame de rendu HUD.
     */
    fun render(graphics: GuiGraphics, partialTick: Float) {
        val mc = Minecraft.getInstance()
        ClientWalletCache.checkSync(mc)

        if (!ClientWalletCache.hudEnabled) return
        if (mc.options.hideGui) return

        val window = mc.window
        val screenW = window.guiScaledWidth
        val screenH = window.guiScaledHeight

        // Position : coin inférieur droit
        val startX = screenW - WIDTH - MARGIN
        val startY = screenH - HEIGHT - MARGIN

        // Rendu de la texture de fond
        graphics.blit(BACKGROUND, startX, startY, 0f, 0f, WIDTH, HEIGHT, WIDTH, HEIGHT)

        val font = mc.font
        val totalText = WalletOverlay.formatCompact(ClientWalletCache.balance)
        val textW = font.width(totalText)

        // Centrer la valeur dans la zone gauche de la texture (les 44 premiers pixels de largeur)
        val textX = startX + 2 + (41 - textW) / 2
        val textY = startY + (HEIGHT - 8) / 2

        graphics.drawString(font, totalText, textX, textY, 0xFFFFFF, true)
    }
}
