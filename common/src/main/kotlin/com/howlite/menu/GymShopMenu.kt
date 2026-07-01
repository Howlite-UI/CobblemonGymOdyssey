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
    val items: List<GymShop.GymShopEntry>
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
        readItems(buf, playerInventory.player.level().registryAccess())
    )

    override fun stillValid(player: Player): Boolean = true

    override fun clickMenuButton(player: Player, id: Int): Boolean {
        if (player !is ServerPlayer) return false
        val selectedIndex = id / 100
        val quantity = (id % 100) + 1

        if (selectedIndex !in items.indices) return false
        val entry = items[selectedIndex]

        val costStackSingle = entry.costItem
        val resultStackSingle = entry.resultItem
        val costItemType = costStackSingle.item

        // 2. Vérification s'il s'agit d'une pièce du portefeuille
        val costItemPath = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(costItemType).toString()
        val coinType = getCoinType(costItemPath)
        if (coinType != null) {
            val totalCostCCC = coinType.valueCCC * costStackSingle.count * quantity
            val success = WalletManager.removeAndSync(player, totalCostCCC)
            if (success) {
                val stack = resultStackSingle.copyWithCount(resultStackSingle.count * quantity)
                giveOrDrop(player, stack)
                playSuccessSound(player)
                return true
            }
        } else {
            // Physique (ex: émeraude, jetons de gym, etc.) avec support complet de composants NBT/Components
            val requiredCostStack = costStackSingle.copyWithCount(costStackSingle.count * quantity)
            if (hasRequiredItems(player, requiredCostStack)) {
                consumeItems(player, requiredCostStack)
                val stack = resultStackSingle.copyWithCount(resultStackSingle.count * quantity)
                giveOrDrop(player, stack)
                playSuccessSound(player)
                return true
            }
        }

        return false
    }

    private fun playSuccessSound(player: ServerPlayer) {
        player.connection.send(net.minecraft.network.protocol.game.ClientboundSoundPacket(
            net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(net.minecraft.sounds.SoundEvents.VILLAGER_YES),
            net.minecraft.sounds.SoundSource.PLAYERS,
            player.x, player.y, player.z,
            1.0f, 1.0f,
            player.level().random.nextLong()
        ))
    }

    private fun hasRequiredItems(player: ServerPlayer, requiredStack: ItemStack): Boolean {
        var found = 0
        for (stack in player.inventory.items) {
            if (ItemStack.isSameItemSameComponents(stack, requiredStack)) {
                found += stack.count
            }
        }
        return found >= requiredStack.count
    }

    private fun consumeItems(player: ServerPlayer, requiredStack: ItemStack) {
        var remaining = requiredStack.count
        for (stack in player.inventory.items) {
            if (ItemStack.isSameItemSameComponents(stack, requiredStack)) {
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

    private fun getCoinType(itemId: String): CoinType? {
        return when (itemId) {
            "cobblemongymodyssey:cobble_copper_coin" -> CoinType.COPPER
            "cobblemongymodyssey:cobble_silver_coin" -> CoinType.SILVER
            "cobblemongymodyssey:cobble_gold_coin" -> CoinType.GOLD
            "cobblemongymodyssey:cobble_platinum_coin" -> CoinType.PLATINUM
            else -> null
        }
    }

    /**
     * Tente d'ajouter [stack] à l'inventaire du joueur.
     * Si l'inventaire est plein (ou partiellement plein), le reste est droppé
     * aux pieds du joueur — le joueur ne perd jamais l'item qu'il vient d'acheter.
     */
    private fun giveOrDrop(player: ServerPlayer, stack: ItemStack) {
        // inventory.add() tente de remplir les slots existants + vides.
        // Si elle retourne false, TOUT le stack n'a pas pu être placé.
        // Si elle retourne true, le stack a été entièrement absorbé.
        // Cas particulier : add() peut modifier stack.count si elle a placé une partie.
        val copy = stack.copy()
        val added = player.inventory.add(copy)
        if (!added && !copy.isEmpty) {
            // L'inventaire était plein — on droppe le reste par terre
            player.drop(copy, false)
        }
        player.inventory.setChanged()
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
        private fun readItems(buf: FriendlyByteBuf, registries: net.minecraft.core.RegistryAccess): List<GymShop.GymShopEntry> {
            val regBuf = net.minecraft.network.RegistryFriendlyByteBuf(buf, registries)
            val size = regBuf.readInt()
            val list = mutableListOf<GymShop.GymShopEntry>()
            for (i in 0 until size) {
                list.add(
                    GymShop.GymShopEntry(
                        requiredBadge = regBuf.readUtf(),
                        costItem = ItemStack.OPTIONAL_STREAM_CODEC.decode(regBuf),
                        resultItem = ItemStack.OPTIONAL_STREAM_CODEC.decode(regBuf)
                    )
                )
            }
            return list
        }
    }
}
