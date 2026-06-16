package com.howlite.client.screen

import com.howlite.CobblemonGymOdyssey
import com.howlite.items.CobbleCoins
import com.howlite.wallet.ClientWalletCache
import com.howlite.wallet.WalletNetwork
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack

/**
 * Overlay wallet affiché par-dessus l'inventaire du joueur.
 *
 * Activé/désactivé via [InventoryWalletButton]. N'est pas un écran séparé —
 * il est rendu directement dans le render() de l'inventaire via mixin/event.
 *
 * Layout de l'overlay (inventory_wallet_background.png) :
 * ┌──────────────────────────────────┐
 * │  [🟤] 42   [⚪] 7   [🟡] 3   [💎]0 │  ← 4 slots item (visuel uniquement)
 * │                                  │
 * │  Total: 72 342 CCC               │  ← valeur totale formatée
 * │                                  │
 * │  [Auto]          [HUD]           │  ← 2 switches
 * └──────────────────────────────────┘
 *
 * Dimensions de l'overlay : calculées à partir de la texture background.
 */
object WalletOverlay {

    var isOpen: Boolean = false

    private val BACKGROUND: ResourceLocation = ResourceLocation.fromNamespaceAndPath(
        CobblemonGymOdyssey.MOD_ID, "textures/gui/coin/inventory_wallet_background.png"
    )
    private val SWITCH_BTN: ResourceLocation = ResourceLocation.fromNamespaceAndPath(
        CobblemonGymOdyssey.MOD_ID, "textures/gui/coin/inventory_wallet_switch_button.png"
    )

    // Dimensions de l'overlay (adapter selon la taille réelle de inventory_wallet_background.png)
    const val OVERLAY_W = 120
    const val OVERLAY_H = 72

    // Taille des slots visuels de pièces
    private const val SLOT_SIZE = 16
    private const val SLOT_PADDING = 4

    // Taille des boutons switch
    private const val BTN_W = 46
    private const val BTN_H = 14

    /**
     * Appelé par le mixin/event après le rendu du fond de l'inventaire.
     * [guiLeft] / [guiTop] sont les coordonnées de l'inventaire dans l'écran.
     */
    fun render(graphics: GuiGraphics, guiLeft: Int, guiTop: Int, mouseX: Int, mouseY: Int) {
        if (!isOpen) return

        // Positionnement : au-dessus et à droite du bouton wallet
        // (positionné par rapport à l'inventaire, ajuster si nécessaire)
        val ox = guiLeft + 176 + 4   // à droite du panneau inventaire standard (176px)
        val oy = guiTop + 4

        // Fond
        graphics.blit(BACKGROUND, ox, oy, 0f, 0f, OVERLAY_W, OVERLAY_H, OVERLAY_W, OVERLAY_H)

        val mc = Minecraft.getInstance()
        val font = mc.font

        // ── 4 slots visuels avec items ──
        val coins = listOf(
            CobbleCoins.COBBLE_COPPER_COIN.get()   to ClientWalletCache.copper,
            CobbleCoins.COBBLE_SILVER_COIN.get()   to ClientWalletCache.silver,
            CobbleCoins.COBBLE_GOLD_COIN.get()     to ClientWalletCache.gold,
            CobbleCoins.COBBLE_PLATINUM_COIN.get() to ClientWalletCache.platinum
        )

        val slotStartX = ox + 6
        val slotY = oy + 6

        coins.forEachIndexed { index, (item, count) ->
            val slotX = slotStartX + index * (SLOT_SIZE + SLOT_PADDING)
            if (count > 0L) {
                val stack = ItemStack(item, count.coerceAtMost(64L).toInt())
                graphics.renderItem(stack, slotX, slotY)
                // Affiche le compte en dessous si > 99
                if (count > 99) {
                    graphics.drawString(font, "${count}", slotX, slotY + SLOT_SIZE + 1, 0xFFFFFF, true)
                }
            }
        }

        // ── Total en texte ──
        val totalText = "Total: ${formatBalance(ClientWalletCache.balance)}"
        graphics.drawString(font, totalText, ox + 6, oy + SLOT_SIZE + 14, 0xFFFFFF, true)

        // ── Bouton Switch 1 : Auto-Collect ──
        val btn1X = ox + 6
        val btn1Y = oy + OVERLAY_H - BTN_H - 6
        val autoOn = ClientWalletCache.autoCollect
        drawSwitch(graphics, font, btn1X, btn1Y, "Auto", autoOn)

        // ── Bouton Switch 2 : HUD ──
        val btn2X = ox + OVERLAY_W - BTN_W - 6
        val hudOn = ClientWalletCache.hudEnabled
        drawSwitch(graphics, font, btn2X, btn1Y, "HUD", hudOn)
    }

    /**
     * Gère les clics sur l'overlay. Retourne true si le clic a été consommé.
     */
    fun mouseClicked(guiLeft: Int, guiTop: Int, mouseX: Double, mouseY: Double): Boolean {
        if (!isOpen) return false

        val ox = guiLeft + 176 + 4
        val oy = guiTop + 4

        val btn1X = ox + 6
        val btn1Y = oy + OVERLAY_H - BTN_H - 6
        val btn2X = ox + OVERLAY_W - BTN_W - 6

        // Switch 1 : Auto-Collect
        if (mouseX >= btn1X && mouseX <= btn1X + BTN_W && mouseY >= btn1Y && mouseY <= btn1Y + BTN_H) {
            val newVal = !ClientWalletCache.autoCollect
            ClientWalletCache.autoCollect = newVal
            WalletNetwork.sendToggle(0, newVal)
            return true
        }

        // Switch 2 : HUD
        if (mouseX >= btn2X && mouseX <= btn2X + BTN_W && mouseY >= btn1Y && mouseY <= btn1Y + BTN_H) {
            val newVal = !ClientWalletCache.hudEnabled
            ClientWalletCache.hudEnabled = newVal
            WalletNetwork.sendToggle(1, newVal)
            return true
        }

        return false
    }

    private fun drawSwitch(graphics: GuiGraphics, font: net.minecraft.client.gui.Font,
                           x: Int, y: Int, label: String, enabled: Boolean) {
        // Teinte verte si actif, grise si inactif
        val color = if (enabled) 0xFF55FF55.toInt() else 0xFFAAAAAA.toInt()
        graphics.blit(SWITCH_BTN, x, y, 0f, 0f, BTN_W, BTN_H, BTN_W, BTN_H)
        graphics.drawCenteredString(font, label, x + BTN_W / 2, y + (BTN_H - 8) / 2, color)
    }

    private fun formatBalance(ccc: Long): String {
        if (ccc == 0L) return "0 CCC"
        val p = ccc / 1_000_000L
        val g = (ccc % 1_000_000L) / 10_000L
        val s = (ccc % 10_000L) / 100L
        val c = ccc % 100L
        return buildString {
            if (p > 0) append("${p} CPC ")
            if (g > 0) append("${g} CGC ")
            if (s > 0) append("${s} CSC ")
            if (c > 0) append("${c} CCC")
        }.trim()
    }
}
