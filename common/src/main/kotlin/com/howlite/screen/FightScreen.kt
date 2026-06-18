package com.howlite.screen

import com.howlite.CobblemonGymOdyssey
import com.howlite.menu.BadgeCaseMenu
import com.howlite.data.PvpFightRecord
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.authlib.GameProfile
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.PlayerFaceRenderer
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvents
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import kotlin.math.max

class FightScreen(
    private val parentMenu: BadgeCaseMenu,
    private val playerEntries: List<PvpPlayerEntry>
) : Screen(Component.translatable("cobblemongymodyssey.fight.title")) {

    data class PvpPlayerEntry(val uuid: UUID, val name: String, val isOnline: Boolean)

    companion object {
        const val GUI_WIDTH = 184
        const val GUI_HEIGHT = 135

        val BACKGROUND_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CobblemonGymOdyssey.MOD_ID,
            "textures/gui/fight/fight_background.png"
        )
        val CARD_ONLINE_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CobblemonGymOdyssey.MOD_ID,
            "textures/gui/fight/player_card_background_online.png"
        )
        val CARD_OFFLINE_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CobblemonGymOdyssey.MOD_ID,
            "textures/gui/fight/player_card_background_offline.png"
        )
        val SCROLLER_TRACK_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CobblemonGymOdyssey.MOD_ID,
            "textures/gui/fight/scroller_background.png"
        )
        val SCROLLER_THUMB_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CobblemonGymOdyssey.MOD_ID,
            "textures/gui/fight/scroller.png"
        )
        val BACK_BUTTON_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CobblemonGymOdyssey.MOD_ID,
            "textures/gui/back_button.png"
        )
        val BACK_BUTTON_ICON_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CobblemonGymOdyssey.MOD_ID,
            "textures/gui/back_button_icon.png"
        )
    }

    private var sortedPlayers: List<PvpPlayerEntry> = emptyList()
    private var selectedPlayer: PvpPlayerEntry? = null
    private var scrollY = 0.0
    private var isDraggingScroller = false

    override fun init() {
        super.init()
        val selfUuid = minecraft?.player?.uuid
        val prevSelected = selectedPlayer
        sortedPlayers = playerEntries.sortedWith(
            compareBy<PvpPlayerEntry> { it.uuid != selfUuid }
                .thenBy { !it.isOnline }
                .thenBy { it.name.lowercase() }
        )
        selectedPlayer = sortedPlayers.find { it.uuid == prevSelected?.uuid } ?: sortedPlayers.firstOrNull()
        minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 0.8f))
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(graphics, mouseX, mouseY, partialTick)

        val guiX = (width - GUI_WIDTH) / 2
        val guiY = (height - GUI_HEIGHT) / 2

        // 1. Render Main Window Background
        RenderSystem.setShaderTexture(0, BACKGROUND_TEXTURE)
        graphics.blit(BACKGROUND_TEXTURE, guiX, guiY, 0f, 0f, GUI_WIDTH, GUI_HEIGHT, GUI_WIDTH, GUI_HEIGHT)

        // 2. Render Left Player List (Scissored to fit inside bounds, X=9 to X=86)
        val listMinX = guiX + 9
        val listMinY = guiY + 32
        val listMaxX = guiX + 86
        val listMaxY = guiY + 136

        // Draw Player Cards
        val cardHeight = 22
        val spacing = 2
        val itemHeight = cardHeight + spacing

        // Set scissor to prevent rendering outside the window boundaries
        graphics.enableScissor(listMinX, listMinY, listMaxX, listMaxY)

        val selfUuid = minecraft?.player?.uuid
        sortedPlayers.forEachIndexed { index, player ->
            val cardX = listMinX
            val cardY = listMinY + (index * itemHeight) - scrollY.toInt()

            // Only render if visible on screen
            if (cardY + cardHeight >= listMinY && cardY <= listMaxY) {
                val isSelected = selectedPlayer?.uuid == player.uuid
                val cardTexture = if (player.isOnline) CARD_ONLINE_TEXTURE else CARD_OFFLINE_TEXTURE
                val cardV = if (isSelected) 22f else 0f

                RenderSystem.setShaderTexture(0, cardTexture)
                graphics.blit(cardTexture, cardX, cardY, 0f, cardV, 77, 22, 77, 44)

                // Render player head (16x16, Steve/Alex fallback integrated in skinManager)
                val profile = GameProfile(player.uuid, player.name)
                val skin = minecraft!!.skinManager.getInsecureSkin(profile)
                PlayerFaceRenderer.draw(graphics, skin.texture, cardX + 3, cardY + 3, 16)

                // Render face-to-face quick record right-aligned
                val record = parentMenu.pvpFights[player.uuid.toString()]
                val wins = record?.wins ?: 0
                val losses = record?.losses ?: 0
                val h2hStr = if (player.uuid == selfUuid) {
                    "${parentMenu.pvpWins}-${parentMenu.pvpLosses}"
                } else {
                    "$wins-$losses"
                }
                val h2hWidth = font.width(h2hStr)
                graphics.drawString(font, h2hStr, cardX + 74 - h2hWidth, cardY + 7, 0x00FFCC, false)

                // Render name truncated based on available space
                var nameTrunc = player.name
                val maxNameWidth = 48 - h2hWidth
                while (nameTrunc.isNotEmpty() && font.width(nameTrunc) > maxNameWidth) {
                    nameTrunc = nameTrunc.substring(0, nameTrunc.length - 1)
                }
                if (nameTrunc.length < player.name.length) {
                    if (nameTrunc.length > 2) {
                        nameTrunc = nameTrunc.substring(0, nameTrunc.length - 2) + ".."
                    }
                }
                graphics.drawString(
                    font,
                    nameTrunc,
                    cardX + 22,
                    cardY + 7,
                    if (player.isOnline) 0xFFFFFF else 0x8E8E93,
                    false
                )
            }
        }

        graphics.disableScissor()

        // 3. Render Scrollbar (Shifted UP 6px, RIGHT 3px)
        val totalHeight = sortedPlayers.size * itemHeight - spacing
        val maxScroll = max(0, totalHeight - 104)

        RenderSystem.setShaderTexture(0, SCROLLER_TRACK_TEXTURE)
        graphics.blit(SCROLLER_TRACK_TEXTURE, guiX + 89, guiY + 26, 0f, 0f, 8, 104, 8, 104)

        if (maxScroll > 0) {
            val thumbY = (guiY + 26) + (scrollY / maxScroll) * (104 - 11)
            RenderSystem.setShaderTexture(0, SCROLLER_THUMB_TEXTURE)
            graphics.blit(SCROLLER_THUMB_TEXTURE, guiX + 90, thumbY.toInt(), 0f, 0f, 6, 11, 6, 11)
        }

        // 4. Render Right Panel (Stats details of selected player)
        val rightX = guiX + 99
        val rightY = guiY + 32

        val selected = selectedPlayer
        if (selected != null) {
            // Header: Name
            var selName = selected.name
            if (font.width(selName) > 75) {
                selName = selName.substring(0, 10) + "..."
            }
            graphics.drawString(font, selName, rightX, rightY, 0xFFFFFF, true)

            // Line 1: Status
            val statusStr = if (selected.isOnline) {
                "§a" + Component.translatable("cobblemongymodyssey.fight.online").string
            } else {
                "§c" + Component.translatable("cobblemongymodyssey.fight.offline").string
            }
            graphics.drawString(font, statusStr, rightX, rightY + 14, 0xFFFFFF, false)

            if (selected.uuid == selfUuid) {
                // If it is the client player, display global stats
                val winsText = Component.translatable("cobblemongymodyssey.fight.global_wins", parentMenu.pvpWins).string
                graphics.drawString(font, winsText, rightX, rightY + 28, 0x8E8E93, false)

                val lossesText = Component.translatable("cobblemongymodyssey.fight.global_losses", parentMenu.pvpLosses).string
                graphics.drawString(font, lossesText, rightX, rightY + 40, 0x8E8E93, false)

                val completedRegions = com.howlite.data.GymRegion.entries.count { region ->
                    val regionBadges = com.howlite.data.GymBadge.entries.filter { it.region == region }
                    regionBadges.isNotEmpty() && regionBadges.all { it in parentMenu.unlockedBadges }
                }
                val maxDailyRewards = 3 + completedRegions
                val dailyText = Component.translatable("cobblemongymodyssey.fight.daily_rewards", parentMenu.pvpRewardsClaimedToday, maxDailyRewards).string
                graphics.drawString(font, dailyText, rightX, rightY + 52, 0xFFA800, false)

                // Display dynamic reward amount for next victory (Split into two lines to prevent horizontal overflow)
                val nextReward = 50 + 5 * parentMenu.unlockedBadges.size
                val rewardLabel = Component.translatable("cobblemongymodyssey.fight.next_win_reward_label").string
                val rewardValue = Component.translatable("cobblemongymodyssey.fight.next_win_reward_value", nextReward).string
                graphics.drawString(font, rewardLabel, rightX, rightY + 64, 0x00FFCC, false)
                graphics.drawString(font, rewardValue, rightX, rightY + 74, 0x00FFCC, false)
            } else {
                // Retrieve PvP details
                val opponentUuidStr = selected.uuid.toString()
                val record = parentMenu.pvpFights[opponentUuidStr]

                // Line 2: Consecutive days
                val consecutive = record?.consecutiveDays ?: 0
                val streakText = Component.translatable("cobblemongymodyssey.fight.streak", consecutive).string
                graphics.drawString(font, streakText, rightX, rightY + 28, 0x8E8E93, false)

                // Line 3: Multiplier
                val todayStr = LocalDate.now(ZoneOffset.UTC).toString()
                val yesterdayStr = LocalDate.now(ZoneOffset.UTC).minusDays(1).toString()
                val nextConsecutive = if (record != null && record.lastFightDate == todayStr) {
                    consecutive
                } else if (record != null && record.lastFightDate == yesterdayStr) {
                    consecutive + 1
                } else {
                    1
                }
                val mult = when (nextConsecutive) {
                    1 -> 100
                    2 -> 50
                    3 -> 25
                    else -> 0
                }
                val multText = Component.translatable("cobblemongymodyssey.fight.multiplier", mult).string
                graphics.drawString(font, multText, rightX, rightY + 40, 0xFFA800, false)

                // Line 4: Last fight date (Split into two lines if active to prevent horizontal overflow)
                if (record == null || record.lastFightDate.isEmpty()) {
                    val lastFight = Component.translatable("cobblemongymodyssey.badge_case.lcd.none").string
                    graphics.drawString(font, "§7Date: §f$lastFight", rightX, rightY + 52, 0xFFFFFF, false)
                } else {
                    val lastFightTitle = Component.translatable("cobblemongymodyssey.fight.last_fight").string
                    graphics.drawString(font, "§7$lastFightTitle", rightX, rightY + 52, 0xFFFFFF, false)
                    graphics.drawString(font, "§f${record.lastFightDate}", rightX, rightY + 62, 0xFFFFFF, false)
                }

                // Line 5 & 6: Head-to-head score (shifted down to rightY + 76 and rightY + 88 to accommodate split date)
                val h2hTitle = "§b" + Component.translatable("cobblemongymodyssey.fight.h2h").string
                val h2hTitleWidth = font.width(h2hTitle)
                graphics.drawString(
                    font,
                    h2hTitle,
                    rightX + (76 - h2hTitleWidth) / 2,
                    rightY + 76,
                    0xFFFFFF,
                    false
                )
                val h2hWins = record?.wins ?: 0
                val h2hLosses = record?.losses ?: 0
                val scoreText = Component.translatable("cobblemongymodyssey.fight.h2h_score", h2hWins, h2hLosses).string
                val scoreWidth = font.width(scoreText)
                graphics.drawString(font, scoreText, rightX + (76 - scoreWidth) / 2, rightY + 88, 0xFFFFFF, false)
            }

        } else {
            // No players in the list
            val noPlayersText = Component.translatable("cobblemongymodyssey.fight.no_players").string
            graphics.drawCenteredString(font, noPlayersText, rightX + 38, rightY + 45, 0x57606F)
        }

        // 5. Render Header Labels (Title only, global stats are on player's card)
        val titleText = Component.translatable("cobblemongymodyssey.fight.title").string
        graphics.drawString(font, titleText, guiX + 31, guiY + 9, 0xFFFFFF, true)

        // 6. Draw Back Button (Bottom-left outside the interface)
        val backX = guiX - 29
        val backY = guiY + 122
        val isBackHovered = mouseX >= backX && mouseX < backX + 26 && mouseY >= backY && mouseY < backY + 13
        val backV = if (isBackHovered) 13f else 0f

        RenderSystem.setShaderTexture(0, BACK_BUTTON_TEXTURE)
        graphics.blit(BACK_BUTTON_TEXTURE, backX, backY, 0f, backV, 26, 13, 26, 26)

        RenderSystem.setShaderTexture(0, BACK_BUTTON_ICON_TEXTURE)
        graphics.blit(BACK_BUTTON_ICON_TEXTURE, backX + 2, backY + 1, 0f, 0f, 21, 11, 21, 11)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val guiX = (width - GUI_WIDTH) / 2
        val guiY = (height - GUI_HEIGHT) / 2

        // Back button click (Bottom-left outside the interface)
        val backX = guiX - 29
        val backY = guiY + 122
        if (mouseX >= backX && mouseX < backX + 26 && mouseY >= backY && mouseY < backY + 13) {
            minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f))
            closeAndReturn()
            return true
        }

        // Scroller click / drag
        val maxScroll = max(0, sortedPlayers.size * 24 - 2 - 104)
        if (maxScroll > 0 && mouseX >= guiX + 89 && mouseX < guiX + 97 && mouseY >= guiY + 26 && mouseY < guiY + 130) {
            isDraggingScroller = true
            updateScrollFromMouse(mouseY - (guiY + 26), maxScroll)
            minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 0.9f))
            return true
        }

        // Player card click
        val listMinX = guiX + 9
        val listMinY = guiY + 32
        val listMaxX = guiX + 86
        val listMaxY = guiY + 136
        if (mouseX >= listMinX && mouseX < listMaxX && mouseY >= listMinY && mouseY < listMaxY) {
            val clickedIdx = ((mouseY - listMinY + scrollY) / 24).toInt()
            if (clickedIdx >= 0 && clickedIdx < sortedPlayers.size) {
                selectedPlayer = sortedPlayers[clickedIdx]
                minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f))
                return true
            }
        }

        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0) {
            isDraggingScroller = false
        }
        return super.mouseReleased(mouseX, mouseY, button)
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, dragX: Double, dragY: Double): Boolean {
        if (isDraggingScroller) {
            val guiY = (height - GUI_HEIGHT) / 2
            val maxScroll = max(0, sortedPlayers.size * 24 - 2 - 104)
            updateScrollFromMouse(mouseY - (guiY + 26), maxScroll)
            return true
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        val maxScroll = max(0, sortedPlayers.size * 24 - 2 - 104)
        if (maxScroll > 0) {
            this.scrollY = (this.scrollY - scrollY * 12.0).coerceIn(0.0, maxScroll.toDouble())
            return true
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
    }

    private fun updateScrollFromMouse(relativeY: Double, maxScroll: Int) {
        val thumbProgress = (relativeY - 5.5) / (104.0 - 11.0)
        this.scrollY = (thumbProgress * maxScroll).coerceIn(0.0, maxScroll.toDouble())
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (keyCode == 256) { // Escape
            closeAndReturn()
            return true
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    private fun closeAndReturn() {
        val mc = minecraft ?: return
        mc.setScreen(BadgeCaseScreen(parentMenu, mc.player!!.inventory, Component.translatable("cobblemongymodyssey.badge_case.title")))
    }

    override fun isPauseScreen(): Boolean = false
}
