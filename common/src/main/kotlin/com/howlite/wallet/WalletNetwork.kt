package com.howlite.wallet

import com.howlite.CobblemonGymOdyssey
import dev.architectury.networking.NetworkManager
import io.netty.buffer.Unpooled
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer

/**
 * Gestion des packets réseau liés au wallet.
 *
 * Packets :
 *  - S2C [WALLET_SYNC_ID]   : serveur → client, synchronise l'état complet du wallet.
 *  - C2S [WALLET_TOGGLE_ID] : client → serveur, bascule autoCollect ou hudEnabled.
 */
object WalletNetwork {

    val WALLET_SYNC_ID: ResourceLocation =
        ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "wallet_sync")

    val WALLET_TOGGLE_ID: ResourceLocation =
        ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "wallet_toggle")

    // -------------------------------------------------------------------------
    // Enregistrement des receivers (côté commun)
    // -------------------------------------------------------------------------

    fun registerServerReceivers() {
        // C2S : le client bascule un setting (0 = autoCollect, 1 = hudEnabled)
        NetworkManager.registerReceiver(
            NetworkManager.Side.C2S,
            WALLET_TOGGLE_ID
        ) { buf, context ->
            val switchId = buf.readByte()
            val newValue = buf.readBoolean()
            context.queue {
                val player = context.player as? ServerPlayer ?: return@queue
                val wallet = WalletManager.get(player)
                when (switchId.toInt()) {
                    0 -> wallet.autoCollect = newValue
                    1 -> wallet.hudEnabled = newValue
                }
                syncToClient(player, wallet)
            }
        }
    }

    fun registerClientReceivers() {
        // S2C : le serveur envoie l'état complet du wallet
        NetworkManager.registerReceiver(
            NetworkManager.Side.S2C,
            WALLET_SYNC_ID
        ) { buf, context ->
            val balance = buf.readLong()
            val autoCollect = buf.readBoolean()
            val hud = buf.readBoolean()
            context.queue {
                ClientWalletCache.balance = balance
                ClientWalletCache.autoCollect = autoCollect
                ClientWalletCache.hudEnabled = hud
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helpers d'envoi
    // -------------------------------------------------------------------------

    /** Envoie l'état complet du wallet au client (appel serveur). */
    fun syncToClient(player: ServerPlayer, wallet: WalletData) {
        val buf = RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess())
        buf.writeLong(wallet.balanceCCC)
        buf.writeBoolean(wallet.autoCollect)
        buf.writeBoolean(wallet.hudEnabled)
        NetworkManager.sendToPlayer(player, WALLET_SYNC_ID, buf)
    }

    /** Envoie un toggle depuis le client vers le serveur. */
    fun sendToggle(switchId: Int, newValue: Boolean) {
        // Côté client : pas d'accès au registryAccess() sans Minecraft.getInstance()
        // On utilise un buffer simple (pas de registres nécessaires pour bool/byte)
        val buf = RegistryFriendlyByteBuf(
            Unpooled.buffer(),
            net.minecraft.client.Minecraft.getInstance().level!!.registryAccess()
        )
        buf.writeByte(switchId)
        buf.writeBoolean(newValue)
        NetworkManager.sendToServer(WALLET_TOGGLE_ID, buf)
    }
}
