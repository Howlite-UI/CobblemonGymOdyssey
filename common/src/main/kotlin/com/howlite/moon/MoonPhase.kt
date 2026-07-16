package com.howlite.moon

/**
 * Phases lunaires personnalisées du mod CobblemonGymOdyssey.
 */
enum class MoonPhase(
    val tintR: Float,
    val tintG: Float,
    val tintB: Float,
    val displayName: String,
    val descriptionKey: String
) {
    NONE(
        tintR        = 1.0f,
        tintG        = 1.0f,
        tintB        = 1.0f,
        displayName  = "cobblemongymodyssey.moon.phase.none",
        descriptionKey = "cobblemongymodyssey.moon.phase.none.desc"
    ),

    BLUE_MOON(
        tintR        = 0.416f,
        tintG        = 0.678f,
        tintB        = 1.0f,
        displayName  = "cobblemongymodyssey.moon.phase.blue_moon",
        descriptionKey = "cobblemongymodyssey.moon.phase.blue_moon.desc"
    ),

    RED_MOON(
        tintR        = 1.0f,
        tintG        = 0.227f,
        tintB        = 0.102f,
        displayName  = "cobblemongymodyssey.moon.phase.red_moon",
        descriptionKey = "cobblemongymodyssey.moon.phase.red_moon.desc"
    ),

    PURPLE_MOON(
        tintR        = 0.608f,
        tintG        = 0.247f,
        tintB        = 1.0f,
        displayName  = "cobblemongymodyssey.moon.phase.purple_moon",
        descriptionKey = "cobblemongymodyssey.moon.phase.purple_moon.desc"
    ),

    FULL_MOON(
        tintR        = 1.0f,
        tintG        = 0.980f,
        tintB        = 0.804f,
        displayName  = "cobblemongymodyssey.moon.phase.full_moon",
        descriptionKey = "cobblemongymodyssey.moon.phase.full_moon.desc"
    );

    val isSpecial: Boolean get() = this != NONE

    val nightNotificationKey: String?
        get() = if (isSpecial) "cobblemongymodyssey.moon.notification.$name" else null

    companion object {
        /** Multiplicateur shiny Blue Moon. */
        const val SHINY_MULTIPLIER_BLUE_MOON = 5f
        /** Nombre d'IVs garantis Full Moon. */
        const val IV_GUARANTEED_COUNT = 3
        /** Multiplicateur EXP Purple Moon. */
        const val EXP_MULTIPLIER_PURPLE = 1.5f

        fun fromName(name: String): MoonPhase =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: NONE
    }
}
