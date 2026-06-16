package com.howlite.events

import com.howlite.items.CobbleCoins
import com.howlite.wallet.CoinType
import com.howlite.wallet.WalletManager
import dev.architectury.event.EventResult
import dev.architectury.event.events.common.PlayerEvent
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource

/**
 * Intercepte le ramassage des 4 pièces Cobble et les redirige
 * directement dans le [WalletData] du joueur au lieu de l'inventaire.
 *
 * Si [com.howlite.wallet.WalletData.autoCollect] est désactivé,
 * les pièces tombent normalement dans l'inventaire.
 */
object CoinPickupHandler {

    fun register() {
        PlayerEvent.PICKUP_ITEM_PRE.register { player, itemEntity, stack ->
            // Seulement côté serveur
            if (player !is ServerPlayer) return@register EventResult.pass()

            val coinType = getCoinType(stack.item) ?: return@register EventResult.pass()

            val wallet = WalletManager.get(player)
            if (!wallet.autoCollect) return@register EventResult.pass()

            // Auto-collecte : ajouter au wallet et consommer l'item
            val amount = stack.count.toLong()
            WalletManager.addAndSync(player, coinType, amount)

            // Son de ramassage discret
            player.level().playSound(
                null,
                player.x, player.y, player.z,
                SoundEvents.ITEM_PICKUP,
                SoundSource.PLAYERS,
                0.2f,
                0.8f + player.level().random.nextFloat() * 0.4f
            )

            // Supprimer l'entité item du monde
            itemEntity.kill()

            // Annuler le pickup normal (item consommé par le wallet)
            EventResult.interruptFalse()
        }
    }

    private fun getCoinType(item: net.minecraft.world.item.Item): CoinType? {
        return when (item) {
            CobbleCoins.COBBLE_COPPER_COIN.get()   -> CoinType.COPPER
            CobbleCoins.COBBLE_SILVER_COIN.get()   -> CoinType.SILVER
            CobbleCoins.COBBLE_GOLD_COIN.get()     -> CoinType.GOLD
            CobbleCoins.COBBLE_PLATINUM_COIN.get() -> CoinType.PLATINUM
            else -> null
        }
    }
}
