package com.howlite.events

import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.events.pokemon.LevelUpEvent
import com.howlite.api.PlayerProgressApi

/**
 * Intercepte les gains d'expérience Pokémon de l'API Cobblemon pour appliquer
 * le Level Cap personnel du dresseur.
 *
 * ## Deux niveaux de protection contre les Candy XP XL
 *
 * ### 1) [CobblemonEvents.EXPERIENCE_GAINED_EVENT_PRE]
 * Bloque le gain d'XP si le Pokémon est déjà au cap ou au-dessus.
 * Couvre les gains normaux (combat, rare candy standard).
 *
 * ### 2) [CobblemonEvents.LEVEL_UP_EVENT]
 * Correctif de sécurité : les Candy XP XL ajoutent une grande quantité d'XP
 * d'un seul coup, ce qui peut faire monter le Pokémon de plusieurs niveaux en
 * contournant le check PRE (ex: lvl 9 → lvl 12 avec cap 10).
 * Ce second listener force le nouveau niveau au cap via [LevelUpEvent.setNewLevel]
 * immédiatement lors de la montée.
 *
 * ## API Cobblemon utilisée
 * - **Événement PRE** : [CobblemonEvents.EXPERIENCE_GAINED_EVENT_PRE]
 * - **Événement Level-up** : [CobblemonEvents.LEVEL_UP_EVENT] → [LevelUpEvent]
 * - **Pokémon** : `event.pokemon`
 * - **Nouveau niveau** : `event.newLevel` / `event.setNewLevel(n)`
 * - **Propriétaire** : `event.pokemon.getOwnerPlayer()` → `ServerPlayer?`
 */
object LevelCapEventHandler {

    fun register() {
        // --- Protection 1 : bloquer le gain d'XP avant qu'il soit appliqué ---
        CobblemonEvents.EXPERIENCE_GAINED_EVENT_PRE.subscribe { event ->
            val pokemon = event.pokemon
            val player = pokemon.getOwnerPlayer() ?: return@subscribe
            val data = PlayerProgressApi.get(player)

            if (pokemon.level >= data.levelCap) {
                event.cancel()
            }
        }

        // --- Protection 2 : corriger le niveau lors d'une montée (Candy XP XL) ---
        // Les Candy XP XL peuvent ajouter assez d'XP pour dépasser le cap en
        // une seule opération. On plafonne le nouveau niveau au cap via setNewLevel.
        CobblemonEvents.LEVEL_UP_EVENT.subscribe { event ->
            val pokemon = event.pokemon
            val player = pokemon.getOwnerPlayer() ?: return@subscribe
            val data = PlayerProgressApi.get(player)

            if (event.newLevel > data.levelCap) {
                event.newLevel = data.levelCap
            }
        }
    }
}

