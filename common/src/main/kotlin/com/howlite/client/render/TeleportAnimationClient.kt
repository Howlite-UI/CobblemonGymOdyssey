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
 * Animation de téléportation style Pokémon B&W "Fly Field Skill" avec effets "juicy".
 *
 * Phases ZOOM_UP (2000ms = 40 ticks serveur) :
 *  1. LAUNCH  (0→25%)  : Caméra recule derrière le joueur/Pokémon monté, monte 8 blocs, pitch légère plongée.
 *  2. SOAR    (25→80%) : Caméra suit depuis l'arrière-haut, lignes BG_line + poke_line défilent à haute vitesse.
 *  3. FLASH   (80→100%): Flash blanc couvre l'écran — moment de la téléportation physique.
 *
 * Phases ZOOM_DOWN (1500ms = 30 ticks serveur) :
 *  1. ARRIVE  (0→20%)  : Flash blanc se dissipe, caméra à haute altitude top-down.
 *  2. DESCENT (20→75%) : Lignes ralentissent puis s'effacent, caméra redescend en arc.
 *  3. LAND    (75→100%): Retour progressif à la vue normale.
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
    // bg_line.png : 128×32  |  poke_line.png : 128×64
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

    /** Hauteur de la caméra en phase LAUNCH (blocs au-dessus du joueur). */
    private const val PULL_HEIGHT = 8.0
    /** Hauteur de la caméra en phase SOAR / ARRIVE. */
    private const val SOAR_HEIGHT = 14.0
    /** Distance horizontale derrière le joueur pendant SOAR. */
    private const val SOAR_DIST   = 10.0
    /** Pitch caméra en SOAR (légère plongée Pokémon). */
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
    // Camera override (appelé par CameraMixin chaque frame)
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
            // ── LAUNCH : remonte derrière le joueur, pitch NORMAL → SOAR_PITCH
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
            // ── SOAR : s'éloigne + monte davantage, pitch fixe
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
            // ── FLASH : caméra figée en position SOAR pendant le flash blanc
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
            // ── ARRIVE : caméra haute altitude top-down (flash se dissipe)
            p <= 0.20 -> {
                pos[0] = playerPos.x
                pos[1] = playerPos.y + SOAR_HEIGHT + 6.0
                pos[2] = playerPos.z
                rot[0] = playerYaw
                rot[1] = 90.0f
            }
            // ── DESCENT : caméra redescend en arc vers l'arrière du joueur
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
            // ── LAND : retour progressif à la vue première personne
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
    // Overlay GUI (appelé par GuiMixin chaque frame)
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
            // LAUNCH (0→25%) : pas d'overlay
            p <= 0.25 -> { /* rien */ }

            // SOAR (25→80%) : lignes qui accélèrent
            p <= 0.80 -> {
                val t     = ((p - 0.25) / 0.55).coerceIn(0.0, 1.0)
                val ease  = smoothstep(t)
                // Vitesse montant de 80 px/s à 600 px/s
                val speed = 80.0 + 520.0 * ease
                scrollOffset += dt * speed / 1000.0

                drawLinesCentered(g, BG_LINE,  BG_LINE_W,  BG_LINE_H,  w, h, scrollOffset,         (ease * 0.85).toFloat())
                drawLinesCentered(g, POKE_LINE, POKE_LINE_W, POKE_LINE_H, w, h, scrollOffset * 1.4, (ease * 1.0).toFloat())

                // Teinte bleue légère pendant SOAR
                val tintA = ((ease * 0.28) * 255).toInt().coerceIn(0, 255)
                g.fill(0, 0, w, h, (tintA shl 24) or 0x88CCFF)

                renderRideablePokemonModel(g, w, h, (ease * 1.0).toFloat(), dt, p, true)
            }

            // FLASH (80→100%) : lignes à max + blanc
            else -> {
                val tFlash = ((p - 0.80) / 0.20).coerceIn(0.0, 1.0)
                scrollOffset += dt * 600.0 / 1000.0

                drawLinesCentered(g, BG_LINE,  BG_LINE_W,  BG_LINE_H,  w, h, scrollOffset,         0.85f)
                drawLinesCentered(g, POKE_LINE, POKE_LINE_W, POKE_LINE_H, w, h, scrollOffset * 1.4, 1.0f)

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
            // ARRIVE (0→20%) : flash blanc + lignes rapides qui s'effacent
            p <= 0.20 -> {
                val t    = (p / 0.20).coerceIn(0.0, 1.0)
                val ease = smoothstep(t)
                val speed = 600.0 * (1.0 - ease * 0.5)
                scrollOffset += dt * speed / 1000.0

                val lineA = (1.0f - ease * 0.5f).toFloat()
                drawLinesCentered(g, POKE_LINE, POKE_LINE_W, POKE_LINE_H, w, h, scrollOffset * 1.4, lineA)
                drawLinesCentered(g, BG_LINE,  BG_LINE_W,  BG_LINE_H,  w, h, scrollOffset,         lineA * 0.8f)

                val flashAlpha = (1.0f - ease.toFloat()).coerceIn(0f, 1f)
                g.fill(0, 0, w, h, ((flashAlpha * 255).toInt().coerceIn(0, 255) shl 24) or 0xFFFFFF)

                renderRideablePokemonModel(g, w, h, lineA, dt, p, false)
            }

            // DESCENT (20→75%) : lignes décélèrent et disparaissent
            p <= 0.75 -> {
                val t    = ((p - 0.20) / 0.55).coerceIn(0.0, 1.0)
                val ease = smoothstep(t)
                val speed = 300.0 * (1.0 - ease)
                scrollOffset += dt * speed / 1000.0

                val lineA = (1.0f - ease.toFloat()).coerceIn(0f, 1f)
                drawLinesCentered(g, BG_LINE,  BG_LINE_W,  BG_LINE_H,  w, h, scrollOffset,         lineA * 0.7f)
                drawLinesCentered(g, POKE_LINE, POKE_LINE_W, POKE_LINE_H, w, h, scrollOffset * 1.4, lineA * 0.5f)

                renderRideablePokemonModel(g, w, h, lineA, dt, p, false)
            }

            // LAND (75→100%) : rien
            else -> { /* rien */ }
        }
    }

    // ------------------------------------------------------------------
    // Rendu des lignes de vitesse
    // ------------------------------------------------------------------

    /**
     * Lignes de fond (bg_line) : étirées verticalement pour couvrir tout l'écran.
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
     * Lignes Pokémon (poke_line) : rendues à leur hauteur native,
     * centrées verticalement sur l'écran, comme une barre horizontale.
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

        // Calcul de la trajectoire de vol dynamique (style cinématique Pokémon B&W)
        var targetX = w / 2.0
        var targetY = h / 2.0

        if (isZoomUp) {
            val t = ((p - 0.25) / 0.55).coerceIn(0.0, 1.0)
            targetX = when {
                t <= 0.30 -> {
                    // Entrée en scène par la gauche (swoop-in)
                    val tIn = t / 0.30
                    -80.0 + (w / 2.0 + 80.0) * smoothstep(tIn)
                }
                t >= 0.80 -> {
                    // Sortie accélérée par la droite (swoop-out)
                    val tOut = (t - 0.80) / 0.20
                    w / 2.0 + (w / 2.0 + 80.0) * smoothstep(tOut)
                }
                else -> w / 2.0
            }
        } else {
            // ZOOM_DOWN (t de 0.0 à 1.0 sur la durée active de descente 75%)
            val t = (p / 0.75).coerceIn(0.0, 1.0)
            targetX = when {
                t <= 0.35 -> {
                    // Entrée par le haut-gauche (descente du ciel)
                    val tIn = t / 0.35
                    -80.0 + (w / 2.0 + 80.0) * smoothstep(tIn)
                }
                t >= 0.80 -> {
                    // Plongeon final vers le bas-droite pour atterrir
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

        // Balancement sinusoïdal vertical doux (flottement dans les airs)
        val swayY = sin(System.currentTimeMillis() * 0.005) * 8.0
        val finalY = targetY + swayY

        val pose = g.pose()
        pose.pushPose()
        pose.translate(targetX, finalY + 17.0, 300.0)

        // Scale du Pokémon
        val baseScale = 12.0f
        pose.scale(baseScale, baseScale, 1.0f)

        // Orientation oblique droite (profil de vol)
        val rotation = Quaternionf().fromEulerXYZDegrees(Vector3f(10f, 135f, 0f))

        try {
            RenderSystem.enableBlend()
            RenderSystem.setShaderColor(1f, 1f, 1f, alpha.coerceIn(0f, 1f))

            // Conversion correcte du delta de temps en ticks d'animation
            val partialTicks = (dt / 50.0).toFloat()
            drawProfilePokemon(
                renderablePokemon = pokemon.asRenderablePokemon(),
                matrixStack = pose,
                rotation = rotation,
                poseType = PoseType.FLY, // Forcer la pose de vol (FLY / HOVER)
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
