package com.howlite.fabric.wallet

import net.minecraft.resources.ResourceLocation
import org.ladysnake.cca.api.v3.component.ComponentKey
import org.ladysnake.cca.api.v3.component.ComponentRegistry

/**
 * Clé CCA pour le composant wallet du joueur.
 */
object FabricWalletComponents {

    val PLAYER_WALLET: ComponentKey<FabricWalletProvider> =
        ComponentRegistry.getOrCreate(
            ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "player_wallet"),
            FabricWalletProvider::class.java
        )
}
