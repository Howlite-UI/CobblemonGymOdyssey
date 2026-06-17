package com.howlite.items

import com.howlite.CobblemonGymOdyssey
import com.howlite.wallet.CoinType
import com.howlite.wallet.WalletManager
import com.howlite.wallet.WalletNetwork
import dev.architectury.registry.CreativeTabRegistry
import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

/**
 * Enregistrement des 4 pièces de monnaie Cobble et de leur onglet créatif.
 */
object CobbleCoins {

    private val ITEMS: DeferredRegister<Item> =
        DeferredRegister.create(CobblemonGymOdyssey.MOD_ID, Registries.ITEM)

    private val TABS: DeferredRegister<CreativeModeTab> =
        DeferredRegister.create(CobblemonGymOdyssey.MOD_ID, Registries.CREATIVE_MODE_TAB)

    val COBBLE_COINS_TAB: RegistrySupplier<CreativeModeTab> = TABS.register("cobble_coins") {
        CreativeTabRegistry.create(
            Component.translatable("itemGroup.cobblemongymodyssey.cobble_coins")
        ) { ItemStack(COBBLE_COPPER_COIN.get()) }
    }

    val COBBLE_COPPER_COIN: RegistrySupplier<Item> = ITEMS.register("cobble_copper_coin") {
        CoinItem(Item.Properties().stacksTo(99), CoinType.COPPER)
    }

    val COBBLE_SILVER_COIN: RegistrySupplier<Item> = ITEMS.register("cobble_silver_coin") {
        CoinItem(Item.Properties().stacksTo(99), CoinType.SILVER)
    }

    val COBBLE_GOLD_COIN: RegistrySupplier<Item> = ITEMS.register("cobble_gold_coin") {
        CoinItem(Item.Properties().stacksTo(99), CoinType.GOLD)
    }

    val COBBLE_PLATINUM_COIN: RegistrySupplier<Item> = ITEMS.register("cobble_platinum_coin") {
        CoinItem(Item.Properties().stacksTo(99), CoinType.PLATINUM)
    }

    fun register() {
        ITEMS.register()
        TABS.register()

        CreativeTabRegistry.append(
            COBBLE_COINS_TAB,
            COBBLE_COPPER_COIN,
            COBBLE_SILVER_COIN,
            COBBLE_GOLD_COIN,
            COBBLE_PLATINUM_COIN
        )
    }
}

/**
 * Classe d'item personnalisée pour les pièces de monnaie.
 * Un clic droit avec l'item en main le dépose directement dans la bourse.
 */
class CoinItem(properties: Item.Properties, val coinType: CoinType) : Item(properties) {
    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(hand)
        if (!level.isClientSide) {
            val serverPlayer = player as ServerPlayer
            val toDeposit = stack.count
            val wallet = WalletManager.get(serverPlayer)
            
            // Ajouter les pièces au wallet et synchroniser le client
            wallet.addCoins(coinType, toDeposit.toLong())
            WalletNetwork.syncToClient(serverPlayer, wallet)
            
            // Son de ramassage discret
            level.playSound(
                null,
                player.x, player.y, player.z,
                SoundEvents.ITEM_PICKUP,
                SoundSource.PLAYERS,
                0.2f,
                0.8f + level.random.nextFloat() * 0.4f
            )
            
            // Consommer tout le stack
            stack.shrink(toDeposit)
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide)
    }
}
