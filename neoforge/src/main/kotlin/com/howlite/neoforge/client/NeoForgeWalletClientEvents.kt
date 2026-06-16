package com.howlite.neoforge.client

import com.howlite.CobblemonGymOdyssey
import com.howlite.client.screen.InventoryWalletButton
import com.howlite.client.screen.WalletHudOverlay
import com.howlite.client.screen.WalletOverlay
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RenderGuiEvent
import net.neoforged.neoforge.client.event.ScreenEvent

/**
 * Gestion des events NeoForge FORGE bus côté client pour le wallet.
 *
 * - Injection du bouton wallet dans l'InventoryScreen.
 * - Rendu de l'overlay wallet par-dessus l'inventaire.
 * - Rendu du HUD wallet en coin d'écran.
 * - Clics souris sur l'overlay.
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
            // Position : à droite du slot résultat de craft, entre craft et inventaire
            val btnX = screen.guiLeft + 161
            val btnY = screen.guiTop + 44
            event.addListener(InventoryWalletButton(btnX, btnY, 12, 12))
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
            if (WalletOverlay.mouseClicked(screen.guiLeft, screen.guiTop,
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
