package com.howlite.neoforge.client

import com.howlite.CobblemonGymOdyssey
import com.howlite.client.screen.InventoryWalletButton
import com.howlite.client.screen.WalletHudOverlay
import com.howlite.client.screen.WalletOverlay
import com.howlite.menu.BadgeCaseMenus
import com.howlite.screen.BadgeCaseScreen
import com.howlite.wallet.WalletNetwork
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent

/**
 * Enregistrement côté client NeoForge de l'écran de la Boîte à Badges.
 *
 * Utilise [RegisterMenuScreensEvent] (bus MOD, dist CLIENT) plutôt que
 * [dev.architectury.registry.menu.MenuRegistry] qui peut présenter des
 * problèmes de timing sur NeoForge 1.21.x.
 */
@Suppress("DEPRECATION")
@EventBusSubscriber(
    modid = CobblemonGymOdyssey.MOD_ID,
    bus   = EventBusSubscriber.Bus.MOD,
    value = [Dist.CLIENT]
)
object NeoForgeClientSetup {

    @SubscribeEvent
    fun onRegisterScreens(event: RegisterMenuScreensEvent) {
        event.register(BadgeCaseMenus.BADGE_CASE_MENU_TYPE.get()) { menu, inv, title ->
            BadgeCaseScreen(menu, inv, title)
        }
        event.register(BadgeCaseMenus.GYM_SHOP_MENU_TYPE.get()) { menu, inv, title ->
            com.howlite.client.screen.GymShopScreen(menu, inv, title)
        }
        event.register(BadgeCaseMenus.GYM_SHOP_EDIT_MENU_TYPE.get()) { menu, inv, title ->
            com.howlite.client.screen.GymShopEditScreen(menu, inv, title)
        }
    }

    @SubscribeEvent
    fun onClientSetup(event: net.neoforged.fml.event.lifecycle.FMLClientSetupEvent) {
        com.howlite.client.GymClientInit.init()
        // Enregistrer les packets wallet S2C
        WalletNetwork.registerClientReceivers()
    }
}
