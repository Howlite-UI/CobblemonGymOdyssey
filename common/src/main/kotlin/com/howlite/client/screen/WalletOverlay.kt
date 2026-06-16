package com.howlite.client.screen

import com.howlite.CobblemonGymOdyssey
import com.howlite.items.CobbleCoins
import com.howlite.wallet.ClientWalletCache
import com.howlite.wallet.CoinType
import com.howlite.wallet.WalletNetwork
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack

/**
 * Overlay wallet affiché par-dessus l'inventaire du joueur.
 */
object WalletOverlay {

    var isOpen: Boolean = false

    private val BACKGROUND: ResourceLocation = ResourceLocation.fromNamespaceAndPath(
        CobblemonGymOdyssey.MOD_ID, "textures/gui/coin/inventory_wallet.png"
    )
    private val SWITCH_BTN: ResourceLocation = ResourceLocation.fromNamespaceAndPath(
        CobblemonGymOdyssey.MOD_ID, "textures/gui/coin/inventory_wallet_switch_button.png"
    )

    const val OVERLAY_W = 103
    const val OVERLAY_H = 44

    private const val SLOT_SIZE = 16

    // Dimensions des switches (14x7 pour chaque état)
    private const val BTN_W = 14
    private const val BTN_H = 7

    /**
     * Formate un nombre de pièces en limitant à 7 caractères max avec des suffixes M / B.
     */
    fun formatCompact(value: Long): String {
        if (value < 1_000_000L) {
            return value.toString()
        }
        if (value < 1_000_000_000L) {
            val millions = value / 1_000_000L
            val tenths = (value % 1_000_000L) / 100_000L
            return if (millions < 10L && tenths > 0L) {
                "${millions}.${tenths}M"
            } else {
                "${millions}M"
            }
        }
        val billions = value / 1_000_000_000L
        val tenths = (value % 1_000_000_000L) / 100_000_000L
        return if (billions < 10L && tenths > 0L) {
            "${billions}.${tenths}B"
        } else {
            "${billions}B"
        }
    }

    /**
     * Appelé après le rendu du fond de l'inventaire.
     */
    fun render(graphics: GuiGraphics, guiLeft: Int, guiTop: Int, mouseX: Int, mouseY: Int) {
        if (!isOpen) return

        // Ajusté 1 pixel plus à gauche (était + 4)
        val ox = guiLeft + 176 + 3
        val oy = guiTop + 4

        // Fond
        graphics.blit(BACKGROUND, ox, oy, 0f, 0f, OVERLAY_W, OVERLAY_H, OVERLAY_W, OVERLAY_H)

        val mc = Minecraft.getInstance()
        val font = mc.font

        // ── 4 slots visuels avec items ──
        val coins = listOf(
            Triple(CobbleCoins.COBBLE_COPPER_COIN.get(), ClientWalletCache.copper, ClientWalletCache.balance >= CoinType.COPPER.valueCCC),
            Triple(CobbleCoins.COBBLE_SILVER_COIN.get(), ClientWalletCache.silver, ClientWalletCache.balance >= CoinType.SILVER.valueCCC),
            Triple(CobbleCoins.COBBLE_GOLD_COIN.get(), ClientWalletCache.gold, ClientWalletCache.balance >= CoinType.GOLD.valueCCC),
            Triple(CobbleCoins.COBBLE_PLATINUM_COIN.get(), ClientWalletCache.platinum, ClientWalletCache.balance >= CoinType.PLATINUM.valueCCC)
        )

        // Coordonnées X des 4 slots par rapport au coin de l'overlay : 10, 28, 46, 64
        val slotXOffsets = listOf(10, 28, 46, 64)
        val slotYOffset = 6

        coins.forEachIndexed { index, (item, count, shouldShow) ->
            if (shouldShow) {
                val slotX = ox + slotXOffsets[index]
                val slotY = oy + slotYOffset
                val displayCount = if (count > 0L) count.coerceAtMost(64L).toInt() else 1
                val stack = ItemStack(item, displayCount)
                graphics.renderItem(stack, slotX, slotY)
                
                // Affiche la quantité (si > 1 ou si == 0 pour indiquer la dispo à 0)
                if (count > 1 || count == 0L) {
                    val countStr = count.toString()
                    val textWidth = font.width(countStr)
                    val scale = 0.75f
                    
                    graphics.pose().pushPose()
                    graphics.pose().scale(scale, scale, 1.0f)
                    val scaledX = ((slotX + 16) / scale - textWidth).toInt()
                    val scaledY = ((slotY + 10) / scale).toInt()
                    graphics.drawString(font, countStr, scaledX, scaledY, 0xFFFFFF, true)
                    graphics.pose().popPose()
                }
            }
        }

        // ── Balance totale compacte dans la zone noire en bas à gauche ──
        val totalText = formatCompact(ClientWalletCache.balance)
        graphics.drawString(font, totalText, ox + 10, oy + 29, 0xFFFFFF, true)

        // ── Bouton Switch 1 : Auto-Collect (empilés verticalement à droite) ──
        val btnX = ox + 86
        val btn1Y = oy + 12
        drawSwitch(graphics, btnX, btn1Y, ClientWalletCache.autoCollect)

        // ── Bouton Switch 2 : HUD (empilés verticalement à droite) ──
        val btn2Y = oy + 26
        drawSwitch(graphics, btnX, btn2Y, ClientWalletCache.hudEnabled)
    }

    /**
     * Gère les clics sur l'overlay.
     */
    fun mouseClicked(guiLeft: Int, guiTop: Int, mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!isOpen) return false

        // Ajusté 1 pixel plus à gauche (était + 4)
        val ox = guiLeft + 176 + 3
        val oy = guiTop + 4

        // 1. Clic sur les slots de pièces (retrait/dépot)
        val slotXOffsets = listOf(10, 28, 46, 64)
        val slotYOffset = 6

        val mc = Minecraft.getInstance()
        val carried = mc.player?.containerMenu?.carried ?: ItemStack.EMPTY
        val isShift = net.minecraft.client.gui.screens.Screen.hasShiftDown()

        for (index in 0..3) {
            val slotX = ox + slotXOffsets[index]
            val slotY = oy + slotYOffset
            if (mouseX >= slotX && mouseX <= slotX + SLOT_SIZE &&
                mouseY >= slotY && mouseY <= slotY + SLOT_SIZE) {
                
                val coinItem = when (index) {
                    0 -> CobbleCoins.COBBLE_COPPER_COIN.get()
                    1 -> CobbleCoins.COBBLE_SILVER_COIN.get()
                    2 -> CobbleCoins.COBBLE_GOLD_COIN.get()
                    else -> CobbleCoins.COBBLE_PLATINUM_COIN.get()
                }

                if (!carried.isEmpty) {
                    // Si on transporte le bon type de pièce, on la dépose
                    if (carried.item == coinItem) {
                        WalletNetwork.sendWithdraw(index, button, isShift)
                    }
                } else {
                    // Si le curseur est vide, on retire les pièces de la bourse
                    val isAvailable = when (index) {
                        0 -> ClientWalletCache.balance >= CoinType.COPPER.valueCCC
                        1 -> ClientWalletCache.balance >= CoinType.SILVER.valueCCC
                        2 -> ClientWalletCache.balance >= CoinType.GOLD.valueCCC
                        else -> ClientWalletCache.balance >= CoinType.PLATINUM.valueCCC
                    }
                    if (isAvailable) {
                        WalletNetwork.sendWithdraw(index, button, isShift)
                    }
                }
                return true // Toujours consommer le clic sur les slots
            }
        }

        // 2. Clic sur les interrupteurs de basculement (empilés à droite à x = 86)
        val btnX = ox + 86
        val btn1Y = oy + 12
        val btn2Y = oy + 26

        // Switch 1 : Auto-Collect
        if (mouseX >= btnX && mouseX <= btnX + BTN_W && mouseY >= btn1Y && mouseY <= btn1Y + BTN_H) {
            val newVal = !ClientWalletCache.autoCollect
            ClientWalletCache.autoCollect = newVal
            WalletNetwork.sendToggle(0, newVal)
            return true
        }

        // Switch 2 : HUD
        if (mouseX >= btnX && mouseX <= btnX + BTN_W && mouseY >= btn2Y && mouseY <= btn2Y + BTN_H) {
            val newVal = !ClientWalletCache.hudEnabled
            ClientWalletCache.hudEnabled = newVal
            WalletNetwork.sendToggle(1, newVal)
            return true
        }

        return false
    }

    fun isHovering(guiLeft: Int, guiTop: Int, mouseX: Double, mouseY: Double): Boolean {
        if (!isOpen) return false
        val ox = guiLeft + 176 + 3
        val oy = guiTop + 4
        return mouseX >= ox && mouseX <= ox + OVERLAY_W &&
               mouseY >= oy && mouseY <= oy + OVERLAY_H
    }

    private fun drawSwitch(graphics: GuiGraphics, x: Int, y: Int, enabled: Boolean) {
        val textureV = if (enabled) BTN_H.toFloat() else 0f
        graphics.blit(SWITCH_BTN, x, y, 0f, textureV, BTN_W, BTN_H, BTN_W, BTN_H * 2)
    }
}
