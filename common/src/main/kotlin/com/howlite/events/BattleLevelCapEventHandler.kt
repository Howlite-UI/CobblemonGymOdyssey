package com.howlite.events

import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor
import com.howlite.api.PlayerProgressApi
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer

/**
 * Vérifie, avant chaque combat Cobblemon, qu'aucun Pokémon de l'équipe du joueur
 * ne dépasse son Level Cap personnel.
 *
 * Si un Pokémon enfreint la règle, le combat est immédiatement annulé et
 * un message d'erreur localisé est envoyé au joueur dans les deux langues
 * (EN + FR).
 *
 * ## API Cobblemon utilisée
 * - **Événement** : [CobblemonEvents.BATTLE_STARTED_PRE] — déclenché *avant* le lancement
 *   du combat ; annulable via `event.cancel()`.
 * - **`event.battle.actors`** : `Iterable<BattleActor>` — tous les participants.
 * - **[PlayerBattleActor].entity** : `ServerPlayer?` — le joueur côté serveur.
 * - **[PlayerBattleActor].pokemonList** : liste des `BattlePokemon` alignés.
 * - **`BattlePokemon.effectedPokemon`** : le `Pokemon` réel avec son niveau.
 */
object BattleLevelCapEventHandler {

    fun register() {
        CobblemonEvents.BATTLE_STARTED_PRE.subscribe { event ->
            for (actor in event.battle.actors) {
                // On ne vérifie que les joueurs humains
                if (actor !is PlayerBattleActor) continue

                val player = actor.entity ?: continue
                val levelCap = PlayerProgressApi.get(player).levelCap

                // Cherche le premier Pokémon qui dépasse le Level Cap
                val offender = actor.pokemonList
                    .map { it.effectedPokemon }
                    .firstOrNull { it.level > levelCap }

                if (offender != null) {
                    // Annule le combat pour TOUS les participants
                    event.cancel()

                    // Message bilingue FR + EN
                    val pokemonName = offender.getDisplayName().string
                    val level      = offender.level

                    player.sendSystemMessage(
                        Component.translatable(
                            "cobblemongymodyssey.level_cap.battle_blocked",
                            pokemonName, level, levelCap
                        )
                    )

                    // On a trouvé un joueur en infraction — inutile de continuer
                    break
                }
            }
        }
    }
}
