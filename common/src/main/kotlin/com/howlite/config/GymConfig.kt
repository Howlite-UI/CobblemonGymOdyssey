package com.howlite.config

import com.howlite.data.GymBadge
import net.minecraft.resources.ResourceLocation

/**
 * Configuration statique du mapping Classe NPC → Badge.
 *
 * La clé de la map est le [ResourceLocation] de la **classe NPC** Cobblemon
 * (définie dans un datapack sous `data/<namespace>/npcs/<nom>.json`).
 * Cette approche est 100% cross-platform (pas de `persistentData` NeoForge-only).
 *
 * ## Comment créer un NPC Maître d'Arène avec la bonne classe NPC ?
 *
 * 1. Crée un fichier datapack `data/cobblemongymodyssey/npcs/brock.json` avec
 *    les attributs du NPC (skin, équipe, dialogue...).
 * 2. En jeu, le NPC spawné avec cette classe aura l'ID `cobblemongymodyssey:brock`.
 * 3. Ce mapping associe cet ID au [GymBadge.BOULDER_BADGE].
 *
 * Exemple de commande pour spawner un tel NPC en jeu :
 * ```
 * /cobblemon npc spawn cobblemongymodyssey:brock
 * ```
 *
 * TODO(future) : Charger ce mapping depuis un fichier de config TOML extensible.
 */
object GymConfig {

    /**
     * Associe l'identifiant de la classe NPC Cobblemon au badge qu'il accorde.
     *
     * Format : `ResourceLocation("cobblemongymodyssey", "brock")` = identifiant
     * du fichier `data/cobblemongymodyssey/npcs/brock.json` dans le datapack.
     */
    val gymLeaderToBadge: Map<ResourceLocation, GymBadge> = mapOf(
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "brock")    to GymBadge.BOULDER_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "misty")    to GymBadge.CASCADE_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "lt_surge") to GymBadge.THUNDER_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "erika")    to GymBadge.RAINBOW_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "koga")     to GymBadge.SOUL_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "sabrina")  to GymBadge.MARSH_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "blaine")   to GymBadge.VOLCANO_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "giovanni") to GymBadge.EARTH_BADGE,

        // Johto Gym Leaders
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "falkner")  to GymBadge.ZEPHYR_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "bugsy")    to GymBadge.HIVE_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "whitney")  to GymBadge.PLAIN_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "morty")    to GymBadge.FOG_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "chuck")    to GymBadge.STORM_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "jasmine")  to GymBadge.MINERAL_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "pryce")    to GymBadge.GLACIER_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "clair")    to GymBadge.RISING_BADGE,

        // Hoenn Gym Leaders
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "roxanne")  to GymBadge.STONE_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "brawly")   to GymBadge.KNUCKLE_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "wattson")  to GymBadge.DYNAMO_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "flannery") to GymBadge.HEAT_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "norman")   to GymBadge.BALANCE_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "winona")   to GymBadge.FEATHER_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "tate")     to GymBadge.MIND_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "liza")     to GymBadge.MIND_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "wallace")  to GymBadge.RAIN_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "juan")     to GymBadge.RAIN_BADGE,

        // Sinnoh Gym Leaders
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "roark")        to GymBadge.COAL_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "gardenia")     to GymBadge.FOREST_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "maylene")      to GymBadge.COBBLE_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "crasher_wake") to GymBadge.FEN_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "fantina")      to GymBadge.RELIC_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "byron")        to GymBadge.MINE_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "candice")      to GymBadge.ICICLE_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "volkner")      to GymBadge.BEACON_BADGE,

        // Unova Gym Leaders
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "cilan")   to GymBadge.TRIO_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "chili")   to GymBadge.TRIO_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "cress")   to GymBadge.TRIO_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "lenora")  to GymBadge.BASIC_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "cheren")  to GymBadge.BASIC_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "roxie")   to GymBadge.TOXIC_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "burgh")   to GymBadge.INSECT_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "elesa")   to GymBadge.BOLT_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "clay")    to GymBadge.QUAKE_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "skyla")   to GymBadge.JET_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "brycen")  to GymBadge.FREEZE_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "drayden") to GymBadge.LEGEND_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "iris")    to GymBadge.LEGEND_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "marlon")  to GymBadge.WAVE_BADGE,

        // Kalos Gym Leaders
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "viola")   to GymBadge.BUG_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "grant")   to GymBadge.CLIFF_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "korrina") to GymBadge.RUMBLE_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "ramos")   to GymBadge.PLANT_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "clemont") to GymBadge.VOLTAGE_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "valerie") to GymBadge.KALOS_FAIRY_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "olympia") to GymBadge.PSYCHIC_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "wulfric") to GymBadge.ICEBERG_BADGE,

        // Alola Kahunas
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "hala")   to GymBadge.MELEMELE_STAMP,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "olivia") to GymBadge.AKALA_STAMP,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "nanu")   to GymBadge.ULAULA_STAMP,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "hapu")   to GymBadge.PONI_STAMP,

        // Galar Gym Leaders
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "milo")     to GymBadge.GRASS_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "nessa")    to GymBadge.WATER_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "kabu")     to GymBadge.FIRE_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "bea")      to GymBadge.FIGHTING_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "allister") to GymBadge.GHOST_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "opal")     to GymBadge.GALAR_FAIRY_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "bede")     to GymBadge.GALAR_FAIRY_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "gordie")   to GymBadge.ROCK_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "melony")   to GymBadge.ICE_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "piers")    to GymBadge.DARK_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "marnie")   to GymBadge.DARK_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "raihan")   to GymBadge.DRAGON_BADGE,

        // Paldea Gym Leaders
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "katy")     to GymBadge.CORTONDO_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "brassius") to GymBadge.ARTAZON_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "iono")     to GymBadge.LEVINCIA_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "kofu")     to GymBadge.CASCARRAFA_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "larry")    to GymBadge.MEDALI_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "ryme")     to GymBadge.MONTENEVERA_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "tulip")    to GymBadge.ALFORNADA_BADGE,
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "grusha")   to GymBadge.GLASEADO_BADGE
    )
}
