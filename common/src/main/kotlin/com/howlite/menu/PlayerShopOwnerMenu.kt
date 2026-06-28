package com.howlite.menu

import com.howlite.blocks.PlayerShopBlockEntity
import com.howlite.blocks.PlayerShopOffer
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack

class PlayerShopOwnerMenu(
    syncId: Int,
    playerInventory: Inventory,
    val shopPos: BlockPos,
    val shopName: String,
    val ownerName: String,
    val offers: MutableList<PlayerShopOffer>,
    val pendingEarningsCCC: Long,
    private val stockRef: net.minecraft.core.NonNullList<ItemStack>?
) : AbstractContainerMenu(BadgeCaseMenus.PLAYER_SHOP_OWNER_MENU_TYPE.get(), syncId) {

    private val dummyStock = net.minecraft.core.NonNullList.withSize(36, ItemStack.EMPTY)
    private val stockSource = stockRef ?: dummyStock

    init {
        val startX = 36
        // Stock slots (4 rows of 9 = 36 slots)
        for (i in 0 until 36) {
            val row = i / 9
            val col = i % 9
            addSlot(object : Slot(
                net.minecraft.world.SimpleContainer(1), i,
                startX + col * 18, 111 + row * 18
            ) {
                override fun getItem(): ItemStack = stockSource[i]
                override fun set(stack: ItemStack) { stockSource[i] = stack; setChanged() }
                override fun remove(amount: Int): ItemStack {
                    val stack = stockSource[i]
                    return if (stack.isEmpty) ItemStack.EMPTY
                    else {
                        val taken = stack.split(amount)
                        if (stack.isEmpty) stockSource[i] = ItemStack.EMPTY
                        setChanged()
                        taken
                    }
                }
                override fun hasItem(): Boolean = !stockSource[i].isEmpty
                override fun mayPlace(stack: ItemStack): Boolean = true
                override fun getMaxStackSize(): Int = 64
            })
        }

        // Player inventory (3 rows of 9)
        for (row in 0..2) for (col in 0..8)
            addSlot(Slot(playerInventory, col + row * 9 + 9, startX + col * 18, 202 + row * 18))
        // Hotbar (1 row of 9)
        for (col in 0..8)
            addSlot(Slot(playerInventory, col, startX + col * 18, 260))
    }

    constructor(syncId: Int, inv: Inventory, buf: FriendlyByteBuf) : this(
        syncId, inv,
        buf.readBlockPos(),
        buf.readUtf(),
        buf.readUtf(),
        readOffers(buf, inv.player.level().registryAccess()),
        buf.readLong(),
        null
    )

    override fun stillValid(player: Player): Boolean = true

    override fun removed(player: Player) {
        super.removed(player)
        if (player is net.minecraft.server.level.ServerPlayer && stockRef != null) {
            val be = player.serverLevel().getBlockEntity(shopPos) as? PlayerShopBlockEntity
            be?.setChanged()
        }
    }

    override fun clickMenuButton(player: Player, id: Int): Boolean {
        if (player !is net.minecraft.server.level.ServerPlayer) return false
        val be = player.serverLevel().getBlockEntity(shopPos) as? PlayerShopBlockEntity ?: return false
        if (!be.isTrusted(player.uuid) && !player.hasPermissions(2)) return false

        return when (id) {
            0 -> {
                if (be.pendingEarningsCCC > 0) {
                    com.howlite.wallet.WalletManager.addAndSync(player, com.howlite.wallet.CoinType.COPPER, be.pendingEarningsCCC)
                    be.pendingEarningsCCC = 0L
                    be.setChanged()
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                        "cobblemongymodyssey.player_shop.msg.earnings_collected"))
                }
                true
            }
            else -> false
        }
    }

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        var result = ItemStack.EMPTY
        val slot = slots.getOrNull(index) ?: return result
        if (!slot.hasItem()) return result
        val stack = slot.item
        result = stack.copy()

        when {
            index < 36 -> {
                if (!moveItemStackTo(stack, 36, 72, false)) return ItemStack.EMPTY
            }
            else -> {
                if (!moveItemStackTo(stack, 0, 36, false)) return ItemStack.EMPTY
            }
        }
        if (stack.isEmpty) slot.set(ItemStack.EMPTY) else slot.setChanged()
        return result
    }

    companion object {
        private fun readOffers(buf: FriendlyByteBuf, registries: net.minecraft.core.RegistryAccess): MutableList<PlayerShopOffer> {
            val regBuf = net.minecraft.network.RegistryFriendlyByteBuf(buf, registries)
            val count = regBuf.readInt()
            return (0 until count).map {
                PlayerShopOffer(
                    resultItem = net.minecraft.world.item.ItemStack.OPTIONAL_STREAM_CODEC.decode(regBuf),
                    priceCCC   = regBuf.readLong(),
                    costItem   = net.minecraft.world.item.ItemStack.OPTIONAL_STREAM_CODEC.decode(regBuf),
                    costCount  = regBuf.readInt()
                )
            }.toMutableList()
        }
    }
}
