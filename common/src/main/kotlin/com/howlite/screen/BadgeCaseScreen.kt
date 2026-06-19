package com.howlite.screen

import com.howlite.CobblemonGymOdyssey
import com.howlite.data.GymBadge
import com.howlite.data.PokemonSnapshot
import com.howlite.menu.BadgeCaseMenu
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.api.pokemon.PokemonProperties
import com.cobblemon.mod.common.client.render.models.blockbench.FloatingState
import com.cobblemon.mod.common.util.math.fromEulerXYZDegrees
import org.joml.Quaternionf
import org.joml.Vector3f
import com.cobblemon.mod.common.client.gui.drawProfilePokemon
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.player.Inventory

/**
 * Interface graphique de la Boîte à Badges.
 *
 * Rework complet utilisant les assets fournis par l'utilisateur pour correspondre
 * au layout pixel-art premium de l'image de référence.
 * Enrichie avec du "juice" (particules GUI, animations sinusoïdales, typewriter LCD, bruitages interactifs).
 */
class BadgeCaseScreen(
    menu: BadgeCaseMenu,
    inventory: Inventory,
    title: Component
) : AbstractContainerScreen<BadgeCaseMenu>(menu, inventory, title) {

    companion object {
        const val GUI_WIDTH  = 184
        const val GUI_HEIGHT = 145

        val BACKGROUND_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CobblemonGymOdyssey.MOD_ID,
            "textures/gui/badge_box_background.png"
        )
        val BACKGROUND_ALOLA_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CobblemonGymOdyssey.MOD_ID,
            "textures/gui/badge_box_background_alola.png"
        )
        val BACKGROUND_GALAR_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CobblemonGymOdyssey.MOD_ID,
            "textures/gui/badge_box_background_galar.png"
        )
        val TOPBAR_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CobblemonGymOdyssey.MOD_ID,
            "textures/gui/badge_box_topbar.png"
        )
        val LEFT_BUTTON_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CobblemonGymOdyssey.MOD_ID,
            "textures/gui/left_button.png"
        )
        val REGIONS_BUTTON_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CobblemonGymOdyssey.MOD_ID,
            "textures/gui/regions_button.png"
        )
        val SHOP_BUTTON_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CobblemonGymOdyssey.MOD_ID,
            "textures/gui/shop_button.png"
        )
        val FIGHT_BUTTON_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CobblemonGymOdyssey.MOD_ID,
            "textures/gui/fight_button.png"
        )
        val ALTAR_BUTTON_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CobblemonGymOdyssey.MOD_ID,
            "textures/gui/altar_button.png"
        )
        val RIGHT_BUTTON_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CobblemonGymOdyssey.MOD_ID,
            "textures/gui/right_button.png"
        )
    }

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
            0xFF55EFC4.toInt(),
            0xFF21A354.toInt(),
            0xFF2ED573.toInt()
        ),
        JOHTO(
            "cobblemongymodyssey.badge_case.tab.johto",
            listOf(
                GymBadge.ZEPHYR_BADGE, GymBadge.HIVE_BADGE,
                GymBadge.PLAIN_BADGE,  GymBadge.FOG_BADGE,
                GymBadge.STORM_BADGE,  GymBadge.MINERAL_BADGE,
                GymBadge.GLACIER_BADGE, GymBadge.RISING_BADGE
            ),
            0xFF1E90FF.toInt(), // Bleu Cobalt
            0xFF70A1FF.toInt(),
            0xFF0F6EBD.toInt(),
            0xFF70A1FF.toInt()
        ),
        HOENN(
            "cobblemongymodyssey.badge_case.tab.hoenn",
            listOf(
                GymBadge.STONE_BADGE, GymBadge.KNUCKLE_BADGE,
                GymBadge.DYNAMO_BADGE, GymBadge.HEAT_BADGE,
                GymBadge.BALANCE_BADGE, GymBadge.FEATHER_BADGE,
                GymBadge.MIND_BADGE, GymBadge.RAIN_BADGE
            ),
            0xFFFF4757.toInt(), // Rouge Rubis
            0xFFFF6B81.toInt(),
            0xFFC92836.toInt(),
            0xFFFF6B81.toInt()
        ),
        SINNOH(
            "cobblemongymodyssey.badge_case.tab.sinnoh",
            listOf(
                GymBadge.COAL_BADGE, GymBadge.FOREST_BADGE,
                GymBadge.COBBLE_BADGE, GymBadge.FEN_BADGE,
                GymBadge.RELIC_BADGE, GymBadge.MINE_BADGE,
                GymBadge.ICICLE_BADGE, GymBadge.BEACON_BADGE
            ),
            0xFFA29BFE.toInt(), // Violet Platine
            0xFFD6A2E8.toInt(),
            0xFF6D214F.toInt(),
            0xFFD6A2E8.toInt()
        ),
        UNOVA(
            "cobblemongymodyssey.badge_case.tab.unova",
            listOf(
                GymBadge.TOXIC_BADGE, GymBadge.BASIC_BADGE,
                GymBadge.INSECT_BADGE, GymBadge.BOLT_BADGE,
                GymBadge.QUAKE_BADGE, GymBadge.JET_BADGE,
                GymBadge.LEGEND_BADGE, GymBadge.WAVE_BADGE
            ),
            0xFF34495E.toInt(), // Gris foncé / Bleu nuit
            0xFF7F8C8D.toInt(),
            0xFF2C3E50.toInt(),
            0xFF7F8C8D.toInt()
        ),
        KALOS(
            "cobblemongymodyssey.badge_case.tab.kalos",
            listOf(
                GymBadge.BUG_BADGE, GymBadge.CLIFF_BADGE,
                GymBadge.RUMBLE_BADGE, GymBadge.PLANT_BADGE,
                GymBadge.VOLTAGE_BADGE, GymBadge.KALOS_FAIRY_BADGE,
                GymBadge.PSYCHIC_BADGE, GymBadge.ICEBERG_BADGE
            ),
            0xFF3B3B98.toInt(), // Indigo
            0xFFFEA47F.toInt(),
            0xFF1B1464.toInt(),
            0xFFFEA47F.toInt()
        ),
        ALOLA(
            "cobblemongymodyssey.badge_case.tab.alola",
            listOf(
                GymBadge.MELEMELE_STAMP, GymBadge.AKALA_STAMP,
                GymBadge.ULAULA_STAMP, GymBadge.PONI_STAMP
            ),
            0xFFFF9F43.toInt(), // Orange chaud
            0xFFFFF200.toInt(),
            0xFFEE5253.toInt(),
            0xFFFFF200.toInt()
        ),
        GALAR(
            "cobblemongymodyssey.badge_case.tab.galar",
            listOf(
                GymBadge.GRASS_BADGE, GymBadge.WATER_BADGE,
                GymBadge.FIRE_BADGE, GymBadge.FIGHTING_BADGE,
                GymBadge.GHOST_BADGE, GymBadge.GALAR_FAIRY_BADGE,
                GymBadge.ROCK_BADGE, GymBadge.ICE_BADGE,
                GymBadge.DARK_BADGE, GymBadge.DRAGON_BADGE
            ),
            0xFFE056FD.toInt(), // Magenta
            0xFFF8EFBA.toInt(),
            0xFFBE2EDD.toInt(),
            0xFFF8EFBA.toInt()
        ),
        PALDEA(
            "cobblemongymodyssey.badge_case.tab.paldea",
            listOf(
                GymBadge.CORTONDO_BADGE, GymBadge.ARTAZON_BADGE,
                GymBadge.LEVINCIA_BADGE, GymBadge.CASCARRAFA_BADGE,
                GymBadge.MEDALI_BADGE, GymBadge.MONTENEVERA_BADGE,
                GymBadge.ALFORNADA_BADGE, GymBadge.GLASEADO_BADGE
            ),
            0xFF6F1E51.toInt(), // Violet/Bordeaux
            0xFFED4C67.toInt(),
            0xFF353B48.toInt(),
            0xFFED4C67.toInt()
        );

        val ribbonName: String
            get() = when (this) {
                KANTO, JOHTO -> "ribbon_champion"
                HOENN -> "ribbon_champion_hoenn"
                SINNOH -> "ribbon_champion_sinnoh"
                UNOVA -> "ribbon_placeholder"
                KALOS -> "ribbon_champion_kalos"
                ALOLA -> "ribbon_champion_alola"
                GALAR -> "ribbon_champion_galar"
                PALDEA -> "ribbon_champion_paldea"
            }
    }

    /** Classe de particule GUI personnalisée pour animer l'écran */
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
            vy += 0.03f // Légère gravité
            vx *= 0.98f // Résistance de l'air
            age++
        }

        fun render(graphics: GuiGraphics) {
            val alpha = ((1.0f - (age.toFloat() / maxAge.toFloat())) * 255).toInt().coerceIn(0, 255)
            val finalColor = (alpha shl 24) or (color and 0x00FFFFFF)
            val size = if (age > maxAge * 0.7) 1 else 2
            graphics.fill(x.toInt(), y.toInt(), x.toInt() + size, y.toInt() + size, finalColor)
        }
    }

    private var activeRegion: Region = Region.KANTO
    private var scrollOffset: Int = 0

    private var viewedBadgeTeam: GymBadge? = null
    private var lastViewedBadgeTeam: GymBadge? = null

    private val clientPokemonCache: MutableMap<String, List<Pokemon>> = mutableMapOf()
    private val slotStates = List(6) { FloatingState() }

    private fun getClientPokemonList(badgeId: String): List<Pokemon> {
        return clientPokemonCache.getOrPut(badgeId) {
            val snapshots = menu.badgeTeams[badgeId] ?: return@getOrPut emptyList()
            snapshots.map { snapshot ->
                try {
                    val properties = PokemonProperties.parse(
                        "${snapshot.species} level=${snapshot.level} shiny=${if (snapshot.isShiny) "yes" else "no"}"
                    )
                    val pokemon = properties.create()
                    if (snapshot.displayName.isNotEmpty()) {
                        pokemon.nickname = Component.literal(snapshot.displayName)
                    }
                    pokemon
                } catch (e: Exception) {
                    e.printStackTrace()
                    Pokemon()
                }
            }
        }
    }

    // Gestion des animations et particules
    private val particles = mutableListOf<GuiParticle>()
    private val random = java.util.Random()
    private var clientTicks = 0
    private var lcdTextProgress = 0f
    private var lastActiveRegion: Region = Region.KANTO

    override fun init() {
        super.init()
        imageWidth  = GUI_WIDTH
        imageHeight = GUI_HEIGHT
        titleLabelX = -9999
        inventoryLabelY = GUI_HEIGHT + 100
        updateScrollOffset()
    }

    override fun renderLabels(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        // Do nothing to hide default screen labels
    }

    private fun updateScrollOffset() {
        val idx = activeRegion.ordinal
        if (idx < scrollOffset) {
            scrollOffset = idx
        } else if (idx >= scrollOffset + 3) {
            scrollOffset = idx - 2
        }
        scrollOffset = scrollOffset.coerceIn(0, Region.entries.size - 3)
    }

    override fun containerTick() {
        super.containerTick()
        clientTicks++

        // Avancement de l'effet typewriter LCD
        if (lcdTextProgress < 100f) {
            lcdTextProgress += 1.2f
        }

        // Tick des particules
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.tick()
            if (p.age >= p.maxAge) {
                iterator.remove()
            }
        }

        // Faire scintiller de temps en temps les badges obtenus de la région active
        val x = (width - imageWidth) / 2
        val y = (height - imageHeight) / 2
        val obtainedBadgeIndices = activeRegion.badges.indices.filter { activeRegion.badges[it] in menu.unlockedBadges }
        if (obtainedBadgeIndices.isNotEmpty() && random.nextInt(25) == 0) {
            val randomIdx = obtainedBadgeIndices[random.nextInt(obtainedBadgeIndices.size)]
            val slotX = getSlotX(randomIdx)
            val slotY = getSlotY(randomIdx)
            val px = (x + slotX + 2 + random.nextInt(12)).toFloat()
            val py = (y + slotY + 2 + random.nextInt(12)).toFloat()
            particles.add(GuiParticle(
                px, py,
                (random.nextFloat() - 0.5f) * 0.4f,
                -random.nextFloat() * 0.4f - 0.1f,
                activeRegion.highlightColor,
                10 + random.nextInt(8)
            ))
        }
    }

    override fun renderBg(graphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        val x = (width  - imageWidth)  / 2
        val y = (height - imageHeight) / 2
        val region = activeRegion

        // 1. Dessiner le fond principal
        val bgTexture = when (region) {
            Region.ALOLA -> BACKGROUND_ALOLA_TEXTURE
            Region.GALAR -> BACKGROUND_GALAR_TEXTURE
            else -> BACKGROUND_TEXTURE
        }
        graphics.blit(bgTexture, x, y, 0f, 0f, GUI_WIDTH, GUI_HEIGHT, GUI_WIDTH, GUI_HEIGHT)

        // 2. Dessiner et teinter la barre supérieure
        val color = region.primaryColor
        val r = ((color shr 16) and 0xFF) / 255.0f
        val g = ((color shr 8) and 0xFF) / 255.0f
        val b = (color and 0xFF) / 255.0f
        RenderSystem.setShaderColor(r, g, b, 1.0f)
        graphics.blit(TOPBAR_TEXTURE, x + 6, y + 1, 0f, 0f, 172, 17, 172, 17)
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)

        // 3. Dessiner le titre de la ligue dans la barre supérieure
        val bannerText = Component.translatable("cobblemongymodyssey.badge_case.league_format", Component.translatable(region.labelKey)).string.uppercase()
        val bannerW = font.width(bannerText)
        graphics.drawString(font, bannerText, x + (GUI_WIDTH - bannerW) / 2, y + 6, 0xFFFFFF, true)

        if (viewedBadgeTeam != null) {
            renderWinningTeam(graphics, x, y, mouseX, mouseY, partialTick)
        } else {
            renderCenterPokeBallAndRibbon(graphics, x, y, partialTick)
            renderShopButton(graphics, x, y, mouseX, mouseY)
            renderFightButton(graphics, x, y, mouseX, mouseY)
            if (activeRegion == Region.UNOVA || activeRegion == Region.ALOLA || activeRegion == Region.PALDEA) {
                renderAltarButton(graphics, x, y, mouseX, mouseY)
            }
        }

        // 5. Dessiner la grille des badges (TOUJOURS visible !)
        renderBadgeGrid(graphics, x, y, mouseX, mouseY, partialTick)

        // 6. Dessiner les informations LCD (barre inférieure)
        renderLCDDisplay(graphics, x, y)

        // 7. Dessiner la barre d'onglets OU le bouton Retour
        if (viewedBadgeTeam != null) {
            renderBackButton(graphics, x, y, mouseX, mouseY)
        } else {
            renderTabsAndArrows(graphics, x, y, mouseX, mouseY)
        }
    }

    private fun renderCenterPokeBallAndRibbon(graphics: GuiGraphics, x: Int, y: Int, partialTick: Float) {
        val region = activeRegion
        val rx = x + 76
        val isCompleted = region.badges.isNotEmpty() && region.badges.all { it in menu.unlockedBadges }
        val time = clientTicks + partialTick

        if (isCompleted) {
            val glowColor = (region.primaryColor and 0x00FFFFFF)
            val baseAlpha = (24 + (kotlin.math.sin(time * 0.15) * 10)).toInt().coerceIn(0, 255)
            val cx = rx + 16
            val cy = y + 58 + 16

            drawGlowCircle(graphics, cx, cy, 26, ((baseAlpha * 0.15f).toInt() shl 24) or glowColor)
            drawGlowCircle(graphics, cx, cy, 18, ((baseAlpha * 0.35f).toInt() shl 24) or glowColor)
            drawGlowCircle(graphics, cx, cy, 12, ((baseAlpha * 0.65f).toInt() shl 24) or glowColor)
            drawGlowCircle(graphics, cx, cy, 6, (baseAlpha shl 24) or glowColor)
        }

        val bobbingY = if (isCompleted) (kotlin.math.sin(time * 0.08) * 1.5).toFloat() else 0f
        val ry = y + 58 + bobbingY.toInt()

        val ribbonTexName = region.ribbonName
        val ribbonTexPath = if (region == Region.UNOVA) {
            "textures/gui/ribbon/ribbon_placeholder_hollow.png"
        } else if (isCompleted) {
            "textures/gui/ribbon/$ribbonTexName.png"
        } else {
            "textures/gui/ribbon/${ribbonTexName}_hollow.png"
        }
        val ribbonTexture = ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, ribbonTexPath)
        graphics.blit(ribbonTexture, rx, ry, 0f, 0f, 32, 32, 32, 32)
    }

    private fun renderBackButton(graphics: GuiGraphics, x: Int, y: Int, mouseX: Int, mouseY: Int) {
        val backX = x + 68
        val backY = y + 143
        val isBackHovered = mouseX >= backX && mouseX < backX + 48 && mouseY >= backY && mouseY < backY + 14
        val backV = if (isBackHovered) 14f else 0f

        graphics.blit(REGIONS_BUTTON_TEXTURE, backX, backY, 0f, backV, 48, 14, 48, 28)

        val backText = Component.translatable("cobblemongymodyssey.badge_case.back").string
        val backW = font.width(backText)
        graphics.drawString(font, backText, backX + (48 - backW) / 2, backY + 3, if (isBackHovered) 0xFFFFA800.toInt() else 0xFFFFFF, false)
    }

    private fun renderShopButton(graphics: GuiGraphics, x: Int, y: Int, mouseX: Int, mouseY: Int) {
        val shopX = x - 53
        val shopY = y + 32
        val hasAnyBadge = activeRegion.badges.any { it in menu.unlockedBadges }
        val isHovered = mouseX >= shopX && mouseX < shopX + 53 && mouseY >= shopY && mouseY < shopY + 14

        val buttonV = if (hasAnyBadge && isHovered) 14f else 0f

        if (!hasAnyBadge) {
            RenderSystem.setShaderColor(0.5f, 0.5f, 0.5f, 1.0f)
        }
        graphics.blit(SHOP_BUTTON_TEXTURE, shopX, shopY, 0f, buttonV, 53, 14, 53, 28)
        if (!hasAnyBadge) {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
        }

        val text = Component.translatable("cobblemongymodyssey.badge_case.shop_button").string
        val textW = font.width(text)
        val textColor = if (!hasAnyBadge) {
            0x8E8E93
        } else if (isHovered) {
            0xFFFFA800.toInt()
        } else {
            0xFFFFFF
        }
        graphics.drawString(font, text, shopX + 3 + (31 - textW) / 2, shopY + 3, textColor, false)
    }

    private fun renderFightButton(graphics: GuiGraphics, x: Int, y: Int, mouseX: Int, mouseY: Int) {
        val fightX = x - 53
        val fightY = y + 48
        val isHovered = mouseX >= fightX && mouseX < fightX + 53 && mouseY >= fightY && mouseY < fightY + 14
        val buttonV = if (isHovered) 14f else 0f

        graphics.blit(FIGHT_BUTTON_TEXTURE, fightX, fightY, 0f, buttonV, 53, 14, 53, 28)

        val text = Component.translatable("cobblemongymodyssey.badge_case.fight_button").string
        val textW = font.width(text)
        val textColor = if (isHovered) 0xFFFFA800.toInt() else 0xFFFFFF
        graphics.drawString(font, text, fightX + 3 + (31 - textW) / 2, fightY + 3, textColor, false)
    }

    private fun renderAltarButton(graphics: GuiGraphics, x: Int, y: Int, mouseX: Int, mouseY: Int) {
        val altarX = x - 53
        val altarY = y + 64
        val isHovered = mouseX >= altarX && mouseX < altarX + 53 && mouseY >= altarY && mouseY < altarY + 14
        val buttonV = if (isHovered) 14f else 0f

        // Pulsing dark-red glow effect behind the button
        val pulse = (180 + (kotlin.math.sin(clientTicks * 0.12) * 75)).toInt().coerceIn(0, 255)
        val glowColor = (pulse shl 24) or 0x550011
        graphics.fill(altarX - 1, altarY - 1, altarX + 54, altarY + 15, glowColor)

        graphics.blit(ALTAR_BUTTON_TEXTURE, altarX, altarY, 0f, buttonV, 53, 14, 53, 28)

        val text = "AUTEL"
        val textW = font.width(text)
        val textColor = if (isHovered) 0xFFFF4444.toInt() else 0xFFCC2222.toInt()
        graphics.drawString(font, text, altarX + 3 + (31 - textW) / 2, altarY + 3, textColor, false)
    }

    private fun renderWinningTeam(graphics: GuiGraphics, x: Int, y: Int, mouseX: Int, mouseY: Int, partialTick: Float) {
        val badge = viewedBadgeTeam ?: return
        val team = menu.badgeTeams[badge.id] ?: emptyList()

        // Dessiner les 6 emplacements de Pokémon arrangés en 2 lignes de 3 colonnes
        for (i in 0 until 6) {
            val col = i % 3
            val row = i / 3
            val centerX = x + 47 + col * 45
            val centerY = y + 32 + row * 28

            if (i < team.size) {
                // Rendu 3D du modèle de Pokémon
                try {
                    val pokemonList = getClientPokemonList(badge.id)
                    if (i < pokemonList.size) {
                        val pokemon = pokemonList[i]
                        val poseStack = graphics.pose()
                        poseStack.pushPose()
                        poseStack.translate(centerX.toDouble(), (centerY - 15).toDouble(), 0.0)
                        poseStack.scale(3.5F, 3.5F, 1F)
                        drawProfilePokemon(
                            renderablePokemon = pokemon.asRenderablePokemon(),
                            matrixStack = poseStack,
                            rotation = Quaternionf().fromEulerXYZDegrees(Vector3f(13F, 35F, 0F)),
                            state = slotStates[i],
                            partialTicks = partialTick,
                            scale = 6.5F
                        )
                        poseStack.popPose()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Afficher une étoile dorée si le Pokémon est shiny
                val snapshot = team[i]
                if (snapshot.isShiny) {
                    graphics.drawString(font, "★", centerX - 14, centerY - 29, 0xFFFFA800.toInt(), true)
                }
            } else {
                // Emplacement vide subtil
                graphics.drawString(font, "—", centerX - 3, centerY - 19, 0x44555555, true)
            }
        }

        // 3. Dessiner le titre de l'équipe gagnante
        val badgeName = Component.translatable("item.cobblemongymodyssey.${badge.id}").string
        val titleText = Component.translatable("cobblemongymodyssey.badge_case.winning_team", badgeName).string
        val titleW = font.width(titleText)
        graphics.drawString(font, titleText, x + (GUI_WIDTH - titleW) / 2, y + 90, 0xFFFFFF, true)
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(graphics, mouseX, mouseY, partialTick)
        super.render(graphics, mouseX, mouseY, partialTick)

        // Dessiner les particules interactives par-dessus le fond
        particles.forEach { it.render(graphics) }

        renderTooltip(graphics, mouseX, mouseY)
    }

    private fun getSlotX(index: Int): Int {
        if (activeRegion == Region.ALOLA) {
            val alolaSlotXs = intArrayOf(46, 64, 104, 122)
            return alolaSlotXs.getOrElse(index) { 0 }
        }
        if (activeRegion == Region.GALAR) {
            val galarSlotXs = intArrayOf(
                10, 158, // Row 1 (y=93)
                10, 28, 46, 64, 104, 122, 140, 158 // Row 2 (y=111)
            )
            return galarSlotXs.getOrElse(index) { 0 }
        }
        val slotXs = intArrayOf(10, 28, 46, 64, 104, 122, 140, 158)
        return slotXs.getOrElse(index) { 0 }
    }

    private fun getSlotY(index: Int): Int {
        if (activeRegion == Region.GALAR) {
            val galarSlotYs = intArrayOf(
                93, 93,
                111, 111, 111, 111, 111, 111, 111, 111
            )
            return galarSlotYs.getOrElse(index) { 111 }
        }
        return 111
    }

    private fun drawHoverBorder(graphics: GuiGraphics, sx: Int, sy: Int, color: Int) {
        graphics.fill(sx - 1, sy - 1, sx + 17, sy, color) // Top
        graphics.fill(sx - 1, sy + 16, sx + 17, sy + 17, color) // Bottom
        graphics.fill(sx - 1, sy, sx, sy + 16, color) // Left
        graphics.fill(sx + 16, sy, sx + 17, sy + 16, color) // Right
    }

    private fun drawGlowCircle(graphics: GuiGraphics, cx: Int, cy: Int, r: Int, color: Int) {
        for (dy in -r..r) {
            val dx = kotlin.math.sqrt((r * r - dy * dy).toDouble()).toInt()
            if (dx > 0) {
                graphics.fill(cx - dx, cy + dy, cx + dx, cy + dy + 1, color)
            }
        }
    }

    private fun renderBadgeGrid(graphics: GuiGraphics, guiX: Int, guiY: Int, mouseX: Int, mouseY: Int, partialTick: Float) {
        val badges = activeRegion.badges
        val unlockedBadges = menu.unlockedBadges

        if (badges.isEmpty()) {
            val msg = Component.translatable("cobblemongymodyssey.badge_case.coming_soon")
            val msgX = guiX + (GUI_WIDTH - font.width(msg)) / 2
            val msgY = guiY + 68
            graphics.drawString(font, msg, msgX, msgY, 0x57606F, false)
            return
        }

        badges.forEachIndexed { i, badge ->
            val slotX = getSlotX(i)
            val slotY = getSlotY(i)
            val sx = guiX + slotX
            val sy = guiY + slotY
            val isUnlocked = badge in unlockedBadges
            val isHovered = mouseX >= sx && mouseX < sx + 16 && mouseY >= sy && mouseY < sy + 16

            val badgeTexture = ResourceLocation.fromNamespaceAndPath(
                CobblemonGymOdyssey.MOD_ID,
                if (isUnlocked) badge.texturePath else badge.hollowTexturePath
            )

            // Rebond/bobbing interactif au survol
            val offset = if (isHovered) {
                val time = clientTicks + partialTick
                val bob = kotlin.math.sin(time * 0.2f) * 1.2f
                -1f + bob
            } else {
                0f
            }
            val renderY = sy + offset.toInt()

            val useShaderColor = !isUnlocked && badge.texturePath == badge.hollowTexturePath
            if (useShaderColor) {
                // Rendre assombri/presque noir pour simuler un badge verrouillé
                RenderSystem.setShaderColor(0.12f, 0.12f, 0.12f, 0.75f)
            }

            graphics.blit(badgeTexture, sx, renderY, 0f, 0f, 16, 16, 16, 16)

            if (useShaderColor) {
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
            }

            // Rendu de la bordure pulsante au survol
            if (isHovered) {
                val pulse = (160 + (kotlin.math.sin((clientTicks + partialTick) * 0.3) * 95)).toInt().coerceIn(0, 255)
                val hoverColor = (pulse shl 24) or (activeRegion.highlightColor and 0x00FFFFFF)
                drawHoverBorder(graphics, sx, sy, hoverColor)
            }
        }
    }

    private fun renderLCDDisplay(graphics: GuiGraphics, guiX: Int, guiY: Int) {
        val yPos = guiY + 134

        // 1. Dessiner les scanlines LCD (fines lignes rétro horizontales)
        for (sy in 131..143 step 2) {
            graphics.fill(guiX + 2, guiY + sy, guiX + 182, guiY + sy + 1, 0x12000000)
        }

        val rawText = if (viewedBadgeTeam != null) {
            Component.translatable("cobblemongymodyssey.badge_case.lcd.back_hint").string
        } else if (activeRegion.badges.isNotEmpty()) {
            val capText = Component.translatable("cobblemongymodyssey.badge_case.lcd.level_cap", menu.levelCap).string
            val badgeCountText = Component.translatable(
                "cobblemongymodyssey.badge_case.lcd.badges",
                menu.unlockedBadges.count { it in activeRegion.badges },
                activeRegion.badges.size
            ).string
            "$capText   •   $badgeCountText"
        } else {
            Component.translatable("cobblemongymodyssey.badge_case.coming_soon").string
        }

        // Effet d'écriture typewriter
        val textStateChanged = (viewedBadgeTeam != lastViewedBadgeTeam) || (activeRegion != lastActiveRegion)
        if (textStateChanged) {
            lcdTextProgress = 0f
            lastActiveRegion = activeRegion
            lastViewedBadgeTeam = viewedBadgeTeam
        }

        val charsToShow = lcdTextProgress.toInt().coerceIn(0, rawText.length)
        val visibleText = rawText.substring(0, charsToShow)

        val textW = font.width(rawText)
        val startX = guiX + (GUI_WIDTH - textW) / 2
        graphics.drawString(font, visibleText, startX, yPos, 0x00FFCC, true)
    }

    private fun renderTabsAndArrows(graphics: GuiGraphics, guiX: Int, guiY: Int, mouseX: Int, mouseY: Int) {
        // 1. Bouton Gauche (Y = guiY + 147, H = 14)
        val leftX = guiX - 6
        val leftY = guiY + 147
        val isLeftDisabled = activeRegion.ordinal == 0
        val isLeftHovered = !isLeftDisabled && mouseX >= leftX && mouseX < leftX + 26 && mouseY >= leftY && mouseY < leftY + 14
        
        val leftTint = if (isLeftDisabled) 0.5f else 1.0f
        val leftV = if (isLeftHovered) 14f else 0f
        RenderSystem.setShaderColor(leftTint, leftTint, leftTint, 1.0f)
        graphics.blit(LEFT_BUTTON_TEXTURE, leftX, leftY, 0f, leftV, 26, 14, 26, 28)

        // 2. Boutons d'onglets (Y = guiY + 143, H = 14)
        val tabY = guiY + 143
        for (i in 0 until 3) {
            val regIdx = scrollOffset + i
            if (regIdx >= Region.entries.size) break

            val region = Region.entries[regIdx]
            val tabX = guiX + 20 + i * 48
            val isActive = region == activeRegion
            val isHovered = !isActive && mouseX >= tabX && mouseX < tabX + 48 && mouseY >= tabY && mouseY < tabY + 14

            val tabV = if (isActive) 14f else 0f
            val tabTint = 1.0f
            
            RenderSystem.setShaderColor(tabTint, tabTint, tabTint, 1.0f)
            graphics.blit(REGIONS_BUTTON_TEXTURE, tabX, tabY, 0f, tabV, 48, 14, 48, 28)

            val label = Component.translatable(region.labelKey).string
            val textX = tabX + (48 - font.width(label)) / 2
            val textY = tabY + 3
            val textColor = if (isActive) 0xFFFFFF else 0x8E8E93
            graphics.drawString(font, label, textX, textY, textColor, false)
        }

        // 3. Bouton Droit (Y = guiY + 147, H = 14)
        val rightX = guiX + 164
        val rightY = guiY + 147
        val isRightDisabled = activeRegion.ordinal == Region.entries.size - 1
        val isRightHovered = !isRightDisabled && mouseX >= rightX && mouseX < rightX + 26 && mouseY >= rightY && mouseY < rightY + 14

        val rightTint = if (isRightDisabled) 0.5f else 1.0f
        val rightV = if (isRightHovered) 14f else 0f
        RenderSystem.setShaderColor(rightTint, rightTint, rightTint, 1.0f)
        graphics.blit(RIGHT_BUTTON_TEXTURE, rightX, rightY, 0f, rightV, 26, 14, 26, 28)

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
    }

    override fun renderTooltip(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val x = (width  - imageWidth)  / 2
        val y = (height - imageHeight) / 2
        val badges = activeRegion.badges
        val unlockedBadges = menu.unlockedBadges

        if (viewedBadgeTeam == null) {
            val shopX = x - 53
            val shopY = y + 32
            if (mouseX >= shopX && mouseX < shopX + 53 && mouseY >= shopY && mouseY < shopY + 14) {
                val hasAnyBadge = activeRegion.badges.any { it in menu.unlockedBadges }
                val lines = mutableListOf<Component>()
                if (hasAnyBadge) {
                    lines += Component.translatable("cobblemongymodyssey.badge_case.shop.tooltip.open")
                } else {
                    lines += Component.translatable("cobblemongymodyssey.badge_case.shop.tooltip.locked")
                }
                graphics.renderComponentTooltip(font, lines, mouseX, mouseY)
                return
            }

            val fightX = x - 53
            val fightY = y + 48
            if (mouseX >= fightX && mouseX < fightX + 53 && mouseY >= fightY && mouseY < fightY + 14) {
                val lines = mutableListOf<Component>()
                lines += Component.translatable("cobblemongymodyssey.badge_case.fight.tooltip")
                graphics.renderComponentTooltip(font, lines, mouseX, mouseY)
                return
            }

            // Altar button tooltip (only for UNOVA/ALOLA/PALDEA)
            if (activeRegion == Region.UNOVA || activeRegion == Region.ALOLA || activeRegion == Region.PALDEA) {
                val altarX = x - 53
                val altarY = y + 64
                if (mouseX >= altarX && mouseX < altarX + 53 && mouseY >= altarY && mouseY < altarY + 14) {
                    val lines = mutableListOf<Component>()
                    lines += Component.translatable("cobblemongymodyssey.altar.tooltip.title")
                    lines += Component.translatable("cobblemongymodyssey.altar.tooltip.desc")
                    graphics.renderComponentTooltip(font, lines, mouseX, mouseY)
                    return
                }
            }
        }

        if (viewedBadgeTeam != null) {
            val badge = viewedBadgeTeam!!
            val team = menu.badgeTeams[badge.id] ?: emptyList()
            for (i in 0 until 6) {
                val col = i % 3
                val row = i / 3
                val minX = x + 20 + col * 48
                val maxX = minX + 48
                val minY = y + 18 + row * 36
                val maxY = minY + 36
                if (mouseX >= minX && mouseX < maxX && mouseY >= minY && mouseY < maxY) {
                    if (i < team.size) {
                        val pokemon = team[i]
                        val tooltipLines = mutableListOf<Component>()
                        var nameComp = Component.literal(pokemon.displayName)
                        if (pokemon.isShiny) {
                            nameComp = Component.literal("★ ").withStyle { it.withColor(0xFFFFA800.toInt()) }.append(nameComp)
                        }
                        tooltipLines += nameComp
                        tooltipLines += Component.translatable("cobblemongymodyssey.badge_case.tooltip.species", pokemon.species.uppercase())
                        tooltipLines += Component.translatable("cobblemongymodyssey.badge_case.tooltip.level", pokemon.level)
                        graphics.renderComponentTooltip(font, tooltipLines, mouseX, mouseY)
                    }
                    return
                }
            }
            val gridMinY = if (activeRegion == Region.GALAR) 93 else 111
            if (mouseY < y + gridMinY) {
                return
            }
        }

        badges.forEachIndexed { i, badge ->
            val slotX = getSlotX(i)
            val slotY = getSlotY(i)
            val sx = x + slotX
            val sy = y + slotY
            if (mouseX >= sx && mouseX < sx + 16 && mouseY >= sy && mouseY < sy + 16) {
                val lines = mutableListOf<Component>()
                lines += Component.translatable("item.cobblemongymodyssey.${badge.id}")
                if (badge in unlockedBadges) {
                    if (badge.region == com.howlite.data.GymRegion.KANTO) {
                        lines += Component.translatable(
                            "cobblemongymodyssey.badge_case.level_cap_tooltip",
                            badge.levelCap
                        )
                    }
                } else {
                    lines += Component.translatable("cobblemongymodyssey.badge_case.locked_tooltip")
                }
                graphics.renderComponentTooltip(font, lines, mouseX, mouseY)
                return
            }
        }

        val rx = x + 76
        val isCompleted = activeRegion.badges.isNotEmpty() && activeRegion.badges.all { it in unlockedBadges }
        val bobbingY = if (isCompleted) (kotlin.math.sin((clientTicks) * 0.08) * 1.5).toInt() else 0
        val ry = y + 58 + bobbingY

        if (mouseX >= rx && mouseX < rx + 32 && mouseY >= ry && mouseY < ry + 32) {
            val lines = mutableListOf<Component>()
            val regionName = Component.translatable(activeRegion.labelKey).string
            
            if (isCompleted) {
                lines += Component.translatable("cobblemongymodyssey.badge_case.ribbon.completed_title", regionName)
                lines += Component.translatable("cobblemongymodyssey.badge_case.ribbon.completed_desc")
            } else {
                lines += Component.translatable("cobblemongymodyssey.badge_case.ribbon.incomplete_title", regionName)
                lines += Component.translatable("cobblemongymodyssey.badge_case.ribbon.incomplete_desc")
            }
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY)
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val x = (width  - imageWidth)  / 2
        val y = (height - imageHeight) / 2

        if (viewedBadgeTeam != null) {
            val backX = x + 68
            val backY = y + 143
            if (mouseX >= backX && mouseX < backX + 48 && mouseY >= backY && mouseY < backY + 14) {
                viewedBadgeTeam = null
                minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f))
                return true
            }

            // Effet sonore interactif au clic sur les cartes Pokémon
            val badge = viewedBadgeTeam!!
            val team = menu.badgeTeams[badge.id] ?: emptyList()
            for (i in 0 until 6) {
                val col = i % 3
                val row = i / 3
                val minX = x + 20 + col * 48
                val maxX = minX + 48
                val minY = y + 18 + row * 36
                val maxY = minY + 36
                if (mouseX >= minX && mouseX < maxX && mouseY >= minY && mouseY < maxY) {
                    if (i < team.size) {
                        minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.2f))
                    }
                    return true
                }
            }
            val clickedOnGrid = if (activeRegion == Region.GALAR) {
                (mouseY >= y + 93 && mouseY < y + 109) || (mouseY >= y + 111 && mouseY < y + 127)
            } else {
                mouseY >= y + 111 && mouseY < y + 127
            }
            if (!clickedOnGrid) {
                return true // Bloque les autres clics sauf sur la grille des badges
            }
        }

        if (viewedBadgeTeam == null) {
            val shopX = x - 53
            val shopY = y + 32
            if (mouseX >= shopX && mouseX < shopX + 53 && mouseY >= shopY && mouseY < shopY + 14) {
                val hasAnyBadge = activeRegion.badges.any { it in menu.unlockedBadges }
                if (hasAnyBadge) {
                    minecraft?.gameMode?.handleInventoryButtonClick(menu.containerId, activeRegion.ordinal)
                    minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f))
                } else {
                    minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.DISPENSER_FAIL, 1.0f))
                }
                return true
            }

            val fightX = x - 53
            val fightY = y + 48
            if (mouseX >= fightX && mouseX < fightX + 53 && mouseY >= fightY && mouseY < fightY + 14) {
                minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f))
                val buf = net.minecraft.network.RegistryFriendlyByteBuf(
                    io.netty.buffer.Unpooled.buffer(),
                    minecraft?.level?.registryAccess() ?: throw java.lang.IllegalStateException("Registry access not available")
                )
                dev.architectury.networking.NetworkManager.sendToServer(
                    ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "request_pvp_player_list"),
                    buf
                )
                return true
            }

            // Altar button click (UNOVA / ALOLA / PALDEA only)
            if (activeRegion == Region.UNOVA || activeRegion == Region.ALOLA || activeRegion == Region.PALDEA) {
                val altarX = x - 53
                val altarY = y + 64
                if (mouseX >= altarX && mouseX < altarX + 53 && mouseY >= altarY && mouseY < altarY + 14) {
                    minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f))
                    minecraft?.setScreen(
                        com.howlite.client.screen.AltarScreen(activeRegion)
                    )
                    return true
                }
            }
        }

        // Bouton Gauche (Y = y + 147, H = 14)
        val leftX = x - 6
        val leftY = y + 147
        if (mouseX >= leftX && mouseX < leftX + 26 && mouseY >= leftY && mouseY < leftY + 14) {
            if (activeRegion.ordinal > 0) {
                activeRegion = Region.entries[activeRegion.ordinal - 1]
                updateScrollOffset()
                minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f))
                return true
            }
        }

        // Bouton Droit (Y = y + 147, H = 14)
        val rightX = x + 164
        val rightY = y + 147
        if (mouseX >= rightX && mouseX < rightX + 26 && mouseY >= rightY && mouseY < rightY + 14) {
            if (activeRegion.ordinal < Region.entries.size - 1) {
                activeRegion = Region.entries[activeRegion.ordinal + 1]
                updateScrollOffset()
                minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f))
                return true
            }
        }

        // Onglets (Y = y + 143, H = 14)
        val tabY = y + 143
        for (i in 0 until 3) {
            val regIdx = scrollOffset + i
            if (regIdx >= Region.entries.size) break

            val tabX = x + 20 + i * 48
            if (mouseX >= tabX && mouseX < tabX + 48 && mouseY >= tabY && mouseY < tabY + 14) {
                val region = Region.entries[regIdx]
                if (region != activeRegion) {
                    activeRegion = region
                    updateScrollOffset()
                    minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f))
                    return true
                }
            }
        }

        // Interaction avec les badges (Jaillissement de particules et son interactif)
        val badges = activeRegion.badges
        val unlockedBadges = menu.unlockedBadges
        badges.forEachIndexed { i, badge ->
            val slotX = getSlotX(i)
            val slotY = getSlotY(i)
            val sx = x + slotX
            val sy = y + slotY
            if (mouseX >= sx && mouseX < sx + 16 && mouseY >= sy && mouseY < sy + 16) {
                val isUnlocked = badge in unlockedBadges
                if (isUnlocked) {
                    // Éclatement d'étoiles dorées/colorées
                    for (k in 0 until 12) {
                        val vx = (random.nextFloat() - 0.5f) * 1.8f
                        val vy = -random.nextFloat() * 1.5f - 0.5f
                        particles.add(GuiParticle(
                            (sx + 8).toFloat(), (sy + 8).toFloat(),
                            vx, vy,
                            activeRegion.highlightColor,
                            15 + random.nextInt(10)
                        ))
                    }
                    minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.2f))
                    viewedBadgeTeam = badge
                } else {
                    // Particules de fumée gris sombre
                    for (k in 0 until 6) {
                        val vx = (random.nextFloat() - 0.5f) * 0.8f
                        val vy = -random.nextFloat() * 0.8f - 0.2f
                        particles.add(GuiParticle(
                            (sx + 8).toFloat(), (sy + 8).toFloat(),
                            vx, vy,
                            0xFF555555.toInt(),
                            10 + random.nextInt(5)
                        ))
                    }
                    minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.DISPENSER_FAIL, 1.0f))
                }
                return true
            }
        }

        // Interaction avec le ruban central (Feu d'artifice de particules et chime d'améthyste)
        val rx = x + 76
        val isCompleted = activeRegion.badges.isNotEmpty() && activeRegion.badges.all { it in unlockedBadges }
        val bobbingY = if (isCompleted) (kotlin.math.sin((clientTicks) * 0.08) * 1.5).toInt() else 0
        val ry = y + 58 + bobbingY

        if (mouseX >= rx && mouseX < rx + 32 && mouseY >= ry && mouseY < ry + 32) {
            if (isCompleted) {
                for (k in 0 until 24) {
                    val vx = (random.nextFloat() - 0.5f) * 3.0f
                    val vy = (random.nextFloat() - 0.5f) * 3.0f - 1.0f
                    particles.add(GuiParticle(
                        (rx + 16).toFloat(), (ry + 16).toFloat(),
                        vx, vy,
                        activeRegion.highlightColor,
                        20 + random.nextInt(15)
                    ))
                }
                minecraft?.soundManager?.play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f))
                return true
            }
        }

        return super.mouseClicked(mouseX, mouseY, button)
    }
}
