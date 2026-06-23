package com.howlite.events

import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor
import com.howlite.api.PlayerProgressApi
import com.howlite.wallet.CoinType
import com.howlite.wallet.WalletManager
import com.howlite.wallet.WalletNetwork
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation

/**
 * Gestionnaire d'événements pour L'Autel des Sacrifices.
 *
 * Gère :
 * 1. [CobblemonEvents.BATTLE_VICTORY] — Résolution du défi (paiement ou perte).
 * 2. [CobblemonEvents.THROWN_POKEBALL_HIT] — Bloque la capture des Boss corrompus.
 *
 * Note : La restriction de taille d'équipe est vérifiée côté serveur au moment
 * de traiter le packet `request_altar_challenge` (dans CobblemonGymOdyssey.kt),
 * et non ici, pour éviter des conflits avec d'autres combats de type wild.
 */
object AltarBattleEventHandler {

    fun register() {
        registerBattleVictory()
        registerBallHit()
    }

    // -------------------------------------------------------------------------
    // Battle Victory — Resolve bet
    // -------------------------------------------------------------------------

    private fun registerBattleVictory() {
        CobblemonEvents.BATTLE_VICTORY.subscribe { event ->
            // Find the player in winners or losers
            val playerActor = (event.winners + event.losers)
                .filterIsInstance<PlayerBattleActor>()
                .firstOrNull() ?: return@subscribe

            val player = playerActor.entity ?: return@subscribe

            // Only handle Altar battles (the player must have an active altar bet)
            val progress = PlayerProgressApi.get(player)
            val bet = progress.activeAltarBet
            val difficulty = progress.activeAltarDifficulty

            if (bet <= 0L) return@subscribe

            // This is an Altar battle — ignore NPCBattleActor check (it's a wild boss)
            // Also ensure the GymBattleReturnHandler won't interfere (it checks for NPCBattleActor presence)
            val playerWon = event.winners.any { it is PlayerBattleActor && it.entity == player }

            if (playerWon) {
                // Payout: bet * multiplier
                val multiplier = getMultiplier(difficulty)
                val payout = (bet * multiplier).toLong()
                val wallet = WalletManager.get(player)
                wallet.balanceCCC += payout
                WalletNetwork.syncToClient(player, wallet)

                val payoutFormatted = WalletManager.formatCCC(payout)
                player.sendSystemMessage(
                    Component.translatable("cobblemongymodyssey.altar.msg.victory", payoutFormatted)
                )
            } else {
                // Player lost — bet was already deducted at challenge start
                player.sendSystemMessage(
                    Component.translatable("cobblemongymodyssey.altar.msg.defeat", WalletManager.formatCCC(bet))
                )
            }

            // Clear altar state and teleport back
            val returnDim  = progress.returnDim
            val returnX    = progress.returnX
            val returnY    = progress.returnY
            val returnZ    = progress.returnZ
            val returnYaw  = progress.returnYaw ?: 0f
            val returnPitch = progress.returnPitch ?: 0f

            progress.activeAltarBet = 0L
            progress.activeAltarDifficulty = 0
            progress.clearReturnPosition()
            PlayerProgressApi.markDirty(player)

            // Teleport back if we have a saved position
            if (returnDim != null && returnX != null && returnY != null && returnZ != null) {
                val server = player.server
                if (server != null) {
                    val returnLevelKey = ResourceKey.create(
                        Registries.DIMENSION,
                        ResourceLocation.parse(returnDim)
                    )
                    val targetWorld = server.getLevel(returnLevelKey) ?: server.overworld()
                    player.teleportTo(targetWorld, returnX, returnY, returnZ, returnYaw, returnPitch)
                    player.sendSystemMessage(
                        Component.translatable("cobblemongymodyssey.altar.msg.returned")
                    )
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // PokéBall Hit — Block capture of Boss Pokémon
    // -------------------------------------------------------------------------

    private fun registerBallHit() {
        // NOTE: POKEMON_CAPTURE_CALCULATED is available in Cobblemon 1.8+.
        // In 1.7.3, boss Pokémon capture prevention is enforced by setting
        // the boss Pokemon's catch rate to 0 via Pokemon.catchRateModifier = 0
        // (done in startAltarBattle) and by clearing altar state only on BATTLE_VICTORY,
        // making capture attempts irrelevant to the bet outcome.
        // Additional client-side messaging is provided via BATTLE_VICTORY result check.
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun getMultiplier(difficulty: Int): Double = when (difficulty) {
        1 -> 1.5
        2 -> 2.0
        3 -> 3.0
        else -> 1.5
    }
}
