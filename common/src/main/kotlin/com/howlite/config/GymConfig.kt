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
        ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "clair")    to GymBadge.RISING_BADGE
    )
}
