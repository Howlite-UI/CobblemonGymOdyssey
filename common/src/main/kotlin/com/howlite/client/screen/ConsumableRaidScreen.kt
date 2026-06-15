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
        const val GUI_HEIGHT = 93
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

    override fun init() {
        super.init()
        try {
            pokemon = raidBoss.bossProperties?.create()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Play initial chime on GUI open
        minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, 0.7f))

        // Initial burst of particles in virtual coordinates
        val virtualWidth = width / GUI_SCALE
        val virtualHeight = height / GUI_SCALE
        val guiX = ((virtualWidth - GUI_WIDTH) / 2).toInt()
        val guiY = ((virtualHeight - GUI_HEIGHT) / 2).toInt()
        for (i in 0 until 12) {
            val px = (guiX + 69 + (random.nextFloat() - 0.5f) * 30f)
            val py = (guiY + 50 + (random.nextFloat() - 0.5f) * 20f)
            val color = when (random.nextInt(3)) {
                0 -> 0xFFFF0055.toInt() // Magenta
                1 -> 0xFF9E00FF.toInt() // Purple
                else -> 0xFFFF3333.toInt() // Red
            }
            particles.add(
                GuiParticle(
                    px, py,
                    (random.nextFloat() - 0.5f) * 1.6f,
                    (random.nextFloat() - 0.5f) * 1.6f - 0.4f,
                    color,
                    15 + random.nextInt(10)
                )
            )
        }
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

        // Ambient particles rise from the raid den in virtual coordinates
        val virtualWidth = width / GUI_SCALE
        val virtualHeight = height / GUI_SCALE
        val guiX = ((virtualWidth - GUI_WIDTH) / 2).toInt()
        val guiY = ((virtualHeight - GUI_HEIGHT) / 2).toInt()
        if (random.nextInt(3) == 0) {
            val px = (guiX + 69 + (random.nextFloat() - 0.5f) * 36f)
            val py = (guiY + 58 + (random.nextFloat() - 0.5f) * 10f)
            val color = when (random.nextInt(3)) {
                0 -> 0xFFFF0055.toInt()
                1 -> 0xFF9E00FF.toInt()
                else -> 0xFFFF3333.toInt()
            }
            particles.add(
                GuiParticle(
                    px, py,
                    (random.nextFloat() - 0.5f) * 0.4f,
                    -random.nextFloat() * 0.4f - 0.1f,
                    color,
                    20 + random.nextInt(15)
                )
            )
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

        // 2. Draw pulsating den glow behind Pokemon model
        val time = clientTicks + partialTick
        val baseAlpha = (26 + (kotlin.math.sin(time * 0.12) * 12)).toInt().coerceIn(0, 255)
        val glowColor = 0xFF0055 // Magenta/Red raid energy color
        val cx = guiX + 69
        val cy = guiY + 46

        drawGlowCircle(graphics, cx, cy, 22, ((baseAlpha * 0.15f).toInt() shl 24) or glowColor)
        drawGlowCircle(graphics, cx, cy, 16, ((baseAlpha * 0.35f).toInt() shl 24) or glowColor)
        drawGlowCircle(graphics, cx, cy, 11, ((baseAlpha * 0.65f).toInt() shl 24) or glowColor)
        drawGlowCircle(graphics, cx, cy, 6, (baseAlpha shl 24) or glowColor)

        // 3. Draw stars
        val starCount = raidBoss.tier.ordinal + 1
        val starsStr = "★".repeat(starCount)
        graphics.drawString(font, starsStr, guiX + 10, guiY + 10, 0xFFFFA800.toInt(), true)

        // 4. Draw Pokemon Name and Type Icons
        val species = raidBoss.displaySpecies
        if (species != null) {
            val displayName = species.translatedName.string

            // Typewriter LCD effect
            val charsToShow = lcdTextProgress.toInt().coerceIn(0, displayName.length)
            val visibleName = displayName.substring(0, charsToShow)
            
            // Mechanical tick sounds as text appears
            if (charsToShow > lastCharsCount) {
                lastCharsCount = charsToShow
                minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.8f))
            }
            
            graphics.drawString(font, visibleName, guiX + 10, guiY + 22, 0xFFFFFF, true)

            // Draw type icon(s)
            val typesList = species.types.toList()
            typesList.forEachIndexed { index, type ->
                val typeName = type.name.lowercase()
                val typeTexture = ResourceLocation.fromNamespaceAndPath(
                    CobblemonGymOdyssey.MOD_ID,
                    "textures/gui/type/$typeName.png"
                )
                val iconY = guiY + 10 + (index * 20)
                setupNearestNeighbor(typeTexture)
                graphics.blit(typeTexture, guiX + 110, iconY, 18, 18, 0f, 0f, 36, 36, 36, 36)
            }
        }

        // 5. Render rotating 3D Pokemon model (Centered)
        val renderable = pokemon
        if (renderable != null) {
            val modelPoseStack = graphics.pose()
            modelPoseStack.pushPose()
            modelPoseStack.translate((guiX + 69).toDouble(), (guiY + 54).toDouble(), 100.0)
            modelPoseStack.scale(1.1f, 1.1f, 1.0f) // Scaled by 1.1 inside the 2.0 scaled poseStack = 2.2 total scale
            
            val rotationYaw = (clientTicks + partialTick) * 0.8f
            
            try {
                drawProfilePokemon(
                    renderablePokemon = renderable.asRenderablePokemon(),
                    matrixStack = modelPoseStack,
                    rotation = Quaternionf().fromEulerXYZDegrees(Vector3f(15f, 180f + rotationYaw, 0f)),
                    state = floatingState,
                    partialTicks = partialTick,
                    scale = 6.5f
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            modelPoseStack.popPose()
        }

        // 6. Draw Buttons
        val isAcceptHovered = virtualMouseX >= guiX && virtualMouseX < guiX + 69 && virtualMouseY >= guiY + 81 && virtualMouseY < guiY + 93
        val isDenyHovered = virtualMouseX >= guiX + 69 && virtualMouseX < guiX + 137 && virtualMouseY >= guiY + 81 && virtualMouseY < guiY + 93

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
                    (guiX + 34 + (random.nextFloat() - 0.5f) * 40f),
                    (guiY + 87 + (random.nextFloat() - 0.5f) * 4f),
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
                    (guiX + 103 + (random.nextFloat() - 0.5f) * 40f),
                    (guiY + 87 + (random.nextFloat() - 0.5f) * 4f),
                    (random.nextFloat() - 0.5f) * 0.3f,
                    -random.nextFloat() * 0.4f,
                    0xFF9E00FF.toInt(),
                    8 + random.nextInt(5)
                )
            )
        }

        val acceptV = if (isAcceptHovered) 12f else 0f
        setupNearestNeighbor(ACCEPT_BUTTON_TEXTURE)
        graphics.blit(ACCEPT_BUTTON_TEXTURE, guiX, guiY + 81, 0f, acceptV, 69, 12, 69, 24)

        val denyV = if (isDenyHovered) 12f else 0f
        setupNearestNeighbor(DENY_BUTTON_TEXTURE)
        graphics.blit(DENY_BUTTON_TEXTURE, guiX + 69, guiY + 81, 0f, denyV, 68, 12, 68, 24)

        // Render particles (scaled)
        particles.forEach { it.render(graphics) }

        poseStack.popPose() // Pop the 2.0x scale
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val virtualWidth = width / GUI_SCALE
        val virtualHeight = height / GUI_SCALE
        val guiX = ((virtualWidth - GUI_WIDTH) / 2).toInt()
        val guiY = ((virtualHeight - GUI_HEIGHT) / 2).toInt()

        val virtualMouseX = mouseX / GUI_SCALE
        val virtualMouseY = mouseY / GUI_SCALE

        val isAcceptHovered = virtualMouseX >= guiX && virtualMouseX < guiX + 69 && virtualMouseY >= guiY + 81 && virtualMouseY < guiY + 93
        val isDenyHovered = virtualMouseX >= guiX + 69 && virtualMouseX < guiX + 137 && virtualMouseY >= guiY + 81 && virtualMouseY < guiY + 93

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
