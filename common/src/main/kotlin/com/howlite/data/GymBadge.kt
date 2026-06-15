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
    val texturePath: String,
    val hollowTexturePath: String
) {
    BOULDER_BADGE("boulder_badge", 20, GymRegion.KANTO, "textures/item/indigo/boulder_badge.png", "textures/item/indigo/hollow_boulder_badge.png"),
    CASCADE_BADGE("cascade_badge", 30, GymRegion.KANTO, "textures/item/indigo/cascade_badge.png", "textures/item/indigo/hollow_cascade_badge.png"),
    THUNDER_BADGE("thunder_badge", 40, GymRegion.KANTO, "textures/item/indigo/thunder_badge.png", "textures/item/indigo/hollow_thunder_badge.png"),
    RAINBOW_BADGE("rainbow_badge", 50, GymRegion.KANTO, "textures/item/indigo/rainbow_badge.png", "textures/item/indigo/hollow_rainbow_badge.png"),
    SOUL_BADGE("soul_badge", 60, GymRegion.KANTO, "textures/item/indigo/soul_badge.png", "textures/item/indigo/hollow_soul_badge.png"),
    MARSH_BADGE("marsh_badge", 70, GymRegion.KANTO, "textures/item/indigo/marsh_badge.png", "textures/item/indigo/hollow_marsh_badge.png"),
    VOLCANO_BADGE("volcano_badge", 80, GymRegion.KANTO, "textures/item/indigo/volcano_badge.png", "textures/item/indigo/hollow_volcano_badge.png"),
    EARTH_BADGE("earth_badge", 100, GymRegion.KANTO, "textures/item/indigo/earth_badge.png", "textures/item/indigo/hollow_earth_badge.png"),

    ZEPHYR_BADGE("zephyr_badge", 0, GymRegion.JOHTO, "textures/item/johto/johto_zephyr_badge.png", "textures/item/johto/johto_zephyr_badge_hollow.png"),
    HIVE_BADGE("hive_badge", 0, GymRegion.JOHTO, "textures/item/johto/johto_hive_badge.png", "textures/item/johto/johto_hive_badge_hollow.png"),
    PLAIN_BADGE("plain_badge", 0, GymRegion.JOHTO, "textures/item/johto/johto_plain_badge.png", "textures/item/johto/johto_plain_badge_hollow.png"),
    FOG_BADGE("fog_badge", 0, GymRegion.JOHTO, "textures/item/johto/johto_fog_badge.png", "textures/item/johto/johto_fog_badge_hollow.png"),
    STORM_BADGE("storm_badge", 0, GymRegion.JOHTO, "textures/item/johto/johto_storm_badge.png", "textures/item/johto/johto_storm_badge_hollow.png"),
    MINERAL_BADGE("mineral_badge", 0, GymRegion.JOHTO, "textures/item/johto/johto_mineral_badge.png", "textures/item/johto/johto_mineral_badge_hollow.png"),
    GLACIER_BADGE("glacier_badge", 0, GymRegion.JOHTO, "textures/item/johto/johto_glacier_badge.png", "textures/item/johto/johto_glacier_badge_hollow.png"),
    RISING_BADGE("rising_badge", 0, GymRegion.JOHTO, "textures/item/johto/johto_rising_badge.png", "textures/item/johto/johto_rising_badge_hollow.png");

    companion object {
        /**
         * Retrouve un badge par son identifiant de chaîne.
         * Retourne null si l'ID est inconnu.
         */
        fun fromId(id: String): GymBadge? = entries.find { it.id == id }
    }
}
