package com.howlite.fabric.client

import com.howlite.menu.BadgeCaseMenus
import com.howlite.screen.BadgeCaseScreen
import dev.architectury.registry.menu.MenuRegistry
import net.fabricmc.api.ClientModInitializer

/**
 * Point d'entrée client Fabric.
 *
 * - Enregistre la factory d'écran de la Boîte à Badges.
 * - Le slot Trinkets est entièrement data-driven (JSON dans resources),
 *   aucun code API n'est nécessaire côté client.
 */
class ExampleModFabricClient : ClientModInitializer {
    override fun onInitializeClient() {
        com.howlite.client.GymClientInit.init()

        // Associe le MenuType à l'écran côté client
        MenuRegistry.registerScreenFactory(BadgeCaseMenus.BADGE_CASE_MENU_TYPE.get()) { menu, inv, title ->
            BadgeCaseScreen(menu, inv, title)
        }
    }
}

