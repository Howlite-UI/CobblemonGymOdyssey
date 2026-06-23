package com.howlite.menu

import com.google.gson.GsonBuilder
import com.howlite.shop.GymShop
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack

class GymShopEditMenu(
    syncId: Int,
    playerInventory: Inventory,
    val config: GymShop.ShopsConfig
) : AbstractContainerMenu(BadgeCaseMenus.GYM_SHOP_EDIT_MENU_TYPE.get(), syncId) {

    init {
        // Ajouter les slots de l'inventaire principal du joueur (27 slots, index 9-35)
        for (row in 0..2) {
            for (col in 0..8) {
                val index = col + row * 9 + 9
                val x = 12 + col * 18
                val y = 140 + row * 18
                this.addSlot(Slot(playerInventory, index, x, y))
            }
        }

        // Ajouter les slots du hotbar (9 slots, index 0-8)
        for (col in 0..8) {
            val x = 12 + col * 18
            val y = 198
            this.addSlot(Slot(playerInventory, col, x, y))
        }
    }

    // Constructeur secondaire client
    constructor(syncId: Int, playerInventory: Inventory, buf: FriendlyByteBuf) : this(
        syncId,
        playerInventory,
        gson.fromJson(buf.readUtf(), GymShop.ShopsConfig::class.java)
    )

    override fun stillValid(player: Player): Boolean = true

    override fun quickMoveStack(player: Player, index: Int): ItemStack = ItemStack.EMPTY

    companion object {
        private val gson = GsonBuilder().create()
    }
}
