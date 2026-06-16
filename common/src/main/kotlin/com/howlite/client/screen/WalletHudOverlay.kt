package com.howlite.client.screen

import com.howlite.items.CobbleCoins
import com.howlite.wallet.ClientWalletCache
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.item.ItemStack

/**
 * HUD de balance affiché en coin d'écran quand [ClientWalletCache.hudEnabled] est true.
 *
 * Affichage : 4 slots visuels d'items dans le coin inférieur gauche.
 * Les slots vides (valeur 0) ne sont pas affichés.
 *
 * Enregistré dans :
 *  - Fabric : via Architectury [dev.architectury.event.events.client.ClientGuiEvent.RENDER_HUD]
 *  - NeoForge : via [net.neoforged.neoforge.client.event.RenderGuiLayerEvent]
 */
object WalletHudOverlay {

    private const val SLOT_SIZE = 16
    private const val PADDING = 2
    private const val MARGIN = 4

    /**
     * Appelé chaque frame de rendu HUD.
     * [partialTick] disponible pour animations futures.
     */
    fun render(graphics: GuiGraphics, partialTick: Float) {
        if (!ClientWalletCache.hudEnabled) return

        val mc = Minecraft.getInstance()
        if (mc.options.hideGui) return

        val window = mc.window
        val screenH = window.guiScaledHeight

        // Coins à afficher (uniquement si > 0)
        val visible = buildList {
            if (ClientWalletCache.copper > 0)   add(CobbleCoins.COBBLE_COPPER_COIN.get()   to ClientWalletCache.copper)
            if (ClientWalletCache.silver > 0)   add(CobbleCoins.COBBLE_SILVER_COIN.get()   to ClientWalletCache.silver)
            if (ClientWalletCache.gold > 0)     add(CobbleCoins.COBBLE_GOLD_COIN.get()     to ClientWalletCache.gold)
            if (ClientWalletCache.platinum > 0) add(CobbleCoins.COBBLE_PLATINUM_COIN.get() to ClientWalletCache.platinum)
        }

        if (visible.isEmpty()) return

        // Position : coin inférieur gauche
        val startX = MARGIN
        val startY = screenH - SLOT_SIZE - MARGIN - 10  // -10 pour laisser de la place au texte

        val font = mc.font

        visible.forEachIndexed { index, (item, count) ->
            val x = startX + index * (SLOT_SIZE + PADDING)
            val y = startY

            // Fond semi-transparent
            graphics.fill(x - 1, y - 1, x + SLOT_SIZE + 1, y + SLOT_SIZE + 1, 0x88000000.toInt())

            // Rendu de l'item
            val stack = ItemStack(item, count.coerceAtMost(64L).toInt())
            graphics.renderItem(stack, x, y)

            // Compte si > 1
            if (count > 1) {
                val countStr = if (count >= 1000) "${count / 1000}k" else count.toString()
                graphics.drawString(font, countStr, x + SLOT_SIZE - font.width(countStr), y + SLOT_SIZE - 7, 0xFFFFFF, true)
            }
        }
    }
}
