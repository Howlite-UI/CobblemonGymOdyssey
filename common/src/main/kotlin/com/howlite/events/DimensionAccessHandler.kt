package com.howlite.events

import com.howlite.api.PlayerProgressApi
import com.howlite.data.GymRegion
import dev.architectury.event.events.common.PlayerEvent
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.Level

/**
 * Gere le systeme de progression dimensionnelle.
 *
 * ## Regles de deblocage
 * - **Nether** (minecraft:the_nether) : accessible apres avoir complete la ligue Johto.
 * - **End**    (minecraft:the_end)    : accessible apres avoir complete la ligue Hoenn.
 *
 * ## Mecanisme
 * [PlayerEvent.CHANGE_DIMENSION] est l'evenement Architectury specifiquement concu pour les
 * transitions de dimension cote serveur. Il se declenche apres que le joueur a ete transfere
 * vers la nouvelle dimension. Si le joueur n'est pas qualifie, on le reteleporte immediatement
 * vers l'Overworld (spawn) via server.execute pour eviter tout conflit avec la transition en cours.
 *
 * Note : Les joueurs en mode creatif ou OP (niveau 2+) sont exemptes de ces restrictions.
 */
object DimensionAccessHandler {

    private val NETHER_KEY: ResourceKey<Level> = Level.NETHER
    private val END_KEY:    ResourceKey<Level> = Level.END

    fun register() {
        PlayerEvent.CHANGE_DIMENSION.register { player, oldDim, newDim ->
            if (player !is ServerPlayer) return@register
            // Seul le mode creatif est exempte (pas les OP en survie)
            if (player.isCreative) return@register

            val server = player.server

            when (newDim) {
                NETHER_KEY -> {
                    val progress = PlayerProgressApi.get(player)
                    if (!progress.hasCompletedRegion(GymRegion.JOHTO)) {
                        val overworld: ServerLevel = server.overworld()
                        val spawnPos = overworld.sharedSpawnPos
                        // Planifier le retour au prochain tick serveur
                        server.execute {
                            player.teleportTo(
                                overworld,
                                spawnPos.x.toDouble() + 0.5,
                                overworld.getHeight(
                                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                                    spawnPos.x, spawnPos.z
                                ).toDouble(),
                                spawnPos.z.toDouble() + 0.5,
                                0f, 0f
                            )
                            player.sendSystemMessage(
                                Component.translatable("cobblemongymodyssey.progression.nether_locked")
                            )
                        }
                    }
                }

                END_KEY -> {
                    val progress = PlayerProgressApi.get(player)
                    if (!progress.hasCompletedRegion(GymRegion.HOENN)) {
                        val overworld: ServerLevel = server.overworld()
                        val spawnPos = overworld.sharedSpawnPos
                        server.execute {
                            player.teleportTo(
                                overworld,
                                spawnPos.x.toDouble() + 0.5,
                                overworld.getHeight(
                                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                                    spawnPos.x, spawnPos.z
                                ).toDouble(),
                                spawnPos.z.toDouble() + 0.5,
                                0f, 0f
                            )
                            player.sendSystemMessage(
                                Component.translatable("cobblemongymodyssey.progression.end_locked")
                            )
                        }
                    }
                }
            }
        }
    }
}
