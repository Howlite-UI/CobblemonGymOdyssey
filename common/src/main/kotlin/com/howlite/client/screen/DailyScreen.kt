package com.howlite.client.screen

import com.howlite.CobblemonGymOdyssey
import com.howlite.screen.BadgeCaseScreen
import com.howlite.screen.BadgeCaseScreen.Region
import com.howlite.wallet.ClientWalletCache
import com.howlite.wallet.WalletManager
import com.mojang.blaze3d.systems.RenderSystem
import dev.architectury.networking.NetworkManager
import io.netty.buffer.Unpooled
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvents
import java.util.Random

/**
 * Interface de Récupération de l'Allocation Quotidienne (Daily Allowance Screen).
 * Rendu pixel-art premium avec particules d'étincelles dorées, bruitages et retours d'états dynamiques.
 */
@Suppress("DEPRECATION")
class DailyScreen(
    private val region: Region,
    private val parentScreen: BadgeCaseScreen
) : Screen(Component.literal("Allocation Quotidienne")) {

    companion object {
        const val GUI_WIDTH = 184
        const val GUI_HEIGHT = 122

        val BG_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CobblemonGymOdyssey.MOD_ID, "textures/gui/daily/daily_background.png"
        )
        val ACCEPT_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CobblemonGymOdyssey.MOD_ID, "textures/gui/daily/accept_button.png"
        )
    }

    private val random = Random()
    private val particles = mutableListOf<SparkleParticle>()
    private var clientTicks = 0

    // Particle effect representing gold sparkles
    inner class SparkleParticle(
        var x: Float,
        var y: Float,
        val color: Int,
        val maxAge: Int
    ) {
        private var vx = (random.nextFloat() - 0.5f) * 0.4f
        private var vy = -(random.nextFloat() * 0.4f + 0.2f)
        var age = 0

        fun tick() {
            x += vx
            y += vy
            age++
        }

        fun render(graphics: GuiGraphics) {
            val alpha = ((1f - age.toFloat() / maxAge) * 200).toInt().coerceIn(0, 200)
            val finalColor = (alpha shl 24) or (color and 0x00FFFFFF)
            val size = if (age < maxAge / 2) 2 else 1
            graphics.fill(x.toInt(), y.toInt(), x.toInt() + size, y.toInt() + size, finalColor)
        }
    }

    override fun init() {
        super.init()
        minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, 0.6f))
    }

    override fun tick() {
        super.tick()
        clientTicks++

        // Emit gold sparkle particles if there is any unclaimed allowance
        val gx = (width - GUI_WIDTH) / 2
        val gy = (height - GUI_HEIGHT) / 2
        val todayStr = java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString()
        val allClaimed = Region.entries.all { r -> parentScreen.menu.dailyAllowanceClaims[r.name] == todayStr }

        if (!allClaimed && clientTicks % 3 == 0) {
            val px = (gx + 10 + random.nextInt(GUI_WIDTH - 20)).toFloat()
            val py = (gy + GUI_HEIGHT - 6).toFloat()
            particles.add(SparkleParticle(px, py, 0xFFFFA800.toInt(), 20 + random.nextInt(15)))
        }

        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.tick()
            if (p.age >= p.maxAge) {
                iterator.remove()
            }
        }
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(graphics, mouseX, mouseY, partialTick)

        val gx = (width - GUI_WIDTH) / 2
        val gy = (height - GUI_HEIGHT) / 2

        // 1. Draw Background
        graphics.blit(BG_TEXTURE, gx, gy, 0f, 0f, GUI_WIDTH, GUI_HEIGHT, 184, 122)

        // 2. Draw Title
        val title = Component.translatable("cobblemongymodyssey.daily.title").string
        val titleW = font.width(title)
        graphics.drawString(font, title, gx + (GUI_WIDTH - titleW) / 2, gy + 7, 0xFFFFFFFF.toInt(), true)

        // 3. Stats calculation (progressive rewards based on region index)
        val unlockedInRegion = region.badges.count { it in parentScreen.menu.unlockedBadges }
        val totalBadgesInRegion = region.badges.size

        val baseVal = (region.ordinal + 1) * 10_000L
        val bonusVal = unlockedInRegion * (region.ordinal + 1) * 5_000L
        val totalVal = baseVal + bonusVal

        val todayStr = java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString()
        val alreadyClaimed = parentScreen.menu.dailyAllowanceClaims[region.name] == todayStr
        val allClaimed = Region.entries.all { r -> parentScreen.menu.dailyAllowanceClaims[r.name] == todayStr }

        // Display labels inside the central body area
        val labelBase = Component.translatable("cobblemongymodyssey.daily.base_reward", WalletManager.formatCCC(baseVal)).string
        val labelBadges = Component.translatable("cobblemongymodyssey.daily.badges_unlocked", unlockedInRegion, totalBadgesInRegion).string
        val labelBonus = Component.translatable("cobblemongymodyssey.daily.badge_bonus", WalletManager.formatCCC(bonusVal)).string
        val labelTotal = Component.translatable("cobblemongymodyssey.daily.total_reward", WalletManager.formatCCC(totalVal)).string

        val textX = gx + 92
        val textColor = 0xFF222222.toInt() // dark charcoal color for readability

        graphics.drawString(font, labelBase, textX - font.width(labelBase) / 2, gy + 22, textColor, false)
        graphics.drawString(font, labelBadges, textX - font.width(labelBadges) / 2, gy + 35, textColor, false)
        graphics.drawString(font, labelBonus, textX - font.width(labelBonus) / 2, gy + 48, textColor, false)
        graphics.drawString(font, labelTotal, textX - font.width(labelTotal) / 2, gy + 61, textColor, false)

        // Status text label
        val statusText = if (alreadyClaimed) {
            Component.translatable("cobblemongymodyssey.daily.status_claimed").string
        } else {
            Component.translatable("cobblemongymodyssey.daily.status_ready").string
        }
        val statusColor = if (alreadyClaimed) 0xFF8E8E93.toInt() else 0xFF21A354.toInt()
        graphics.drawString(font, statusText, textX - font.width(statusText) / 2, gy + 74, statusColor, false)

        // 4. Buttons rendering (side-by-side: gx + 12 and gx + 99)
        // A. CLAIM Button (73x17)
        val acceptX = gx + 12
        val acceptY = gy + 88
        val isHovered = mouseX >= acceptX && mouseX < acceptX + 73 && mouseY >= acceptY && mouseY < acceptY + 17
        val buttonV = if (isHovered && !alreadyClaimed) 17f else 0f

        if (alreadyClaimed) {
            RenderSystem.setShaderColor(0.5f, 0.5f, 0.5f, 1.0f)
        }
        graphics.blit(ACCEPT_TEXTURE, acceptX, acceptY, 0f, buttonV, 73, 17, 73, 34)
        if (alreadyClaimed) {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
        }

        val btnLabel = if (alreadyClaimed) {
            Component.translatable("cobblemongymodyssey.daily.claimed").string
        } else {
            Component.translatable("cobblemongymodyssey.daily.claim").string
        }
        val btnColor = if (alreadyClaimed) 0xFFCCCCCC.toInt() else if (isHovered) 0xFFFFA800.toInt() else 0xFFFFFF
        graphics.drawString(font, btnLabel, acceptX + (73 - font.width(btnLabel)) / 2, acceptY + 4, btnColor, true)

        // B. CLAIM ALL Button (73x17)
        val claimAllX = gx + 99
        val claimAllY = gy + 88
        val isAllHovered = mouseX >= claimAllX && mouseX < claimAllX + 73 && mouseY >= claimAllY && mouseY < claimAllY + 17
        val allButtonV = if (isAllHovered && !allClaimed) 17f else 0f

        if (allClaimed) {
            RenderSystem.setShaderColor(0.5f, 0.5f, 0.5f, 1.0f)
        }
        graphics.blit(ACCEPT_TEXTURE, claimAllX, claimAllY, 0f, allButtonV, 73, 17, 73, 34)
        if (allClaimed) {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
        }

        val allBtnLabel = if (allClaimed) {
            Component.translatable("cobblemongymodyssey.daily.claimed_all").string
        } else {
            Component.translatable("cobblemongymodyssey.daily.claim_all").string
        }
        val allBtnColor = if (allClaimed) 0xFFCCCCCC.toInt() else if (isAllHovered) 0xFFFFA800.toInt() else 0xFFFFFF
        graphics.drawString(font, allBtnLabel, claimAllX + (73 - font.width(allBtnLabel)) / 2, claimAllY + 4, allBtnColor, true)

        // 5. Render sparkles on top
        particles.forEach { it.render(graphics) }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val gx = (width - GUI_WIDTH) / 2
        val gy = (height - GUI_HEIGHT) / 2

        val acceptX = gx + 12
        val acceptY = gy + 88

        val todayStr = java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString()

        // Click on CLAIM (Single region)
        if (mouseX >= acceptX && mouseX < acceptX + 73 && mouseY >= acceptY && mouseY < acceptY + 17) {
            val alreadyClaimed = parentScreen.menu.dailyAllowanceClaims[region.name] == todayStr

            if (alreadyClaimed) {
                minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.DISPENSER_FAIL, 1.0f))
            } else {
                minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f))

                val buf = RegistryFriendlyByteBuf(
                    Unpooled.buffer(),
                    minecraft?.level?.registryAccess() ?: throw IllegalStateException("Registry access not available")
                )
                buf.writeUtf(region.name)

                NetworkManager.sendToServer(
                    ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "claim_daily_allowance"),
                    buf
                )
                onClose()
            }
            return true
        }

        // Click on CLAIM ALL
        val claimAllX = gx + 99
        if (mouseX >= claimAllX && mouseX < claimAllX + 73 && mouseY >= acceptY && mouseY < acceptY + 17) {
            val allClaimed = Region.entries.all { r -> parentScreen.menu.dailyAllowanceClaims[r.name] == todayStr }

            if (allClaimed) {
                minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.DISPENSER_FAIL, 1.0f))
            } else {
                minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f))

                val buf = RegistryFriendlyByteBuf(
                    Unpooled.buffer(),
                    minecraft?.level?.registryAccess() ?: throw IllegalStateException("Registry access not available")
                )

                NetworkManager.sendToServer(
                    ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "claim_all_daily_allowances"),
                    buf
                )
                onClose()
            }
            return true
        }

        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun isPauseScreen(): Boolean = false
}

