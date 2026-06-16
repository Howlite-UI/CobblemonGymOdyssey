package com.howlite.neoforge.client

import com.howlite.CobblemonGymOdyssey
import com.howlite.client.screen.InventoryWalletButton
import com.howlite.client.screen.WalletHudOverlay
import com.howlite.client.screen.WalletOverlay
import com.howlite.wallet.WalletNetwork
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RenderGuiEvent
import net.neoforged.neoforge.client.event.ScreenEvent

/**
 * Gestion des events NeoForge FORGE bus côté client pour le wallet.
 *
 * Implémente toutes les interactions de l'inventaire en Kotlin (bouton, overlay, clics, Shift-Clics)
 * pour éviter les limitations d'ordre de compilation Java/Kotlin.
 */
@EventBusSubscriber(
    modid = CobblemonGymOdyssey.MOD_ID,
    bus   = EventBusSubscriber.Bus.GAME,
    value = [Dist.CLIENT]
)
object NeoForgeWalletClientEvents {

    @SubscribeEvent
    fun onScreenInit(event: ScreenEvent.Init.Post) {
        val screen = event.screen
        if (screen is InventoryScreen) {
            val btnX = screen.guiLeft + 161
            val btnY = screen.guiTop + 44
            event.addListener(InventoryWalletButton(btnX, btnY, 14, 17))
        }
    }

    @SubscribeEvent
    fun onScreenRender(event: ScreenEvent.Render.Post) {
        val screen = event.screen
        if (screen is InventoryScreen) {
            WalletOverlay.render(event.guiGraphics, screen.guiLeft, screen.guiTop,
                event.mouseX, event.mouseY)
        }
    }

    @SubscribeEvent
    fun onScreenMouseClick(event: ScreenEvent.MouseButtonPressed.Pre) {
        val screen = event.screen
        if (screen is InventoryScreen) {
            // 1. Clic sur les slots ou switches de l'overlay
            if (WalletOverlay.mouseClicked(screen.guiLeft, screen.guiTop,
                    event.mouseX, event.mouseY, event.button)) {
                event.isCanceled = true
                return
            }

            // 2. Shift-Clic rapide sur une pièce de l'inventaire standard pour la déposer
            if (WalletOverlay.isOpen && event.button == 0 && net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
                val slot = screen.slotUnderMouse
                if (slot != null && slot.hasItem()) {
                    val stack = slot.item
                    val isCoin = stack.item == com.howlite.items.CobbleCoins.COBBLE_COPPER_COIN.get() ||
                                 stack.item == com.howlite.items.CobbleCoins.COBBLE_SILVER_COIN.get() ||
                                 stack.item == com.howlite.items.CobbleCoins.COBBLE_GOLD_COIN.get() ||
                                 stack.item == com.howlite.items.CobbleCoins.COBBLE_PLATINUM_COIN.get()
                    if (isCoin) {
                        WalletNetwork.sendDepositSlot(slot.index)
                        event.isCanceled = true
                    }
                }
            }
        }
    }

    @SubscribeEvent
    fun onScreenMouseRelease(event: ScreenEvent.MouseButtonReleased.Pre) {
        val screen = event.screen
        if (screen is InventoryScreen) {
            if (WalletOverlay.isHovering(screen.guiLeft, screen.guiTop,
                    event.mouseX, event.mouseY)) {
                event.isCanceled = true
            }
        }
    }

    @SubscribeEvent
    fun onRenderHud(event: RenderGuiEvent.Post) {
        WalletHudOverlay.render(event.guiGraphics, event.partialTick.gameTimeDeltaTicks)
    }
}
