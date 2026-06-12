package com.howlite.events

import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor
import com.cobblemon.mod.common.entity.npc.NPCBattleActor
import com.cobblemon.mod.common.Cobblemon
import com.howlite.data.PokemonSnapshot
import com.howlite.api.PlayerProgressApi
import com.howlite.config.GymConfig
import com.howlite.items.GymBadgeItems
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack

/**
 * Détecte les victoires en combat contre un Maître d'Arène et accorde
 * le badge correspondant au joueur victorieux, mettant ainsi à jour son Level Cap.
 *
 * ## API Cobblemon utilisée (confirmée par inspection du JAR sources 1.6.1)
 * - **Événement** : [CobblemonEvents.BATTLE_VICTORY] → `EventObservable<BattleVictoryEvent>`
 * - **`event.winners`** : `List<BattleActor>` — les acteurs victorieux
 * - **`event.losers`** : `List<BattleActor>` — les acteurs vaincus
 * - **[PlayerBattleActor].entity** : `ServerPlayer?` (via `EntityBackedBattleActor<ServerPlayer>`)
 * - **[NPCBattleActor].npc** : `NPCEntity` — l'entité NPC en jeu
 * - **NPCEntity.npc.id** : `ResourceLocation` — identifiant de la classe NPC (datapack)
 *
 * ## Identification du Maître d'Arène
 * Le NPC doit avoir une **classe NPC** dont l'identifiant est présent dans
 * [GymConfig.gymLeaderToBadge] (ex: `cobblemongymodyssey:brock`).
 *
 * La classe NPC est définie dans un fichier datapack :
 * `data/cobblemongymodyssey/npcs/brock.json`
 *
 * Pour spawner un tel NPC en jeu :
 * ```
 * /cobblemon npc spawn cobblemongymodyssey:brock
 * ```
 */
object GymBattleEventHandler {

    fun register() {
        CobblemonEvents.BATTLE_VICTORY.subscribe { event ->
            // --- Trouver le joueur gagnant parmi les vainqueurs ---
            val playerActor = event.winners
                .filterIsInstance<PlayerBattleActor>()
                .firstOrNull() ?: return@subscribe

            // PlayerBattleActor.entity est une ServerPlayer?
            val player = playerActor.entity ?: return@subscribe

            // --- Trouver un NPC parmi les perdants ---
            // BattleVictoryEvent expose directement event.losers (confirmé sources)
            val npcActor = event.losers
                .filterIsInstance<NPCBattleActor>()
                .firstOrNull() ?: return@subscribe

            // NPCBattleActor.npc est directement la NPCEntity
            // NPCEntity.npc est la NPCClass (la définition du NPC)
            // NPCClass.id est le ResourceLocation de la classe dans le datapack
            val npcClassId = npcActor.npc.npc.id

            val badge = GymConfig.gymLeaderToBadge[npcClassId] ?: return@subscribe

            // --- Accorder le badge si le joueur ne l'a pas encore ---
            val data = PlayerProgressApi.get(player)
            val item = GymBadgeItems.getItemForBadge(badge)
            if (!data.hasBadge(badge)) {
                // Capturer l'équipe du joueur
                try {
                    val party = Cobblemon.storage.getParty(player)
                    val snapshots = party.filterNotNull().map { pokemon ->
                        PokemonSnapshot(
                            species = pokemon.species.name,
                            level = pokemon.level,
                            isShiny = pokemon.shiny,
                            displayName = pokemon.getDisplayName().string
                        )
                    }
                    data.recordTeam(badge.id, snapshots)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                data.earnBadge(badge)
                PlayerProgressApi.markDirty(player)

                // Accorder l'item physique du badge
                val itemStack = ItemStack(item)
                if (!player.inventory.add(itemStack)) {
                    player.drop(itemStack, false)
                }

                // Feedback visuel au joueur
                player.sendSystemMessage(
                    Component.translatable(
                        "cobblemongymodyssey.gym_battle.obtained",
                        Component.translatable(item.descriptionId),
                        badge.levelCap
                    )
                )
            } else {
                player.sendSystemMessage(
                    Component.translatable(
                        "cobblemongymodyssey.gym_battle.already_owned",
                        Component.translatable(item.descriptionId)
                    )
                )
            }
        }
    }

    /** Transforme "BOULDER_BADGE" → "Boulder Badge" pour l'affichage. */
    private fun formatBadgeName(enumName: String): String =
        enumName.split("_")
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { it.uppercaseChar() }
            }
}
