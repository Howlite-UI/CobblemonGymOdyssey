package com.howlite.data

/**
 * Représente un badge d'Arène avec son identifiant unique et le Level Cap
 * qu'il débloque pour le dresseur.
 *
 * Le [levelCap] correspond au niveau maximum que peuvent atteindre les Pokémon
 * du joueur après avoir remporté ce badge.
 */
enum class GymBadge(val id: String, val levelCap: Int) {
    BOULDER_BADGE("boulder_badge", 20),
    CASCADE_BADGE("cascade_badge", 30),
    THUNDER_BADGE("thunder_badge", 40),
    RAINBOW_BADGE("rainbow_badge", 50),
    SOUL_BADGE("soul_badge", 60),
    MARSH_BADGE("marsh_badge", 70),
    VOLCANO_BADGE("volcano_badge", 80),
    EARTH_BADGE("earth_badge", 100);

    companion object {
        /**
         * Retrouve un badge par son identifiant de chaîne.
         * Retourne null si l'ID est inconnu.
         */
        fun fromId(id: String): GymBadge? = entries.find { it.id == id }
    }
}
