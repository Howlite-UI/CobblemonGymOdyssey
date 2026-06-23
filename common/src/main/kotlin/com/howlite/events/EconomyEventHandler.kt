package com.howlite.events

import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.battles.model.actor.ActorType
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor
import com.howlite.api.PlayerProgressApi
import com.howlite.wallet.WalletManager
import com.howlite.wallet.WalletNetwork
import dev.architectury.event.events.common.PlayerEvent
import dev.architectury.event.events.common.LootEvent
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.storage.loot.LootPool
import net.minecraft.world.level.storage.loot.entries.LootItem
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator
import java.time.LocalDate
import java.time.ZoneOffset

object EconomyEventHandler {

    fun register() {
        registerLootTableInjection()
        registerWildDefeats()
        registerWildCaptures()
        registerPCReleases()
        registerRaidRewards()
    }

    private fun registerLootTableInjection() {
        LootEvent.MODIFY_LOOT_TABLE.register { key, context, builtin ->
            if (builtin && key.location().path.startsWith("chests/")) {
                val poolBuilder = LootPool.lootPool()
                    .setRolls(UniformGenerator.between(1f, 3f))
                    .add(LootItem.lootTableItem(com.howlite.items.CobbleCoins.COBBLE_COPPER_COIN.get()).setWeight(70))
                    .add(LootItem.lootTableItem(com.howlite.items.CobbleCoins.COBBLE_SILVER_COIN.get()).setWeight(25))
                    .add(LootItem.lootTableItem(com.howlite.items.CobbleCoins.COBBLE_GOLD_COIN.get()).setWeight(5))

                context.addPool(poolBuilder)
            }
        }
    }

    private fun registerWildDefeats() {
        CobblemonEvents.BATTLE_VICTORY.subscribe { event ->
            if (event.wasWildCapture) return@subscribe // Captured rewards handled separately

            val winningPlayers = event.winners
                .filterIsInstance<PlayerBattleActor>()
                .mapNotNull { it.entity }

            if (winningPlayers.isEmpty()) return@subscribe

            val wildLosers = event.losers
                .filter { it.type == ActorType.WILD }

            if (wildLosers.isEmpty()) return@subscribe

            // Calculate total reward for all defeated wild Pokémon
            var totalReward = 0L
            val defeatedNames = mutableListOf<String>()

            for (actor in wildLosers) {
                for (battlePokemon in actor.pokemonList) {
                    val pokemon = battlePokemon.originalPokemon
                    val base = 10L
                    val lvlBonus = pokemon.level * 2L
                    val shinyBonus = if (pokemon.shiny) 100L else 0L // 1 Silver Coin (100 CCC)
                    val legBonus = if (pokemon.isLegendary() || pokemon.isMythical()) 10_000L else 0L // 1 Gold Coin (10000 CCC)
                    
                    totalReward += base + lvlBonus + shinyBonus + legBonus
                    defeatedNames.add(pokemon.getDisplayName().string)
                }
            }

            if (totalReward <= 0L) return@subscribe

            val coinStr = WalletManager.formatCCC(totalReward)
            for (player in winningPlayers) {
                val wallet = WalletManager.get(player)
                wallet.balanceCCC += totalReward
                WalletNetwork.syncToClient(player, wallet)

                val namesStr = defeatedNames.joinToString(", ")
                player.sendSystemMessage(
                    Component.translatable(
                        "cobblemongymodyssey.economy.wild_defeat",
                        namesStr,
                        coinStr
                    )
                )
            }
        }
    }

    private fun registerWildCaptures() {
        CobblemonEvents.POKEMON_CAPTURED.subscribe { event ->
            val player = event.player
            val pokemon = event.pokemon
            val base = 25L
            val lvlBonus = pokemon.level * 5L
            val shinyBonus = if (pokemon.shiny) 5_000L else 0L // 5 Silver Coins (5000 CCC)
            val legBonus = if (pokemon.isLegendary() || pokemon.isMythical()) 20_000L else 0L // 2 Gold Coins (20000 CCC)
            val total = base + lvlBonus + shinyBonus + legBonus

            val wallet = WalletManager.get(player)
            wallet.balanceCCC += total
            WalletNetwork.syncToClient(player, wallet)

            val coinStr = WalletManager.formatCCC(total)
            player.sendSystemMessage(
                Component.translatable(
                    "cobblemongymodyssey.economy.captured_pokemon",
                    pokemon.getDisplayName(),
                    coinStr
                )
            )
        }
    }

    private fun registerPCReleases() {
        CobblemonEvents.POKEMON_RELEASED_EVENT_POST.subscribe { event ->
            val player = event.player
            val pokemon = event.pokemon
            val base = 10L // 10 Copper
            val lvlBonus = pokemon.level * 1L // 1 Copper per level
            val ivPoints = pokemon.ivs.total()
            val ivBonus = ivPoints * 1L // 1 Copper per IV point
            val shinyBonus = if (pokemon.shiny) 10_000L else 0L // 1 Gold Coin (10000 CCC)
            val legBonus = if (pokemon.isLegendary() || pokemon.isMythical()) 50_000L else 0L // 5 Gold Coins (50000 CCC)
            val total = base + lvlBonus + ivBonus + shinyBonus + legBonus

            val wallet = WalletManager.get(player)
            wallet.balanceCCC += total
            WalletNetwork.syncToClient(player, wallet)

            val coinStr = WalletManager.formatCCC(total)
            player.sendSystemMessage(
                Component.translatable(
                    "cobblemongymodyssey.economy.released_pokemon",
                    pokemon.getDisplayName(),
                    pokemon.level,
                    coinStr
                )
            )
        }
    }

    private fun registerRaidRewards() {
        try {
            com.necro.raid.dens.common.events.RaidEvents.RAID_END.subscribe { event ->
                if (event.isWin) {
                    val player = event.player() ?: return@subscribe
                    val tier = event.raidBoss()?.tier ?: return@subscribe
                    val rewardCCC = when (tier) {
                        com.necro.raid.dens.common.data.raid.RaidTier.TIER_ONE -> 500L       // 5 Silver
                        com.necro.raid.dens.common.data.raid.RaidTier.TIER_TWO -> 1_000L     // 10 Silver
                        com.necro.raid.dens.common.data.raid.RaidTier.TIER_THREE -> 5_000L   // 50 Silver (0.5 Gold)
                        com.necro.raid.dens.common.data.raid.RaidTier.TIER_FOUR -> 10_000L   // 1 Gold
                        com.necro.raid.dens.common.data.raid.RaidTier.TIER_FIVE -> 50_000L   // 5 Gold
                        com.necro.raid.dens.common.data.raid.RaidTier.TIER_SIX -> 100_000L   // 10 Gold
                        com.necro.raid.dens.common.data.raid.RaidTier.TIER_SEVEN -> 200_000L // 20 Gold
                    }

                    val wallet = WalletManager.get(player)
                    wallet.balanceCCC += rewardCCC
                    WalletNetwork.syncToClient(player, wallet)

                    val coinStr = WalletManager.formatCCC(rewardCCC)
                    player.sendSystemMessage(
                        Component.translatable(
                            "cobblemongymodyssey.economy.raid_victory",
                            coinStr
                        )
                    )
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}
