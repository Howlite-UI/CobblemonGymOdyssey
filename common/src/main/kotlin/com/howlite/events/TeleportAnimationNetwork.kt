package com.howlite.events

import com.howlite.CobblemonGymOdyssey
import dev.architectury.networking.NetworkManager
import io.netty.buffer.Unpooled
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer

/**
 * Gestion reseau pour l animation de teleportation.
 *
 * Paquets S2C :
 *  - [START_TP_ANIM_ID]   : zoom-up (teleportation differee via /tp ou Waystone).
 *  - [END_TP_ANIM_ID]     : zoom-down apres la TP physique.
 */
object TeleportAnimationNetwork {

    val START_TP_ANIM_ID: ResourceLocation =
        ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "start_tp_anim")

    val END_TP_ANIM_ID: ResourceLocation =
        ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "end_tp_anim")

    // -------------------------------------------------------------------------
    // Envoi depuis le serveur
    // -------------------------------------------------------------------------

    fun sendStartAnimation(player: ServerPlayer, startX: Double, startY: Double, startZ: Double) {
        val buf = RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess())
        buf.writeDouble(startX)
        buf.writeDouble(startY)
        buf.writeDouble(startZ)
        NetworkManager.sendToPlayer(player, START_TP_ANIM_ID, buf)
    }

    fun sendEndAnimation(player: ServerPlayer) {
        val buf = RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess())
        NetworkManager.sendToPlayer(player, END_TP_ANIM_ID, buf)
    }

    // -------------------------------------------------------------------------
    // Reception cote client
    // -------------------------------------------------------------------------

    fun registerClientReceivers() {
        NetworkManager.registerReceiver(
            NetworkManager.Side.S2C,
            START_TP_ANIM_ID
        ) { buf, context ->
            val startX = buf.readDouble()
            val startY = buf.readDouble()
            val startZ = buf.readDouble()
            context.queue {
                com.howlite.client.render.TeleportAnimationClient.startZoomUp(startX, startY, startZ)
            }
        }

        NetworkManager.registerReceiver(
            NetworkManager.Side.S2C,
            END_TP_ANIM_ID
        ) { _, context ->
            context.queue {
                com.howlite.client.render.TeleportAnimationClient.startZoomDown()
            }
        }
    }
}
