package com.howlite.events

import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor
import com.howlite.api.PlayerProgressApi
import com.howlite.data.GymBadge
import com.howlite.data.GymRegion
import com.howlite.data.PvpFightRecord
import com.howlite.wallet.CoinType
import com.howlite.wallet.WalletManager
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import java.time.LocalDate
import java.time.ZoneOffset

object PvpBattleEventHandler {

    private const val BASE_REWARD = 50L // 50 Silver Coins (equivalent to 5000 Copper)

    fun register() {
        CobblemonEvents.BATTLE_VICTORY.subscribe { event ->
            // En PvP, le gagnant et le perdant sont des joueurs humains.
            val winningPlayers = event.winners
                .filterIsInstance<PlayerBattleActor>()
                .mapNotNull { it.entity }

            val losingPlayers = event.losers
                .filterIsInstance<PlayerBattleActor>()
                .mapNotNull { it.entity }

            // Si on n'a pas au moins un joueur gagnant et un perdant, ce n'est pas du PvP
            if (winningPlayers.isEmpty() || losingPlayers.isEmpty()) return@subscribe

            val todayStr = LocalDate.now(ZoneOffset.UTC).toString()
            val yesterdayStr = LocalDate.now(ZoneOffset.UTC).minusDays(1).toString()

            for (winner in winningPlayers) {
                val progress = PlayerProgressApi.get(winner)

                // Réinitialisation quotidienne si le jour a changé
                if (progress.lastPvpResetDate != todayStr) {
                    progress.lastPvpResetDate = todayStr
                    progress.pvpRewardsClaimedToday = 0
                    PlayerProgressApi.markDirty(winner)
                }

                // Calcul dynamique de la limite quotidienne de combats récompensés
                // Base de 3, +1 pour chaque région terminée (tous les badges obtenus)
                val completedRegions = GymRegion.entries.count { region ->
                    val regionBadges = GymBadge.entries.filter { it.region == region }
                    regionBadges.isNotEmpty() && regionBadges.all { progress.hasBadge(it) }
                }
                val maxDailyRewards = 3 + completedRegions

                for (loser in losingPlayers) {
                    // Limite globale quotidienne dynamique
                    if (progress.pvpRewardsClaimedToday >= maxDailyRewards) {
                        winner.sendSystemMessage(
                            Component.translatable("cobblemongymodyssey.pvp.daily_limit_reached", maxDailyRewards)
                        )
                        continue
                    }

                    val opponentUuid = loser.uuid.toString()
                    val record = progress.pvpFights[opponentUuid]

                    // Limite : une fois par adversaire par jour
                    if (record != null && record.lastFightDate == todayStr) {
                        winner.sendSystemMessage(
                            Component.translatable("cobblemongymodyssey.pvp.already_fought_today", loser.scoreboardName)
                        )
                        continue
                    }

                    // Calcul de la série de jours consécutifs
                    val consecutive = if (record != null && record.lastFightDate == yesterdayStr) {
                        record.consecutiveDays + 1
                    } else {
                        1
                    }

                    // Calcul du multiplicateur de récompense
                    val multiplier = when (consecutive) {
                        1 -> 1.0
                        2 -> 0.5
                        3 -> 0.25
                        else -> 0.0
                    }

                    // Enregistrer le combat pour le gagnant
                    val oldRecord = progress.pvpFights[opponentUuid]
                    val newRecord = PvpFightRecord(
                        lastFightDate = todayStr,
                        consecutiveDays = consecutive,
                        wins = (oldRecord?.wins ?: 0) + 1,
                        losses = oldRecord?.losses ?: 0
                    )
                    progress.recordPvpFight(opponentUuid, newRecord)
                    progress.pvpWins++
                    println("[GymOdyssey] Server: Winner ${winner.scoreboardName} (${winner.uuid}) progress updated. Wins: ${progress.pvpWins}, Losses: ${progress.pvpLosses}, Opponent fights: ${progress.pvpFights.size}")

                    // Enregistrer le combat pour le perdant
                    val loserProgress = PlayerProgressApi.get(loser)
                    val oldLoserRecord = loserProgress.pvpFights[winner.uuid.toString()]
                    val newLoserRecord = PvpFightRecord(
                        lastFightDate = oldLoserRecord?.lastFightDate ?: "",
                        consecutiveDays = oldLoserRecord?.consecutiveDays ?: 0,
                        wins = oldLoserRecord?.wins ?: 0,
                        losses = (oldLoserRecord?.losses ?: 0) + 1
                    )
                    loserProgress.recordPvpFight(winner.uuid.toString(), newLoserRecord)
                    loserProgress.pvpLosses++
                    println("[GymOdyssey] Server: Loser ${loser.scoreboardName} (${loser.uuid}) progress updated. Wins: ${loserProgress.pvpWins}, Losses: ${loserProgress.pvpLosses}, Opponent fights: ${loserProgress.pvpFights.size}")
                    PlayerProgressApi.markDirty(loser)

                    // Attribuer la récompense si le multiplicateur est positif
                    // Base: 50 Silver (5000 Copper). Pour chaque badge possédés par le gagnant: +5 Silver (+500 Copper)
                    val totalBadges = progress.badges.size
                    val baseRewardAmount = BASE_REWARD + 5L * totalBadges
                    val rewardAmount = (baseRewardAmount * multiplier).toLong()
                    if (rewardAmount > 0) {
                        progress.pvpRewardsClaimedToday++
                        WalletManager.addAndSync(winner, CoinType.SILVER, rewardAmount)
                        winner.sendSystemMessage(
                            Component.translatable(
                                "cobblemongymodyssey.pvp.reward_received",
                                rewardAmount,
                                loser.scoreboardName,
                                progress.pvpRewardsClaimedToday,
                                maxDailyRewards
                            )
                        )
                    } else {
                        winner.sendSystemMessage(
                            Component.translatable("cobblemongymodyssey.pvp.no_reward_consecutive", loser.scoreboardName)
                        )
                    }

                    PlayerProgressApi.markDirty(winner)
                }
            }
        }
    }
}
