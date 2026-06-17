package com.howlite.client.render

import com.howlite.blocks.UnownStoneActivatedBlockEntity
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
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
 * Renderer for the Unown Stone Activated block.
 *
 * — texture path must include "textures/" prefix and ".png" suffix for entity render types.
 * — Rune layout (count, position, rotation, size) is seeded from blockPos so every
 *   placed block looks different but stays stable.
 * — 1 or 2 runes can appear simultaneously, randomly offset and rotated on the top face.
 * — Each rune fades in, holds, then fades out before cycling to the next glyph.
 *
 * zarbi_rune.png layout: 7 px wide × 208 px tall.
 *   Each glyph occupies rows [i*8 .. i*8+6] (7 px glyph + 1 px gap).
 *   26 glyphs total.
 */
class UnownStoneActivatedRenderer(
    @Suppress("UNUSED_PARAMETER") context: BlockEntityRendererProvider.Context
) : BlockEntityRenderer<UnownStoneActivatedBlockEntity> {

    // ── Texture ───────────────────────────────────────────────────────────────
    companion object {
        private val RUNE_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "cobblemongymodyssey",
            "textures/block/zarbi/zarbi_rune.png"   // full path required for entity RT
        )

        private const val TOTAL_GLYPHS  = 26
        private const val GLYPH_PX      = 7f
        private const val SPACING_PX    = 1f
        private const val TEX_HEIGHT    = 208f      // 26 × 8 = 208 px

        // Animation timings (ticks at 20 TPS)
        private const val FADE_IN   = 15f
        private const val HOLD      = 60f
        private const val FADE_OUT  = 20f
        private const val CYCLE     = FADE_IN + HOLD + FADE_OUT  // 95 ticks per glyph

        // Y position of the rune quad (just above the top face)
        private const val Y_TOP = 1.002f
    }

    // ── Per-block layout (seeded from BlockPos) ────────────────────────────────
    private data class RuneConfig(
        val cx: Float,          // center X on top face
        val cz: Float,          // center Z on top face
        val halfSize: Float,    // half-side of the square rune quad
        val cosR: Float,        // pre-computed cos(rotation)
        val sinR: Float,        // pre-computed sin(rotation)
        val glyphOffset: Int,   // phase offset in the glyph cycle
        val timeOffset: Float,  // animation phase offset (ticks)
        val colorPhase: Float   // hue offset
    )

    private val layoutCache = HashMap<Long, List<RuneConfig>>()

    private fun getLayout(posKey: Long): List<RuneConfig> {
        return layoutCache.getOrPut(posKey) {
            // Deterministic pseudo-random from block position
            val rng = java.util.Random(posKey xor 6364136223846793005L)

            val numRunes = if (rng.nextFloat() < 0.35f) 2 else 1

            List(numRunes) {
                val rot  = rng.nextFloat() * Math.PI.toFloat() * 2f
                RuneConfig(
                    cx          = 0.15f + rng.nextFloat() * 0.70f,  // keep within [0.15, 0.85]
                    cz          = 0.15f + rng.nextFloat() * 0.70f,
                    halfSize    = 0.22f + rng.nextFloat() * 0.22f,  // [0.22, 0.44]
                    cosR        = cos(rot),
                    sinR        = sin(rot),
                    glyphOffset = rng.nextInt(TOTAL_GLYPHS),
                    timeOffset  = rng.nextFloat() * CYCLE,           // stagger two runes
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
        val rawTick  = be.animationTick + partialTick
        val layout   = getLayout(be.blockPos.asLong())
        val buffer   = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(RUNE_TEXTURE))
        val last     = poseStack.last()
        val mat      = last.pose()

        for (rune in layout) {
            // ── Animation ──────────────────────────────────────────────────
            val shifted      = (rawTick + rune.timeOffset) % (CYCLE * TOTAL_GLYPHS)
            val cycleIndex   = (shifted / CYCLE).toInt()
            val tickInCycle  = shifted % CYCLE

            val alpha: Float = when {
                tickInCycle < FADE_IN               -> tickInCycle / FADE_IN
                tickInCycle < FADE_IN + HOLD        -> 1f
                else -> 1f - (tickInCycle - FADE_IN - HOLD) / FADE_OUT
            }.coerceIn(0f, 1f)

            if (alpha <= 0f) continue

            // ── Glyph UV ───────────────────────────────────────────────────
            val glyph = (cycleIndex + rune.glyphOffset) % TOTAL_GLYPHS
            val v0 = (glyph * (GLYPH_PX + SPACING_PX)) / TEX_HEIGHT
            val v1 = (glyph * (GLYPH_PX + SPACING_PX) + GLYPH_PX) / TEX_HEIGHT
            // u covers the full 7-px width of the texture
            val u0 = 0f
            val u1 = 1f

            // ── Neon colour (violet → blue → magenta) ──────────────────────
            val hue = rawTick * 0.008f + rune.colorPhase
            val r   = ((sin(hue)                        ) * 0.35f + 0.75f).coerceIn(0f, 1f)
            val g   = ((sin(hue + 2.09f)                ) * 0.10f + 0.15f).coerceIn(0f, 1f)
            val b   = ((sin(hue + Math.PI.toFloat())    ) * 0.40f + 0.90f).coerceIn(0f, 1f)

            val ri = (r * 255).toInt()
            val gi = (g * 255).toInt()
            val bi = (b * 255).toInt()
            val ai = (alpha * 255).toInt()

            // ── Rotated quad on the top face ───────────────────────────────
            // Corners in local rune space (before rotation):
            //   (-hs, -hs) = u0,v1   (hs, -hs) = u1,v1
            //   (hs,   hs) = u1,v0   (-hs, hs) = u0,v0
            val hs = rune.halfSize
            addRotatedVertex(buffer, mat, last,  -hs, -hs, rune, u0, v1, ri, gi, bi, ai)
            addRotatedVertex(buffer, mat, last,   hs, -hs, rune, u1, v1, ri, gi, bi, ai)
            addRotatedVertex(buffer, mat, last,   hs,  hs, rune, u1, v0, ri, gi, bi, ai)
            addRotatedVertex(buffer, mat, last,  -hs,  hs, rune, u0, v0, ri, gi, bi, ai)
        }
    }

    /** Rotate (dx, dz) around the rune center, then emit a vertex. */
    private fun addRotatedVertex(
        buffer: VertexConsumer,
        mat: Matrix4f,
        pose: PoseStack.Pose,
        dx: Float, dz: Float,
        rune: RuneConfig,
        u: Float, v: Float,
        r: Int, g: Int, b: Int, a: Int
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

    override fun shouldRenderOffScreen(be: UnownStoneActivatedBlockEntity) = false
}
