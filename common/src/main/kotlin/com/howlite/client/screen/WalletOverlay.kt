package com.howlite.client.screen

import com.howlite.CobblemonGymOdyssey
import com.howlite.items.CobbleCoins
import com.howlite.wallet.ClientWalletCache
import com.howlite.wallet.CoinType
import com.howlite.wallet.WalletNetwork
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvents
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
     * Formate le nombre affiché dans un slot spécifique (supporte K et M pour rester compact).
     */
    fun formatSlotCount(value: Long): String {
        if (value < 1000L) {
            return value.toString()
        }
        if (value < 1_000_000L) {
            val thousands = value / 1000L
            val tenths = (value % 1000L) / 100L
            return if (thousands < 10L && tenths > 0L) {
                "${thousands}.${tenths}K"
            } else {
                "${thousands}K"
            }
        }
        val millions = value / 1_000_000L
        val tenths = (value % 1_000_000L) / 100_000L
        return if (millions < 10L && tenths > 0L) {
            "${millions}.${tenths}M"
        } else {
            "${millions}M"
        }
    }

    /**
     * Appelé après le rendu du fond de l'inventaire.
     */
    fun render(graphics: GuiGraphics, guiLeft: Int, guiTop: Int, mouseX: Int, mouseY: Int) {
        val mc = Minecraft.getInstance()
        ClientWalletCache.checkSync(mc)

        if (!isOpen) return

        // Ajusté 1 pixel plus à gauche (était + 4)
        val ox = guiLeft + 176 + 3
        val oy = guiTop + 4

        // Fond
        graphics.blit(BACKGROUND, ox, oy, 0f, 0f, OVERLAY_W, OVERLAY_H, OVERLAY_W, OVERLAY_H)

        val font = mc.font

        // ── 4 slots visuels avec items ──
        val balance = ClientWalletCache.balance
        val coins = listOf(
            Triple(CobbleCoins.COBBLE_COPPER_COIN.get(), balance / CoinType.COPPER.valueCCC, balance >= CoinType.COPPER.valueCCC),
            Triple(CobbleCoins.COBBLE_SILVER_COIN.get(), balance / CoinType.SILVER.valueCCC, balance >= CoinType.SILVER.valueCCC),
            Triple(CobbleCoins.COBBLE_GOLD_COIN.get(), balance / CoinType.GOLD.valueCCC, balance >= CoinType.GOLD.valueCCC),
            Triple(CobbleCoins.COBBLE_PLATINUM_COIN.get(), balance / CoinType.PLATINUM.valueCCC, balance >= CoinType.PLATINUM.valueCCC)
        )

        // Coordonnées X des 4 slots par rapport au coin de l'overlay : 11, 29, 47, 65 (+1px à droite)
        val slotXOffsets = listOf(11, 29, 47, 65)
        val slotYOffset = 6

        coins.forEachIndexed { index, (item, count, shouldShow) ->
            if (shouldShow) {
                val slotX = ox + slotXOffsets[index]
                val slotY = oy + slotYOffset
                val displayCount = if (count > 0L) count.coerceAtMost(99L).toInt() else 1
                val stack = ItemStack(item, displayCount)
                graphics.renderItem(stack, slotX, slotY)
                
                // Affiche la quantité (si > 1 ou si == 0 pour indiquer la dispo à 0)
                if (count > 1 || count == 0L) {
                    val countStr = formatSlotCount(count)
                    val scale = 0.55f
                    graphics.pose().pushPose()
                    graphics.pose().scale(scale, scale, 1.0f)
                    val scaledX = ((slotX + 16) / scale - 17).toInt()
                    val scaledY = ((slotY + 16) / scale - 17).toInt()
                    graphics.renderItemDecorations(font, stack, scaledX, scaledY, countStr)
                    graphics.pose().popPose()
                }
            }
        }

        // ── Balance totale compacte dans la zone noire en bas à gauche (+2px à droite, -1px en haut) ──
        val totalText = formatCompact(ClientWalletCache.balance)
        graphics.drawString(font, totalText, ox + 12, oy + 28, 0xFFFFFF, true)

        // ── Bouton Switch 1 : Auto-Collect (empilés verticalement à droite, -2px à gauche) ──
        val btnX = ox + 84
        val btn1Y = oy + 12
        drawSwitch(graphics, btnX, btn1Y, ClientWalletCache.autoCollect)

        // ── Bouton Switch 2 : HUD (empilés verticalement à droite, -2px à gauche) ──
        val btn2Y = oy + 26
        drawSwitch(graphics, btnX, btn2Y, ClientWalletCache.hudEnabled)

        // ── Info-bulles (Tooltips) au survol des interrupteurs ──
        if (mouseX >= btnX && mouseX <= btnX + BTN_W && mouseY >= btn1Y && mouseY <= btn1Y + BTN_H) {
            graphics.renderComponentTooltip(font, listOf(Component.translatable("cobblemongymodyssey.wallet.tooltip.autocollect")), mouseX, mouseY)
        } else if (mouseX >= btnX && mouseX <= btnX + BTN_W && mouseY >= btn2Y && mouseY <= btn2Y + BTN_H) {
            graphics.renderComponentTooltip(font, listOf(Component.translatable("cobblemongymodyssey.wallet.tooltip.hud")), mouseX, mouseY)
        }
    }

    /**
     * Gère les clics sur l'overlay.
     */
    fun mouseClicked(guiLeft: Int, guiTop: Int, mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!isOpen) return false

        // Ajusté 1 pixel plus à gauche (était + 4)
        val ox = guiLeft + 176 + 3
        val oy = guiTop + 4

        // 1. Clic sur les slots de pièces (retrait/dépot, +1px à droite)
        val slotXOffsets = listOf(11, 29, 47, 65)
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

        // 2. Clic sur les interrupteurs de basculement (empilés à droite à x = 84, -2px à gauche)
        val btnX = ox + 84
        val btn1Y = oy + 12
        val btn2Y = oy + 26

        // Switch 1 : Auto-Collect
        if (mouseX >= btnX && mouseX <= btnX + BTN_W && mouseY >= btn1Y && mouseY <= btn1Y + BTN_H) {
            val newVal = !ClientWalletCache.autoCollect
            ClientWalletCache.autoCollect = newVal
            WalletNetwork.sendToggle(0, newVal)
            mc.soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f))
            return true
        }

        // Switch 2 : HUD
        if (mouseX >= btnX && mouseX <= btnX + BTN_W && mouseY >= btn2Y && mouseY <= btn2Y + BTN_H) {
            val newVal = !ClientWalletCache.hudEnabled
            ClientWalletCache.hudEnabled = newVal
            WalletNetwork.sendToggle(1, newVal)
            mc.soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f))
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
