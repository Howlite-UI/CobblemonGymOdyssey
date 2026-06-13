package com.howlite.data

/**
 * Représente un badge d'Arène avec son identifiant unique et le Level Cap
 * qu'il débloque pour le dresseur.
 *
 * Le [levelCap] correspond au niveau maximum que peuvent atteindre les Pokémon
 * du joueur après avoir remporté ce badge.
 */
enum class GymRegion {
    KANTO,
    JOHTO,
    HOENN,
    SINNOH,
    UNOVA,
    KALOS,
    ALOLA,
    GALAR,
    PALDEA
}

/**
 * Représente un badge d'Arène avec son identifiant unique et le Level Cap
 * qu'il débloque pour le dresseur.
 *
 * Le [levelCap] correspond au niveau maximum que peuvent atteindre les Pokémon
 * du joueur après avoir remporté ce badge.
 */
enum class GymBadge(
    val id: String,
    val levelCap: Int,
    val region: GymRegion,
    val texturePath: String
) {
    BOULDER_BADGE("boulder_badge", 20, GymRegion.KANTO, "textures/item/boulder_badge.png"),
    CASCADE_BADGE("cascade_badge", 30, GymRegion.KANTO, "textures/item/cascade_badge.png"),
    THUNDER_BADGE("thunder_badge", 40, GymRegion.KANTO, "textures/item/thunder_badge.png"),
    RAINBOW_BADGE("rainbow_badge", 50, GymRegion.KANTO, "textures/item/rainbow_badge.png"),
    SOUL_BADGE("soul_badge", 60, GymRegion.KANTO, "textures/item/soul_badge.png"),
    MARSH_BADGE("marsh_badge", 70, GymRegion.KANTO, "textures/item/marsh_badge.png"),
    VOLCANO_BADGE("volcano_badge", 80, GymRegion.KANTO, "textures/item/volcano_badge.png"),
    EARTH_BADGE("earth_badge", 100, GymRegion.KANTO, "textures/item/earth_badge.png"),

    ZEPHYR_BADGE("zephyr_badge", 0, GymRegion.JOHTO, "textures/item/badge_placeholder/zephyr_johto_badge.png"),
    HIVE_BADGE("hive_badge", 0, GymRegion.JOHTO, "textures/item/badge_placeholder/hive_johto_badge.png"),
    PLAIN_BADGE("plain_badge", 0, GymRegion.JOHTO, "textures/item/badge_placeholder/plain_johto_badge.png"),
    FOG_BADGE("fog_badge", 0, GymRegion.JOHTO, "textures/item/badge_placeholder/fog_johto_badge.png"),
    STORM_BADGE("storm_badge", 0, GymRegion.JOHTO, "textures/item/badge_placeholder/storm_johto_badge.png"),
    MINERAL_BADGE("mineral_badge", 0, GymRegion.JOHTO, "textures/item/badge_placeholder/mineral_johto_badge.png"),
    GLACIER_BADGE("glacier_badge", 0, GymRegion.JOHTO, "textures/item/badge_placeholder/glacier_johto_badge.png"),
    RISING_BADGE("rising_badge", 0, GymRegion.JOHTO, "textures/item/badge_placeholder/rising_johto_badge.png");

    companion object {
        /**
         * Retrouve un badge par son identifiant de chaîne.
         * Retourne null si l'ID est inconnu.
         */
        fun fromId(id: String): GymBadge? = entries.find { it.id == id }
    }
}
