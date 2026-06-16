package com.howlite.wallet

import com.howlite.CobblemonGymOdyssey
import dev.architectury.networking.NetworkManager
import io.netty.buffer.Unpooled
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer

/**
 * Gestion des packets réseau liés au wallet.
 *
 * Packets :
 *  - S2C [WALLET_SYNC_ID]   : serveur → client, synchronise l'état complet du wallet.
 *  - C2S [WALLET_TOGGLE_ID] : client → serveur, bascule autoCollect ou hudEnabled.
 */
object WalletNetwork {

    val WALLET_SYNC_ID: ResourceLocation =
        ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "wallet_sync")

    val WALLET_TOGGLE_ID: ResourceLocation =
        ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "wallet_toggle")

    val WALLET_WITHDRAW_ID: ResourceLocation =
        ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "wallet_withdraw")

    val WALLET_DEPOSIT_SLOT_ID: ResourceLocation =
        ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "wallet_deposit_slot")

    // -------------------------------------------------------------------------
    // Enregistrement des receivers (côté commun)
    // -------------------------------------------------------------------------

    fun registerServerReceivers() {
        // C2S : le client bascule un setting (0 = autoCollect, 1 = hudEnabled)
        NetworkManager.registerReceiver(
            NetworkManager.Side.C2S,
            WALLET_TOGGLE_ID
        ) { buf, context ->
            val switchId = buf.readByte()
            val newValue = buf.readBoolean()
            context.queue {
                val player = context.player as? ServerPlayer ?: return@queue
                val wallet = WalletManager.get(player)
                when (switchId.toInt()) {
                    0 -> wallet.autoCollect = newValue
                    1 -> wallet.hudEnabled = newValue
                }
                syncToClient(player, wallet)
            }
        }

        // C2S : le client dépose des pièces depuis un slot de l'inventaire via Shift-Click
        NetworkManager.registerReceiver(
            NetworkManager.Side.C2S,
            WALLET_DEPOSIT_SLOT_ID
        ) { buf, context ->
            val slotId = buf.readInt()
            context.queue {
                val player = context.player as? ServerPlayer ?: return@queue
                if (slotId >= 0 && slotId < player.containerMenu.slots.size) {
                    val slot = player.containerMenu.slots[slotId]
                    if (slot.hasItem()) {
                        val stack = slot.item
                        val coinType = CoinType.values().find { stack.item == it.getItem() }
                        if (coinType != null) {
                            val count = stack.count.toLong()
                            val wallet = WalletManager.get(player)
                            wallet.addCoins(coinType, count)
                            slot.set(net.minecraft.world.item.ItemStack.EMPTY)
                            player.containerMenu.broadcastChanges()
                            syncToClient(player, wallet)
                        }
                    }
                }
            }
        }

        // C2S : le client retire des pièces en item ou en dépose
        NetworkManager.registerReceiver(
            NetworkManager.Side.C2S,
            WALLET_WITHDRAW_ID
        ) { buf, context ->
            val coinTypeOrdinal = buf.readByte().toInt()
            val button = buf.readByte().toInt() // 0 = Left click, 1 = Right click
            val isShift = buf.readBoolean()
            context.queue {
                val player = context.player as? ServerPlayer ?: return@queue
                val wallet = WalletManager.get(player)
                if (coinTypeOrdinal in 0..3) {
                    val coinType = CoinType.entries[coinTypeOrdinal]
                    val item = coinType.getItem()
                    val carried = player.containerMenu.carried
                    
                    if (!carried.isEmpty) {
                        // Deposit carried coin into wallet
                        if (carried.item == item) {
                            val toDeposit = if (button == 1) 1 else carried.count
                            wallet.addCoins(coinType, toDeposit.toLong())
                            carried.shrink(toDeposit)
                            player.containerMenu.setCarried(carried)
                            syncToClient(player, wallet)
                        }
                    } else {
                        // Withdraw from wallet
                        val available = wallet.balanceCCC / coinType.valueCCC
                        if (available > 0) {
                            val limit = if (button == 1) 32L else 64L
                            val toWithdraw = available.coerceAtMost(limit)
                            if (wallet.removeCoins(coinType, toWithdraw)) {
                                val itemStack = net.minecraft.world.item.ItemStack(item, toWithdraw.toInt())
                                if (isShift) {
                                    // Shift-Click: add directly to inventory
                                    player.inventory.add(itemStack)
                                    if (!itemStack.isEmpty) {
                                        player.drop(itemStack, false)
                                    }
                                } else {
                                    // Normal click: set on cursor
                                    player.containerMenu.setCarried(itemStack)
                                }
                                syncToClient(player, wallet)
                            }
                        }
                    }
                }
            }
        }
    }

    fun registerClientReceivers() {
        // S2C : le serveur envoie l'état complet du wallet
        NetworkManager.registerReceiver(
            NetworkManager.Side.S2C,
            WALLET_SYNC_ID
        ) { buf, context ->
            val balance = buf.readLong()
            val autoCollect = buf.readBoolean()
            val hud = buf.readBoolean()
            context.queue {
                ClientWalletCache.balance = balance
                ClientWalletCache.autoCollect = autoCollect
                ClientWalletCache.hudEnabled = hud
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helpers d'envoi
    // -------------------------------------------------------------------------

    /** Envoie l'état complet du wallet au client (appel serveur). */
    fun syncToClient(player: ServerPlayer, wallet: WalletData) {
        val buf = RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess())
        buf.writeLong(wallet.balanceCCC)
        buf.writeBoolean(wallet.autoCollect)
        buf.writeBoolean(wallet.hudEnabled)
        NetworkManager.sendToPlayer(player, WALLET_SYNC_ID, buf)
    }

    /** Envoie un toggle depuis le client vers le serveur. */
    fun sendToggle(switchId: Int, newValue: Boolean) {
        val buf = RegistryFriendlyByteBuf(
            Unpooled.buffer(),
            net.minecraft.client.Minecraft.getInstance().level!!.registryAccess()
        )
        buf.writeByte(switchId)
        buf.writeBoolean(newValue)
        NetworkManager.sendToServer(WALLET_TOGGLE_ID, buf)
    }

    /** Envoie une demande de retrait de pièces. */
    fun sendWithdraw(coinTypeOrdinal: Int, button: Int, isShift: Boolean) {
        val buf = RegistryFriendlyByteBuf(
            Unpooled.buffer(),
            net.minecraft.client.Minecraft.getInstance().level!!.registryAccess()
        )
        buf.writeByte(coinTypeOrdinal)
        buf.writeByte(button)
        buf.writeBoolean(isShift)
        NetworkManager.sendToServer(WALLET_WITHDRAW_ID, buf)
    }

    /** Envoie un dépôt rapide de pièce depuis un slot d'inventaire. */
    fun sendDepositSlot(slotId: Int) {
        val buf = RegistryFriendlyByteBuf(
            Unpooled.buffer(),
            net.minecraft.client.Minecraft.getInstance().level!!.registryAccess()
        )
        buf.writeInt(slotId)
        NetworkManager.sendToServer(WALLET_DEPOSIT_SLOT_ID, buf)
    }
}
