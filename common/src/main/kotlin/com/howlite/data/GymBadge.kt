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

    ZEPHYR_BADGE("zephyr_badge", 20, GymRegion.JOHTO, "textures/item/johto/johto_zephyr_badge.png", "textures/item/johto/johto_zephyr_badge_hollow.png"),
    HIVE_BADGE("hive_badge", 30, GymRegion.JOHTO, "textures/item/johto/johto_hive_badge.png", "textures/item/johto/johto_hive_badge_hollow.png"),
    PLAIN_BADGE("plain_badge", 40, GymRegion.JOHTO, "textures/item/johto/johto_plain_badge.png", "textures/item/johto/johto_plain_badge_hollow.png"),
    FOG_BADGE("fog_badge", 50, GymRegion.JOHTO, "textures/item/johto/johto_fog_badge.png", "textures/item/johto/johto_fog_badge_hollow.png"),
    STORM_BADGE("storm_badge", 60, GymRegion.JOHTO, "textures/item/johto/johto_storm_badge.png", "textures/item/johto/johto_storm_badge_hollow.png"),
    MINERAL_BADGE("mineral_badge", 70, GymRegion.JOHTO, "textures/item/johto/johto_mineral_badge.png", "textures/item/johto/johto_mineral_badge_hollow.png"),
    GLACIER_BADGE("glacier_badge", 80, GymRegion.JOHTO, "textures/item/johto/johto_glacier_badge.png", "textures/item/johto/johto_glacier_badge_hollow.png"),
    RISING_BADGE("rising_badge", 100, GymRegion.JOHTO, "textures/item/johto/johto_rising_badge.png", "textures/item/johto/johto_rising_badge_hollow.png"),

    // Hoenn Badges
    STONE_BADGE("stone_badge", 20, GymRegion.HOENN, "textures/item/badge_placeholder/stone_hoenn_badge.png", "textures/item/badge_placeholder/stone_hoenn_badge.png"),
    KNUCKLE_BADGE("knuckle_badge", 30, GymRegion.HOENN, "textures/item/badge_placeholder/knuckle_hoenn_badge.png", "textures/item/badge_placeholder/knuckle_hoenn_badge.png"),
    DYNAMO_BADGE("dynamo_badge", 40, GymRegion.HOENN, "textures/item/badge_placeholder/dynamo_hoenn_badge.png", "textures/item/badge_placeholder/dynamo_hoenn_badge.png"),
    HEAT_BADGE("heat_badge", 50, GymRegion.HOENN, "textures/item/badge_placeholder/heat_hoenn_badge.png", "textures/item/badge_placeholder/heat_hoenn_badge.png"),
    BALANCE_BADGE("balance_badge", 60, GymRegion.HOENN, "textures/item/badge_placeholder/balance_hoenn_badge.png", "textures/item/badge_placeholder/balance_hoenn_badge.png"),
    FEATHER_BADGE("feather_badge", 70, GymRegion.HOENN, "textures/item/badge_placeholder/feather_hoenn_badge.png", "textures/item/badge_placeholder/feather_hoenn_badge.png"),
    MIND_BADGE("mind_badge", 80, GymRegion.HOENN, "textures/item/badge_placeholder/mind_hoenn_badge.png", "textures/item/badge_placeholder/mind_hoenn_badge.png"),
    RAIN_BADGE("rain_badge", 100, GymRegion.HOENN, "textures/item/badge_placeholder/rain_hoenn_badge.png", "textures/item/badge_placeholder/rain_hoenn_badge.png"),

    // Sinnoh Badges
    COAL_BADGE("coal_badge", 20, GymRegion.SINNOH, "textures/item/badge_placeholder/coal_sinnoh_badge.png", "textures/item/badge_placeholder/coal_sinnoh_badge.png"),
    FOREST_BADGE("forest_badge", 30, GymRegion.SINNOH, "textures/item/badge_placeholder/forest_sinnoh_badge.png", "textures/item/badge_placeholder/forest_sinnoh_badge.png"),
    COBBLE_BADGE("cobble_badge", 40, GymRegion.SINNOH, "textures/item/badge_placeholder/cobble_sinnoh_badge.png", "textures/item/badge_placeholder/cobble_sinnoh_badge.png"),
    FEN_BADGE("fen_badge", 50, GymRegion.SINNOH, "textures/item/badge_placeholder/fen_sinnoh_badge.png", "textures/item/badge_placeholder/fen_sinnoh_badge.png"),
    RELIC_BADGE("relic_badge", 60, GymRegion.SINNOH, "textures/item/badge_placeholder/relic_sinnoh_badge.png", "textures/item/badge_placeholder/relic_sinnoh_badge.png"),
    MINE_BADGE("mine_badge", 70, GymRegion.SINNOH, "textures/item/badge_placeholder/mine_sinnoh_badge.png", "textures/item/badge_placeholder/mine_sinnoh_badge.png"),
    ICICLE_BADGE("icicle_badge", 80, GymRegion.SINNOH, "textures/item/badge_placeholder/icicle_sinnoh_badge.png", "textures/item/badge_placeholder/icicle_sinnoh_badge.png"),
    BEACON_BADGE("beacon_badge", 100, GymRegion.SINNOH, "textures/item/badge_placeholder/beacon_sinnoh_badge.png", "textures/item/badge_placeholder/beacon_sinnoh_badge.png"),

    // Unova Badges
    TRIO_BADGE("trio_badge", 20, GymRegion.UNOVA, "textures/item/badge_placeholder/trio_unova_badge.png", "textures/item/badge_placeholder/trio_unova_badge.png"),
    BASIC_BADGE("basic_badge", 25, GymRegion.UNOVA, "textures/item/badge_placeholder/basic_unova_badge.png", "textures/item/badge_placeholder/basic_unova_badge.png"),
    TOXIC_BADGE("toxic_badge", 30, GymRegion.UNOVA, "textures/item/badge_placeholder/toxic_unova_badge.png", "textures/item/badge_placeholder/toxic_unova_badge.png"),
    INSECT_BADGE("insect_badge", 40, GymRegion.UNOVA, "textures/item/badge_placeholder/insect_unova_badge.png", "textures/item/badge_placeholder/insect_unova_badge.png"),
    BOLT_BADGE("bolt_badge", 50, GymRegion.UNOVA, "textures/item/badge_placeholder/bolt_unova_badge.png", "textures/item/badge_placeholder/bolt_unova_badge.png"),
    QUAKE_BADGE("quake_badge", 60, GymRegion.UNOVA, "textures/item/badge_placeholder/quake_unova_badge.png", "textures/item/badge_placeholder/quake_unova_badge.png"),
    JET_BADGE("jet_badge", 70, GymRegion.UNOVA, "textures/item/badge_placeholder/jet_unova_badge.png", "textures/item/badge_placeholder/jet_unova_badge.png"),
    FREEZE_BADGE("freeze_badge", 80, GymRegion.UNOVA, "textures/item/badge_placeholder/freeze_unova_badge.png", "textures/item/badge_placeholder/freeze_unova_badge.png"),
    LEGEND_BADGE("legend_badge", 90, GymRegion.UNOVA, "textures/item/badge_placeholder/legend_unova_badge.png", "textures/item/badge_placeholder/legend_unova_badge.png"),
    WAVE_BADGE("wave_badge", 100, GymRegion.UNOVA, "textures/item/badge_placeholder/wave_unova_badge.png", "textures/item/badge_placeholder/wave_unova_badge.png"),

    // Kalos Badges
    BUG_BADGE("bug_badge", 20, GymRegion.KALOS, "textures/item/badge_placeholder/bug_kalos_badge.png", "textures/item/badge_placeholder/bug_kalos_badge.png"),
    CLIFF_BADGE("cliff_badge", 30, GymRegion.KALOS, "textures/item/badge_placeholder/cliff_kalos_badge.png", "textures/item/badge_placeholder/cliff_kalos_badge.png"),
    RUMBLE_BADGE("rumble_badge", 40, GymRegion.KALOS, "textures/item/badge_placeholder/rumble_kalos_badge.png", "textures/item/badge_placeholder/rumble_kalos_badge.png"),
    PLANT_BADGE("plant_badge", 50, GymRegion.KALOS, "textures/item/badge_placeholder/plant_kalos_badge.png", "textures/item/badge_placeholder/plant_kalos_badge.png"),
    VOLTAGE_BADGE("voltage_badge", 60, GymRegion.KALOS, "textures/item/badge_placeholder/voltage_kalos_badge.png", "textures/item/badge_placeholder/voltage_kalos_badge.png"),
    KALOS_FAIRY_BADGE("kalos_fairy_badge", 70, GymRegion.KALOS, "textures/item/badge_placeholder/fairy_kalos_badge.png", "textures/item/badge_placeholder/fairy_kalos_badge.png"),
    PSYCHIC_BADGE("psychic_badge", 80, GymRegion.KALOS, "textures/item/badge_placeholder/psychic_kalos_badge.png", "textures/item/badge_placeholder/psychic_kalos_badge.png"),
    ICEBERG_BADGE("iceberg_badge", 100, GymRegion.KALOS, "textures/item/badge_placeholder/iceberg_kalos_badge.png", "textures/item/badge_placeholder/iceberg_kalos_badge.png"),

    // Alola Grand Trials (Trial Stamps)
    MELEMELE_STAMP("melemele_stamp", 25, GymRegion.ALOLA, "textures/item/gym_ticket_placeholder.png", "textures/item/gym_ticket_placeholder.png"),
    AKALA_STAMP("akala_stamp", 50, GymRegion.ALOLA, "textures/item/gym_ticket_placeholder.png", "textures/item/gym_ticket_placeholder.png"),
    ULAULA_STAMP("ulaula_stamp", 75, GymRegion.ALOLA, "textures/item/gym_ticket_placeholder.png", "textures/item/gym_ticket_placeholder.png"),
    PONI_STAMP("poni_stamp", 100, GymRegion.ALOLA, "textures/item/gym_ticket_placeholder.png", "textures/item/gym_ticket_placeholder.png"),

    // Galar Badges
    GRASS_BADGE("grass_badge", 20, GymRegion.GALAR, "textures/item/badge_placeholder/grass_galar_badge.png", "textures/item/badge_placeholder/grass_galar_badge.png"),
    WATER_BADGE("water_badge", 30, GymRegion.GALAR, "textures/item/badge_placeholder/water_galar_badge.png", "textures/item/badge_placeholder/water_galar_badge.png"),
    FIRE_BADGE("fire_badge", 40, GymRegion.GALAR, "textures/item/badge_placeholder/fire_galar_badge.png", "textures/item/badge_placeholder/fire_galar_badge.png"),
    FIGHTING_BADGE("fighting_badge", 50, GymRegion.GALAR, "textures/item/badge_placeholder/fighting_galar_badge.png", "textures/item/badge_placeholder/fighting_galar_badge.png"),
    GHOST_BADGE("ghost_badge", 50, GymRegion.GALAR, "textures/item/badge_placeholder/ghost_galar_badge.png", "textures/item/badge_placeholder/ghost_galar_badge.png"),
    GALAR_FAIRY_BADGE("galar_fairy_badge", 60, GymRegion.GALAR, "textures/item/badge_placeholder/fairy_galar_badge.png", "textures/item/badge_placeholder/fairy_galar_badge.png"),
    ROCK_BADGE("rock_badge", 70, GymRegion.GALAR, "textures/item/badge_placeholder/rock_galar_badge.png", "textures/item/badge_placeholder/rock_galar_badge.png"),
    ICE_BADGE("ice_badge", 70, GymRegion.GALAR, "textures/item/badge_placeholder/ice_galar_badge.png", "textures/item/badge_placeholder/ice_galar_badge.png"),
    DARK_BADGE("dark_badge", 80, GymRegion.GALAR, "textures/item/badge_placeholder/dark_galar_badge.png", "textures/item/badge_placeholder/dark_galar_badge.png"),
    DRAGON_BADGE("dragon_badge", 100, GymRegion.GALAR, "textures/item/badge_placeholder/dragon_galar_badge.png", "textures/item/badge_placeholder/dragon_galar_badge.png"),

    // Paldea Badges
    CORTONDO_BADGE("cortondo_badge", 20, GymRegion.PALDEA, "textures/item/badge_placeholder/bug_paldea_badge.png", "textures/item/badge_placeholder/bug_paldea_badge.png"),
    ARTAZON_BADGE("artazon_badge", 30, GymRegion.PALDEA, "textures/item/badge_placeholder/grass_paldea_badge.png", "textures/item/badge_placeholder/grass_paldea_badge.png"),
    LEVINCIA_BADGE("levincia_badge", 40, GymRegion.PALDEA, "textures/item/badge_placeholder/electric_paldea_badge.png", "textures/item/badge_placeholder/electric_paldea_badge.png"),
    CASCARRAFA_BADGE("cascarrafa_badge", 50, GymRegion.PALDEA, "textures/item/badge_placeholder/water_paldea_badge.png", "textures/item/badge_placeholder/water_paldea_badge.png"),
    MEDALI_BADGE("medali_badge", 60, GymRegion.PALDEA, "textures/item/badge_placeholder/normal_paldea_badge.png", "textures/item/badge_placeholder/normal_paldea_badge.png"),
    MONTENEVERA_BADGE("montenevera_badge", 70, GymRegion.PALDEA, "textures/item/badge_placeholder/ghost_paldea_badge.png", "textures/item/badge_placeholder/ghost_paldea_badge.png"),
    ALFORNADA_BADGE("alfornada_badge", 80, GymRegion.PALDEA, "textures/item/badge_placeholder/psychic_paldea_badge.png", "textures/item/badge_placeholder/psychic_paldea_badge.png"),
    GLASEADO_BADGE("glaseado_badge", 100, GymRegion.PALDEA, "textures/item/badge_placeholder/ice_paldea_badge.png", "textures/item/badge_placeholder/ice_paldea_badge.png");

    companion object {
        /**
         * Retrouve un badge par son identifiant de chaîne.
         * Retourne null si l'ID est inconnu.
         */
        fun fromId(id: String): GymBadge? = entries.find { it.id == id }
    }
}
