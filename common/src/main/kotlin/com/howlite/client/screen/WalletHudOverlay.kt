package com.howlite.client.screen

import com.howlite.CobblemonGymOdyssey
import com.howlite.wallet.ClientWalletCache
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.resources.ResourceLocation

/**
 * HUD de balance affiché en coin d'écran quand [ClientWalletCache.hudEnabled] est true.
 *
 * Affichage : Le count total en copper dans un cadre de fond minimaliste (wallet_background_ingame.png)
 * positionné sur le bord droit de l'écran.
 * Animé avec un effet de bounce et des particules d'or lors d'une augmentation de solde.
 */
object WalletHudOverlay {

    private val BACKGROUND: ResourceLocation = ResourceLocation.fromNamespaceAndPath(
        CobblemonGymOdyssey.MOD_ID, "textures/gui/coin/wallet_background_ingame.png"
    )

    private const val MARGIN = 4
    private const val WIDTH = 60
    private const val HEIGHT = 14

    // ── Variables d'animation ──
    private var lastKnownBalance: Long = -1L
    private var lastIncreaseTime: Long = 0L
    private var activeTier: CoinTier = CoinTier.COPPER

    enum class CoinTier(
        val particleCount: Int,
        val maxScale: Float,
        val textFlashColor: Int,
        val colors: IntArray
    ) {
        COPPER(
            particleCount = 10,
            maxScale = 1.06f,
            textFlashColor = 0xD27D2D, // Orange Cuivre
            colors = intArrayOf(0xD27D2D, 0xE58E26, 0xCD7F32)
        ),
        SILVER(
            particleCount = 20,
            maxScale = 1.12f,
            textFlashColor = 0xA5D6A7, // Vert/Silver clair
            colors = intArrayOf(0xC0C0C0, 0xE0E0E0, 0x88C0D0)
        ),
        GOLD(
            particleCount = 35,
            maxScale = 1.20f,
            textFlashColor = 0xFFD700, // Or
            colors = intArrayOf(0xFFD700, 0xFFFF80, 0xFFA500)
        ),
        PLATINUM(
            particleCount = 55,
            maxScale = 1.32f,
            textFlashColor = 0x8BE9FD, // Platine/Cyan
            colors = intArrayOf(0x8BE9FD, 0xE0F7FA, 0xFFFFFF, 0xB4F8C8)
        )
    }

    class HudParticle(
        val x0: Float,
        val y0: Float,
        val vx: Float,
        val vy: Float,
        val driftFrequency: Float,
        val driftAmplitude: Float,
        val driftPhase: Float,
        val color: Int,
        val maxLife: Float,
        val spawnTime: Long,
        val isSparkle: Boolean
    )

    private val particles = mutableListOf<HudParticle>()
    private val random = java.util.Random()

    private fun spawnParticles(startX: Float, startY: Float, tier: CoinTier) {
        val now = System.currentTimeMillis()
        // Limiter le nombre total de particules pour éviter tout lag
        if (particles.size > 80) {
            particles.subList(0, particles.size - 40).clear()
        }

        for (i in 0 until tier.particleCount) {
            val rx = startX + random.nextFloat() * WIDTH
            val ry = startY + HEIGHT / 2f + random.nextFloat() * (HEIGHT / 2f)
            val color = tier.colors[random.nextInt(tier.colors.size)]

            when (tier) {
                CoinTier.COPPER -> {
                    // Copper : Petites particules courtes, montent lentement de manière rectiligne
                    val vx = -4f + random.nextFloat() * 8f
                    val vy = -8f - random.nextFloat() * 10f
                    val maxLife = 0.8f + random.nextFloat() * 0.4f
                    particles.add(HudParticle(rx, ry, vx, vy, 1f, 1f, 0f, color, maxLife, now, false))
                }
                CoinTier.SILVER -> {
                    // Silver : Flottement moyen, légère ondulation
                    val vx = -8f + random.nextFloat() * 16f
                    val vy = -12f - random.nextFloat() * 12f
                    val driftFrequency = 2f + random.nextFloat() * 2f
                    val driftAmplitude = 2f + random.nextFloat() * 3f
                    val driftPhase = random.nextFloat() * 2f * Math.PI.toFloat()
                    val maxLife = 1.2f + random.nextFloat() * 0.6f
                    particles.add(HudParticle(rx, ry, vx, vy, driftFrequency, driftAmplitude, driftPhase, color, maxLife, now, false))
                }
                CoinTier.GOLD -> {
                    // Gold : Mouvement plus dynamique, certaines étincelles scintillantes
                    val vx = -12f + random.nextFloat() * 24f
                    val vy = -16f - random.nextFloat() * 16f
                    val driftFrequency = 3f + random.nextFloat() * 3f
                    val driftAmplitude = 4f + random.nextFloat() * 4f
                    val driftPhase = random.nextFloat() * 2f * Math.PI.toFloat()
                    val maxLife = 1.6f + random.nextFloat() * 0.8f
                    val isSparkle = random.nextFloat() < 0.35f // 35% de scintillantes
                    particles.add(HudParticle(rx, ry, vx, vy, driftFrequency, driftAmplitude, driftPhase, color, maxLife, now, isSparkle))
                }
                CoinTier.PLATINUM -> {
                    // Platinum : Grand éclat en burst/fontaine et flottement complexe scintillante
                    val isCircle = i % 2 == 0
                    val vx: Float
                    val vy: Float
                    if (isCircle) {
                        val angle = random.nextFloat() * 2f * Math.PI.toFloat()
                        val speed = 20f + random.nextFloat() * 25f
                        vx = kotlin.math.cos(angle) * speed
                        vy = kotlin.math.sin(angle) * speed - 10f
                    } else {
                        vx = -15f + random.nextFloat() * 30f
                        vy = -20f - random.nextFloat() * 25f
                    }
                    val driftFrequency = 4f + random.nextFloat() * 4f
                    val driftAmplitude = 5f + random.nextFloat() * 6f
                    val driftPhase = random.nextFloat() * 2f * Math.PI.toFloat()
                    val maxLife = 2.2f + random.nextFloat() * 1.2f
                    val isSparkle = true // 100% scintillantes
                    particles.add(HudParticle(rx, ry, vx, vy, driftFrequency, driftAmplitude, driftPhase, color, maxLife, now, isSparkle))
                }
            }
        }
    }

    /**
     * Appelé chaque frame de rendu HUD.
     */
    fun render(graphics: GuiGraphics, partialTick: Float) {
        val mc = Minecraft.getInstance()
        ClientWalletCache.checkSync(mc)

        val currentBalance = ClientWalletCache.balance

        if (!ClientWalletCache.hudEnabled) {
            lastKnownBalance = currentBalance
            particles.clear()
            return
        }
        if (mc.options.hideGui) return

        val window = mc.window
        val screenW = window.guiScaledWidth
        val screenH = window.guiScaledHeight

        // Position : coin inférieur droit
        val startX = screenW - WIDTH - MARGIN
        val startY = screenH - HEIGHT - MARGIN
        val centerX = startX + WIDTH / 2f
        val centerY = startY + HEIGHT / 2f

        // Détection de l'augmentation de la balance
        if (lastKnownBalance == -1L) {
            lastKnownBalance = currentBalance
        } else if (currentBalance > lastKnownBalance) {
            val added = currentBalance - lastKnownBalance
            val tier = when {
                added >= 1_000_000L -> CoinTier.PLATINUM
                added >= 10_000L -> CoinTier.GOLD
                added >= 100L -> CoinTier.SILVER
                else -> CoinTier.COPPER
            }
            lastIncreaseTime = System.currentTimeMillis()
            activeTier = tier
            spawnParticles(startX.toFloat(), startY.toFloat(), tier)
            lastKnownBalance = currentBalance
        } else if (currentBalance < lastKnownBalance) {
            lastKnownBalance = currentBalance
        }

        val elapsed = System.currentTimeMillis() - lastIncreaseTime
        val isAnimating = elapsed < 500L

        // Animation de bounce (mise à l'échelle depuis le centre)
        graphics.pose().pushPose()
        if (isAnimating) {
            val progress = elapsed.toFloat() / 500f
            val maxBounce = activeTier.maxScale - 1.0f
            val scale = 1.0f + maxBounce * kotlin.math.sin(progress * Math.PI.toFloat())
            graphics.pose().translate(centerX, centerY, 0f)
            graphics.pose().scale(scale, scale, 1.0f)
            graphics.pose().translate(-centerX, -centerY, 0f)
        }

        // Rendu de la texture de fond
        graphics.blit(BACKGROUND, startX, startY, 0f, 0f, WIDTH, HEIGHT, WIDTH, HEIGHT)

        val font = mc.font
        val totalText = WalletOverlay.formatCompact(currentBalance)
        val textW = font.width(totalText)

        // Centrer la valeur dans la zone gauche de la texture (les 44 premiers pixels de largeur)
        val textX = startX + 2 + (41 - textW) / 2
        val textY = startY + (HEIGHT - 8) / 2

        // Couleur de texte : interpolation Flash Color -> White si en animation
        val textColor = if (isAnimating) {
            val progress = elapsed.toFloat() / 500f
            val flash = activeTier.textFlashColor
            val flashR = (flash shr 16) and 0xFF
            val flashG = (flash shr 8) and 0xFF
            val flashB = flash and 0xFF
            val r = (flashR + (255 - flashR) * progress).toInt()
            val g = (flashG + (255 - flashG) * progress).toInt()
            val b = (flashB + (255 - flashB) * progress).toInt()
            (r shl 16) or (g shl 8) or b
        } else {
            0xFFFFFF
        }

        graphics.drawString(font, totalText, textX, textY, textColor, true)
        graphics.pose().popPose()

        // Mise à jour et rendu des particules (basé sur le temps système absolu)
        if (particles.isNotEmpty()) {
            val nowMs = System.currentTimeMillis()
            val iterator = particles.iterator()
            while (iterator.hasNext()) {
                val p = iterator.next()
                val elapsedMs = nowMs - p.spawnTime
                val t = elapsedMs.toFloat() / 1000f

                if (t >= p.maxLife) {
                    iterator.remove()
                } else {
                    // Calcul analytique de la position
                    val x = p.x0 + p.vx * t + kotlin.math.sin(t * p.driftFrequency + p.driftPhase) * p.driftAmplitude
                    val y = p.y0 + p.vy * t

                    // Fade in sur 0.2s, puis fade out progressif
                    val alpha = if (t < 0.2f) {
                        (t / 0.2f * 255f).toInt()
                    } else {
                        ((1.0f - (t - 0.2f) / (p.maxLife - 0.2f)) * 255f).toInt()
                    }.coerceIn(0, 255)

                    // Effet scintillant pour les étincelles
                    val finalAlpha = if (p.isSparkle) {
                        val flicker = kotlin.math.sin(t * 25f) * 0.3f + 0.7f
                        (alpha * flicker).toInt().coerceIn(0, 255)
                    } else {
                        alpha
                    }

                    val colorWithAlpha = (finalAlpha shl 24) or (p.color and 0xFFFFFF)
                    
                    if (p.isSparkle && t > 0.1f && t < p.maxLife - 0.2f) {
                        // Rendre une jolie petite étoile/croix scintillante "+"
                        val px = x.toInt()
                        val py = y.toInt()
                        graphics.fill(px, py - 1, px + 1, py + 2, colorWithAlpha)
                        graphics.fill(px - 1, py, px + 2, py + 1, colorWithAlpha)
                    } else {
                        graphics.fill(x.toInt(), y.toInt(), x.toInt() + 2, y.toInt() + 2, colorWithAlpha)
                    }
                }
            }
        }
    }
}
