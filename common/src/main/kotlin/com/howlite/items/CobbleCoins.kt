package com.howlite.items

import com.howlite.CobblemonGymOdyssey
import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
import net.minecraft.core.registries.Registries
import net.minecraft.world.item.Item

/**
 * Enregistrement des 4 pièces de monnaie Cobble.
 *
 * Hiérarchie :
 *   100 CCC (Copper) = 1 CSC (Silver)
 *   100 CSC (Silver) = 1 CGC (Gold)
 *   100 CGC (Gold)   = 1 CPC (Platinum)
 *
 * Les pièces ne s'accumulent PAS dans l'inventaire classique : elles sont
 * interceptées lors du pickup et transférées directement dans le [WalletData]
 * du joueur via [com.howlite.events.CoinPickupHandler].
 */
object CobbleCoins {

    private val ITEMS: DeferredRegister<Item> =
        DeferredRegister.create(CobblemonGymOdyssey.MOD_ID, Registries.ITEM)

    val COBBLE_COPPER_COIN: RegistrySupplier<Item> = ITEMS.register("cobble_copper_coin") {
        Item(Item.Properties().stacksTo(99))
    }

    val COBBLE_SILVER_COIN: RegistrySupplier<Item> = ITEMS.register("cobble_silver_coin") {
        Item(Item.Properties().stacksTo(99))
    }

    val COBBLE_GOLD_COIN: RegistrySupplier<Item> = ITEMS.register("cobble_gold_coin") {
        Item(Item.Properties().stacksTo(99))
    }

    val COBBLE_PLATINUM_COIN: RegistrySupplier<Item> = ITEMS.register("cobble_platinum_coin") {
        Item(Item.Properties().stacksTo(99))
    }

    fun register() {
        ITEMS.register()
    }
}
