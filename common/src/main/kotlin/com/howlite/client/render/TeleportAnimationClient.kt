package com.howlite.client.render

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.client.gui.drawProfilePokemon
import com.cobblemon.mod.common.client.render.models.blockbench.FloatingState
import com.cobblemon.mod.common.util.math.fromEulerXYZDegrees
import com.cobblemon.mod.common.entity.PoseType
import org.joml.Quaternionf
import org.joml.Vector3f

/**
 * Animation de tÃ©lÃ©portation style PokÃ©mon B&W "Fly Field Skill" avec effets "juicy".
 *
 * Phases ZOOM_UP (2000ms = 40 ticks serveur) :
 *  1. LAUNCH  (0â†’25%)  : CamÃ©ra recule derriÃ¨re le joueur/PokÃ©mon montÃ©, monte 8 blocs, pitch lÃ©gÃ¨re plongÃ©e.
 *  2. SOAR    (25â†’80%) : CamÃ©ra suit depuis l'arriÃ¨re-haut, lignes BG_line + poke_line dÃ©filent Ã  haute vitesse.
 *  3. FLASH   (80â†’100%): Flash blanc couvre l'Ã©cran â€” moment de la tÃ©lÃ©portation physique.
 *
 * Phases ZOOM_DOWN (1500ms = 30 ticks serveur) :
 *  1. ARRIVE  (0â†’20%)  : Flash blanc se dissipe, camÃ©ra Ã  haute altitude top-down.
 *  2. DESCENT (20â†’75%) : Lignes ralentissent puis s'effacent, camÃ©ra redescend en arc.
 *  3. LAND    (75â†’100%): Retour progressif Ã  la vue normale.
 */
object TeleportAnimationClient {

    // ------------------------------------------------------------------
    // Phase enum
    // ------------------------------------------------------------------

    enum class TeleportPhase { NONE, ZOOM_UP, ZOOM_DOWN }

    var phase: TeleportPhase = TeleportPhase.NONE
        private set

    // ------------------------------------------------------------------
    // Textures
    // ------------------------------------------------------------------

    private val BG_LINE = ResourceLocation.fromNamespaceAndPath(
        "cobblemongymodyssey", "textures/gui/teleport/bg_line.png"
    )
    private val POKE_LINE = ResourceLocation.fromNamespaceAndPath(
        "cobblemongymodyssey", "textures/gui/teleport/poke_line.png"
    )
    // bg_line.png : 128Ã—32  |  poke_line.png : 128Ã—64
    private const val BG_LINE_W   = 128
    private const val BG_LINE_H   = 32
    private const val POKE_LINE_W = 128
    private const val POKE_LINE_H = 64

    private val floatingState = FloatingState()

    // ------------------------------------------------------------------
    // Timing
    // ------------------------------------------------------------------

    private const val ZOOM_UP_MS   = 2000.0
    private const val ZOOM_DOWN_MS = 1500.0

    // ------------------------------------------------------------------
    // Camera constants
    // ------------------------------------------------------------------

    /** Hauteur de la camÃ©ra en phase LAUNCH (blocs au-dessus du joueur). */
    private const val PULL_HEIGHT = 8.0
    /** Hauteur de la camÃ©ra en phase SOAR / ARRIVE. */
    private const val SOAR_HEIGHT = 14.0
    /** Distance horizontale derriÃ¨re le joueur pendant SOAR. */
    private const val SOAR_DIST   = 10.0
    /** Pitch camÃ©ra en SOAR (lÃ©gÃ¨re plongÃ©e PokÃ©mon). */
    private const val SOAR_PITCH  = 28.0f

    // ------------------------------------------------------------------
    // State
    // ------------------------------------------------------------------

    private var animStartTime: Long  = 0L
    private var startPos:      Vec3  = Vec3.ZERO
    private var initialYaw:    Float = 0f
    private var initialPitch:  Float = 0f
    private var scrollOffset:  Double = 0.0
    private var lastRenderMs:  Long   = 0L

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    fun startZoomUp(x: Double, y: Double, z: Double) {
        val player = Minecraft.getInstance().player ?: return
        animStartTime = System.currentTimeMillis()
        scrollOffset  = 0.0
        lastRenderMs  = animStartTime
        phase         = TeleportPhase.ZOOM_UP
        startPos      = Vec3(x, y, z)
        initialYaw    = player.yRot
        initialPitch  = player.xRot
    }

    fun startZoomDown() {
        animStartTime = System.currentTimeMillis()
        scrollOffset  = 0.0
        lastRenderMs  = animStartTime
        phase         = TeleportPhase.ZOOM_DOWN
        Minecraft.getInstance().player?.let {
            initialPitch = it.xRot
            initialYaw   = it.yRot
        }
    }

    fun stop() { phase = TeleportPhase.NONE }

    // ------------------------------------------------------------------
    // Camera override (appelÃ© par CameraMixin chaque frame)
    // ------------------------------------------------------------------

    @JvmStatic
    fun calculateCameraModifiers(
        pos: DoubleArray, rot: FloatArray,
        @Suppress("UNUSED_PARAMETER") partialTicks: Float
    ): Boolean {
        if (phase == TeleportPhase.NONE) return false
        val player = Minecraft.getInstance().player ?: return false
        val elapsed = (System.currentTimeMillis() - animStartTime).toDouble()

        return when (phase) {
            TeleportPhase.ZOOM_UP   -> zoomUpCamera(pos, rot, elapsed)
            TeleportPhase.ZOOM_DOWN -> zoomDownCamera(pos, rot, elapsed, player.position(), player.yRot, player.xRot)
            TeleportPhase.NONE      -> false
        }
    }

    private fun zoomUpCamera(pos: DoubleArray, rot: FloatArray, elapsed: Double): Boolean {
        val p      = (elapsed / ZOOM_UP_MS).coerceIn(0.0, 1.0)
        val yawRad = Math.toRadians(initialYaw.toDouble())

        when {
            // â”€â”€ LAUNCH : remonte derriÃ¨re le joueur, pitch NORMAL â†’ SOAR_PITCH
            p <= 0.25 -> {
                val t      = smoothstep(p / 0.25)
                val dist   = SOAR_DIST * 0.4 * t
                val height = PULL_HEIGHT * t
                pos[0] = startPos.x + sin(yawRad) * dist
                pos[1] = startPos.y + height
                pos[2] = startPos.z - cos(yawRad) * dist
                rot[0] = initialYaw
                rot[1] = initialPitch + (SOAR_PITCH - initialPitch) * t.toFloat()
            }
            // â”€â”€ SOAR : s'Ã©loigne + monte davantage, pitch fixe
            p <= 0.80 -> {
                val t      = smoothstep((p - 0.25) / 0.55)
                val dist   = SOAR_DIST * 0.4 + (SOAR_DIST - SOAR_DIST * 0.4) * t
                val height = PULL_HEIGHT + (SOAR_HEIGHT - PULL_HEIGHT) * t
                pos[0] = startPos.x + sin(yawRad) * dist
                pos[1] = startPos.y + height
                pos[2] = startPos.z - cos(yawRad) * dist
                rot[0] = initialYaw
                rot[1] = SOAR_PITCH
            }
            // â”€â”€ FLASH : camÃ©ra figÃ©e en position SOAR pendant le flash blanc
            else -> {
                pos[0] = startPos.x + sin(yawRad) * SOAR_DIST
                pos[1] = startPos.y + SOAR_HEIGHT
                pos[2] = startPos.z - cos(yawRad) * SOAR_DIST
                rot[0] = initialYaw
                rot[1] = SOAR_PITCH
            }
        }
        return true
    }

    private fun zoomDownCamera(
        pos: DoubleArray, rot: FloatArray,
        elapsed: Double,
        playerPos: Vec3, playerYaw: Float, playerPitch: Float
    ): Boolean {
        val p = (elapsed / ZOOM_DOWN_MS).coerceIn(0.0, 1.0)
        if (p >= 1.0) { stop(); return false }

        val yawRad = Math.toRadians(playerYaw.toDouble())

        when {
            // â”€â”€ ARRIVE : camÃ©ra haute altitude top-down (flash se dissipe)
            p <= 0.20 -> {
                pos[0] = playerPos.x
                pos[1] = playerPos.y + SOAR_HEIGHT + 6.0
                pos[2] = playerPos.z
                rot[0] = playerYaw
                rot[1] = 90.0f
            }
            // â”€â”€ DESCENT : camÃ©ra redescend en arc vers l'arriÃ¨re du joueur
            p <= 0.75 -> {
                val t      = smoothstep((p - 0.20) / 0.55)
                val height = (SOAR_HEIGHT + 6.0) - ((SOAR_HEIGHT + 6.0) - PULL_HEIGHT) * t
                val dist   = SOAR_DIST * t
                val pitch  = 90.0f - (90.0f - SOAR_PITCH) * t.toFloat()
                pos[0] = playerPos.x + sin(yawRad) * dist
                pos[1] = playerPos.y + height
                pos[2] = playerPos.z - cos(yawRad) * dist
                rot[0] = playerYaw
                rot[1] = pitch
            }
            // â”€â”€ LAND : retour progressif Ã  la vue premiÃ¨re personne
            else -> {
                val t      = smoothstep((p - 0.75) / 0.25)
                val height = PULL_HEIGHT * (1.0 - t)
                val dist   = SOAR_DIST  * (1.0 - t)
                val pitch  = SOAR_PITCH * (1.0f - t.toFloat()) + playerPitch * t.toFloat()
                pos[0] = playerPos.x + sin(yawRad) * dist
                pos[1] = playerPos.y + height
                pos[2] = playerPos.z - cos(yawRad) * dist
                rot[0] = playerYaw
                rot[1] = pitch
            }
        }
        return true
    }

    // ------------------------------------------------------------------
    // Overlay GUI (appelÃ© par GuiMixin chaque frame)
    // ------------------------------------------------------------------

    fun renderOverlay(graphics: GuiGraphics) {
        if (phase == TeleportPhase.NONE) return

        val now     = System.currentTimeMillis()
        val elapsed = (now - animStartTime).toDouble()
        val dt      = (now - lastRenderMs).toDouble().coerceAtLeast(0.0)
        lastRenderMs = now

        val mc     = Minecraft.getInstance()
        val width  = mc.window.guiScaledWidth
        val height = mc.window.guiScaledHeight

        when (phase) {
            TeleportPhase.ZOOM_UP   -> renderZoomUpOverlay(graphics, elapsed, dt, width, height)
            TeleportPhase.ZOOM_DOWN -> renderZoomDownOverlay(graphics, elapsed, dt, width, height)
            TeleportPhase.NONE      -> {}
        }
    }

    // ------------------------------------------------------------------
    // Zoom-up overlay
    // ------------------------------------------------------------------

    private fun renderZoomUpOverlay(g: GuiGraphics, elapsed: Double, dt: Double, w: Int, h: Int) {
        val p = (elapsed / ZOOM_UP_MS).coerceIn(0.0, 1.0)

        when {
            // LAUNCH (0â†’25%) : fondu noir progressif + dÃ©but des lignes trÃ¨s douces
            p <= 0.25 -> {
                val t = (p / 0.25).coerceIn(0.0, 1.0)
                val ease = smoothstep(t)

                // Fond noir qui monte doucement (style B&W)
                val bgAlpha = (ease * 0.75 * 255).toInt().coerceIn(0, 255)
                g.fill(0, 0, w, h, (bgAlpha shl 24) or 0x000000)

                // Vignette cinÃ©matique (bords assombris)
                drawVignette(g, w, h, ease.toFloat() * 0.45f)

                // Lignes trÃ¨s discrÃ¨tes au dÃ©but
                if (ease > 0.3) {
                    val lineAlpha = ((ease - 0.3) / 0.7).toFloat()
                    val speed = 40.0 * ease
                    scrollOffset += dt * speed / 1000.0
                    drawLinesCentered(g, BG_LINE, BG_LINE_W, BG_LINE_H, w, h, scrollOffset, lineAlpha * 0.4f)
                }
            }

            // SOAR (25â†’80%) : lignes qui accÃ©lÃ¨rent + PokÃ©mon en vol
            p <= 0.80 -> {
                val t    = ((p - 0.25) / 0.55).coerceIn(0.0, 1.0)
                val ease = smoothstep(t)

                // Vitesse montant de 80 px/s Ã  700 px/s
                val speed = 80.0 + 620.0 * ease
                scrollOffset += dt * speed / 1000.0

                // Fond sombre persistant (caractÃ©ristique B&W)
                val bgAlpha = (0.82 * 255).toInt()
                g.fill(0, 0, w, h, (bgAlpha shl 24) or 0x000000)

                // Screen shake subtil au peak de vitesse (t 0.5 â†’ 0.85)
                val shakeIntensity = if (t in 0.5..0.85) ((t - 0.5) / 0.35 * (0.85 - t) / 0.35 * 3.0).toInt() else 0
                val shakeY = if (shakeIntensity > 0) ((System.currentTimeMillis() / 40) % 3 - 1).toInt() * shakeIntensity else 0

                val pose = g.pose()
                pose.pushPose()
                pose.translate(0f, shakeY.toFloat(), 0f)

                drawLinesCentered(g, BG_LINE,   BG_LINE_W,   BG_LINE_H,   w, h, scrollOffset,         (ease * 0.9f).toFloat())
                drawLinesCentered(g, POKE_LINE,  POKE_LINE_W, POKE_LINE_H, w, h, scrollOffset * 1.5, (ease * 1.0).toFloat())

                pose.popPose()

                // Teinte bleutÃ©e lÃ©gÃ¨re (conservation de la teinte B&W)
                val tintA = ((ease * 0.15) * 255).toInt().coerceIn(0, 255)
                g.fill(0, 0, w, h, (tintA shl 24) or 0x6699FF)

                // Vignette cinÃ©matique persistante
                drawVignette(g, w, h, 0.40f)

                // Halo lumineux autour de la zone PokÃ©mon
                val haloAlpha = (ease * 0.25 * 255).toInt().coerceIn(0, 255)
                val haloH = POKE_LINE_H * 2
                val haloY = (h - haloH) / 2
                g.fill(0, haloY - 10, w, haloY + haloH + 10, (haloAlpha shl 24) or 0xFFFFFF)

                renderRideablePokemonModel(g, w, h, (ease * 1.0).toFloat(), dt, p, true)
            }

            // FLASH (80â†’100%) : lignes Ã  max + blanc aveuglant
            else -> {
                val tFlash = ((p - 0.80) / 0.20).coerceIn(0.0, 1.0)
                val easeFlash = smoothstep(tFlash)
                scrollOffset += dt * 700.0 / 1000.0

                // Fond noir disparaÃ®t dans le blanc
                val bgAlpha = ((1.0 - easeFlash) * 0.82 * 255).toInt().coerceIn(0, 255)
                g.fill(0, 0, w, h, (bgAlpha shl 24) or 0x000000)

                drawLinesCentered(g, BG_LINE,   BG_LINE_W,   BG_LINE_H,   w, h, scrollOffset,         0.9f)
                drawLinesCentered(g, POKE_LINE,  POKE_LINE_W, POKE_LINE_H, w, h, scrollOffset * 1.5, 1.0f)

                // Flash blanc qui envahit l'Ã©cran
                val flashAlpha = smoothstep(tFlash).toFloat()
                g.fill(0, 0, w, h, ((flashAlpha * 255).toInt().coerceIn(0, 255) shl 24) or 0xFFFFFF)
            }
        }
    }

    // ------------------------------------------------------------------
    // Zoom-down overlay
    // ------------------------------------------------------------------

    private fun renderZoomDownOverlay(g: GuiGraphics, elapsed: Double, dt: Double, w: Int, h: Int) {
        val p = (elapsed / ZOOM_DOWN_MS).coerceIn(0.0, 1.0)

        when {
            // ARRIVE (0â†’20%) : flash blanc + lignes rapides qui s'effacent
            p <= 0.20 -> {
                val t    = (p / 0.20).coerceIn(0.0, 1.0)
                val ease = smoothstep(t)
                val speed = 700.0 * (1.0 - ease * 0.5)
                scrollOffset += dt * speed / 1000.0

                // Fond noir rÃ©apparaÃ®t sous le flash
                val bgAlpha = (ease * 0.82 * 255).toInt().coerceIn(0, 255)
                g.fill(0, 0, w, h, (bgAlpha shl 24) or 0x000000)

                val lineA = (1.0f - ease * 0.4f).toFloat()
                drawLinesCentered(g, POKE_LINE, POKE_LINE_W, POKE_LINE_H, w, h, scrollOffset * 1.5, lineA)
                drawLinesCentered(g, BG_LINE,   BG_LINE_W,   BG_LINE_H,   w, h, scrollOffset,        lineA * 0.8f)

                // Flash blanc qui se dissipe
                val flashAlpha = (1.0f - ease.toFloat()).coerceIn(0f, 1f)
                g.fill(0, 0, w, h, ((flashAlpha * 255).toInt().coerceIn(0, 255) shl 24) or 0xFFFFFF)

                // Vignette
                drawVignette(g, w, h, (ease * 0.40f).toFloat())

                renderRideablePokemonModel(g, w, h, lineA, dt, p, false)
            }

            // DESCENT (20â†’75%) : lignes dÃ©cÃ©lÃ¨rent et disparaissent
            p <= 0.75 -> {
                val t    = ((p - 0.20) / 0.55).coerceIn(0.0, 1.0)
                val ease = smoothstep(t)
                val speed = 400.0 * (1.0 - ease)
                scrollOffset += dt * speed / 1000.0

                val lineA = (1.0f - ease.toFloat()).coerceIn(0f, 1f)

                // Fond noir qui se dissipe avec les lignes
                val bgAlpha = ((1.0 - ease) * 0.82 * 255).toInt().coerceIn(0, 255)
                g.fill(0, 0, w, h, (bgAlpha shl 24) or 0x000000)

                drawLinesCentered(g, BG_LINE,   BG_LINE_W,   BG_LINE_H,   w, h, scrollOffset,        lineA * 0.9f)
                drawLinesCentered(g, POKE_LINE,  POKE_LINE_W, POKE_LINE_H, w, h, scrollOffset * 1.5, lineA * 0.6f)

                // Vignette qui disparaÃ®t
                drawVignette(g, w, h, (lineA * 0.40f).toFloat())

                renderRideablePokemonModel(g, w, h, lineA, dt, p, false)
            }

            // LAND (75â†’100%) : rien
            else -> { /* rien */ }
        }
    }

    // ------------------------------------------------------------------
    // Rendu des lignes de vitesse
    // ------------------------------------------------------------------

    /**
     * Lignes de fond (bg_line) : Ã©tirÃ©es verticalement pour couvrir tout l'Ã©cran.
     */
    private fun drawLinesFullscreen(
        g: GuiGraphics, texture: ResourceLocation,
        texW: Int, texH: Int,
        screenW: Int, screenH: Int,
        scroll: Double, alpha: Float
    ) {
        if (alpha <= 0.005f) return
        RenderSystem.enableBlend()
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha.coerceIn(0f, 1f))

        val scaleY = screenH.toFloat() / texH.toFloat()
        val pose = g.pose()
        pose.pushPose()
        pose.scale(1f, scaleY, 1f)

        val scrollI = (abs(scroll) % texW).toInt()
        var x = -scrollI
        while (x < screenW) {
            g.blit(texture, x, 0, 0f, 0f, texW, texH, texW, texH)
            x += texW
        }

        pose.popPose()
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        RenderSystem.disableBlend()
    }

    /**
     * Lignes PokÃ©mon (poke_line) : rendues Ã  leur hauteur native,
     * centrÃ©es verticalement sur l'Ã©cran, comme une barre horizontale.
     */
    private fun drawLinesCentered(
        g: GuiGraphics, texture: ResourceLocation,
        texW: Int, texH: Int,
        screenW: Int, screenH: Int,
        scroll: Double, alpha: Float
    ) {
        if (alpha <= 0.005f) return
        RenderSystem.enableBlend()
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha.coerceIn(0f, 1f))

        // Calcule la position Y pour centrer la barre verticalement
        val scale = 1f
        val barH = (texH * scale).toInt()
        val yOffset = (screenH - barH) / 2

        val pose = g.pose()
        pose.pushPose()
        pose.translate(0f, yOffset.toFloat(), 0f)
        pose.scale(1f, scale, 1f)

        val scrollI = (abs(scroll) % texW).toInt()
        var x = -scrollI
        while (x < screenW) {
            g.blit(texture, x, 0, 0f, 0f, texW, texH, texW, texH)
            x += texW
        }

        pose.popPose()
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        RenderSystem.disableBlend()
    }

    /**
     * Vignette cinÃ©matique : assombrit les bords de l'Ã©cran pour un effet B&W dramatique.
     * Utilise un dÃ©gradÃ© par paliers (8 bandes) pour rester performant.
     */
    private fun drawVignette(g: GuiGraphics, w: Int, h: Int, alpha: Float) {
        if (alpha <= 0.01f) return
        RenderSystem.enableBlend()
        val steps = 8
        val vigW = (w * 0.30).toInt()
        val vigH = (h * 0.30).toInt()

        for (i in 0 until steps) {
            val frac = 1.0f - (i.toFloat() / steps)
            val ca = (alpha * frac * frac * 255).toInt().coerceIn(0, 255)
            val color = (ca shl 24) or 0x000000
            val sliceW = vigW / steps
            val sliceH = vigH / steps
            // Gauche
            g.fill(i * sliceW, 0, (i + 1) * sliceW, h, color)
            // Droite
            g.fill(w - (i + 1) * sliceW, 0, w - i * sliceW, h, color)
            // Haut
            g.fill(0, i * sliceH, w, (i + 1) * sliceH, color)
            // Bas
            g.fill(0, h - (i + 1) * sliceH, w, h - i * sliceH, color)
        }
        RenderSystem.disableBlend()
    }

    private fun findRideablePokemon(): com.cobblemon.mod.common.pokemon.Pokemon? {
        val player = Minecraft.getInstance().player ?: return null
        try {
            val party = Cobblemon.storage.getParty(player.uuid, player.registryAccess())
            return party.filterNotNull().firstOrNull { pokemon ->
                val behaviours = pokemon.form.riding.behaviours
                behaviours != null && behaviours.isNotEmpty()
            }
        } catch (e: Exception) {
            return null
        }
    }

    private fun renderRideablePokemonModel(
        g: GuiGraphics, w: Int, h: Int, alpha: Float,
        dt: Double, p: Double, isZoomUp: Boolean
    ) {
        if (alpha <= 0.05f) return
        val pokemon = findRideablePokemon() ?: return
        val now = System.currentTimeMillis()

        // -- Trajectoire de vol ----------------------------------------------
        var targetX = w / 2.0
        var targetY = h / 2.0
        var tNorm   = 0.0   // t normalisé [0..1] pour les effets dynamiques

        if (isZoomUp) {
            val t = ((p - 0.25) / 0.55).coerceIn(0.0, 1.0)
            tNorm = t
            targetX = when {
                t <= 0.30 -> {
                    val tIn = t / 0.30
                    -80.0 + (w / 2.0 + 80.0) * smoothstep(tIn)
                }
                t >= 0.80 -> {
                    val tOut = (t - 0.80) / 0.20
                    w / 2.0 + (w / 2.0 + 80.0) * smoothstep(tOut)
                }
                else -> w / 2.0
            }
        } else {
            val t = (p / 0.75).coerceIn(0.0, 1.0)
            tNorm = t
            targetX = when {
                t <= 0.35 -> {
                    val tIn = t / 0.35
                    -80.0 + (w / 2.0 + 80.0) * smoothstep(tIn)
                }
                t >= 0.80 -> {
                    val tOut = (t - 0.80) / 0.20
                    w / 2.0 + (w / 2.0 + 80.0) * smoothstep(tOut)
                }
                else -> w / 2.0
            }
            targetY = when {
                t <= 0.35 -> {
                    val tIn = t / 0.35
                    (h / 2.0 - 60.0) + 60.0 * smoothstep(tIn)
                }
                t >= 0.80 -> {
                    val tOut = (t - 0.80) / 0.20
                    (h / 2.0) + 60.0 * smoothstep(tOut)
                }
                else -> h / 2.0
            }
        }

        // -- Sway sinusoidal (vol dynamique) ---------------------------------
        // Plus rapide et ample au centre (plateau de vol), plus doux en entree/sortie
        val swaySpeed = if (tNorm in 0.30..0.75) 0.008 else 0.005
        val swayAmplitude = if (tNorm in 0.30..0.75) 10.0 else 5.0
        val swayY = sin(now * swaySpeed) * swayAmplitude
        val finalY = targetY + swayY

        // -- Bank roll (inclinaison Z = virage physique) ----------------------
        // Entree par la gauche : penche en avant, se redresse au centre
        // Sortie par la droite : repique pour accelerer
        val bankZ: Float = when {
            tNorm <= 0.30 -> {
                val tIn = (tNorm / 0.30).toFloat()
                // Penche fort en entree, se redresse progressivement
                -18f * (1f - smoothstep(tIn.toDouble()).toFloat()) + 3f * tIn
            }
            tNorm >= 0.80 -> {
                val tOut = ((tNorm - 0.80) / 0.20).toFloat()
                // Incline en sortie (accel vers la droite)
                3f + 18f * smoothstep(tOut.toDouble()).toFloat()
            }
            else -> {
                // Au plateau : leger balancement synchronise avec le sway
                (sin(now * swaySpeed + Math.PI) * 3.5).toFloat()
            }
        }

        // -- Pitch dip (inclinaison X = plongeon de tete) --------------------
        // Pique vers le bas en acceleration, vol horizontal au plateau
        val pitchX: Float = when {
            tNorm <= 0.30 -> {
                val tIn = (tNorm / 0.30).toFloat()
                // Entree : pique puis redresse
                22f - 12f * smoothstep(tIn.toDouble()).toFloat()
            }
            tNorm >= 0.80 -> {
                val tOut = ((tNorm - 0.80) / 0.20).toFloat()
                // Sortie : repique legerement
                10f + 10f * smoothstep(tOut.toDouble()).toFloat()
            }
            else -> 10f // Vol horizontal stable
        }

        // -- Scale pulse (battements d'ailes = respiration de taille) --------
        val scalePulse = 1.0f + (sin(now * swaySpeed * 2.2) * 0.045).toFloat()
        val baseScale = 12.0f * scalePulse

        // -- Flash d'entree (surbrillance au moment du swoop-in) -------------
        // Le pokemon apparait brillant puis revient a couleur normale
        val entryFlash: Float = if (tNorm <= 0.18) {
            val tFlash = (tNorm / 0.18).toFloat()
            (1f - tFlash) * (1f - tFlash) * 0.55f
        } else 0f
        val colorR = (1f + entryFlash).coerceIn(0f, 1f)
        val colorGB = (1f + entryFlash * 0.65f).coerceIn(0f, 1f)

        // -- Rendu -----------------------------------------------------------
        val rotation = Quaternionf().fromEulerXYZDegrees(Vector3f(pitchX, 135f, bankZ))

        val pose = g.pose()
        pose.pushPose()
        pose.translate(targetX, finalY + 28.0, 300.0)
        pose.scale(baseScale, baseScale, 1.0f)

        try {
            RenderSystem.enableBlend()
            RenderSystem.setShaderColor(colorR, colorGB, colorGB, alpha.coerceIn(0f, 1f))

            val partialTicks = (dt / 50.0).toFloat()
            drawProfilePokemon(
                renderablePokemon = pokemon.asRenderablePokemon(),
                matrixStack = pose,
                rotation = rotation,
                poseType = PoseType.FLY,
                state = floatingState,
                partialTicks = partialTicks,
                scale = 1.0f,
                applyProfileTransform = false
            )
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            pose.popPose()
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        }
    }

    // ------------------------------------------------------------------
    // Maths
    // ------------------------------------------------------------------

    private fun smoothstep(t: Double): Double {
        val c = t.coerceIn(0.0, 1.0)
        return c * c * (3.0 - 2.0 * c)
    }
}
