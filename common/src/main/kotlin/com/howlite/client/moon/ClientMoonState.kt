package com.howlite.client.moon

import com.howlite.moon.MoonConfig
import com.howlite.moon.MoonPhase

/**
 * État de la phase lunaire côté client.
 *
 * Reçoit les mises à jour depuis [com.howlite.moon.MoonNetwork] et gère
 * la **transition de couleur progressive** (lerp) entre la couleur blanche
 * (lune vanilla) et la couleur de la phase active.
 *
 * ## Transition
 * Quand une nouvelle phase spéciale est reçue, [tintProgress] passe de 0→1
 * sur [MoonConfig.instance.transitionDurationTicks] ticks client.
 * Lorsque la phase revient à NONE, la transition s'inverse (1→0).
 *
 * ## Thread safety
 * Toutes les mutations se font sur le thread de rendu Minecraft.
 * Les lectures depuis [MoonTintRenderer] sont sûres car sur le même thread.
 */
object ClientMoonState {

    /** Phase actuellement active côté client. */
    var currentPhase: MoonPhase = MoonPhase.NONE
        private set

    /**
     * Progression de la transition de teinte [0.0 .. 1.0].
     * 0 = blanc pur (vanilla), 1 = teinte complète de la phase.
     */
    var tintProgress: Float = 0f
        private set

    /** Direction de la transition : +1 vers la couleur cible, -1 vers blanc. */
    private var tintDirection: Float = 0f

    // ─── API publique ─────────────────────────────────────────────────────

    /**
     * Appelé depuis [com.howlite.moon.MoonNetwork] lorsqu'un paquet S2C arrive.
     * Lance la transition vers la nouvelle phase.
     */
    fun applyPhase(phase: MoonPhase) {
        if (currentPhase == phase) return
        currentPhase = phase
        tintDirection = if (phase.isSpecial) 1f else -1f
    }

    /**
     * Appelé chaque tick client (depuis [MoonTintRenderer] ou un TickEvent).
     * Avance la progression de la transition.
     */
    fun tick() {
        val duration = MoonConfig.instance.transitionDurationTicks.toFloat()
        val delta = 1f / duration.coerceAtLeast(1f)

        tintProgress = (tintProgress + tintDirection * delta).coerceIn(0f, 1f)
    }

    /**
     * Retourne la teinte RGB interpolée courante.
     *
     * @return Triple(R, G, B) dans [0.0 .. 1.0].
     *         Quand [tintProgress] == 0 → blanc (1, 1, 1).
     *         Quand [tintProgress] == 1 → couleur cible de [currentPhase].
     */
    fun getCurrentTint(): Triple<Float, Float, Float> {
        val t = tintProgress
        val phase = currentPhase

        // Teinte cible de la phase (blanc pour NONE)
        val targetR = phase.tintR
        val targetG = phase.tintG
        val targetB = phase.tintB

        // Lerp linéaire depuis blanc vers la couleur cible
        val r = 1f + (targetR - 1f) * t
        val g = 1f + (targetG - 1f) * t
        val b = 1f + (targetB - 1f) * t

        return Triple(r, g, b)
    }

    /**
     * Réinitialise l'état (utile lors de la déconnexion).
     */
    fun reset() {
        currentPhase = MoonPhase.NONE
        tintProgress = 0f
        tintDirection = 0f
    }
}
