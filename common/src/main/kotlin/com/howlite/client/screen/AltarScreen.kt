package com.howlite.client.screen

import com.howlite.CobblemonGymOdyssey
import com.howlite.screen.BadgeCaseScreen.Region
import com.howlite.wallet.ClientWalletCache
import com.howlite.wallet.CoinType
import dev.architectury.networking.NetworkManager
import io.netty.buffer.Unpooled
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvents

/**
 * L'Autel des Sacrifices — Écran de pari.
 *
 * Reworked layout based on altar_background.png (184x135):
 *  - Header bar: Title.
 *  - Middle Grey Box: Structured description, regional max limit, difficulty info.
 *  - Input Row: [-] [ EditBox (CGC) ] [+] supporting Shift (+10) and Ctrl+Shift (max/min).
 *  - Buttons Row: RETOUR / DÉFIER.
 *  - Bottom Bar: Short-formatted player balance (e.g. 100k).
 */
@Suppress("DEPRECATION")
class AltarScreen(private val region: Region, private val parentScreen: Screen? = null) : Screen(Component.literal("L'Autel des Sacrifices")) {

    companion object {
        const val GUI_WIDTH  = 184
        const val GUI_HEIGHT = 135

        val BG_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CobblemonGymOdyssey.MOD_ID, "textures/gui/altar/altar_background.png"
        )
        val ACCEPT_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CobblemonGymOdyssey.MOD_ID, "textures/gui/altar/accept_button.png"
        )
        val REFUSE_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CobblemonGymOdyssey.MOD_ID, "textures/gui/altar/refuse_button.png"
        )
        val PLUS_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CobblemonGymOdyssey.MOD_ID, "textures/gui/altar/quantity_text_plus_button.png"
        )
        val MINUS_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CobblemonGymOdyssey.MOD_ID, "textures/gui/altar/quantity_text_minus_button.png"
        )
        val QTY_BG_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CobblemonGymOdyssey.MOD_ID, "textures/gui/altar/quantity_text_background.png"
        )

        // Max bets per region in CCC (Copper Cobble Coins)
        fun getMaxBetCCC(region: Region): Long = when (region) {
            Region.UNOVA  -> 100_000L   // 10 Gold (CGC)
            Region.ALOLA  -> 200_000L   // 20 Gold (CGC)
            Region.PALDEA -> 1_000_000L // 100 Gold (CGC)
            else          -> 100_000L
        }

        // Payout multipliers
        fun getMultiplier(difficulty: Int): Double = when (difficulty) {
            1 -> 1.5  // Easy
            2 -> 2.0  // Medium
            3 -> 3.0  // Hard
            else -> 1.5
        }

        // Max pokemon allowed
        fun getMaxPokemon(difficulty: Int): Int = when (difficulty) {
            1 -> 3  // Easy  — 3 Pkm
            2 -> 2  // Medium — 2 Pkm
            3 -> 1  // Hard  — 1 Pkm
            else -> 3
        }
    }

    // ---- GUI state ----
    private var betGold: Long = 0L       // Gold coins (CGC) in bet
    private lateinit var betInput: EditBox

    private var selectedDifficulty: Int = when (region) {
        Region.UNOVA -> 1
        Region.ALOLA -> 2
        Region.PALDEA -> 3
        else -> 1
    }

    private var clientTicks = 0
    private val random = java.util.Random()
    private val particles = mutableListOf<FlameParticle>()

    // Feedback message (displayed briefly after validation errors)
    private var feedbackMsg: String? = null
    private var feedbackTicks = 0

    // Hover tracking for sound feedback
    private var lastAcceptHovered = false
    private var lastRefuseHovered = false

    /** Rising flame particle for the dark-corruption aesthetic */
    inner class FlameParticle(
        var x: Float,
        var y: Float,
        val color: Int,
        val maxAge: Int
    ) {
        var vx: Float = (random.nextFloat() - 0.5f) * 0.4f
        var vy: Float = -(random.nextFloat() * 0.5f + 0.3f)
        var age: Int = 0

        fun tick() {
            x += vx
            y += vy
            vx *= 0.97f
            age++
        }

        fun render(graphics: GuiGraphics) {
            val alpha = ((1f - age.toFloat() / maxAge) * 200).toInt().coerceIn(0, 200)
            val c = (alpha shl 24) or (color and 0x00FFFFFF)
            val size = if (age < maxAge / 3) 2 else 1
            graphics.fill(x.toInt(), y.toInt(), x.toInt() + size, y.toInt() + size, c)
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun totalBetCCC(): Long = betGold * CoinType.GOLD.valueCCC

    private fun formatShortBalance(value: Long): String {
        return if (value >= 1_000_000_000L) {
            val d = value / 1_000_000_000.0
            String.format(java.util.Locale.US, "%.1fB", d)
        } else if (value >= 1_000_000L) {
            val d = value / 1_000_000.0
            String.format(java.util.Locale.US, "%.1fM", d)
        } else if (value >= 1_000L) {
            val d = value / 1_000.0
            if (value % 1000 == 0L) {
                "${value / 1000}k"
            } else {
                String.format(java.util.Locale.US, "%.1fk", d)
            }
        } else {
            value.toString()
        }
    }

    private fun showFeedback(msg: String) {
        feedbackMsg = msg
        feedbackTicks = 60
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    override fun init() {
        super.init()
        minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, 0.6f))

        val gx = (width - GUI_WIDTH) / 2
        val gy = (height - GUI_HEIGHT) / 2

        // Set initial bet value based on limit
        val maxBetCGC = getMaxBetCCC(region) / 10000L
        val playerBalanceCGC = ClientWalletCache.balance / 10000L
        val limitCGC = minOf(maxBetCGC, playerBalanceCGC)
        betGold = betGold.coerceIn(0L, limitCGC)

        betInput = EditBox(
            font,
            gx + 73 + 4,
            gy + 81,
            29,
            9,
            Component.literal("Bet")
        )
        betInput.isBordered = false
        betInput.setTextColor(0xFFFFFFFF.toInt())
        betInput.value = betGold.toString()
        betInput.setFilter { it.all { char -> char.isDigit() } }
        betInput.setResponder { text ->
            val parsed = text.toLongOrNull() ?: 0L
            val maxCGC = getMaxBetCCC(region) / 10000L
            val balCGC = ClientWalletCache.balance / 10000L
            val limit = minOf(maxCGC, balCGC)

            if (parsed > limit) {
                betInput.value = limit.toString()
                betGold = limit
            } else {
                betGold = parsed.coerceAtLeast(0L)
            }
        }
        betInput.isFocused = true
        addRenderableWidget(betInput)
    }

    override fun tick() {
        super.tick()
        clientTicks++

        // Spawn rising flame particles from the bottom of the GUI
        val guiX = (width - GUI_WIDTH) / 2
        val guiY = (height - GUI_HEIGHT) / 2
        if (clientTicks % 3 == 0) {
            val px = (guiX + 4 + random.nextInt(GUI_WIDTH - 8)).toFloat()
            val py = (guiY + GUI_HEIGHT - 2).toFloat()
            val colorChoice = if (random.nextBoolean()) 0xFF3300 else 0x990033
            particles.add(FlameParticle(px, py, colorChoice, 18 + random.nextInt(12)))
        }

        val it = particles.iterator()
        while (it.hasNext()) {
            val p = it.next(); p.tick(); if (p.age >= p.maxAge) it.remove()
        }

        if (feedbackTicks > 0) feedbackTicks--
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(graphics, mouseX, mouseY, partialTick)

        val gx = (width  - GUI_WIDTH)  / 2
        val gy = (height - GUI_HEIGHT) / 2

        // ── 1. Background texture ──
        graphics.blit(BG_TEXTURE, gx, gy, 0f, 0f, GUI_WIDTH, GUI_HEIGHT, 184, 135)

        // ── 2. Title (centered on top purple header bar) ──
        val title = Component.translatable("cobblemongymodyssey.altar.title").string
        val titleW = font.width(title)
        graphics.drawString(font, title, gx + (GUI_WIDTH - titleW) / 2, gy + 6, 0xFFFFFFFF.toInt(), true)

        // ── 3. Structured Text inside the Description Box (Y: 16 to 74) ──
        val maxGold = getMaxBetCCC(region) / 10000L
        
        val diffNameKey = when (selectedDifficulty) {
            1 -> "cobblemongymodyssey.altar.easy"
            2 -> "cobblemongymodyssey.altar.medium"
            3 -> "cobblemongymodyssey.altar.hard"
            else -> "cobblemongymodyssey.altar.easy"
        }
        val diffName = Component.translatable(diffNameKey).string
        val bossLevel = when (selectedDifficulty) {
            1 -> 150
            2 -> 200
            3 -> 300
            else -> 150
        }
        val mult = getMultiplier(selectedDifficulty)
        val maxPkm = getMaxPokemon(selectedDifficulty)

        val badgeCaseScreen = parentScreen as? com.howlite.screen.BadgeCaseScreen
        val fightsToday = badgeCaseScreen?.menu?.altarFightsToday?.get(region.name) ?: 0
        val remainingFights = (5 - fightsToday).coerceAtLeast(0)

        val line2 = Component.translatable("cobblemongymodyssey.altar.desc.difficulty", diffName, bossLevel.toString()).string
        val line3 = Component.translatable("cobblemongymodyssey.altar.desc.details", mult.toString(), maxPkm.toString()).string
        val line4 = Component.translatable("cobblemongymodyssey.altar.desc.limits", maxGold.toString(), remainingFights.toString()).string

        val textX = gx + 92
        val textColor = 0xFF222222.toInt() // Dark text for the light grey box

        graphics.drawString(font, line2, textX - font.width(line2) / 2, gy + 24, textColor, false)
        graphics.drawString(font, line3, textX - font.width(line3) / 2, gy + 38, textColor, false)
        graphics.drawString(font, line4, textX - font.width(line4) / 2, gy + 52, textColor, false)

        // ── 4. Bet adjustment row buttons ──
        // Minus button (11x12)
        val minX = gx + 62
        val minY = gy + 79
        val isMinHover = mouseX in minX until minX + 11 && mouseY in minY until minY + 12
        val minV = if (isMinHover) 12f else 0f
        graphics.blit(MINUS_TEXTURE, minX, minY, 0f, minV, 11, 12, 11, 24)

        // Quantity background (37x12)
        val qtyX = gx + 73
        graphics.blit(QTY_BG_TEXTURE, qtyX, gy + 79, 0f, 0f, 37, 12, 37, 12)

        // Plus button (11x12)
        val plusX = gx + 110
        val plusY = gy + 79
        val isPlusHover = mouseX in plusX until plusX + 11 && mouseY in plusY until plusY + 12
        val plusV = if (isPlusHover) 12f else 0f
        graphics.blit(PLUS_TEXTURE, plusX, plusY, 0f, plusV, 11, 12, 11, 24)

        // ── 5. Feedback message ──
        if (feedbackTicks > 0 && feedbackMsg != null) {
            val alpha = (feedbackTicks.toFloat() / 60f * 255).toInt().coerceIn(0, 255)
            val fc = (alpha shl 24) or 0xFF4444
            val fw = font.width(feedbackMsg!!)
            graphics.drawString(font, feedbackMsg!!, gx + (GUI_WIDTH - fw) / 2, gy + 91, fc, true)
        }

        // ── 6. Accept / Refuse buttons ──
        val acceptX = gx + 99
        val acceptY = gy + 96
        val acceptW = 73; val acceptH = 17
        val refuseX = gx + 12
        val refuseY = acceptY

        val isAcceptH = mouseX in acceptX until acceptX + acceptW && mouseY in acceptY until acceptY + acceptH
        val isRefuseH = mouseX in refuseX until refuseX + 73 && mouseY in refuseY until refuseY + acceptH

        if (isAcceptH && !lastAcceptHovered)
            minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.1f))
        if (isRefuseH && !lastRefuseHovered)
            minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.1f))
        lastAcceptHovered = isAcceptH
        lastRefuseHovered = isRefuseH

        val aV = if (isAcceptH) 17f else 0f
        graphics.blit(ACCEPT_TEXTURE, acceptX, acceptY, 0f, aV, 73, 17, 73, 34)
        val rV = if (isRefuseH) 17f else 0f
        graphics.blit(REFUSE_TEXTURE, refuseX, refuseY, 0f, rV, 73, 17, 73, 34)

        val acceptLabel = Component.translatable("cobblemongymodyssey.altar.defier").string
        val refuseLabel = Component.translatable("cobblemongymodyssey.altar.back").string
        val acColor = if (isAcceptH) 0xFFFF6666.toInt() else 0xFFFF2222.toInt()
        val refColor = if (isRefuseH) 0xFFCCCCCC.toInt() else 0xFFAAAAAA.toInt()
        graphics.drawString(font, acceptLabel, acceptX + (73 - font.width(acceptLabel)) / 2, acceptY + 4, acColor, true)
        graphics.drawString(font, refuseLabel, refuseX + (73 - font.width(refuseLabel)) / 2, refuseY + 4, refColor, false)

        // ── 7. Player Balance display at the bottom ──
        val balText = formatShortBalance(ClientWalletCache.balance)
        val balW = font.width(balText)
        graphics.drawString(font, balText, gx + 95 - balW, gy + 121, 0xFFFFFFFF.toInt(), false)

        // ── 8. Flame particles (on top) ──
        particles.forEach { it.render(graphics) }
    }

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val gx = (width  - GUI_WIDTH)  / 2
        val gy = (height - GUI_HEIGHT) / 2

        // ── Minus button (11x12) at (gx + 62, gy + 79) ──
        val minX = gx + 62
        val minY = gy + 79
        if (mouseX.toInt() in minX until minX + 11 && mouseY.toInt() in minY until minY + 12) {
            val isMin = hasShiftDown() && hasControlDown()
            if (isMin) {
                betGold = 0L
            } else {
                val delta = if (hasShiftDown()) 10L else 1L
                betGold = (betGold - delta).coerceAtLeast(0L)
            }
            betInput.value = betGold.toString()
            minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.2f))
            return true
        }

        // ── Plus button (11x12) at (gx + 110, gy + 79) ──
        val plusX = gx + 110
        val plusY = gy + 79
        if (mouseX.toInt() in plusX until plusX + 11 && mouseY.toInt() in plusY until plusY + 12) {
            val maxBetCGC = getMaxBetCCC(region) / 10000L
            val playerBalanceCGC = ClientWalletCache.balance / 10000L
            val limitCGC = minOf(maxBetCGC, playerBalanceCGC)

            val isMax = hasShiftDown() && hasControlDown()
            if (isMax) {
                betGold = limitCGC
            } else {
                val delta = if (hasShiftDown()) 10L else 1L
                betGold = (betGold + delta).coerceAtMost(limitCGC)
            }
            betInput.value = betGold.toString()
            minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.2f))
            return true
        }

        // ── Accept (DÉFIER) at (gx + 99, gy + 96) ──
        val acceptX = gx + 99
        val acceptY = gy + 96
        val acceptW = 73; val acceptH = 17
        if (mouseX.toInt() in acceptX until acceptX + acceptW && mouseY.toInt() in acceptY until acceptY + acceptH) {
            val badgeCaseScreen = parentScreen as? com.howlite.screen.BadgeCaseScreen
            val fightsToday = badgeCaseScreen?.menu?.altarFightsToday?.get(region.name) ?: 0
            if (fightsToday >= 5) {
                showFeedback(Component.translatable("cobblemongymodyssey.altar.msg.limit_reached").string)
                minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.DISPENSER_FAIL, 1.0f))
            } else {
                handleDefier()
            }
            return true
        }

        // ── Refuse (RETOUR) at (gx + 12, gy + 96) ──
        val refuseX = gx + 12
        if (mouseX.toInt() in refuseX until refuseX + 73 && mouseY.toInt() in acceptY until acceptY + acceptH) {
            minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f))
            if (parentScreen != null) {
                minecraft?.setScreen(parentScreen)
            } else {
                onClose()
            }
            return true
        }

        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (betInput.keyPressed(keyCode, scanCode, modifiers)) {
            return true
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun charTyped(codePoint: Char, modifiers: Int): Boolean {
        if (betInput.charTyped(codePoint, modifiers)) {
            return true
        }
        return super.charTyped(codePoint, modifiers)
    }

    private fun handleDefier() {
        val bet = totalBetCCC()

        // ── Validations ──
        if (bet <= 0L) {
            showFeedback(Component.translatable("cobblemongymodyssey.altar.msg.invalid_bet").string)
            minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.DISPENSER_FAIL, 1.0f))
            return
        }
        if (bet > ClientWalletCache.balance) {
            showFeedback(Component.translatable("cobblemongymodyssey.altar.msg.insufficient_balance").string)
            minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.DISPENSER_FAIL, 1.0f))
            return
        }
        val maxBet = getMaxBetCCC(region)
        if (bet > maxBet) {
            showFeedback(Component.translatable("cobblemongymodyssey.altar.msg.exceed_limit").string)
            minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.DISPENSER_FAIL, 1.0f))
            return
        }

        // ── Send C2S packet ──
        minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f))

        val buf = RegistryFriendlyByteBuf(
            Unpooled.buffer(),
            minecraft?.level?.registryAccess() ?: run {
                showFeedback(Component.translatable("cobblemongymodyssey.altar.msg.internal_error").string)
                return
            }
        )
        buf.writeLong(bet)
        buf.writeInt(selectedDifficulty)
        buf.writeUtf(region.name)

        NetworkManager.sendToServer(
            ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "request_altar_challenge"),
            buf
        )
        onClose()
    }

    override fun isPauseScreen(): Boolean = false
}
