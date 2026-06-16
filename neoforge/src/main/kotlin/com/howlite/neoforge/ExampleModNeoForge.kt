package com.howlite.neoforge

import com.howlite.CobblemonGymOdyssey
import com.howlite.api.PlayerProgressApi
import com.howlite.neoforge.data.NeoForgeAttachments
import com.howlite.neoforge.data.NeoForgePlayerProgressProvider
import com.howlite.neoforge.wallet.NeoForgeWalletAttachment
import com.howlite.wallet.WalletManager
import com.howlite.wallet.WalletNetwork
import net.neoforged.fml.common.Mod
import net.neoforged.fml.javafmlmod.FMLModContainer
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS

/**
 * Point d'entrée NeoForge du mod.
 *
 * Responsabilités :
 * 1. Enregistrer les [NeoForgeAttachments] sur le mod event bus.
 * 2. Injecter le [NeoForgePlayerProgressProvider] dans l'API commune.
 * 3. Déléguer l'initialisation des événements Cobblemon à [CobblemonGymOdyssey.init].
 */
@Mod(CobblemonGymOdyssey.MOD_ID)
class ExampleModNeoForge {
    init {
        println("[GymOdyssey] ExampleModNeoForge constructor running!")
        // 1. Enregistrer les Data Attachments sur le mod event bus
        NeoForgeAttachments.register(MOD_BUS)
        NeoForgeWalletAttachment.register(MOD_BUS)

        // 2. Brancher le provider NeoForge dans l'API commune
        PlayerProgressApi.provider = NeoForgePlayerProgressProvider()

        // Brancher le provider Wallet NeoForge
        WalletManager.provider = com.howlite.wallet.WalletProvider { player ->
            player.getData(NeoForgeWalletAttachment.PLAYER_WALLET)
        }

        // Enregistrer les packets réseau wallet (serveur)
        WalletNetwork.registerServerReceivers()

        // 3. Initialiser les listeners d'événements Cobblemon (Common)
        CobblemonGymOdyssey.init()
    }
}
