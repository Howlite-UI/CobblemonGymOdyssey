package com.howlite.blocks

import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.core.NonNullList
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.world.ContainerHelper
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import java.util.UUID

data class PlayerShopOffer(
    var resultItem: ItemStack = ItemStack.EMPTY,
    var priceCCC: Long = 0L,
    var costItem: ItemStack = ItemStack.EMPTY,
    var costCount: Int = 1
) {
    val isCoinOffer: Boolean get() = priceCCC > 0L
    val lotSize: Int get() = resultItem.count.coerceAtLeast(1)

    fun toNbt(registries: HolderLookup.Provider): CompoundTag {
        val tag = CompoundTag()
        tag.put("result", resultItem.saveOptional(registries))
        tag.putLong("price_ccc", priceCCC)
        tag.put("cost_item", costItem.saveOptional(registries))
        tag.putInt("cost_count", costCount)
        return tag
    }

    companion object {
        fun fromNbt(tag: CompoundTag, registries: HolderLookup.Provider): PlayerShopOffer {
            val result   = ItemStack.parseOptional(registries, tag.getCompound("result"))
            val price    = tag.getLong("price_ccc")
            val costItem = ItemStack.parseOptional(registries, tag.getCompound("cost_item"))
            val costCnt  = tag.getInt("cost_count").coerceAtLeast(1)
            return PlayerShopOffer(result, price, costItem, costCnt)
        }
    }
}

class PlayerShopBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(GymBlocks.PLAYER_SHOP_BLOCK_ENTITY.get(), pos, state) {

    companion object {
        const val MAX_OFFERS       = 16
        const val STOCK_SLOT_COUNT = 27
    }

    var ownerUUID: UUID? = null
    var ownerName: String = "unknown"
    var shopName: String = "Player Shop"
    val trustedPlayers: MutableList<UUID> = mutableListOf()
    val offers: MutableList<PlayerShopOffer> = mutableListOf()
    val stockItems: NonNullList<ItemStack> = NonNullList.withSize(STOCK_SLOT_COUNT, ItemStack.EMPTY)
    var pendingEarningsCCC: Long = 0L

    fun isOwner(uuid: UUID): Boolean = ownerUUID == uuid
    fun isTrusted(uuid: UUID): Boolean = isOwner(uuid) || trustedPlayers.contains(uuid)

    fun availableStockFor(offer: PlayerShopOffer): Int {
        if (offer.resultItem.isEmpty) return 0
        val neededItem = offer.resultItem.item
        var totalFound = 0
        for (stack in stockItems) {
            if (!stack.isEmpty && stack.item == neededItem) totalFound += stack.count
        }
        return totalFound / offer.lotSize
    }

    fun consumeStockFor(offer: PlayerShopOffer, qty: Int): Boolean {
        var needed = offer.lotSize * qty
        if (needed <= 0) return false
        var found = 0
        for (stack in stockItems) {
            if (!stack.isEmpty && stack.item == offer.resultItem.item) found += stack.count
        }
        if (found < needed) return false
        for (i in stockItems.indices) {
            val stack = stockItems[i]
            if (!stack.isEmpty && stack.item == offer.resultItem.item) {
                if (stack.count >= needed) {
                    stack.shrink(needed)
                    if (stack.isEmpty) stockItems[i] = ItemStack.EMPTY
                    needed = 0
                    break
                } else {
                    needed -= stack.count
                    stockItems[i] = ItemStack.EMPTY
                }
            }
        }
        setChanged()
        return needed == 0
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        ownerUUID?.let { tag.putUUID("owner_uuid", it) }
        tag.putString("owner_name", ownerName)
        tag.putString("shop_name", shopName)

        val trustedList = ListTag()
        for (uuid in trustedPlayers) {
            val t = CompoundTag(); t.putUUID("uuid", uuid); trustedList.add(t)
        }
        tag.put("trusted", trustedList)

        val offerList = ListTag()
        for (offer in offers) offerList.add(offer.toNbt(registries))
        tag.put("offers", offerList)

        tag.putLong("pending_earnings", pendingEarningsCCC)

        ContainerHelper.saveAllItems(tag, stockItems, registries)
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        ownerUUID = if (tag.hasUUID("owner_uuid")) tag.getUUID("owner_uuid") else null
        ownerName = tag.getString("owner_name").ifEmpty { "unknown" }
        shopName  = tag.getString("shop_name").ifEmpty { "Player Shop" }

        trustedPlayers.clear()
        val trustedList = tag.getList("trusted", Tag.TAG_COMPOUND.toInt())
        for (i in 0 until trustedList.size) {
            val t = trustedList.getCompound(i)
            if (t.hasUUID("uuid")) trustedPlayers.add(t.getUUID("uuid"))
        }

        offers.clear()
        val offerList = tag.getList("offers", Tag.TAG_COMPOUND.toInt())
        for (i in 0 until offerList.size) offers.add(PlayerShopOffer.fromNbt(offerList.getCompound(i), registries))

        pendingEarningsCCC = tag.getLong("pending_earnings")

        for (i in stockItems.indices) stockItems[i] = ItemStack.EMPTY
        ContainerHelper.loadAllItems(tag, stockItems, registries)
    }
}
