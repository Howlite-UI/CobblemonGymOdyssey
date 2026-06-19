package com.howlite.events

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.CobblemonEntities
import com.cobblemon.mod.common.api.pokemon.PokemonProperties
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.pokemon.Pokemon
import net.minecraft.server.level.ServerPlayer
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.core.registries.Registries
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player

/**
 * Utility to start an Altar of Sacrifices boss challenge.
 * Teleports the player to a unique platform in the gym_dimension and spawns the stationary, invulnerable boss.
 */
object AltarBossSpawner {

    // -------------------------------------------------------------------------
    // Boss pools by type
    // -------------------------------------------------------------------------

    private val BOSS_POOL: Map<String, List<String>> = mapOf(
        "fighting" to listOf("lucario", "machamp", "conkeldurr", "gallade", "koraidon"),
        "water"    to listOf("kyogre", "gyarados", "blastoise", "swampert", "greninja"),
        "ground"   to listOf("groudon", "garchomp", "mamoswine", "excadrill", "rhyperior"),
        "rock"     to listOf("tyranitar", "aerodactyl", "gigalith", "terrakion"),
        "electric" to listOf("zapdos", "raikou", "electivire", "luxray", "regieleki"),
        "grass"    to listOf("venusaur", "sceptile", "rillaboom", "kartana", "celebi"),
        "fire"     to listOf("charizard", "arcanine", "volcarona", "blaziken", "reshiram"),
        "ice"      to listOf("mamoswine", "weavile", "glaceon", "kyurem", "baxcalibur"),
        "poison"   to listOf("gengar", "crobat", "nidoking", "nidoqueen", "eternatus"),
        "flying"   to listOf("lugia", "rayquaza", "corviknight", "talonflame", "staraptor"),
        "psychic"  to listOf("mewtwo", "alakazam", "metagross", "gardevoir", "latios"),
        "bug"      to listOf("scizor", "heracross", "volcarona", "buzzwole", "genesect"),
        "ghost"    to listOf("gengar", "giratina", "dragapult", "chandelure", "aegislash"),
        "dark"     to listOf("darkrai", "yveltal", "hydreigon", "tyranitar", "weavile"),
        "dragon"   to listOf("rayquaza", "garchomp", "dragonite", "salamence", "haxorus"),
        "steel"    to listOf("dialga", "metagross", "scizor", "aegislash", "melmetal"),
        "fairy"    to listOf("zacian", "xerneas", "sylveon", "gardevoir", "togekiss"),
        "normal"   to listOf("regigigas", "snorlax", "slaking", "staraptor"),
        "default"  to listOf("mewtwo", "giratina", "rayquaza", "dialga", "zacian")
    )

    // Type effectiveness counters
    private val TYPE_COUNTERS: Map<String, String> = mapOf(
        "normal"   to "fighting",
        "fire"     to "water",
        "water"    to "electric",
        "grass"    to "fire",
        "electric" to "ground",
        "ice"      to "fire",
        "fighting" to "psychic",
        "poison"   to "ground",
        "ground"   to "water",
        "flying"   to "electric",
        "psychic"  to "dark",
        "bug"      to "fire",
        "rock"     to "water",
        "ghost"    to "dark",
        "dragon"   to "fairy",
        "dark"     to "fighting",
        "steel"    to "fire",
        "fairy"    to "steel"
    )

    private fun isPhysicalAttacker(species: String): Boolean {
        val physicals = setOf(
            "lucario", "machamp", "conkeldurr", "gallade", "koraidon",
            "gyarados", "swampert",
            "garchomp", "mamoswine", "excadrill", "rhyperior", "groudon",
            "tyranitar", "aerodactyl", "terrakion",
            "sceptile", "rillaboom", "kartana",
            "arcanine", "blaziken",
            "weavile", "baxcalibur",
            "crobat", "nidoking", "nidoqueen",
            "scizor", "heracross", "buzzwole",
            "aegislash", "dragapult",
            "dragonite", "salamence", "haxorus",
            "dialga", "zacian", "melmetal",
            "snorlax", "slaking"
        )
        return physicals.contains(species.lowercase())
    }

    /**
     * Starts the Altar challenge by teleporting the player to the gym_dimension on a custom platform
     * and spawning a wild Level 100/110/120 uncatchable stationary Boss with optimized stats.
     */
    fun startAltarBattle(player: ServerPlayer, regionName: String, difficulty: Int) {
        try {
            val server = player.server

            // 1. Determine lead Pokémon type
            val party = Cobblemon.storage.getParty(player)
            val leadPokemon = party.firstOrNull() ?: run {
                player.sendSystemMessage(
                    Component.translatable("cobblemongymodyssey.altar.msg.no_lead")
                )
                return
            }
            val leadType = leadPokemon.types.firstOrNull()?.name?.lowercase() ?: "normal"

            // 2. Pick counter type and random boss species
            val counterType = TYPE_COUNTERS[leadType] ?: "default"
            val pool = BOSS_POOL[counterType] ?: BOSS_POOL["default"]!!
            val bossSpecies = pool.random()

            // 3. Boss level based on difficulty/region
            val bossLevel = when (difficulty) {
                1 -> 100
                2 -> 110
                3 -> 120
                else -> 100
            }

            // 4. Build the boss Pokémon with competitive stats (EVs, nature, IVs)
            val nature = if (isPhysicalAttacker(bossSpecies)) "adamant" else "modest"
            val bossProps = PokemonProperties.parse(
                "$bossSpecies level=$bossLevel nature=$nature"
            )
            val bossPokemon: Pokemon = bossProps.create()

            // Set max IVs (31)
            for (stat in com.cobblemon.mod.common.api.pokemon.stats.Stats.entries) {
                bossPokemon.setIV(stat, 31)
            }

            // Set optimized EVs (252 Atk/SpAtk, 252 Speed, 6 HP)
            if (isPhysicalAttacker(bossSpecies)) {
                bossPokemon.setEV(com.cobblemon.mod.common.api.pokemon.stats.Stats.ATTACK, 252)
                bossPokemon.setEV(com.cobblemon.mod.common.api.pokemon.stats.Stats.SPEED, 252)
                bossPokemon.setEV(com.cobblemon.mod.common.api.pokemon.stats.Stats.HP, 6)
            } else {
                bossPokemon.setEV(com.cobblemon.mod.common.api.pokemon.stats.Stats.SPECIAL_ATTACK, 252)
                bossPokemon.setEV(com.cobblemon.mod.common.api.pokemon.stats.Stats.SPEED, 252)
                bossPokemon.setEV(com.cobblemon.mod.common.api.pokemon.stats.Stats.HP, 6)
            }

            val speciesDisplay = bossSpecies.replaceFirstChar { it.uppercaseChar() }
            bossPokemon.nickname = Component.literal("§c[Boss] $speciesDisplay")
            com.cobblemon.mod.common.pokemon.properties.UncatchableProperty.uncatchable().apply(bossPokemon)

            // 5. Get gym_dimension level
            val gymLevelKey = ResourceKey.create(
                Registries.DIMENSION,
                ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "gym_dimension")
            )
            val gymWorld = server.getLevel(gymLevelKey) ?: run {
                player.sendSystemMessage(
                    Component.translatable("cobblemongymodyssey.gym_generator.dimension_not_found")
                )
                return
            }

            // 6. Allocate unique arena index for Altar challenge
            val saveData = com.howlite.world.GymDimensionSavedData.get(server)
            val index = saveData.nextArenaIndex++
            saveData.setDirty()

            val startX = index * 500
            val startY = 64
            val startZ = 0

            // 7. Clean up prior entities in the 50x30x50 box
            val box = AABB(
                startX - 25.0, startY - 10.0, startZ - 25.0,
                startX + 25.0, startY + 25.0, startZ + 25.0
            )
            val entities = gymWorld.getEntitiesOfClass(Entity::class.java, box) { it !is Player }
            entities.forEach { it.discard() }

            // 8. Clear blocks in the area
            val mutablePos = BlockPos.MutableBlockPos()
            for (dx in -25..25) {
                for (dy in -5..25) {
                    for (dz in -25..25) {
                        mutablePos.set(startX + dx, startY + dy, startZ + dz)
                        gymWorld.setBlock(mutablePos, Blocks.AIR.defaultBlockState(), 2)
                    }
                }
            }

            // 9. Generate Altar of Sacrifices platform (Obsidian floor with Crying Obsidian core and Red Concrete trim)
            for (dx in -7..7) {
                for (dz in -7..7) {
                    val pos = BlockPos(startX + dx, startY, startZ + dz)
                    val blockState = if (kotlin.math.abs(dx) == 7 || kotlin.math.abs(dz) == 7) {
                        Blocks.RED_CONCRETE.defaultBlockState()
                    } else if (dx == 0 && dz == 0) {
                        Blocks.CRYING_OBSIDIAN.defaultBlockState()
                    } else {
                        Blocks.OBSIDIAN.defaultBlockState()
                    }
                    gymWorld.setBlock(pos, blockState, 2)
                }
            }

            // Red stained glass walls (3 blocks high)
            for (dy in 1..3) {
                for (dx in -7..7) {
                    gymWorld.setBlock(BlockPos(startX + dx, startY + dy, startZ - 7), Blocks.RED_STAINED_GLASS.defaultBlockState(), 2)
                    gymWorld.setBlock(BlockPos(startX + dx, startY + dy, startZ + 7), Blocks.RED_STAINED_GLASS.defaultBlockState(), 2)
                }
                for (dz in -6..6) {
                    gymWorld.setBlock(BlockPos(startX - 7, startY + dy, startZ + dz), Blocks.RED_STAINED_GLASS.defaultBlockState(), 2)
                    gymWorld.setBlock(BlockPos(startX + 7, startY + dy, startZ + dz), Blocks.RED_STAINED_GLASS.defaultBlockState(), 2)
                }
            }

            // 10. Teleport the player
            val spawnX = startX + 0.5
            val spawnY = startY.toDouble() + 1.0
            val spawnZ = startZ.toDouble() - 4.0
            player.teleportTo(gymWorld, spawnX, spawnY, spawnZ, 0f, 0f)

            // 11. Spawn the boss PokemonEntity with No AI and Invulnerability
            val bossX = startX + 0.5
            val bossY = startY.toDouble() + 1.0
            val bossZ = startZ.toDouble() + 3.0

            val bossEntity = PokemonEntity(gymWorld, bossPokemon, CobblemonEntities.POKEMON)
            bossEntity.isInvulnerable = true
            bossEntity.setNoAi(true)
            bossEntity.setPos(bossX, bossY, bossZ)
            bossEntity.pokemon = bossPokemon
            gymWorld.addFreshEntity(bossEntity)

            player.sendSystemMessage(
                Component.translatable("cobblemongymodyssey.altar.msg.boss_appeared", speciesDisplay)
            )

        } catch (e: Exception) {
            e.printStackTrace()
            // Refund the player if spawning/teleporting fails
            player.sendSystemMessage(
                Component.translatable("cobblemongymodyssey.altar.msg.refunded")
            )
            val progress = com.howlite.api.PlayerProgressApi.get(player)
            val bet = progress.activeAltarBet
            if (bet > 0L) {
                val wallet = com.howlite.wallet.WalletManager.get(player)
                wallet.balanceCCC += bet
                com.howlite.wallet.WalletNetwork.syncToClient(player, wallet)
                progress.activeAltarBet = 0L
                progress.activeAltarDifficulty = 0
                progress.clearReturnPosition()
                com.howlite.api.PlayerProgressApi.markDirty(player)
            }
        }
    }
}
