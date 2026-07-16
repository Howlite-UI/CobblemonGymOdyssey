package com.howlite.neoforge.client

import com.howlite.client.moon.ClientMoonState
import com.howlite.moon.MoonConfig
import com.howlite.moon.MoonPhase
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferUploader
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.Minecraft
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RenderLevelStageEvent
import net.neoforged.neoforge.event.tick.LevelTickEvent
import org.joml.Matrix4f

/**
 * Renderer de teinte lunaire côté client NeoForge.
 *
 * Inscrit dans le GAME bus (événements en jeu) sur le thread CLIENT uniquement.
 *
 * ## Stratégie
 * - Écoute [LevelTickEvent.Pre] pour avancer la transition de couleur ([ClientMoonState.tick]).
 * - Écoute [RenderLevelStageEvent.Stage.AFTER_SKY] pour dessiner un quad coloré semi-transparent
 *   superposé à la lune, sans toucher au LevelRenderer vanilla.
 *
 * ## Compatibilité Iris/Shaders
 * Si [MoonConfig.instance.enableShaderCompatMode] est vrai et qu'Iris est détecté, le rendu
 * est désactivé. Les effets gameplay (shiny, IVs) restent actifs.
 */
@EventBusSubscriber(
    modid = com.howlite.CobblemonGymOdyssey.MOD_ID,
    bus   = EventBusSubscriber.Bus.GAME,
    value = [Dist.CLIENT]
)
object MoonTintRenderer {

    private val irisPresent: Boolean by lazy {
        try {
            Class.forName("net.irisshaders.iris.Iris")
            println("[GymOdyssey/Moon] Iris détecté — rendu de teinte lunaire désactivé.")
            true
        } catch (_: ClassNotFoundException) {
            false
        }
    }

    private fun shouldRender(): Boolean {
        val config = MoonConfig.instance
        if (!config.enableMoonRendering) return false
        if (config.enableShaderCompatMode && irisPresent) return false
        return ClientMoonState.currentPhase.isSpecial && ClientMoonState.tintProgress > 0.01f
    }

    // ─── Tick client ─────────────────────────────────────────────────────

    @SubscribeEvent
    fun onClientLevelTick(event: LevelTickEvent.Pre) {
        if (Minecraft.getInstance().isLocalServer || true) {
            ClientMoonState.tick()
        }
    }

    // ─── Rendu ───────────────────────────────────────────────────────────

    @SubscribeEvent
    fun onRenderLevelStage(event: RenderLevelStageEvent) {
        if (event.stage != RenderLevelStageEvent.Stage.AFTER_SKY) return
        if (!shouldRender()) return

        val mc = Minecraft.getInstance()
        val level = mc.level ?: return

        // Ne teinter que pendant la nuit (entre 0.5 et 1.04 normalisé)
        val timeOfDay = (level.dayTime % 24000L).toDouble() / 24000.0
        val isNight = timeOfDay > 0.5 || timeOfDay < 0.04
        if (!isNight) return

        renderMoonTint(event)
    }

    private fun renderMoonTint(event: RenderLevelStageEvent) {
        val (r, g, b) = ClientMoonState.getCurrentTint()
        val alpha = 0.40f * ClientMoonState.tintProgress

        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()
        RenderSystem.disableDepthTest()
        RenderSystem.depthMask(false)

        val poseStack = event.poseStack
        poseStack.pushPose()

        val mc = Minecraft.getInstance()
        val level = mc.level!!
        val celestialAngle = level.getTimeOfDay(event.partialTick.gameTimeDeltaTicks)

        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-90f))
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(celestialAngle * 360f))

        val matrix: Matrix4f = poseStack.last().pose()

        // La lune vanilla est à y=100, quad de ~10 unités
        val moonDist = 100.0f
        val size = 12.0f

        val tessellator = Tesselator.getInstance()
        val bufferBuilder = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR)

        bufferBuilder.addVertex(matrix, -size, moonDist, -size).setColor(r, g, b, alpha)
        bufferBuilder.addVertex(matrix,  size, moonDist, -size).setColor(r, g, b, alpha)
        bufferBuilder.addVertex(matrix,  size, moonDist,  size).setColor(r, g, b, alpha)
        bufferBuilder.addVertex(matrix, -size, moonDist,  size).setColor(r, g, b, alpha)

        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow())

        poseStack.popPose()

        RenderSystem.depthMask(true)
        RenderSystem.enableDepthTest()
        RenderSystem.disableBlend()
    }
}
