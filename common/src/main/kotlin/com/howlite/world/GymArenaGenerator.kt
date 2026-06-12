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

        // 1. Récupérer ou allouer un index d'arène unique pour le joueur
        val saveData = GymDimensionSavedData.get(server)
        if (progress.arenaIndex == null) {
            progress.arenaIndex = saveData.nextArenaIndex++
            saveData.setDirty()
            PlayerProgressApi.markDirty(player)
        }
        val index = progress.arenaIndex!!

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
            "boulder_badge" -> "brock"
            "cascade_badge" -> "misty"
            "thunder_badge" -> "lt_surge"
            "rainbow_badge" -> "erika"
            "soul_badge" -> "koga"
            "marsh_badge" -> "sabrina"
            "volcano_badge" -> "blaine"
            "earth_badge" -> "giovanni"
            else -> "brock"
        }

        player.sendSystemMessage(
            Component.translatable("cobblemongymodyssey.gym_generator.teleporting", formatName(leaderId))
        )

        // 5. Générer d'abord l'arène (pose les blocs et fait spawner le Champion)
        generate(gymWorld, startX, startY, startZ, player, leaderId)

        // 6. Déterminer les coordonnées de spawn pour le joueur
        val structureLoc = ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "gym_$leaderId")
        val templateOpt = server.structureManager.get(structureLoc)

        val spawnX: Double
        val spawnY: Double
        val spawnZ: Double
        if (templateOpt.isPresent) {
            spawnX = startX + 0.5
            spawnY = startY.toDouble() + 1.0
            spawnZ = startZ + 0.5
        } else {
            // Dans le cas de l'arène de test (fallback), on téléporte devant le portail de retour (Z = -1.0) face au NPC (Sud, yaw = 0f)
            spawnX = startX + 0.5
            spawnY = startY.toDouble() + 1.0
            spawnZ = startZ - 1.0
        }

        // 7. Téléporter le joueur en toute sécurité sur le sol déjà généré
        player.teleportTo(gymWorld, spawnX, spawnY, spawnZ, 0f, 0f)
    }

    /**
     * Génère la structure et nettoie la zone.
     */
    fun generate(world: ServerLevel, startX: Int, startY: Int, startZ: Int, player: ServerPlayer, leaderId: String) {
        val server = world.server
        val manager = server.structureManager
        val structureLoc = ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "gym_$leaderId")
        val templateOpt = manager.get(structureLoc)

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

        var npcY = startY.toDouble() + 1.0
        var npcZ = startZ.toDouble() + 2.5

        // 4. Placer la structure NBT
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
        val commandSource = server.createCommandSourceStack()
            .withPosition(Vec3(startX.toDouble() + 0.5, npcY, npcZ))
            .withPermission(4)
            .withSuppressedOutput()

        server.commands.performPrefixedCommand(
            commandSource,
            "cobblemon npc spawn cobblemongymodyssey:$leaderId"
        )
    }

    private fun formatName(name: String): String =
        name.lowercase().replaceFirstChar { it.uppercaseChar() }
}
