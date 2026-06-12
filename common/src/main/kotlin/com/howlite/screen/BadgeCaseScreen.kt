package com.howlite.screen

import com.howlite.CobblemonGymOdyssey
import com.howlite.data.GymBadge
import com.howlite.menu.BadgeCaseMenu
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory

/**
 * Interface graphique de la Boîte à Badges.
 *
 * Rendu entièrement programmatique (sans texture de fond statique) pour un rendu pixel-art 3D
 * net, rétro et premium, similaire à un appareil Pokématos/Pokédex portable.
 *
 * ## Rendu dynamique par région
 * L'appareil change de couleur de coque et de thème graphique en fonction de la région sélectionnée.
 *
 * ## Rendu des badges
 * - Badge obtenu -> Texture 1:1 en couleur avec effet de surbrillance au survol.
 * - Badge non obtenu -> Recess sombre affichant la silhouette noire/sombre du badge.
 *
 * ## Écran LCD rétro-éclairé
 * Affiche au bas de l'écran le Level Cap actuel du joueur et sa progression globale.
 */
class BadgeCaseScreen(
    menu: BadgeCaseMenu,
    inventory: Inventory,
    title: Component
) : AbstractContainerScreen<BadgeCaseMenu>(menu, inventory, title) {

    companion object {
        const val GUI_WIDTH  = 176
        const val GUI_HEIGHT = 200

        const val TAB_WIDTH  = 38
        const val TAB_HEIGHT = 16
    }

    /**
     * Enumération des Régions supportées.
     * Chaque région possède sa propre couleur primaire, ses reflets 3D (highlight/shadow)
     * et la couleur du texte de son titre d'écran.
     */
    enum class Region(
        val labelKey: String,
        val badges: List<GymBadge>,
        val primaryColor: Int,
        val highlightColor: Int,
        val shadowColor: Int,
        val screenTitleColor: Int
    ) {
        KANTO(
            "cobblemongymodyssey.badge_case.tab.kanto",
            listOf(
                GymBadge.BOULDER_BADGE, GymBadge.CASCADE_BADGE,
                GymBadge.THUNDER_BADGE, GymBadge.RAINBOW_BADGE,
                GymBadge.SOUL_BADGE,    GymBadge.MARSH_BADGE,
                GymBadge.VOLCANO_BADGE, GymBadge.EARTH_BADGE
            ),
            0xFF2ED573.toInt(), // Vert Émeraude
            0xFF55EFC4.toInt(), // Émeraude clair (Reflet)
            0xFF21A354.toInt(), // Émeraude sombre (Ombre)
            0xFF2ED573.toInt()
        ),
        JOHTO(
            "cobblemongymodyssey.badge_case.tab.johto",
            emptyList(),
            0xFF1E90FF.toInt(), // Bleu Cobalt
            0xFF70A1FF.toInt(), // Bleu clair
            0xFF0F6EBD.toInt(), // Bleu sombre
            0xFF70A1FF.toInt()
        ),
        HOENN(
            "cobblemongymodyssey.badge_case.tab.hoenn",
            emptyList(),
            0xFFFF4757.toInt(), // Rouge Rubis
            0xFFFF6B81.toInt(), // Rouge clair
            0xFFC92836.toInt(), // Rouge sombre
            0xFFFF6B81.toInt()
        ),
        SINNOH(
            "cobblemongymodyssey.badge_case.tab.sinnoh",
            emptyList(),
            0xFFA29BFE.toInt(), // Violet Platine
            0xFFD6A2E8.toInt(), // Violet clair
            0xFF6D214F.toInt(), // Violet sombre
            0xFFD6A2E8.toInt()
        );
    }

    private var activeRegion: Region = Region.KANTO

    override fun init() {
        super.init()
        imageWidth  = GUI_WIDTH
        imageHeight = GUI_HEIGHT
        titleLabelX = -9999 // Masquer le titre par défaut pour utiliser notre bannière LCD
        inventoryLabelY = GUI_HEIGHT + 100 // Hors écran
    }

    override fun renderBg(graphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        val x = (width  - imageWidth)  / 2
        val y = (height - imageHeight) / 2
        val region = activeRegion

        // 1. Contour noir externe de l'appareil
        drawBorder(graphics, x - 7, y - 7, x + imageWidth + 7, y + imageHeight + 7, 0xFF000000.toInt())

        // 2. Coque de couleur primaire de la région
        graphics.fill(x - 6, y - 6, x + imageWidth + 6, y + imageHeight + 6, region.primaryColor)

        // 3. Reliefs 3D de la coque
        graphics.fill(x - 6, y - 6, x + imageWidth + 6, y - 5, region.highlightColor) // Haut
        graphics.fill(x - 6, y - 6, x - 5, y + imageHeight + 6, region.highlightColor) // Gauche
        graphics.fill(x - 6, y + imageHeight + 5, x + imageWidth + 6, y + imageHeight + 6, region.shadowColor) // Bas
        graphics.fill(x + imageWidth + 5, y - 6, x + imageWidth + 6, y + imageHeight + 6, region.shadowColor) // Droite

        // 4. Rendu des Onglets
        renderTabs(graphics, x, y, mouseX, mouseY)

        // 5. Fond et contours de l'écran interne
        val scrX1 = x + 6
        val scrY1 = y + 26
        val scrX2 = x + imageWidth - 6
        val scrY2 = y + imageHeight - 6

        // Fond noir/marine sombre
        graphics.fill(scrX1, scrY1, scrX2, scrY2, 0xFF10141D.toInt())

        // Relief de renfoncement de l'écran
        graphics.fill(scrX1, scrY1, scrX2, scrY1 + 1, 0xFF050609.toInt()) // Ombre haute
        graphics.fill(scrX1, scrY1, scrX1 + 1, scrY2, 0xFF050609.toInt()) // Ombre gauche
        graphics.fill(scrX2 - 1, scrY1 + 1, scrX2, scrY2, 0xFF252D3A.toInt()) // Reflet droit
        graphics.fill(scrX1 + 1, scrY2 - 1, scrX2, scrY2, 0xFF252D3A.toInt()) // Reflet bas
        
        drawBorder(graphics, scrX1 - 1, scrY1 - 1, scrX2 + 1, scrY2 + 1, 0xFF000000.toInt()) // Bord noir de l'écran

        // 6. Bannière d'en-tête de l'écran
        val banY1 = scrY1 + 4
        val banY2 = scrY1 + 18
        val bannerBgColor = (region.primaryColor and 0x00FFFFFF) or 0x35000000 // Teinte translucide
        graphics.fill(scrX1 + 4, banY1, scrX2 - 4, banY2, bannerBgColor)
        graphics.fill(scrX1 + 4, banY2, scrX2 - 4, banY2 + 1, region.shadowColor) // Ligne de séparation

        val bannerText = "— " + Component.translatable(region.labelKey).string.uppercase() + " LEAGUE —"
        val bannerW = font.width(bannerText)
        graphics.drawString(font, bannerText, x + (imageWidth - bannerW) / 2, banY1 + 3, region.screenTitleColor, false)

        // 7. Grille des Badges
        renderBadgeGrid(graphics, x, y, mouseX, mouseY)

        // 8. Écran LCD du bas (Level Cap)
        renderLCDDisplay(graphics, x, y)
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(graphics, mouseX, mouseY, partialTick)
        super.render(graphics, mouseX, mouseY, partialTick)
        renderTooltip(graphics, mouseX, mouseY)
    }

    private fun renderTabs(graphics: GuiGraphics, guiX: Int, guiY: Int, mouseX: Int, mouseY: Int) {
        val tabCount = Region.entries.size
        val spacing = 2
        val startX = guiX + (GUI_WIDTH - (tabCount * TAB_WIDTH + (tabCount - 1) * spacing)) / 2

        Region.entries.forEachIndexed { i, region ->
            val tabX = startX + i * (TAB_WIDTH + spacing)
            val isActive = region == activeRegion
            val hasContent = region.badges.isNotEmpty()
            
            val tabY = if (isActive) guiY - 14 else guiY - 10
            val currentTabH = if (isActive) 15 else 11

            val baseColor = if (isActive) region.primaryColor else 0xFF2F3542.toInt()
            val highlight = if (isActive) region.highlightColor else 0xFF57606F.toInt()
            val shadow = if (isActive) region.shadowColor else 0xFF1E272E.toInt()

            graphics.fill(tabX, tabY, tabX + TAB_WIDTH, tabY + currentTabH, baseColor)

            // Reliefs
            graphics.fill(tabX, tabY, tabX + TAB_WIDTH, tabY + 1, highlight)
            graphics.fill(tabX, tabY, tabX + 1, tabY + currentTabH, highlight)
            graphics.fill(tabX + TAB_WIDTH - 1, tabY + 1, tabX + TAB_WIDTH, tabY + currentTabH, shadow)

            // Contours noirs
            graphics.fill(tabX, tabY - 1, tabX + TAB_WIDTH, tabY, 0xFF000000.toInt())
            graphics.fill(tabX - 1, tabY, tabX, tabY + currentTabH, 0xFF000000.toInt())
            graphics.fill(tabX + TAB_WIDTH, tabY, tabX + TAB_WIDTH + 1, tabY + currentTabH, 0xFF000000.toInt())
            if (!isActive) {
                graphics.fill(tabX, guiY - 1, tabX + TAB_WIDTH, guiY, 0xFF000000.toInt())
            }

            val label = font.split(Component.translatable(region.labelKey), TAB_WIDTH - 4).firstOrNull()
                ?: return@forEachIndexed
            val textColor = when {
                isActive -> 0xFFFFFF
                hasContent -> 0xCCCCCC
                else -> 0x747D8C
            }
            
            val textX = tabX + (TAB_WIDTH - font.width(label)) / 2
            val textY = tabY + (currentTabH - 8) / 2
            graphics.drawString(font, label, textX, textY, textColor, false)
        }
    }

    private fun renderBadgeGrid(graphics: GuiGraphics, guiX: Int, guiY: Int, mouseX: Int, mouseY: Int) {
        val badges = activeRegion.badges
        val unlockedBadges = menu.unlockedBadges

        if (badges.isEmpty()) {
            val msg = Component.translatable("cobblemongymodyssey.badge_case.coming_soon")
            val msgX = guiX + (GUI_WIDTH - font.width(msg)) / 2
            val msgY = guiY + 26 + (GUI_HEIGHT - 26 - 36) / 2
            graphics.drawString(font, msg, msgX, msgY, 0x57606F, false)
            return
        }

        // Ligne de séparation écran gauche/droite
        graphics.fill(guiX + 52, guiY + 48, guiX + 53, guiY + 138, 0xFF252C38.toInt())

        // Rendu du trophée en pixel-art
        drawPixelArtTrophy(graphics, guiX + 10, guiY + 48)

        badges.forEachIndexed { i, badge ->
            val (slotX, slotY) = slotPosition(guiX, guiY, i)
            val isUnlocked = badge in unlockedBadges

            // Fond renfoncé du slot
            graphics.fill(slotX, slotY, slotX + 20, slotY + 20, 0xFF0A0D14.toInt())
            
            // Bordures de relief internes
            graphics.fill(slotX, slotY, slotX + 20, slotY + 1, 0xFF040507.toInt())
            graphics.fill(slotX, slotY, slotX + 1, slotY + 20, 0xFF040507.toInt())
            graphics.fill(slotX + 19, slotY + 1, slotX + 20, slotY + 20, 0xFF1D2433.toInt())
            graphics.fill(slotX + 1, slotY + 19, slotX + 20, slotY + 20, 0xFF1D2433.toInt())

            // Arrondir les angles avec la couleur de fond
            val bg = 0xFF10141D.toInt()
            graphics.fill(slotX, slotY, slotX + 1, slotY + 1, bg)
            graphics.fill(slotX + 19, slotY, slotX + 20, slotY + 1, bg)
            graphics.fill(slotX, slotY + 19, slotX + 1, slotY + 20, bg)
            graphics.fill(slotX + 19, slotY + 19, slotX + 20, slotY + 20, bg)

            // Surbrillance au survol de la souris
            val isHovered = mouseX >= slotX && mouseX < slotX + 20 && mouseY >= slotY && mouseY < slotY + 20
            if (isHovered) {
                val hoverColor = activeRegion.highlightColor
                graphics.fill(slotX, slotY, slotX + 20, slotY + 1, hoverColor)
                graphics.fill(slotX, slotY + 19, slotX + 20, slotY + 20, hoverColor)
                graphics.fill(slotX, slotY, slotX + 1, slotY + 20, hoverColor)
                graphics.fill(slotX + 19, slotY, slotX + 20, slotY + 20, hoverColor)
            }

            val badgeTexture = ResourceLocation.fromNamespaceAndPath(
                CobblemonGymOdyssey.MOD_ID,
                "textures/item/${badge.id}.png"
            )

            val iconX = slotX + 2
            val iconY = slotY + 2

            if (isUnlocked) {
                graphics.blit(
                    badgeTexture, iconX, iconY, 0f, 0f,
                    16, 16, 16, 16
                )
            } else {
                // Rendu silhouette sombre pour les badges non débloqués
                RenderSystem.setShaderColor(0.06f, 0.06f, 0.06f, 0.75f)
                graphics.blit(
                    badgeTexture, iconX, iconY, 0f, 0f,
                    16, 16, 16, 16
                )
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
            }
        }
    }

    private fun renderLCDDisplay(graphics: GuiGraphics, guiX: Int, guiY: Int) {
        val lcdX = guiX + 12
        val lcdY = guiY + 148
        val lcdW = 152
        val lcdH = 36

        // Fond LCD bleu-cyan sombre rétro-éclairé
        graphics.fill(lcdX, lcdY, lcdX + lcdW, lcdY + lcdH, 0xFF082C3C.toInt())

        // Reliefs internes
        graphics.fill(lcdX, lcdY, lcdX + lcdW, lcdY + 1, 0xFF04161E.toInt())
        graphics.fill(lcdX, lcdY, lcdX + 1, lcdY + lcdH, 0xFF04161E.toInt())
        graphics.fill(lcdX + lcdW - 1, lcdY + 1, lcdX + lcdW, lcdY + lcdH, 0xFF104A64.toInt())
        graphics.fill(lcdX + 1, lcdY + lcdH - 1, lcdX + lcdW, lcdY + lcdH, 0xFF104A64.toInt())

        // Contour extérieur
        drawBorder(graphics, lcdX - 1, lcdY - 1, lcdX + lcdW + 1, lcdY + lcdH + 1, 0xFF000000.toInt())

        if (activeRegion.badges.isNotEmpty()) {
            val capText = Component.translatable("cobblemongymodyssey.badge_case.lcd.level_cap", menu.levelCap).string
            graphics.drawString(font, capText, lcdX + 8, lcdY + 8, 0xFF00FFCC.toInt(), false)

            val badgeCountText = Component.translatable(
                "cobblemongymodyssey.badge_case.lcd.badges",
                menu.unlockedBadges.size,
                activeRegion.badges.size
            ).string
            graphics.drawString(font, badgeCountText, lcdX + 8, lcdY + 20, 0xFF00CC99.toInt(), false)
        } else {
            val waitText = Component.translatable("cobblemongymodyssey.badge_case.coming_soon").string
            graphics.drawString(font, waitText, lcdX + 8, lcdY + 14, 0xFF00CC99.toInt(), false)
        }
    }

    private fun drawPixelArtTrophy(graphics: GuiGraphics, tx: Int, ty: Int) {
        val cx = tx + 8
        val cy = ty + 12

        val gold = 0xFFF1C40F.toInt()
        val darkGold = 0xFFD35400.toInt()
        val lightGold = 0xFFFFEAA7.toInt()
        val silver = 0xFFBDC3C7.toInt()
        val darkSilver = 0xFF7F8C8D.toInt()
        val black = 0xFF000000.toInt()

        // 1. Socle
        graphics.fill(cx + 1, cy + 22, cx + 13, cy + 24, black)
        graphics.fill(cx + 2, cy + 21, cx + 12, cy + 23, darkSilver)
        graphics.fill(cx + 3, cy + 20, cx + 11, cy + 21, silver)

        // 2. Tige
        graphics.fill(cx + 6, cy + 13, cx + 8, cy + 20, darkGold)
        graphics.fill(cx + 7, cy + 13, cx + 9, cy + 20, gold)

        // 3. Corps de la coupe (contour + fond)
        graphics.fill(cx + 2, cy + 1, cx + 12, cy + 13, black)
        graphics.fill(cx + 3, cy + 2, cx + 11, cy + 12, darkGold)
        graphics.fill(cx + 4, cy + 2, cx + 10, cy + 11, gold)
        graphics.fill(cx + 4, cy + 3, cx + 6, cy + 8, lightGold) // Reflet gauche

        // 4. Anses
        // Anse gauche
        graphics.fill(cx + 0, cy + 3, cx + 2, cy + 9, black)
        graphics.fill(cx + 1, cy + 4, cx + 3, cy + 8, gold)
        graphics.fill(cx + 1, cy + 3, cx + 3, cy + 4, gold)
        // Anse droite
        graphics.fill(cx + 12, cy + 3, cx + 14, cy + 9, black)
        graphics.fill(cx + 11, cy + 4, cx + 13, cy + 8, darkGold)
        graphics.fill(cx + 11, cy + 3, cx + 13, cy + 4, darkGold)

        // 5. Gemme centrale rouge
        graphics.fill(cx + 6, cy + 6, cx + 8, cy + 8, 0xFFE74C3C.toInt())
    }

    private fun slotPosition(guiX: Int, guiY: Int, index: Int): Pair<Int, Int> {
        val col = index % 4
        val row = index / 4
        val gx = guiX + 60
        val gy = guiY + 52
        return Pair(
            gx + col * 27, // 20px de taille + 7px d'espacement
            gy + row * 34  // 20px de taille + 14px d'espacement
        )
    }

    private fun drawBorder(graphics: GuiGraphics, x1: Int, y1: Int, x2: Int, y2: Int, color: Int) {
        graphics.fill(x1, y1, x2, y1 + 1, color)
        graphics.fill(x1, y2 - 1, x2, y2, color)
        graphics.fill(x1, y1, x1 + 1, y2, color)
        graphics.fill(x2 - 1, y1, x2, y2, color)
    }

    override fun renderTooltip(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val x = (width  - imageWidth)  / 2
        val y = (height - imageHeight) / 2
        val badges = activeRegion.badges
        val unlockedBadges = menu.unlockedBadges

        badges.forEachIndexed { i, badge ->
            val (slotX, slotY) = slotPosition(x, y, i)
            if (mouseX >= slotX && mouseX < slotX + 20 && mouseY >= slotY && mouseY < slotY + 20) {
                val lines = mutableListOf<Component>()
                lines += Component.translatable("item.cobblemongymodyssey.${badge.id}")
                if (badge in unlockedBadges) {
                    lines += Component.translatable(
                        "cobblemongymodyssey.badge_case.level_cap_tooltip",
                        badge.levelCap
                    )
                } else {
                    lines += Component.translatable("cobblemongymodyssey.badge_case.locked_tooltip")
                }
                graphics.renderComponentTooltip(font, lines, mouseX, mouseY)
            }
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val x = (width  - imageWidth)  / 2
        val y = (height - imageHeight) / 2

        val tabCount = Region.entries.size
        val spacing = 2
        val startX = x + (GUI_WIDTH - (tabCount * TAB_WIDTH + (tabCount - 1) * spacing)) / 2

        Region.entries.forEachIndexed { i, region ->
            val tabX = startX + i * (TAB_WIDTH + spacing)
            val tabY = y - 14
            if (mouseX >= tabX && mouseX <= tabX + TAB_WIDTH &&
                mouseY >= tabY && mouseY <= tabY + TAB_HEIGHT) {
                activeRegion = region
                return true
            }
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }
}
