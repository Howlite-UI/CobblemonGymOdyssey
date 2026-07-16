package com.howlite.moon

import com.howlite.CobblemonGymOdyssey
import dev.architectury.networking.NetworkManager
import io.netty.buffer.Unpooled
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer

/**
 * Gestion des paquets réseau pour la synchronisation des phases lunaires.
 *
 * ### `sync_moon_phase` (S2C)
 * Payload : `phaseName: String`
 *
 * ### `request_moon_phase` (C2S)
 * Payload : vide
 */
object MoonNetwork {

    val SYNC_MOON_PHASE = ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "sync_moon_phase")
    val REQUEST_MOON_PHASE = ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "request_moon_phase")

    fun registerServerReceivers() {
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, REQUEST_MOON_PHASE) { _, context ->
            context.queue {
                val player = context.player as? ServerPlayer ?: return@queue
                syncPhaseToPlayer(player, MoonManager.currentPhase)
            }
        }
    }

    /**
     * À appeler depuis [com.howlite.client.GymClientInit.init] (côté client uniquement).
     */
    fun registerClientReceivers() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SYNC_MOON_PHASE) { buf, context ->
            val phaseName = buf.readUtf()
            val phase = MoonPhase.fromName(phaseName)
            context.queue {
                com.howlite.client.moon.ClientMoonState.applyPhase(phase)
            }
        }
    }

    fun syncPhaseToPlayer(player: ServerPlayer, phase: MoonPhase) {
        val buf = RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess())
        buf.writeUtf(phase.name)
        NetworkManager.sendToPlayer(player, SYNC_MOON_PHASE, buf)
    }

    fun broadcastPhase(server: MinecraftServer, phase: MoonPhase) {
        server.playerList.players.forEach { player ->
            syncPhaseToPlayer(player, phase)
        }
    }
}
