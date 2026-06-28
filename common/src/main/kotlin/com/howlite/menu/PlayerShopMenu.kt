package com.howlite.menu

import com.howlite.blocks.PlayerShopBlockEntity
import com.howlite.blocks.PlayerShopOffer
import com.howlite.wallet.CoinType
import com.howlite.wallet.WalletManager
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack

class PlayerShopMenu(
    syncId: Int,
    playerInventory: Inventory,
    val shopPos: BlockPos,
    val shopName: String,
    val ownerName: String,
    val offers: List<ShopOfferView>
) : AbstractContainerMenu(BadgeCaseMenus.PLAYER_SHOP_MENU_TYPE.get(), syncId) {

    data class ShopOfferView(
        val resultItem: ItemStack,
        val priceCCC: Long,
        val costItem: ItemStack,
        val costCount: Int,
        var availableStock: Int
    )

    init {
        val startX = 12
        for (row in 0..2) for (col in 0..8)
            addSlot(Slot(playerInventory, col + row * 9 + 9, startX + col * 18, 98 + row * 18))
        for (col in 0..8)
            addSlot(Slot(playerInventory, col, startX + col * 18, 156))
    }

    constructor(syncId: Int, inv: Inventory, buf: FriendlyByteBuf) : this(
        syncId, inv,
        buf.readBlockPos(),
        buf.readUtf(),
        buf.readUtf(),
        readOffers(buf, inv.player.level().registryAccess())
    )

    override fun stillValid(player: Player): Boolean = true

    override fun clickMenuButton(player: Player, id: Int): Boolean {
        if (player !is ServerPlayer) return false
        val offerIdx = id / 1000
        val qty      = (id % 1000) + 1

        val level = player.serverLevel()
        val be = level.getBlockEntity(shopPos) as? PlayerShopBlockEntity ?: return false

        val offer = be.offers.getOrNull(offerIdx) ?: return false
        if (offer.resultItem.isEmpty) return false

        val availLots = be.availableStockFor(offer)
        if (availLots < qty) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                "cobblemongymodyssey.player_shop.msg.out_of_stock"))
            return false
        }

        if (offer.isCoinOffer) {
            val totalCCC = offer.priceCCC * qty
            if (!WalletManager.removeAndSync(player, totalCCC)) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                    "cobblemongymodyssey.player_shop.msg.insufficient_funds"))
                return false
            }
            val ownerUUID = be.ownerUUID
            if (ownerUUID != null) {
                val ownerOnline = player.server?.playerList?.getPlayer(ownerUUID)
                if (ownerOnline != null) {
                    WalletManager.addAndSync(ownerOnline, CoinType.COPPER, totalCCC)
                } else {
                    be.pendingEarningsCCC += totalCCC
                }
            }
        } else {
            val costItem = offer.costItem.item
            val needed   = offer.costCount * qty
            if (!hasItems(player, costItem, needed)) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                    "cobblemongymodyssey.player_shop.msg.insufficient_items"))
                return false
            }
            consumeItems(player, costItem, needed)
            val payment = ItemStack(costItem, needed)
            for (i in be.stockItems.indices) {
                if (be.stockItems[i].isEmpty) {
                    be.stockItems[i] = payment
                    break
                }
            }
        }

        if (!be.consumeStockFor(offer, qty)) return false
        be.setChanged()

        // Send updated stock to client
        val newStock = be.availableStockFor(offer)
        val outBuf = io.netty.buffer.Unpooled.buffer()
        val resBuf = net.minecraft.network.RegistryFriendlyByteBuf(outBuf, player.registryAccess())
        resBuf.writeInt(offerIdx)
        resBuf.writeInt(newStock)
        dev.architectury.networking.NetworkManager.sendToPlayer(
            player,
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.howlite.CobblemonGymOdyssey.MOD_ID, "player_shop_sync_stock"),
            resBuf
        )

        giveOrDrop(player, ItemStack(offer.resultItem.item, offer.lotSize * qty))
        playSuccessSound(player)
        return true
    }

    override fun quickMoveStack(player: Player, index: Int): ItemStack = ItemStack.EMPTY

    private fun hasItems(player: ServerPlayer, item: net.minecraft.world.item.Item, count: Int): Boolean {
        var found = 0
        for (stack in player.inventory.items) if (stack.item == item) found += stack.count
        return found >= count
    }

    private fun consumeItems(player: ServerPlayer, item: net.minecraft.world.item.Item, count: Int) {
        var remaining = count
        for (stack in player.inventory.items) {
            if (stack.item == item) {
                if (stack.count >= remaining) { stack.shrink(remaining); break }
                else { remaining -= stack.count; stack.count = 0 }
            }
        }
    }

    private fun giveOrDrop(player: ServerPlayer, stack: ItemStack) {
        val copy = stack.copy()
        if (!player.inventory.add(copy) && !copy.isEmpty) player.drop(copy, false)
        player.inventory.setChanged()
    }

    private fun playSuccessSound(player: ServerPlayer) {
        player.connection.send(net.minecraft.network.protocol.game.ClientboundSoundPacket(
            net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(
                net.minecraft.sounds.SoundEvents.VILLAGER_YES),
            net.minecraft.sounds.SoundSource.PLAYERS,
            player.x, player.y, player.z, 1.0f, 1.0f,
            player.level().random.nextLong()
        ))
    }

    companion object {
        fun readOffers(buf: FriendlyByteBuf, registries: net.minecraft.core.RegistryAccess): List<ShopOfferView> {
            val regBuf = net.minecraft.network.RegistryFriendlyByteBuf(buf, registries)
            val count = regBuf.readInt()
            return (0 until count).map {
                ShopOfferView(
                    resultItem     = net.minecraft.world.item.ItemStack.OPTIONAL_STREAM_CODEC.decode(regBuf),
                    priceCCC       = regBuf.readLong(),
                    costItem       = net.minecraft.world.item.ItemStack.OPTIONAL_STREAM_CODEC.decode(regBuf),
                    costCount      = regBuf.readInt(),
                    availableStock = regBuf.readInt()
                )
            }
        }
    }
}
