package com.howlite.client.render

import com.howlite.blocks.UnownStoneActivatedBlockEntity
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.ResourceLocation
import org.joml.Matrix4f
import kotlin.math.sin
import kotlin.math.cos

/**
 * Juicy renderer for the Unown Stone Activated block.
 *
 * Visual layers per rune (ordered back → front):
 *   1. 3 bloom halo quads  (same glyph, larger + very transparent → soft glow)
 *   2. Main rune quad       (pulsing scale, full alpha)
 *   3. Rising spark quads   (4 tiny billboarded glyphs that drift upward)
 *
 * Colours: pure neon palette — violet / magenta / cyan (green kept near 0).
 * Layout: seeded from BlockPos → each block is permanently unique.
 */
class UnownStoneActivatedRenderer(
    @Suppress("UNUSED_PARAMETER") context: BlockEntityRendererProvider.Context
) : BlockEntityRenderer<UnownStoneActivatedBlockEntity> {

    companion object {
        private val RUNE_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "cobblemongymodyssey",
            "textures/block/zarbi/zarbi_rune.png"
        )

        private const val TOTAL_GLYPHS = 26
        private const val GLYPH_PX     = 7f
        private const val SPACING_PX   = 1f
        private const val TEX_HEIGHT   = 208f       // 26 × 8

        // Animation timings (ticks)
        private const val FADE_IN  = 15f
        private const val HOLD     = 60f
        private const val FADE_OUT = 20f
        private const val CYCLE    = FADE_IN + HOLD + FADE_OUT   // 95 ticks / glyph

        // Geometry constants
        private const val Y_TOP       = 1.002f      // slightly above block surface
        private const val SPARK_COUNT = 4
        private const val SPARK_RISE  = 0.65f       // blocks above surface sparks travel
    }

    // ── Per-block layout ──────────────────────────────────────────────────────
    private data class RuneConfig(
        val cx: Float, val cz: Float,
        val halfSize: Float,
        val cosR: Float, val sinR: Float,
        val glyphOffset: Int,
        val timeOffset: Float,
        val colorPhase: Float
    )

    private val layoutCache = HashMap<Long, List<RuneConfig>>()

    private fun getLayout(posKey: Long): List<RuneConfig> {
        return layoutCache.getOrPut(posKey) {
            val rng = java.util.Random(posKey xor 6364136223846793005L)
            val n = if (rng.nextFloat() < 0.35f) 2 else 1
            List(n) {
                val rot = rng.nextFloat() * Math.PI.toFloat() * 2f
                RuneConfig(
                    cx          = 0.15f + rng.nextFloat() * 0.70f,
                    cz          = 0.15f + rng.nextFloat() * 0.70f,
                    halfSize    = 0.22f + rng.nextFloat() * 0.22f,
                    cosR        = cos(rot),
                    sinR        = sin(rot),
                    glyphOffset = rng.nextInt(TOTAL_GLYPHS),
                    timeOffset  = rng.nextFloat() * CYCLE,
                    colorPhase  = rng.nextFloat() * Math.PI.toFloat() * 2f
                )
            }
        }
    }

    // ── Main render ───────────────────────────────────────────────────────────
    override fun render(
        be: UnownStoneActivatedBlockEntity,
        partialTick: Float,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        packedLight: Int,
        packedOverlay: Int
    ) {
        val rawTick = be.animationTick + partialTick
        val layout  = getLayout(be.blockPos.asLong())
        val buffer  = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(RUNE_TEXTURE))
        val last    = poseStack.last()
        val mat     = last.pose()

        // Camera yaw for billboarded sparks
        val camYawRad = -Minecraft.getInstance().entityRenderDispatcher.camera.yRot *
                        Math.PI.toFloat() / 180f
        val camCos = cos(camYawRad)
        val camSin = sin(camYawRad)

        for (rune in layout) {

            // ── Animation ──────────────────────────────────────────────────
            val shifted     = (rawTick + rune.timeOffset) % (CYCLE * TOTAL_GLYPHS)
            val cycleIdx    = (shifted / CYCLE).toInt()
            val tickInCycle = shifted % CYCLE

            val alpha = when {
                tickInCycle < FADE_IN            -> tickInCycle / FADE_IN
                tickInCycle < FADE_IN + HOLD     -> 1f
                else -> 1f - (tickInCycle - FADE_IN - HOLD) / FADE_OUT
            }.coerceIn(0f, 1f)

            if (alpha <= 0.01f) continue

            // ── Glyph UV ───────────────────────────────────────────────────
            val glyph = (cycleIdx + rune.glyphOffset) % TOTAL_GLYPHS
            val v0 = (glyph * (GLYPH_PX + SPACING_PX)) / TEX_HEIGHT
            val v1 = (glyph * (GLYPH_PX + SPACING_PX) + GLYPH_PX) / TEX_HEIGHT

            // ── Pure neon palette (no green) ───────────────────────────────
            val hue = rawTick * 0.008f + rune.colorPhase
            val r   = (sin(hue)          * 0.5f + 0.60f).coerceIn(0f, 1f)
            val g   = (sin(hue + 2.09f)  * 0.08f + 0.07f).coerceIn(0f, 1f)  // near-zero
            val b   = (sin(hue + 3.67f)  * 0.45f + 0.95f).coerceIn(0f, 1f)
            val ri  = (r * 255).toInt()
            val gi  = (g * 255).toInt()
            val bi  = (b * 255).toInt()

            // ── Pulse (breathe) ────────────────────────────────────────────
            val pulse = 1f + sin(rawTick * 0.18f + rune.colorPhase) * 0.06f

            // ── 3 × Bloom halo (back → front, largest → smallest) ──────────
            for (bloom in 3 downTo 1) {
                val bScale = pulse * (1f + bloom * 0.55f)
                val bAlpha = (alpha * 0.22f / bloom.toFloat()).coerceIn(0f, 1f)
                val bai    = (bAlpha * 255).toInt()
                val bhs    = rune.halfSize * bScale
                addRotated(buffer, mat, last, -bhs, -bhs, rune, 0f, v1, ri, gi, bi, bai)
                addRotated(buffer, mat, last,  bhs, -bhs, rune, 1f, v1, ri, gi, bi, bai)
                addRotated(buffer, mat, last,  bhs,  bhs, rune, 1f, v0, ri, gi, bi, bai)
                addRotated(buffer, mat, last, -bhs,  bhs, rune, 0f, v0, ri, gi, bi, bai)
            }

            // ── Main rune ──────────────────────────────────────────────────
            val hs = rune.halfSize * pulse
            val ai = (alpha * 255).toInt()
            addRotated(buffer, mat, last, -hs, -hs, rune, 0f, v1, ri, gi, bi, ai)
            addRotated(buffer, mat, last,  hs, -hs, rune, 1f, v1, ri, gi, bi, ai)
            addRotated(buffer, mat, last,  hs,  hs, rune, 1f, v0, ri, gi, bi, ai)
            addRotated(buffer, mat, last, -hs,  hs, rune, 0f, v0, ri, gi, bi, ai)

            // ── Rising sparks ──────────────────────────────────────────────
            for (sk in 0 until SPARK_COUNT) {
                val skSeed  = sk * 1.618f + rune.colorPhase
                val skPhase = ((rawTick * 0.022f + skSeed) % 1f).let { if (it < 0) it + 1f else it }
                val skY     = Y_TOP + skPhase * SPARK_RISE
                // Ease-out² so sparks slow and fade near the top
                val skAlpha = (alpha * (1f - skPhase) * (1f - skPhase) * 0.85f).coerceIn(0f, 1f)
                if (skAlpha <= 0.01f) continue

                val skAi = (skAlpha * 255).toInt()
                val skX  = rune.cx + sin(skSeed * 6.28f) * rune.halfSize * 0.55f
                val skZ  = rune.cz + cos(skSeed * 6.28f) * rune.halfSize * 0.55f
                val skH  = 0.05f   // half-width of the spark quad
                // Billboard: axis-aligned along camera yaw
                val dX   = skH * camCos
                val dZ   = skH * camSin

                buffer.addVertex(mat, skX - dX, skY,          skZ - dZ)
                    .setColor(ri, gi, bi, skAi).setUv(0f, v0)
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                    .setNormal(last, 0f, 1f, 0f)
                buffer.addVertex(mat, skX + dX, skY,          skZ + dZ)
                    .setColor(ri, gi, bi, skAi).setUv(1f, v0)
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                    .setNormal(last, 0f, 1f, 0f)
                buffer.addVertex(mat, skX + dX, skY + skH * 2f, skZ + dZ)
                    .setColor(ri, gi, bi, skAi).setUv(1f, v1)
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                    .setNormal(last, 0f, 1f, 0f)
                buffer.addVertex(mat, skX - dX, skY + skH * 2f, skZ - dZ)
                    .setColor(ri, gi, bi, skAi).setUv(0f, v1)
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                    .setNormal(last, 0f, 1f, 0f)
            }
        }
    }

    // ── Helper: rotate (dx, dz) around rune center then emit vertex ──────────
    private fun addRotated(
        buffer: VertexConsumer, mat: Matrix4f, pose: PoseStack.Pose,
        dx: Float, dz: Float, rune: RuneConfig,
        u: Float, v: Float, r: Int, g: Int, b: Int, a: Int
    ) {
        val wx = dx * rune.cosR - dz * rune.sinR + rune.cx
        val wz = dx * rune.sinR + dz * rune.cosR + rune.cz
        buffer.addVertex(mat, wx, Y_TOP, wz)
            .setColor(r, g, b, a)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(LightTexture.FULL_BRIGHT)
            .setNormal(pose, 0f, 1f, 0f)
    }

    // Sparks extend above the block so disable off-screen culling
    override fun shouldRenderOffScreen(be: UnownStoneActivatedBlockEntity) = true
}
