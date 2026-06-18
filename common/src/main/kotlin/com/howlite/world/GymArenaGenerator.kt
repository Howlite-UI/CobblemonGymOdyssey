package com.howlite.world

import com.howlite.api.PlayerProgressApi
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

object GymArenaGenerator {

    /**
     * Gère la téléportation d'un joueur vers son arène unique et la génère/réinitialise.
     */
    fun teleportAndGenerate(player: ServerPlayer, targetBadgeId: String? = null) {
        val server = player.server
        val progress = PlayerProgressApi.get(player)

        // 1. Allouer un nouvel index d'arène unique à chaque téléportation pour éviter les superpositions
        val saveData = GymDimensionSavedData.get(server)
        val index = saveData.nextArenaIndex++
        saveData.setDirty()
        
        // Mettre à jour progress pour la cohérence (optionnel)
        progress.arenaIndex = index
        PlayerProgressApi.markDirty(player)

        // 2. Récupérer le monde de la dimension d'arènes
        val gymLevelKey = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "gym_dimension")
        )
        val gymWorld = server.getLevel(gymLevelKey)
        if (gymWorld == null) {
            player.sendSystemMessage(Component.translatable("cobblemongymodyssey.gym_generator.dimension_not_found"))
            return
        }

        // 3. Calculer les coordonnées uniques (grille le long de l'axe X)
        val startX = index * 500
        val startY = 64
        val startZ = 0

        // 4. Déterminer le badge à utiliser (argument ou progression du joueur)
        val badgeId = if (targetBadgeId.isNullOrBlank()) {
            val badgeCount = progress.badges.size
            when (badgeCount) {
                0 -> "boulder_badge"
                1 -> "cascade_badge"
                2 -> "thunder_badge"
                3 -> "rainbow_badge"
                4 -> "soul_badge"
                5 -> "marsh_badge"
                6 -> "volcano_badge"
                7 -> "earth_badge"
                else -> "boulder_badge"
            }
        } else {
            targetBadgeId
        }

        // Déterminer le leader en fonction du badge
        val leaderId = when (badgeId) {
            // Kanto
            "boulder_badge" -> "brock"
            "cascade_badge" -> "misty"
            "thunder_badge" -> "lt_surge"
            "rainbow_badge" -> "erika"
            "soul_badge" -> "koga"
            "marsh_badge" -> "sabrina"
            "volcano_badge" -> "blaine"
            "earth_badge" -> "giovanni"
            // Johto
            "zephyr_badge" -> "falkner"
            "hive_badge" -> "bugsy"
            "plain_badge" -> "whitney"
            "fog_badge" -> "morty"
            "storm_badge" -> "chuck"
            "mineral_badge" -> "jasmine"
            "glacier_badge" -> "pryce"
            "rising_badge" -> "clair"
            // Hoenn
            "stone_badge" -> "roxanne"
            "knuckle_badge" -> "brawly"
            "dynamo_badge" -> "wattson"
            "heat_badge" -> "flannery"
            "balance_badge" -> "norman"
            "feather_badge" -> "winona"
            "mind_badge" -> "tate"
            "rain_badge" -> "wallace"
            // Sinnoh
            "coal_badge" -> "roark"
            "forest_badge" -> "gardenia"
            "cobble_badge" -> "maylene"
            "fen_badge" -> "crasher_wake"
            "relic_badge" -> "fantina"
            "mine_badge" -> "byron"
            "icicle_badge" -> "candice"
            "beacon_badge" -> "volkner"
            // Unova
            "trio_badge" -> "cilan"
            "basic_badge" -> "cheren"
            "toxic_badge" -> "roxie"
            "insect_badge" -> "burgh"
            "bolt_badge" -> "elesa"
            "quake_badge" -> "clay"
            "jet_badge" -> "skyla"
            "freeze_badge" -> "brycen"
            "legend_badge" -> "drayden"
            "wave_badge" -> "marlon"
            // Kalos
            "bug_badge" -> "viola"
            "cliff_badge" -> "grant"
            "rumble_badge" -> "korrina"
            "plant_badge" -> "ramos"
            "voltage_badge" -> "clemont"
            "kalos_fairy_badge" -> "valerie"
            "psychic_badge" -> "olympia"
            "iceberg_badge" -> "wulfric"
            // Alola
            "melemele_stamp" -> "hala"
            "akala_stamp" -> "olivia"
            "ulaula_stamp" -> "nanu"
            "poni_stamp" -> "hapu"
            // Galar
            "grass_badge" -> "milo"
            "water_badge" -> "nessa"
            "fire_badge" -> "kabu"
            "fighting_badge" -> "bea"
            "ghost_badge" -> "allister"
            "galar_fairy_badge" -> "opal"
            "rock_badge" -> "gordie"
            "ice_badge" -> "melony"
            "dark_badge" -> "piers"
            "dragon_badge" -> "raihan"
            // Paldea
            "cortondo_badge" -> "katy"
            "artazon_badge" -> "brassius"
            "levincia_badge" -> "iono"
            "cascarrafa_badge" -> "kofu"
            "medali_badge" -> "larry"
            "montenevera_badge" -> "ryme"
            "alfornada_badge" -> "tulip"
            "glaseado_badge" -> "grusha"
            
            else -> "brock"
        }

        player.sendSystemMessage(
            Component.translatable("cobblemongymodyssey.gym_generator.teleporting", formatName(leaderId))
        )

        // 5. Générer d'abord l'arène (pose les blocs et fait spawner le Champion)
        generate(gymWorld, startX, startY, startZ, player, leaderId)

        // 6. Déterminer les coordonnées de spawn pour le joueur
        val (resolvedLoc, templateOpt) = resolveStructure(server, leaderId)

        val spawnX: Double
        val spawnY: Double
        val spawnZ: Double
        val yaw: Float
        if (templateOpt.isPresent) {
            val template = templateOpt.get()
            if (resolvedLoc.path == "gymleaderplaceholder") {
                // Pour le placeholder, le joueur doit spawn en 10.5, 3.0, 45.5 par rapport à l'origine du build et facing le north
                val originX = startX - template.size.x / 2.0
                val originY = startY.toDouble()
                val originZ = startZ - template.size.z / 2.0
                spawnX = originX + 10.5
                spawnY = originY + 3.0
                spawnZ = originZ + 45.5
                yaw = 180f // facing North
            } else {
                // Pour les arènes customs, spawn au centre par défaut
                spawnX = startX + 0.5
                spawnY = startY.toDouble() + 1.0
                spawnZ = startZ + 0.5
                yaw = 0f
            }
        } else {
            // Dans le cas de l'arène de test (fallback), on téléporte devant le portail de retour (Z = -1.0) face au NPC (Sud, yaw = 0f)
            spawnX = startX + 0.5
            spawnY = startY.toDouble() + 1.0
            spawnZ = startZ - 1.0
            yaw = 0f
        }

        // 7. Téléporter le joueur en toute sécurité sur le sol déjà généré
        player.teleportTo(gymWorld, spawnX, spawnY, spawnZ, yaw, 0f)
    }

    /**
     * Génère la structure et nettoie la zone.
     */
    fun generate(world: ServerLevel, startX: Int, startY: Int, startZ: Int, player: ServerPlayer, leaderId: String) {
        val server = world.server

        // 1. Nettoyer la zone (les entités non-joueurs)
        val box = AABB(
            startX - 25.0, startY - 10.0, startZ - 25.0,
            startX + 25.0, startY + 25.0, startZ + 25.0
        )
        val entities = world.getEntitiesOfClass(Entity::class.java, box) { it !is Player }
        entities.forEach { it.discard() }

        // 2. Remplacer les blocs de la zone par du vide (Air)
        val mutablePos = BlockPos.MutableBlockPos()
        for (dx in -25..25) {
            for (dy in -5..25) {
                for (dz in -25..25) {
                    mutablePos.set(startX + dx, startY + dy, startZ + dz)
                    world.setBlock(mutablePos, Blocks.AIR.defaultBlockState(), 2)
                }
            }
        }

        // 3. Générer un bloc de bedrock sous les pieds par sécurité
        world.setBlock(BlockPos(startX, startY, startZ), Blocks.BEDROCK.defaultBlockState(), 2)

        var npcX = startX.toDouble() + 0.5
        var npcY = startY.toDouble() + 1.0
        var npcZ = startZ.toDouble() + 2.5

        // 4. Placer la structure NBT
        val (resolvedLoc, templateOpt) = resolveStructure(server, leaderId)
        if (templateOpt.isPresent) {
            val template = templateOpt.get()
            val settings = StructurePlaceSettings()
                .setMirror(Mirror.NONE)
                .setRotation(Rotation.NONE)
                .setIgnoreEntities(false)

            // Centrer la structure sur (startX, startZ)
            val pos = BlockPos(
                startX - template.size.x / 2,
                startY,
                startZ - template.size.z / 2
            )
            template.placeInWorld(world, pos, pos, settings, world.random, 2)

            if (resolvedLoc.path == "gymleaderplaceholder") {
                // Pour le placeholder, le Gym Leader doit spawn en 10.5, 3.0, 11.5 par rapport à l'origine du build
                val originX = startX - template.size.x / 2.0
                val originY = startY.toDouble()
                val originZ = startZ - template.size.z / 2.0
                
                npcX = originX + 10.5
                npcY = originY + 3.0
                npcZ = originZ + 11.5

                // Placer un portail de retour 6 blocs derrière le Gym Leader (à Z = 5.5, donc bloc en Z = 5) sans limite de temps
                val portalPos = BlockPos((originX + 10).toInt(), (originY + 3).toInt(), (originZ + 5).toInt())
                val portalState = com.howlite.blocks.GymBlocks.GYM_LEADER_TELEPORTER.get().defaultBlockState()
                    .setValue(com.howlite.blocks.GymLeaderTeleporterBlock.PORTAL_OPEN, true)
                    .setValue(com.howlite.blocks.GymLeaderTeleporterBlock.FACING, net.minecraft.core.Direction.SOUTH)
                world.setBlock(portalPos, portalState, 3)

                val be = world.getBlockEntity(portalPos) as? com.howlite.blocks.GymLeaderTeleporterBlockEntity
                if (be != null) {
                    be.portalTicks = 99999999
                    be.setChanged()
                }
            }
        } else {
            // Fallback si la structure NBT n'est pas présente
            player.sendSystemMessage(
                Component.translatable("cobblemongymodyssey.gym_generator.dev_warning", leaderId)
            )
            // Générer le sol en bedrock de 15x15 à la hauteur startY
            for (dx in -7..7) {
                for (dz in -7..7) {
                    world.setBlock(BlockPos(startX + dx, startY, startZ + dz), Blocks.BEDROCK.defaultBlockState(), 2)
                }
            }

            // Murs en verre sur les 4 côtés (hauteur de 3 blocs, de Y = startY + 1 à Y = startY + 3)
            for (dy in 1..3) {
                for (dx in -7..7) {
                    world.setBlock(BlockPos(startX + dx, startY + dy, startZ - 7), Blocks.GLASS.defaultBlockState(), 2)
                    world.setBlock(BlockPos(startX + dx, startY + dy, startZ + 7), Blocks.GLASS.defaultBlockState(), 2)
                }
                for (dz in -6..6) {
                    world.setBlock(BlockPos(startX - 7, startY + dy, startZ + dz), Blocks.GLASS.defaultBlockState(), 2)
                    world.setBlock(BlockPos(startX + 7, startY + dy, startZ + dz), Blocks.GLASS.defaultBlockState(), 2)
                }
            }

            // Générer et activer le portail de retour à (startX, startY + 1, startZ - 4) faisant face au Sud
            val portalPos = BlockPos(startX, startY + 1, startZ - 4)
            val portalState = com.howlite.blocks.GymBlocks.GYM_LEADER_TELEPORTER.get().defaultBlockState()
                .setValue(com.howlite.blocks.GymLeaderTeleporterBlock.PORTAL_OPEN, true)
                .setValue(com.howlite.blocks.GymLeaderTeleporterBlock.FACING, net.minecraft.core.Direction.SOUTH)
            world.setBlock(portalPos, portalState, 3)

            val be = world.getBlockEntity(portalPos) as? com.howlite.blocks.GymLeaderTeleporterBlockEntity
            if (be != null) {
                be.portalTicks = 999999
                be.setChanged()
            }

            // Placer le spawn du leader plus loin pour le fallback
            npcZ = startZ.toDouble() + 4.5
        }

        // 5. Faire spawner le Gym Leader NPC à l'aide de la commande Cobblemon
        val shouldSpawnNpc = !templateOpt.isPresent || resolvedLoc.path == "gymleaderplaceholder"
        if (shouldSpawnNpc) {
            val commandSource = server.createCommandSourceStack()
                .withLevel(world)
                .withPosition(Vec3(npcX, npcY, npcZ))
                .withPermission(4)
                .withSuppressedOutput()

            server.commands.performPrefixedCommand(
                commandSource,
                "npcspawn cobblemongymodyssey:$leaderId"
            )
        }
    }

    /**
     * Résout la structure à utiliser pour un Champion donné.
     * Retourne une paire contenant la ResourceLocation résolue et le StructureTemplate optionnel.
     */
    private fun resolveStructure(server: net.minecraft.server.MinecraftServer, leaderId: String): Pair<ResourceLocation, java.util.Optional<net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate>> {
        val manager = server.structureManager
        
        // 1. Essayer `${leaderId}gymleaderarea` (ex: brockgymleaderarea)
        val customLoc = ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "${leaderId}gymleaderarea")
        val customTemplate = manager.get(customLoc)
        if (customTemplate.isPresent) {
            return Pair(customLoc, customTemplate)
        }

        // 2. Essayer `gym_$leaderId` (ex: gym_brock)
        val legacyLoc = ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "gym_$leaderId")
        val legacyTemplate = manager.get(legacyLoc)
        if (legacyTemplate.isPresent) {
            return Pair(legacyLoc, legacyTemplate)
        }

        // 3. Fallback sur `gymleaderplaceholder`
        val placeholderLoc = ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "gymleaderplaceholder")
        val placeholderTemplate = manager.get(placeholderLoc)
        return Pair(placeholderLoc, placeholderTemplate)
    }

    private fun formatName(name: String): String =
        name.lowercase().replaceFirstChar { it.uppercaseChar() }
}
