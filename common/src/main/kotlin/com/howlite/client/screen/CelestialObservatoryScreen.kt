package com.howlite.client.screen

import com.howlite.client.moon.ClientMoonState
import com.howlite.moon.MoonPhase
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

/**
 * Écran GUI de l'Observatoire Céleste.
 *
 * ## Contenu affiché
 * - **En-tête** : Nom de l'observatoire avec icône de lune.
 * - **Phase actuelle** : Nom traduit de la phase, avec couleur thématique.
 * - **Effets actifs** : Description des effets de gameplay en cours.
 * - **Prochaine phase** : Aperçu de la prochaine phase spéciale.
 * - **Indicateur visuel** : Cercle coloré représentant la teinte de la lune.
 *
 * ## Design
 * Fenêtre centrée, fond semi-transparent, style "parchemin astronomique".
 * Utilise les couleurs de [MoonPhase] pour une cohérence visuelle.
 */
class CelestialObservatoryScreen : Screen(Component.translatable("cobblemongymodyssey.gui.celestial_observatory.title")) {

    companion object {
        private const val GUI_WIDTH  = 240
        private const val GUI_HEIGHT = 180

        /** Couleur de fond du panel principal (ARGB). */
        private const val BG_COLOR      = 0xD0050510.toInt()
        /** Couleur de la bordure. */
        private const val BORDER_COLOR  = 0xFF8040C0.toInt()
        /** Couleur du texte secondaire. */
        private const val TEXT_SECONDARY = 0xAAAAAA
    }

    private var guiLeft = 0
    private var guiTop  = 0

    override fun init() {
        super.init()
        guiLeft = (width  - GUI_WIDTH)  / 2
        guiTop  = (height - GUI_HEIGHT) / 2
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        // Fond foncé de l'écran
        renderBackground(graphics, mouseX, mouseY, partialTick)

        val x = guiLeft
        val y = guiTop

        // ── Panel principal ───────────────────────────────────────────────
        graphics.fill(x, y, x + GUI_WIDTH, y + GUI_HEIGHT, BG_COLOR)

        // Bordures décoratives
        drawBorders(graphics, x, y)

        // ── Titre ─────────────────────────────────────────────────────────
        val titleText = Component.literal("✦ ").append(title).append(Component.literal(" ✦"))
        graphics.drawCenteredString(font, titleText, x + GUI_WIDTH / 2, y + 10, 0xCCA0FF)

        // Séparateur
        graphics.fill(x + 10, y + 22, x + GUI_WIDTH - 10, y + 23, BORDER_COLOR)

        // ── Phase actuelle ────────────────────────────────────────────────
        val current = ClientMoonState.currentPhase
        val (tr, tg, tb) = ClientMoonState.getCurrentTint()
        val phaseColor = packColor(tr, tg, tb)

        // Cercle indicateur de phase (simulé avec des pixels)
        drawMoonCircle(graphics, x + 20, y + 40, current, tr, tg, tb)

        // Texte de phase
        graphics.drawString(font,
            Component.literal("§7Phase active :"), x + 55, y + 32, TEXT_SECONDARY, false)
        graphics.drawString(font,
            Component.translatable(current.displayName),
            x + 55, y + 44, phaseColor, false)

        // Séparateur fin
        graphics.fill(x + 10, y + 62, x + GUI_WIDTH - 10, y + 63, 0x55FFFFFF)

        // ── Effets actifs ─────────────────────────────────────────────────
        graphics.drawString(font,
            Component.literal("§7Effets cette nuit :"), x + 12, y + 70, TEXT_SECONDARY, false)

        val effectLines = getEffectLines(current)
        effectLines.forEachIndexed { i, line ->
            graphics.drawString(font, line, x + 16, y + 82 + i * 12, 0xDDDDDD, false)
        }

        // Séparateur
        val separatorY = y + 82 + effectLines.size * 12 + 4
        graphics.fill(x + 10, separatorY, x + GUI_WIDTH - 10, separatorY + 1, 0x55FFFFFF)

        // ── Prochaine phase ───────────────────────────────────────────────
        val next = com.howlite.moon.MoonManager.nextPhase
        val nextColor = packColor(next.tintR, next.tintG, next.tintB)

        graphics.drawString(font,
            Component.literal("§7Prochaine phase :"), x + 12, separatorY + 8, TEXT_SECONDARY, false)
        graphics.drawString(font,
            Component.literal("➤ ").append(Component.translatable(next.displayName)),
            x + 16, separatorY + 20, nextColor, false)

        // Footer
        graphics.drawCenteredString(font,
            Component.literal("§8[Clic droit pour fermer]"),
            x + GUI_WIDTH / 2, y + GUI_HEIGHT - 14, 0x444444)

        super.render(graphics, mouseX, mouseY, partialTick)
    }

    // ─── Utilitaires de rendu ─────────────────────────────────────────────

    private fun drawBorders(graphics: GuiGraphics, x: Int, y: Int) {
        // Coin supérieur gauche / inférieur droit
        graphics.fill(x,                   y,                    x + GUI_WIDTH,     y + 2,         BORDER_COLOR)
        graphics.fill(x,                   y + GUI_HEIGHT - 2,   x + GUI_WIDTH,     y + GUI_HEIGHT, BORDER_COLOR)
        graphics.fill(x,                   y,                    x + 2,             y + GUI_HEIGHT, BORDER_COLOR)
        graphics.fill(x + GUI_WIDTH - 2,   y,                    x + GUI_WIDTH,     y + GUI_HEIGHT, BORDER_COLOR)
    }

    /** Dessine un "cercle" de lune (approximé par des pixels fillés). */
    private fun drawMoonCircle(
        graphics: GuiGraphics, cx: Int, cy: Int,
        phase: MoonPhase,
        r: Float, g: Float, b: Float
    ) {
        val radius = 14
        val color = if (phase.isSpecial) packColor(r, g, b) else 0xFFDDDDDD.toInt()
        val outerColor = packColor(r * 0.5f, g * 0.5f, b * 0.5f).and(0xAAFFFFFF.toInt())

        // Cercle simple par raster
        for (dy in -radius..radius) {
            for (dx in -radius..radius) {
                val dist = dx * dx + dy * dy
                if (dist <= radius * radius) {
                    val c = if (dist > (radius - 3) * (radius - 3)) outerColor else color
                    graphics.fill(cx + dx, cy + dy, cx + dx + 1, cy + dy + 1, c)
                }
            }
        }
    }

    private fun getEffectLines(phase: MoonPhase): List<Component> {
        return when (phase) {
            MoonPhase.BLUE_MOON   -> listOf(
                Component.literal("✦ Shiny ×${com.howlite.moon.MoonConfig.instance.shinyMultiplierBlueMoon.toInt()} lors du spawn")
            )
            MoonPhase.RED_MOON    -> listOf(
                Component.literal("✦ Spawn types Dark/Ghost ×2"),
                Component.literal("✦ Léger boost shiny Nocturnes")
            )
            MoonPhase.PURPLE_MOON -> listOf(
                Component.literal("✦ EXP Cobblemon ×${com.howlite.moon.MoonConfig.instance.expMultiplierPurpleMoon}")
            )
            MoonPhase.FULL_MOON   -> listOf(
                Component.literal("✦ ${com.howlite.moon.MoonConfig.instance.ivCountFullMoon} IVs garantis à 31 au spawn")
            )
            MoonPhase.NONE        -> listOf(
                Component.literal("§8Nuit ordinaire — aucun effet spécial.")
            )
        }
    }

    /** Convertit des composantes RGB [0..1] en int ARGB 0xFF______. */
    private fun packColor(r: Float, g: Float, b: Float): Int {
        val ri = (r * 255).toInt().coerceIn(0, 255)
        val gi = (g * 255).toInt().coerceIn(0, 255)
        val bi = (b * 255).toInt().coerceIn(0, 255)
        return 0xFF000000.toInt() or (ri shl 16) or (gi shl 8) or bi
    }

    override fun isPauseScreen(): Boolean = false
    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (keyCode == 256) { // ESC
            onClose()
            return true
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }
}
