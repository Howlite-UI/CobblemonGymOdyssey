package com.howlite.moon

import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.cobblemon.mod.common.pokemon.Pokemon

/**
 * Modifie les Pokémon sauvages lors de leur spawn en fonction de la phase lunaire active.
 *
 * ## Effets par phase
 *
 * ### [MoonPhase.BLUE_MOON]
 * Chaque Pokémon qui spawn a une chance augmentée d'être shiny.
 * - Le taux de base Cobblemon est 1/4096 (≈ 0.0244%).
 * - Avec ×5 : ~0.122% (1/819).
 * - Implémentation : on remplace le flag shiny avec probabilité `baseRate * multiplier`.
 *
 * ### [MoonPhase.FULL_MOON]
 * [MoonConfig.ivCountFullMoon] IVs aléatoires parmi les 6 stats sont garantis à 31.
 *
 * ### [MoonPhase.RED_MOON]
 * Les Pokémon de type Dark ou Ghost voient leur flag `shiny` légèrement boosté
 * et une note est ajoutée (aucun event Cobblemon de spawn-rate direct n'existe).
 * En pratique : aucune modification d'IV/shiny supplémentaire, l'effet est narratif/notif.
 *
 * ### [MoonPhase.PURPLE_MOON]
 * Aucune modification au spawn ; l'effet EXP ×1.5 est géré séparément
 * (via [CobblemonEvents.POKEMON_FAINTED] si disponible, ou tag custom).
 */
object MoonSpawnModifier {

    /** Taux de base shiny Cobblemon (1 sur 4096). */
    private const val BASE_SHINY_RATE = 1f / 4096f

    /** Liste des types considérés comme "nocturnes" pour la Red Moon. */
    private val NOCTURNAL_TYPES = setOf("dark", "ghost")

    fun register() {
        CobblemonEvents.POKEMON_ENTITY_SPAWN.subscribe { event ->
            val phase = MoonManager.currentPhase
            if (phase == MoonPhase.NONE) return@subscribe

            val pokemon = event.entity.pokemon
            applyPhaseModifiers(pokemon, phase)
        }

        println("[GymOdyssey/Moon] MoonSpawnModifier enregistré.")
    }

    private fun applyPhaseModifiers(pokemon: Pokemon, phase: MoonPhase) {
        when (phase) {
            MoonPhase.BLUE_MOON   -> applyBlueMoon(pokemon)
            MoonPhase.FULL_MOON   -> applyFullMoon(pokemon)
            MoonPhase.RED_MOON    -> applyRedMoon(pokemon)
            MoonPhase.PURPLE_MOON -> { /* Effet EXP — pas de modification au spawn */ }
            MoonPhase.NONE        -> { /* Rien */ }
        }
    }

    // ─── Blue Moon ───────────────────────────────────────────────────────────

    private fun applyBlueMoon(pokemon: Pokemon) {
        if (pokemon.shiny) return // Déjà shiny, pas besoin de reroll
        val multiplier = MoonConfig.instance.shinyMultiplierBlueMoon
        // Calcul : si le random tombe dans multiplier fois le taux de base → shiny
        val effectiveRate = BASE_SHINY_RATE * multiplier
        if (Math.random().toFloat() < effectiveRate) {
            pokemon.shiny = true
        }
    }

    // ─── Full Moon ───────────────────────────────────────────────────────────

    private fun applyFullMoon(pokemon: Pokemon) {
        val ivCount = MoonConfig.instance.ivCountFullMoon.coerceIn(0, 6)
        if (ivCount <= 0) return

        val allStats = listOf(Stats.HP, Stats.ATTACK, Stats.DEFENCE, Stats.SPECIAL_ATTACK, Stats.SPECIAL_DEFENCE, Stats.SPEED)
        // Sélectionner [ivCount] stats aléatoires et les mettre à 31
        val shuffled = allStats.shuffled()
        val guaranteed = shuffled.take(ivCount)

        guaranteed.forEach { stat ->
            pokemon.setIV(stat, 31)
        }
    }

    // ─── Red Moon ────────────────────────────────────────────────────────────

    private fun applyRedMoon(pokemon: Pokemon) {
        // Légère chance de shiny pour les types nocturnes
        if (pokemon.shiny) return
        val isNocturnal = pokemon.species.types.any { type ->
            type.name.lowercase() in NOCTURNAL_TYPES
        }
        if (isNocturnal) {
            // Taux x2 pour les types dark/ghost
            val effectiveRate = BASE_SHINY_RATE * 2f
            if (Math.random().toFloat() < effectiveRate) {
                pokemon.shiny = true
            }
        }
    }
}
