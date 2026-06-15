package com.howlite.client.screen

import com.howlite.CobblemonGymOdyssey
import com.howlite.blocks.ConsumableRaidBlockEntity
import com.necro.raid.dens.common.data.raid.RaidBoss
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.client.render.models.blockbench.FloatingState
import com.cobblemon.mod.common.client.gui.drawProfilePokemon
import com.cobblemon.mod.common.util.math.fromEulerXYZDegrees
import dev.architectury.networking.NetworkManager
import io.netty.buffer.Unpooled
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvents
import org.joml.Quaternionf
import org.joml.Vector3f
import com.mojang.blaze3d.systems.RenderSystem

/**
 * Custom screen for Gym Odyssey consumable raids.
 * Displays the boss info, its star rating, types (using GUI icons), a rotating 3D model,
 * and two bottom buttons (Accept to enter, Deny to close).
 *
 * Refactored to match the premium standards of BadgeCaseScreen (juice, sounds, typewriter LCD, particles).
 * Restored the 2.0x crisp scale layout inside a pushed/popped pose stack to avoid blurriness.
 */
class ConsumableRaidScreen(
    private val pos: BlockPos,
    private val blockEntity: ConsumableRaidBlockEntity,
    private val raidBoss: RaidBoss
) : Screen(Component.literal("Consumable Raid GUI")) {

    companion object {
        const val GUI_WIDTH = 138
        const val GUI_HEIGHT = 98
        const val GUI_SCALE = 1f

        val BACKGROUND_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CobblemonGymOdyssey.MOD_ID,
            "textures/gui/consumable_raid_block_gui.png"
        )
        val ACCEPT_BUTTON_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CobblemonGymOdyssey.MOD_ID,
            "textures/gui/consumable_raid_block_gui_btn_accept.png"
        )
        val DENY_BUTTON_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CobblemonGymOdyssey.MOD_ID,
            "textures/gui/consumable_raid_block_gui_btn_deny.png"
        )
    }

    /** Custom GUI particle class for raid den effects */
    class GuiParticle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        val color: Int,
        val maxAge: Int
    ) {
        var age = 0

        fun tick() {
            x += vx
            y += vy
            vy -= 0.01f // Rise upwards slightly (raid den energy)
            vx *= 0.96f
            age++
        }

        fun render(graphics: GuiGraphics) {
            val alpha = ((1.0f - (age.toFloat() / maxAge.toFloat())) * 255).toInt().coerceIn(0, 255)
            val finalColor = (alpha shl 24) or (color and 0x00FFFFFF)
            val size = if (age > maxAge * 0.7) 1 else 2
            graphics.fill(x.toInt(), y.toInt(), x.toInt() + size, y.toInt() + size, finalColor)
        }
    }

    private var pokemon: Pokemon? = null
    private val floatingState = FloatingState()
    private var clientTicks = 0

    // Juice elements
    private val particles = mutableListOf<GuiParticle>()
    private val random = java.util.Random()
    private var lcdTextProgress = 0f
    private var lastCharsCount = 0

    // Hover state tracking for sound feedback
    private var lastAcceptHovered = false
    private var lastDenyHovered = false
    private var lastRenderTime = System.currentTimeMillis()

    override fun init() {
        super.init()
        try {
            pokemon = raidBoss.bossProperties?.create()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Play initial chime on GUI open
        minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, 0.7f))
    }

    override fun tick() {
        super.tick()
        clientTicks++

        // LCD typewriter progress
        if (lcdTextProgress < 100f) {
            lcdTextProgress += 1.2f
        }

        // Tick particles
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.tick()
            if (p.age >= p.maxAge) {
                iterator.remove()
            }
        }
    }

    private fun setupNearestNeighbor(texture: ResourceLocation) {
        RenderSystem.setShaderTexture(0, texture)
        com.mojang.blaze3d.platform.GlStateManager._texParameter(3553, 10241, 9728)
        com.mojang.blaze3d.platform.GlStateManager._texParameter(3553, 10240, 9728)
    }

    private fun drawGlowCircle(graphics: GuiGraphics, cx: Int, cy: Int, r: Int, color: Int) {
        for (dy in -r..r) {
            val dx = kotlin.math.sqrt((r * r - dy * dy).toDouble()).toInt()
            if (dx > 0) {
                graphics.fill(cx - dx, cy + dy, cx + dx, cy + dy + 1, color)
            }
        }
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        // Draw the default dark overlay background (unscaled)
        renderBackground(graphics, mouseX, mouseY, partialTick)

        // Compute precise time delta for custom idle animations (resolving the fast x5 tick speed bug)
        val currentTime = System.currentTimeMillis()
        val elapsedTicks = (currentTime - lastRenderTime) / 50.0f
        lastRenderTime = currentTime
        val animationDelta = elapsedTicks.coerceIn(0f, 2.0f)

        if (clientTicks % 40 == 0) {
            println("[ConsumableRaidScreen] currentTime=$currentTime, elapsedTicks=$elapsedTicks, animationDelta=$animationDelta, stateTicks=${floatingState.getPartialTicks()}")
        }

        // Calculate layout coordinates in virtual scaled system
        val virtualWidth = width / GUI_SCALE
        val virtualHeight = height / GUI_SCALE
        val guiX = ((virtualWidth - GUI_WIDTH) / 2).toInt()
        val guiY = ((virtualHeight - GUI_HEIGHT) / 2).toInt()

        // Scale mouse coordinates to match virtual space
        val virtualMouseX = mouseX / GUI_SCALE
        val virtualMouseY = mouseY / GUI_SCALE

        val poseStack = graphics.pose()
        poseStack.pushPose()
        poseStack.scale(GUI_SCALE, GUI_SCALE, 1.0f)

        // 1. Draw main background texture (Nearest-Neighbor filtered)
        setupNearestNeighbor(BACKGROUND_TEXTURE)
        graphics.blit(BACKGROUND_TEXTURE, guiX, guiY, 0f, 0f, GUI_WIDTH, GUI_HEIGHT, GUI_WIDTH, GUI_HEIGHT)

        // 2. [AURA REMOVED] Pulsating den glow has been disabled as requested by the user

        // 3 & 4. Draw Pokemon Name (left) and Stars (right) on the top line (guiY + 4)
        val species = raidBoss.displaySpecies
        if (species != null) {
            val displayName = species.translatedName.string
            val starCount = raidBoss.tier.ordinal + 1
            val starsStr = "★".repeat(starCount)

            // Typewriter LCD effect on the name and stars combined
            val totalChars = displayName.length + starCount
            val charsToShow = lcdTextProgress.toInt().coerceIn(0, totalChars)
            
            // Mechanical tick sounds as text appears
            if (charsToShow > lastCharsCount) {
                lastCharsCount = charsToShow
                minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.8f))
            }
            
            // Draw name at the left (guiX + 12)
            val visibleNameChars = charsToShow.coerceAtMost(displayName.length)
            val visibleName = displayName.substring(0, visibleNameChars)
            graphics.drawString(font, visibleName, guiX + 12, guiY + 5, 0xFFFFFF, true)

            // Draw stars at the right (guiX + 128 - starsWidth)
            if (charsToShow > displayName.length) {
                val starsToShowCount = charsToShow - displayName.length
                val visibleStars = starsStr.substring(0, starsToShowCount)
                val starsWidth = font.width(starsStr)
                val starsX = guiX + 128 - starsWidth
                graphics.drawString(font, visibleStars, starsX, guiY + 5, 0xFFFFA800.toInt(), true)
            }

            // Center types horizontally at the bottom of the screen (Y = guiY + 74)
            val typesList = species.types.toList()
            val totalWidth = if (typesList.size == 2) (18 * 2 + 2) else 18
            val startX = guiX + 69 - (totalWidth / 2)
            typesList.forEachIndexed { index, type ->
                val typeName = type.name.lowercase()
                val typeTexture = ResourceLocation.fromNamespaceAndPath(
                    CobblemonGymOdyssey.MOD_ID,
                    "textures/gui/type/$typeName.png"
                )
                val iconX = startX + (index * 20) // 18px width + 2px gap
                val iconY = guiY + 74
                setupNearestNeighbor(typeTexture)
                graphics.blit(typeTexture, iconX, iconY, 18, 18, 0f, 0f, 36, 36, 36, 36)
            }
        }

        // 5. Render rotating 3D Pokemon model (Centered & scaled up to 3.5f for GUI_SCALE=1f compatibility)
        // Raised to Y = guiY + 32 to center it better inside the Pokeball frame
        val renderable = pokemon
        if (renderable != null) {
            val modelPoseStack = graphics.pose()
            modelPoseStack.pushPose()
            modelPoseStack.translate((guiX + 69).toDouble(), (guiY + 32).toDouble(), 100.0)
            modelPoseStack.scale(3.5f, 3.5f, 1.0f)
            
            val rotationYaw = (clientTicks + partialTick) * 0.8f
            
            try {
                drawProfilePokemon(
                    renderablePokemon = renderable.asRenderablePokemon(),
                    matrixStack = modelPoseStack,
                    rotation = Quaternionf().fromEulerXYZDegrees(Vector3f(15f, 180f + rotationYaw, 0f)),
                    state = floatingState,
                    partialTicks = animationDelta, // Pass correct time delta to fix animation speed
                    scale = 6.5f
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            modelPoseStack.popPose()
        }

        // 6. Draw Buttons (Centered side-by-side: both are 64px wide, meeting at guiX + 69)
        val isAcceptHovered = virtualMouseX >= guiX + 5 && virtualMouseX < guiX + 69 && virtualMouseY >= guiY + 98 && virtualMouseY < guiY + 113
        val isDenyHovered = virtualMouseX >= guiX + 69 && virtualMouseX < guiX + 133 && virtualMouseY >= guiY + 98 && virtualMouseY < guiY + 113

        // Hover sound feedback
        if (isAcceptHovered && !lastAcceptHovered) {
            minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.1f))
        }
        if (isDenyHovered && !lastDenyHovered) {
            minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.1f))
        }
        lastAcceptHovered = isAcceptHovered
        lastDenyHovered = isDenyHovered

        // Spawn hover particles on active buttons
        if (isAcceptHovered && random.nextInt(3) == 0) {
            particles.add(
                GuiParticle(
                    (guiX + 37 + (random.nextFloat() - 0.5f) * 36f),
                    (guiY + 105 + (random.nextFloat() - 0.5f) * 4f),
                    (random.nextFloat() - 0.5f) * 0.3f,
                    -random.nextFloat() * 0.4f,
                    0xFFFF0055.toInt(),
                    8 + random.nextInt(5)
                )
            )
        }
        if (isDenyHovered && random.nextInt(3) == 0) {
            particles.add(
                GuiParticle(
                    (guiX + 101 + (random.nextFloat() - 0.5f) * 36f),
                    (guiY + 105 + (random.nextFloat() - 0.5f) * 4f),
                    (random.nextFloat() - 0.5f) * 0.3f,
                    -random.nextFloat() * 0.4f,
                    0xFF9E00FF.toInt(),
                    8 + random.nextInt(5)
                )
            )
        }

        val acceptV = if (isAcceptHovered) 15f else 0f
        setupNearestNeighbor(ACCEPT_BUTTON_TEXTURE)
        graphics.blit(ACCEPT_BUTTON_TEXTURE, guiX + 5, guiY + 98, 0f, acceptV, 64, 15, 64, 30)

        val denyV = if (isDenyHovered) 15f else 0f
        setupNearestNeighbor(DENY_BUTTON_TEXTURE)
        graphics.blit(DENY_BUTTON_TEXTURE, guiX + 69, guiY + 98, 0f, denyV, 64, 15, 64, 30)

        // Draw text inside the buttons ("Accept" / "Refuse")
        val acceptText = Component.translatable("cobblemongymodyssey.raid.accept")
        val denyText = Component.translatable("cobblemongymodyssey.raid.deny")

        val acceptTextX = (guiX + 5) + (64 - font.width(acceptText)) / 2
        val acceptTextY = guiY + 98 + 3
        val acceptColor = if (isAcceptHovered) 0xFFFFA800.toInt() else 0xFFFFFF
        graphics.drawString(font, acceptText, acceptTextX, acceptTextY, acceptColor, true)

        val denyTextX = (guiX + 69) + (64 - font.width(denyText)) / 2
        val denyTextY = guiY + 98 + 3
        val denyColor = if (isDenyHovered) 0xFFFFA800.toInt() else 0xFFFFFF
        graphics.drawString(font, denyText, denyTextX, denyTextY, denyColor, true)

        // Render particles (scaled)
        particles.forEach { it.render(graphics) }

        poseStack.popPose() // Pop the 1.0x/2.0x scale
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val virtualWidth = width / GUI_SCALE
        val virtualHeight = height / GUI_SCALE
        val guiX = ((virtualWidth - GUI_WIDTH) / 2).toInt()
        val guiY = ((virtualHeight - GUI_HEIGHT) / 2).toInt()

        val virtualMouseX = mouseX / GUI_SCALE
        val virtualMouseY = mouseY / GUI_SCALE

        val isAcceptHovered = virtualMouseX >= guiX + 5 && virtualMouseX < guiX + 69 && virtualMouseY >= guiY + 98 && virtualMouseY < guiY + 113
        val isDenyHovered = virtualMouseX >= guiX + 69 && virtualMouseX < guiX + 133 && virtualMouseY >= guiY + 98 && virtualMouseY < guiY + 113

        if (isAcceptHovered) {
            minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f))
            
            val buf = net.minecraft.network.RegistryFriendlyByteBuf(
                Unpooled.buffer(),
                minecraft?.level?.registryAccess() ?: throw IllegalStateException("Registry access not available")
            )
            buf.writeBlockPos(pos)
            NetworkManager.sendToServer(
                ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "enter_consumable_raid"),
                buf
            )
            onClose()
            return true
        }

        if (isDenyHovered) {
            minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f))
            onClose()
            return true
        }

        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun isPauseScreen(): Boolean = false
}
