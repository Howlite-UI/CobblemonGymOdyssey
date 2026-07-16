package com.howlite.moon

import com.google.gson.GsonBuilder
import com.howlite.CobblemonGymOdyssey
import java.io.File

/**
 * Configuration du système de phases lunaires.
 *
 * Chargée depuis `config/cobblemongymodyssey_moon.json` au démarrage du serveur.
 * Si le fichier n'existe pas, il est créé avec les valeurs par défaut.
 *
 * ## Options disponibles
 * - [enableMoonRendering] : Active la teinte visuelle de la lune côté client.
 *   Mettre à `false` si vous utilisez un pack de shaders comme Complementary ou Iris
 *   qui gère déjà la lune visuellement.
 * - [cycleLengthDays] : Durée d'un cycle lunaire complet (en jours Minecraft).
 * - [shinyMultiplierBlueMoon] : Multiplicateur shiny pendant la Blue Moon.
 * - [ivCountFullMoon] : Nombre d'IVs garantis à 31 pendant la Full Moon.
 * - [notifyOnMoonChange] : Envoie un message global au chat au début de chaque nuit spéciale.
 * - [enableShaderCompatMode] : Si vrai, désactive automatiquement le rendu si Iris est détecté.
 * - [transitionDurationTicks] : Durée (en ticks client) de la transition de couleur (lerp).
 */
data class MoonConfig(
    val enableMoonRendering: Boolean = true,
    val cycleLengthDays: Int = 16,
    val shinyMultiplierBlueMoon: Float = 5f,
    val ivCountFullMoon: Int = 3,
    val expMultiplierPurpleMoon: Float = 1.5f,
    val notifyOnMoonChange: Boolean = true,
    val enableShaderCompatMode: Boolean = true,
    val transitionDurationTicks: Int = 1200   // 60 secondes
) {
    companion object {
        private val GSON = GsonBuilder().setPrettyPrinting().create()
        private val CONFIG_FILE = File("config/${CobblemonGymOdyssey.MOD_ID}_moon.json")

        @Volatile
        private var _instance: MoonConfig = MoonConfig()

        /** Instance courante de la configuration (thread-safe en lecture). */
        val instance: MoonConfig get() = _instance

        /**
         * Charge (ou crée) le fichier de configuration.
         * Doit être appelé depuis le thread serveur principal au démarrage.
         */
        fun load() {
            try {
                if (CONFIG_FILE.exists()) {
                    val text = CONFIG_FILE.readText(Charsets.UTF_8)
                    _instance = GSON.fromJson(text, MoonConfig::class.java) ?: MoonConfig()
                    println("[GymOdyssey/Moon] Configuration chargée depuis ${CONFIG_FILE.path}")
                } else {
                    save(MoonConfig()) // Crée le fichier avec les valeurs par défaut
                    println("[GymOdyssey/Moon] Fichier de configuration créé : ${CONFIG_FILE.path}")
                }
            } catch (e: Exception) {
                System.err.println("[GymOdyssey/Moon] Erreur lors du chargement de la config, utilisation des valeurs par défaut.")
                e.printStackTrace()
                _instance = MoonConfig()
            }
        }

        /** Sauvegarde une configuration sur le disque et met à jour l'instance courante. */
        fun save(config: MoonConfig) {
            try {
                CONFIG_FILE.parentFile?.mkdirs()
                CONFIG_FILE.writeText(GSON.toJson(config), Charsets.UTF_8)
                _instance = config
            } catch (e: Exception) {
                System.err.println("[GymOdyssey/Moon] Erreur lors de la sauvegarde de la config.")
                e.printStackTrace()
            }
        }
    }
}
