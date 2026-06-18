package com.howlite.events

import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor
import com.howlite.api.PlayerProgressApi
import com.howlite.data.PvpFightRecord
import com.howlite.wallet.CoinType
import com.howlite.wallet.WalletManager
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import java.time.LocalDate
import java.time.ZoneOffset

object PvpBattleEventHandler {

    private const val BASE_REWARD = 50L // 50 Silver Coins
    private const val MAX_DAILY_REWARDS = 3

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

                for (loser in losingPlayers) {
                    // Limite globale de 3 par jour
                    if (progress.pvpRewardsClaimedToday >= MAX_DAILY_REWARDS) {
                        winner.sendSystemMessage(
                            Component.translatable("cobblemongymodyssey.pvp.daily_limit_reached", MAX_DAILY_REWARDS)
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

                    // Enregistrer le combat
                    val newRecord = PvpFightRecord(
                        lastFightDate = todayStr,
                        consecutiveDays = consecutive
                    )
                    progress.recordPvpFight(opponentUuid, newRecord)

                    // Attribuer la récompense si le multiplicateur est positif
                    val rewardAmount = (BASE_REWARD * multiplier).toLong()
                    if (rewardAmount > 0) {
                        progress.pvpRewardsClaimedToday++
                        WalletManager.addAndSync(winner, CoinType.SILVER, rewardAmount)
                        winner.sendSystemMessage(
                            Component.translatable(
                                "cobblemongymodyssey.pvp.reward_received",
                                rewardAmount,
                                loser.scoreboardName,
                                progress.pvpRewardsClaimedToday,
                                MAX_DAILY_REWARDS
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
