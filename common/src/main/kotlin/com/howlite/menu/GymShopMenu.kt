package com.howlite.menu

import com.howlite.data.GymRegion
import com.howlite.shop.GymShop
import com.howlite.wallet.CoinType
import com.howlite.wallet.WalletManager
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack

class GymShopMenu(
    syncId: Int,
    playerInventory: Inventory,
    val region: GymRegion,
    val items: List<GymShop.ShopItemConfig>
) : AbstractContainerMenu(BadgeCaseMenus.GYM_SHOP_MENU_TYPE.get(), syncId) {

    init {
        // Ajouter les slots de l'inventaire principal du joueur (27 slots, index 9-35)
        for (row in 0..2) {
            for (col in 0..8) {
                val index = col + row * 9 + 9
                val x = 12 + col * 18
                val y = 98 + row * 18
                this.addSlot(Slot(playerInventory, index, x, y))
            }
        }

        // Ajouter les slots du hotbar (9 slots, index 0-8)
        for (col in 0..8) {
            val x = 12 + col * 18
            val y = 156
            this.addSlot(Slot(playerInventory, col, x, y))
        }
    }

    // Constructeur secondaire client
    constructor(syncId: Int, playerInventory: Inventory, buf: FriendlyByteBuf) : this(
        syncId,
        playerInventory,
        GymRegion.valueOf(buf.readUtf()),
        readItems(buf)
    )

    override fun stillValid(player: Player): Boolean = true

    override fun clickMenuButton(player: Player, id: Int): Boolean {
        if (player !is ServerPlayer) return false
        val selectedIndex = id / 100
        val quantity = (id % 100) + 1

        if (selectedIndex !in items.indices) return false
        val itemConfig = items[selectedIndex]

        // 1. Calcul du coût total
        val costItemLoc = net.minecraft.resources.ResourceLocation.tryParse(itemConfig.costItem) ?: return false
        val costItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(costItemLoc)
        val totalCostCount = itemConfig.costCount.toLong() * quantity

        val resultItemLoc = net.minecraft.resources.ResourceLocation.tryParse(itemConfig.resultItem) ?: return false
        val resultItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(resultItemLoc)
        val totalResultCount = itemConfig.resultCount * quantity

        // 2. Vérification s'il s'agit d'une pièce du portefeuille
        val coinType = getCoinType(itemConfig.costItem)
        if (coinType != null) {
            val totalCostCCC = coinType.valueCCC * totalCostCount
            val success = WalletManager.removeAndSync(player, totalCostCCC)
            if (success) {
                val stack = ItemStack(resultItem, totalResultCount)
                player.inventory.add(stack)
                player.connection.send(net.minecraft.network.protocol.game.ClientboundSoundPacket(
                    net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(net.minecraft.sounds.SoundEvents.VILLAGER_YES),
                    net.minecraft.sounds.SoundSource.PLAYERS,
                    player.x, player.y, player.z,
                    1.0f, 1.0f,
                    player.level().random.nextLong()
                ))
                return true
            }
        } else {
            // Physique (ex: émeraude)
            if (hasRequiredItems(player, costItem, totalCostCount.toInt())) {
                consumeItems(player, costItem, totalCostCount.toInt())
                val stack = ItemStack(resultItem, totalResultCount)
                player.inventory.add(stack)
                player.connection.send(net.minecraft.network.protocol.game.ClientboundSoundPacket(
                    net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(net.minecraft.sounds.SoundEvents.VILLAGER_YES),
                    net.minecraft.sounds.SoundSource.PLAYERS,
                    player.x, player.y, player.z,
                    1.0f, 1.0f,
                    player.level().random.nextLong()
                ))
                return true
            }
        }

        return false
    }

    private fun getCoinType(itemId: String): CoinType? {
        return when (itemId) {
            "cobblemongymodyssey:cobble_copper_coin" -> CoinType.COPPER
            "cobblemongymodyssey:cobble_silver_coin" -> CoinType.SILVER
            "cobblemongymodyssey:cobble_gold_coin" -> CoinType.GOLD
            "cobblemongymodyssey:cobble_platinum_coin" -> CoinType.PLATINUM
            else -> null
        }
    }

    private fun hasRequiredItems(player: ServerPlayer, item: net.minecraft.world.item.Item, count: Int): Boolean {
        var found = 0
        for (stack in player.inventory.items) {
            if (stack.item == item) {
                found += stack.count
            }
        }
        return found >= count
    }

    private fun consumeItems(player: ServerPlayer, item: net.minecraft.world.item.Item, count: Int) {
        var remaining = count
        for (stack in player.inventory.items) {
            if (stack.item == item) {
                if (stack.count >= remaining) {
                    stack.shrink(remaining)
                    break
                } else {
                    remaining -= stack.count
                    stack.count = 0
                }
            }
        }
    }

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        var itemStack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot.hasItem()) {
            val itemStack2 = slot.item
            itemStack = itemStack2.copy()
            if (index < 27) { // Depuis l'inventaire vers le hotbar
                if (!moveItemStackTo(itemStack2, 27, 36, false)) {
                    return ItemStack.EMPTY
                }
            } else { // Depuis le hotbar vers l'inventaire
                if (!moveItemStackTo(itemStack2, 0, 27, false)) {
                    return ItemStack.EMPTY
                }
            }
            if (itemStack2.isEmpty) {
                slot.setByPlayer(ItemStack.EMPTY)
            } else {
                slot.setChanged()
            }
        }
        return itemStack
    }

    companion object {
        private fun readItems(buf: FriendlyByteBuf): List<GymShop.ShopItemConfig> {
            val size = buf.readInt()
            val list = mutableListOf<GymShop.ShopItemConfig>()
            for (i in 0 until size) {
                list.add(
                    GymShop.ShopItemConfig(
                        requiredBadge = "",
                        costItem = buf.readUtf(),
                        costCount = buf.readInt(),
                        resultItem = buf.readUtf(),
                        resultCount = buf.readInt()
                    )
                )
            }
            return list
        }
    }
}
