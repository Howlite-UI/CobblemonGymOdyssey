package com.howlite.events

import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor
import com.cobblemon.mod.common.entity.npc.NPCBattleActor
import com.howlite.api.PlayerProgressApi
import com.howlite.config.GymConfig
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.Level

object GymBattleReturnHandler {

    fun register() {
        CobblemonEvents.BATTLE_VICTORY.subscribe { event ->
            // 1. Trouver le joueur impliqué (gagnant ou perdant)
            val playerActor = (event.winners + event.losers)
                .filterIsInstance<PlayerBattleActor>()
                .firstOrNull() ?: return@subscribe

            val player = playerActor.entity ?: return@subscribe

            // 2. Vérifier si le joueur a une position de retour enregistrée
            val progress = PlayerProgressApi.get(player)
            val returnDimStr = progress.returnDim ?: return@subscribe
            val returnX = progress.returnX ?: return@subscribe
            val returnY = progress.returnY ?: return@subscribe
            val returnZ = progress.returnZ ?: return@subscribe
            val returnYaw = progress.returnYaw ?: 0f
            val returnPitch = progress.returnPitch ?: 0f

            // 3. Vérifier si l'adversaire est un Champion d'Arène (NPC)
            val npcActor = (event.winners + event.losers)
                .filterIsInstance<NPCBattleActor>()
                .firstOrNull() ?: return@subscribe

            val npcClassId = npcActor.npc.npc.id
            if (GymConfig.gymLeaderToBadge.containsKey(npcClassId)) {
                val server = player.server
                val returnLevelKey = ResourceKey.create(
                    Registries.DIMENSION,
                    ResourceLocation.parse(returnDimStr)
                )
                val targetWorld = server.getLevel(returnLevelKey) ?: server.overworld()

                // Nettoyer les coordonnées de retour pour éviter toute téléportation répétée
                progress.clearReturnPosition()
                PlayerProgressApi.markDirty(player)

                // Téléporter le joueur de retour à son bloc de départ
                player.teleportTo(targetWorld, returnX, returnY, returnZ, returnYaw, returnPitch)
                player.sendSystemMessage(
                    Component.literal("§aLe combat est terminé ! Vous avez été ramené à votre bloc de départ.")
                )
            }
        }
    }
}
