package com.howlite.events

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.CobblemonEntities
import com.cobblemon.mod.common.api.pokemon.PokemonProperties
import com.cobblemon.mod.common.battles.BattleFormat
import com.cobblemon.mod.common.battles.BattleRegistry
import com.cobblemon.mod.common.battles.BattleSide
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor
import com.cobblemon.mod.common.battles.actor.PokemonBattleActor
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.battles.ai.StrongBattleAI
import net.minecraft.server.level.ServerPlayer
import net.minecraft.network.chat.Component

/**
 * Utilitaire pour démarrer un combat de boss de l'Autel des Sacrifices.
 *
 * ## Fonctionnement :
 * 1. Sélectionne un Pokémon boss de niveau 120 basé sur la région et la contre-type du lead du joueur.
 * 2. Configure le boss avec un nom personnalisé `§c[Boss] NomEspèce` pour le distinguer.
 * 3. Spawn l'entité boss dans le monde du joueur.
 * 4. Démarre un combat sauvage via [BattleRegistry.startBattle].
 *
 * Le joueur ne peut pas fuir (perte automatique si la fuite est tentée).
 * La capture est bloquée via [AltarBattleEventHandler] (catch rate = 0).
 */
object AltarBossSpawner {

    // -------------------------------------------------------------------------
    // Pools de boss par type
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

    // Type effectiveness: lead Pokémon type → counter type pool
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

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Sélectionne un boss de l'Autel et démarre un combat contre le joueur.
     *
     * @param player Le joueur qui défie l'Autel.
     * @param regionName Le nom de la région (UNOVA, ALOLA, PALDEA).
     * @param difficulty La difficulté sélectionnée (1=Easy, 2=Medium, 3=Hard).
     */
    fun startAltarBattle(player: ServerPlayer, regionName: String, difficulty: Int) {
        try {
            val level = player.serverLevel()

            // 1. Determine lead Pokémon type
            val party = Cobblemon.storage.getParty(player)
            val leadPokemon = party.firstOrNull() ?: run {
                player.sendSystemMessage(
                    Component.literal("§c[Autel] Aucun Pokémon en tête d'équipe !")
                )
                return
            }
            val leadType = leadPokemon.types.firstOrNull()?.name?.lowercase() ?: "normal"

            // 2. Pick a counter type and random boss species
            val counterType = TYPE_COUNTERS[leadType] ?: "default"
            val pool = BOSS_POOL[counterType] ?: BOSS_POOL["default"]!!
            val bossSpecies = pool.random()

            // 3. Build the boss Pokémon with max stats
            val bossProps = PokemonProperties.parse(
                "$bossSpecies level=120 nature=serious"
            )
            val bossPokemon: Pokemon = bossProps.create()

            // Set max IVs
            bossPokemon.setIV(com.cobblemon.mod.common.api.pokemon.stats.Stats.HP, 31)
            bossPokemon.setIV(com.cobblemon.mod.common.api.pokemon.stats.Stats.ATTACK, 31)
            bossPokemon.setIV(com.cobblemon.mod.common.api.pokemon.stats.Stats.DEFENCE, 31)
            bossPokemon.setIV(com.cobblemon.mod.common.api.pokemon.stats.Stats.SPECIAL_ATTACK, 31)
            bossPokemon.setIV(com.cobblemon.mod.common.api.pokemon.stats.Stats.SPECIAL_DEFENCE, 31)
            bossPokemon.setIV(com.cobblemon.mod.common.api.pokemon.stats.Stats.SPEED, 31)

            // Max EVs (distribute 85 to each stat, total 510)
            bossPokemon.setEV(com.cobblemon.mod.common.api.pokemon.stats.Stats.HP, 85)
            bossPokemon.setEV(com.cobblemon.mod.common.api.pokemon.stats.Stats.ATTACK, 85)
            bossPokemon.setEV(com.cobblemon.mod.common.api.pokemon.stats.Stats.DEFENCE, 85)
            bossPokemon.setEV(com.cobblemon.mod.common.api.pokemon.stats.Stats.SPECIAL_ATTACK, 85)
            bossPokemon.setEV(com.cobblemon.mod.common.api.pokemon.stats.Stats.SPECIAL_DEFENCE, 85)
            bossPokemon.setEV(com.cobblemon.mod.common.api.pokemon.stats.Stats.SPEED, 85)

            // Set custom display name with [Boss] tag to identify for capture blocking
            val speciesDisplay = bossSpecies.replaceFirstChar { it.uppercaseChar() }
            bossPokemon.nickname = Component.literal("§c[Boss] $speciesDisplay")
            com.cobblemon.mod.common.pokemon.properties.UncatchableProperty.uncatchable().apply(bossPokemon)

            // 4. Spawn the PokemonEntity next to the player
            val bossEntity = PokemonEntity(level, bossPokemon, CobblemonEntities.POKEMON)
            bossEntity.setPos(player.x + 2.0, player.y, player.z + 2.0)
            bossEntity.pokemon = bossPokemon
            level.addFreshEntity(bossEntity)

            // 5. Build BattleSide actors and start the battle
            val playerPartyBattle = Cobblemon.storage.getParty(player)
            val playerBattlePokemon = playerPartyBattle
                .filterNotNull()
                .take(when (difficulty) { 1 -> 3; 2 -> 2; 3 -> 1; else -> 3 })
                .map { BattlePokemon(it) }

            val playerActor = PlayerBattleActor(player.uuid, playerBattlePokemon)
            val bossActor   = PokemonBattleActor(bossEntity.uuid, BattlePokemon(bossPokemon), 50f, StrongBattleAI(100))

            val side1 = BattleSide(playerActor)
            val side2 = BattleSide(bossActor)

            BattleRegistry.startBattle(
                BattleFormat.GEN_9_SINGLES,
                side1,
                side2
            )

            player.sendSystemMessage(
                Component.literal(
                    "§c✦ L'Autel des Sacrifices §f— Un §e[Boss] $speciesDisplay §fapparaît ! Bonne chance..."
                )
            )

        } catch (e: Exception) {
            e.printStackTrace()
            // If battle fails to start, refund the player
            player.sendSystemMessage(
                Component.literal("§c[Autel] Erreur lors du démarrage du combat. Votre mise a été remboursée.")
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
