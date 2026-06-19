package com.howlite.client.screen

import com.howlite.CobblemonGymOdyssey
import com.howlite.screen.BadgeCaseScreen.Region
import com.howlite.wallet.ClientWalletCache
import com.howlite.wallet.CoinType
import dev.architectury.networking.NetworkManager
import io.netty.buffer.Unpooled
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvents

/**
 * L'Autel des Sacrifices — Écran de pari.
 *
 * Thème visuel : corruption sombre, bords rouges pulsants, particules de flammes ascendantes.
 * Textures utilisées depuis `textures/gui/altar/`.
 *
 * Layout (basé sur altar_background.png) :
 *  - Fond principal (altar_background.png)
 *  - Zone de saisie de mise avec +/- par type de pièce
 *  - Sélection de difficulté : Easy / Medium / Hard
 *  - Bouton DÉFIER (accept_button.png) et RETOUR (refuse_button.png)
 */
@Suppress("DEPRECATION")
class AltarScreen(private val region: Region) : Screen(Component.literal("L'Autel des Sacrifices")) {

    companion object {
        const val GUI_WIDTH  = 184
        const val GUI_HEIGHT = 140

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
        // Gold (CGC) = 10,000 CCC; Platinum (CPC) = 1,000,000 CCC
        fun getMaxBetCCC(region: Region): Long = when (region) {
            Region.UNOVA  -> 100_000L   // 10 Gold
            Region.ALOLA  -> 200_000L   // 20 Gold
            Region.PALDEA -> 1_000_000L // 100 Gold / 1 Platinum
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
    private var betGold: Long = 0L       // Gold coins (CGC) in bet (max 99)
    private var betSilver: Long = 0L     // Silver coins (CSC) in bet (max 99)
    private var betCopper: Long = 0L     // Copper coins (CCC) in bet (max 99)
    private var selectedDifficulty: Int = 1  // 1=Easy, 2=Medium, 3=Hard

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

    private fun totalBetCCC(): Long =
        betGold * CoinType.GOLD.valueCCC +
        betSilver * CoinType.SILVER.valueCCC +
        betCopper * CoinType.COPPER.valueCCC

    private fun formatBalance(): String {
        val p = ClientWalletCache.platinum
        val g = ClientWalletCache.gold
        val s = ClientWalletCache.silver
        val c = ClientWalletCache.copper
        val parts = mutableListOf<String>()
        if (p > 0) parts += "${p}CPC"
        if (g > 0) parts += "${g}CGC"
        if (s > 0) parts += "${s}CSC"
        parts += "${c}CCC"
        return parts.joinToString(" ")
    }

    private fun formatBet(): String {
        val ccc = totalBetCCC()
        if (ccc == 0L) return "0 CCC"
        val g = ccc / CoinType.GOLD.valueCCC
        val rem = ccc % CoinType.GOLD.valueCCC
        val s = rem / CoinType.SILVER.valueCCC
        val cu = rem % CoinType.SILVER.valueCCC
        val parts = mutableListOf<String>()
        if (g > 0) parts += "${g}CGC"
        if (s > 0) parts += "${s}CSC"
        if (cu > 0) parts += "${cu}CCC"
        return parts.joinToString(" ")
    }

    private fun clampBet() {
        val maxCCC = getMaxBetCCC(region)
        val balance = ClientWalletCache.balance

        // Cap each denomination independently first, then overall
        betGold   = betGold.coerceIn(0, 99)
        betSilver = betSilver.coerceIn(0, 99)
        betCopper = betCopper.coerceIn(0, 99)

        // Clamp total to max and balance
        val total = totalBetCCC()
        if (total > maxCCC) {
            // Scale back proportionally from copper upward
            val excess = total - maxCCC
            val copperDrain = minOf(betCopper * CoinType.COPPER.valueCCC, excess)
            betCopper -= copperDrain / CoinType.COPPER.valueCCC
            val rem1 = excess - copperDrain
            if (rem1 > 0) {
                val silverDrain = minOf(betSilver * CoinType.SILVER.valueCCC, rem1)
                betSilver -= silverDrain / CoinType.SILVER.valueCCC
                val rem2 = rem1 - silverDrain
                if (rem2 > 0) betGold -= (rem2 / CoinType.GOLD.valueCCC).coerceAtMost(betGold)
            }
        }
        if (totalBetCCC() > balance) {
            betGold = 0; betSilver = 0; betCopper = 0
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

        // ── 1. Darkening overlay panels (corruption glassmorphism) ──
        // Outer subtle dark gradient
        graphics.fill(gx - 2, gy - 2, gx + GUI_WIDTH + 2, gy + GUI_HEIGHT + 2, 0xCC0A0010.toInt())

        // ── 2. Pulsing crimson border ──
        val borderPulse = (180 + (kotlin.math.sin(clientTicks * 0.08) * 75)).toInt().coerceIn(0, 255)
        val borderColor = (borderPulse shl 24) or 0x990011
        // Top / Bottom / Left / Right borders (2px thick)
        graphics.fill(gx, gy, gx + GUI_WIDTH, gy + 2, borderColor)
        graphics.fill(gx, gy + GUI_HEIGHT - 2, gx + GUI_WIDTH, gy + GUI_HEIGHT, borderColor)
        graphics.fill(gx, gy + 2, gx + 2, gy + GUI_HEIGHT - 2, borderColor)
        graphics.fill(gx + GUI_WIDTH - 2, gy + 2, gx + GUI_WIDTH, gy + GUI_HEIGHT - 2, borderColor)

        // ── 3. Background texture ──
        graphics.blit(BG_TEXTURE, gx, gy, 0f, 0f, GUI_WIDTH, GUI_HEIGHT, 184, 97)

        // ── 4. Title ──
        val title = "✦ L'AUTEL DES SACRIFICES ✦"
        val titleW = font.width(title)
        graphics.drawString(font, title, gx + (GUI_WIDTH - titleW) / 2, gy + 6, 0xFFCC2222.toInt(), true)

        // ── 5. Region / limit info ──
        val maxGold = getMaxBetCCC(region) / CoinType.GOLD.valueCCC
        val limitText = "Limite: ${maxGold}CGC  •  Région: ${region.name}"
        val limitW = font.width(limitText)
        graphics.drawString(font, limitText, gx + (GUI_WIDTH - limitW) / 2, gy + 18, 0xFF886644.toInt(), false)

        // ── 6. Balance display ──
        val balText = "Solde: ${formatBalance()}"
        val balW = font.width(balText)
        graphics.drawString(font, balText, gx + (GUI_WIDTH - balW) / 2, gy + 28, 0xFFFFCC44.toInt(), false)

        // ── 7. Bet adjustment row (Gold / Silver / Copper) ──
        renderCoinRow(graphics, gx, gy, mouseX, mouseY, "CGC", betGold,   0, 43)
        renderCoinRow(graphics, gx, gy, mouseX, mouseY, "CSC", betSilver, 1, 57)
        renderCoinRow(graphics, gx, gy, mouseX, mouseY, "CCC", betCopper, 2, 71)

        // ── 8. Total bet display ──
        val betLabel = "Mise totale: ${formatBet()}"
        val betLabelW = font.width(betLabel)
        graphics.drawString(font, betLabel, gx + (GUI_WIDTH - betLabelW) / 2, gy + 85, 0xFFFFFFFF.toInt(), false)

        // ── 9. Difficulty selection ──
        renderDifficultyButtons(graphics, gx, gy, mouseX, mouseY)

        // ── 10. Feedback message ──
        if (feedbackTicks > 0 && feedbackMsg != null) {
            val alpha = (feedbackTicks.toFloat() / 60f * 255).toInt().coerceIn(0, 255)
            val fc = (alpha shl 24) or 0xFF4444
            val fw = font.width(feedbackMsg!!)
            graphics.drawString(font, feedbackMsg!!, gx + (GUI_WIDTH - fw) / 2, gy + GUI_HEIGHT - 30, fc, true)
        }

        // ── 11. Accept / Refuse buttons ──
        val acceptX = gx + 8
        val acceptY = gy + GUI_HEIGHT - 21
        val acceptW = 73; val acceptH = 17
        val refuseX = gx + GUI_WIDTH - 8 - 73
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

        val acceptLabel = "DÉFIER"
        val refuseLabel = "RETOUR"
        val acColor = if (isAcceptH) 0xFFFF6666.toInt() else 0xFFFF2222.toInt()
        val refColor = if (isRefuseH) 0xFFCCCCCC.toInt() else 0xFFAAAAAA.toInt()
        graphics.drawString(font, acceptLabel, acceptX + (73 - font.width(acceptLabel)) / 2, acceptY + 4, acColor, true)
        graphics.drawString(font, refuseLabel, refuseX + (73 - font.width(refuseLabel)) / 2, refuseY + 4, refColor, false)

        // ── 12. Flame particles (on top) ──
        particles.forEach { it.render(graphics) }

    }

    /**
     * Renders a single coin denomination row:
     *   [–] [qty background showing value] [+]   LABEL
     *
     * @param coinIndex 0=Gold, 1=Silver, 2=Copper
     * @param rowY      Y offset from guiY
     */
    private fun renderCoinRow(
        graphics: GuiGraphics,
        gx: Int, gy: Int,
        mouseX: Int, mouseY: Int,
        label: String,
        value: Long,
        coinIndex: Int,
        rowY: Int
    ) {
        val startX = gx + 20

        // Label
        graphics.drawString(font, label, startX, gy + rowY + 3, 0xFFCCBB44.toInt(), false)

        val labelW = font.width(label)
        val controlX = startX + labelW + 4

        // Minus button (11×12)
        val minX = controlX
        val minY = gy + rowY
        val isMinHover = mouseX in minX until minX + 11 && mouseY in minY until minY + 12
        val minV = if (isMinHover) 12f else 0f
        graphics.blit(MINUS_TEXTURE, minX, minY, 0f, minV, 11, 12, 11, 24)

        // Qty background + value (37×12)
        val qtyX = controlX + 13
        graphics.blit(QTY_BG_TEXTURE, qtyX, gy + rowY, 0f, 0f, 37, 12, 37, 12)
        val valStr = value.toString()
        val valW = font.width(valStr)
        graphics.drawString(font, valStr, qtyX + (37 - valW) / 2, gy + rowY + 2, 0xFFFFFFFF.toInt(), false)

        // Plus button (11×12)
        val plusX = controlX + 52
        val plusY = gy + rowY
        val isPlusHover = mouseX in plusX until plusX + 11 && mouseY in plusY until plusY + 12
        val plusV = if (isPlusHover) 12f else 0f
        graphics.blit(PLUS_TEXTURE, plusX, plusY, 0f, plusV, 11, 12, 11, 24)
    }

    private fun renderDifficultyButtons(
        graphics: GuiGraphics,
        gx: Int, gy: Int,
        mouseX: Int, mouseY: Int
    ) {
        val labels = listOf("Easy ×1.5", "Medium ×2", "Hard ×3")
        val colors = listOf(0xFF44CC44.toInt(), 0xFFFFAA22.toInt(), 0xFFCC2222.toInt())
        val btnW = 48; val btnH = 12
        val totalW = labels.size * btnW + (labels.size - 1) * 4
        var bx = gx + (GUI_WIDTH - totalW) / 2

        graphics.drawString(font, "Difficulté:", gx + 8, gy + 96, 0xFF888888.toInt(), false)

        labels.forEachIndexed { i, lbl ->
            val by = gy + 105
            val isSelected = (i + 1) == selectedDifficulty
            val isHovered  = mouseX in bx until bx + btnW && mouseY in by until by + btnH

            // Button bg
            val bgAlpha = if (isSelected) 0xCC else if (isHovered) 0x88 else 0x44
            val bgColor = (bgAlpha shl 24) or (colors[i] and 0x00FFFFFF)
            graphics.fill(bx, by, bx + btnW, by + btnH, bgColor)

            // Border if selected
            if (isSelected) {
                graphics.fill(bx, by, bx + btnW, by + 1, colors[i])
                graphics.fill(bx, by + btnH - 1, bx + btnW, by + btnH, colors[i])
                graphics.fill(bx, by, bx + 1, by + btnH, colors[i])
                graphics.fill(bx + btnW - 1, by, bx + btnW, by + btnH, colors[i])
            }

            val textColor = if (isSelected || isHovered) 0xFFFFFF else 0xAAAAAA
            val tw = font.width(lbl)
            graphics.drawString(font, lbl, bx + (btnW - tw) / 2, by + 2, textColor, false)

            // Max pokemon hint
            val hint = "${getMaxPokemon(i + 1)} Pkm max"
            val hw = font.width(hint)
            graphics.drawString(font, hint, bx + (btnW - hw) / 2, by + btnH + 2, 0xFF666666.toInt(), false)

            bx += btnW + 4
        }
    }

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val gx = (width  - GUI_WIDTH)  / 2
        val gy = (height - GUI_HEIGHT) / 2

        // ── Coin rows ──
        // We recalculate coin row positions (mirror of renderCoinRow logic)
        val labels = listOf("CGC", "CSC", "CCC")
        val rowYs = intArrayOf(43, 57, 71)
        for (idx in 0..2) {
            val startX = gx + 20
            val lw = font.width(labels[idx])
            val controlX = startX + lw + 4
            val minX = controlX; val qtyX = controlX + 13; val plusX = controlX + 52
            val rowY = gy + rowYs[idx]
            val isMin  = mouseX.toInt() in minX until minX + 11  && mouseY.toInt() in rowY until rowY + 12
            val isPlus = mouseX.toInt() in plusX until plusX + 11 && mouseY.toInt() in rowY until rowY + 12
            if (isMin || isPlus) {
                val delta = if (isPlus) 1L else -1L
                when (idx) {
                    0 -> betGold   = (betGold   + delta).coerceAtLeast(0)
                    1 -> betSilver = (betSilver + delta).coerceAtLeast(0)
                    2 -> betCopper = (betCopper + delta).coerceAtLeast(0)
                }
                clampBet()
                minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.2f))
                return true
            }
        }

        // ── Difficulty buttons ──
        val diffLabels = listOf("Easy ×1.5", "Medium ×2", "Hard ×3")
        val btnW = 48; val btnH = 12
        val totalW = diffLabels.size * btnW + (diffLabels.size - 1) * 4
        var bx = gx + (GUI_WIDTH - totalW) / 2
        val by = gy + 105
        for (i in 0..2) {
            if (mouseX.toInt() in bx until bx + btnW && mouseY.toInt() in by until by + btnH) {
                selectedDifficulty = i + 1
                minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f))
                return true
            }
            bx += btnW + 4
        }

        // ── Accept (DÉFIER) ──
        val acceptX = gx + 8
        val acceptY = gy + GUI_HEIGHT - 21
        val acceptW = 73; val acceptH = 17
        if (mouseX.toInt() in acceptX until acceptX + acceptW && mouseY.toInt() in acceptY until acceptY + acceptH) {
            handleDefier()
            return true
        }

        // ── Refuse (RETOUR) ──
        val refuseX = gx + GUI_WIDTH - 8 - 73
        if (mouseX.toInt() in refuseX until refuseX + 73 && mouseY.toInt() in acceptY until acceptY + acceptH) {
            minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f))
            onClose()
            return true
        }

        return super.mouseClicked(mouseX, mouseY, button)
    }

    private fun handleDefier() {
        val bet = totalBetCCC()

        // ── Validations ──
        if (bet <= 0L) {
            showFeedback("Mise invalide : entrez un montant > 0 !")
            minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.DISPENSER_FAIL, 1.0f))
            return
        }
        if (bet > ClientWalletCache.balance) {
            showFeedback("Solde insuffisant !")
            minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.DISPENSER_FAIL, 1.0f))
            return
        }
        val maxBet = getMaxBetCCC(region)
        if (bet > maxBet) {
            showFeedback("Mise dépasse la limite de cette région !")
            minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.DISPENSER_FAIL, 1.0f))
            return
        }

        // ── Send C2S packet ──
        minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f))

        val buf = RegistryFriendlyByteBuf(
            Unpooled.buffer(),
            minecraft?.level?.registryAccess() ?: run {
                showFeedback("Erreur interne — veuillez réessayer.")
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
