package com.howlite.blocks

import com.howlite.CobblemonGymOdyssey
import com.mojang.serialization.MapCodec
import dev.architectury.networking.NetworkManager
import io.netty.buffer.Unpooled
import net.minecraft.core.BlockPos
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult

@Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
class PlayerShopBlock(properties: Properties) : BaseEntityBlock(properties) {

    private val blockCodec: MapCodec<PlayerShopBlock> = simpleCodec { PlayerShopBlock(it) }
    override fun codec(): MapCodec<PlayerShopBlock> = blockCodec
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        PlayerShopBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        level: Level, state: BlockState, type: BlockEntityType<T>
    ) = null

    override fun setPlacedBy(level: Level, pos: BlockPos, state: BlockState, placer: LivingEntity?, stack: ItemStack) {
        super.setPlacedBy(level, pos, state, placer, stack)
        if (!level.isClientSide && placer is ServerPlayer) {
            val be = level.getBlockEntity(pos) as? PlayerShopBlockEntity ?: return
            be.ownerUUID = placer.uuid
            be.ownerName = placer.name.string
            be.shopName  = "${placer.name.string}'s Shop"
            be.setChanged()
        }
    }

    override fun useWithoutItem(
        state: BlockState, level: Level, pos: BlockPos, player: Player, hitResult: BlockHitResult
    ): InteractionResult {
        if (level.isClientSide) return InteractionResult.SUCCESS
        if (player !is ServerPlayer) return InteractionResult.PASS

        val be = level.getBlockEntity(pos) as? PlayerShopBlockEntity ?: return InteractionResult.PASS

        // Distance check
        if (player.distanceToSqr(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) > 64.0) {
            return InteractionResult.PASS
        }

        val isOwnerOrTrusted = be.isTrusted(player.uuid) || player.hasPermissions(2)
        val isSneaking = player.isCrouching

        // Shift+click for owner/trusted → open owner management screen
        // Normal click → open buyer screen
        if (isSneaking && isOwnerOrTrusted) {
            // Collect pending earnings automatically when owner opens
            if (be.pendingEarningsCCC > 0L && be.isOwner(player.uuid)) {
                com.howlite.wallet.WalletManager.addAndSync(player, com.howlite.wallet.CoinType.COPPER, be.pendingEarningsCCC)
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                    "cobblemongymodyssey.player_shop.msg.earnings_collected_auto",
                    com.howlite.wallet.WalletManager.formatCCC(be.pendingEarningsCCC)
                ))
                be.pendingEarningsCCC = 0L
                be.setChanged()
            }

            dev.architectury.registry.menu.MenuRegistry.openExtendedMenu(
                player,
                object : net.minecraft.world.MenuProvider {
                    override fun getDisplayName() = net.minecraft.network.chat.Component.literal(be.shopName)
                    override fun createMenu(id: Int, inv: net.minecraft.world.entity.player.Inventory, p: Player) =
                        com.howlite.menu.PlayerShopOwnerMenu(
                            id, inv, pos, be.shopName, be.ownerName,
                            be.offers, be.pendingEarningsCCC, be.stockItems
                        )
                }
            ) { outBuf ->
                val regBuf = net.minecraft.network.RegistryFriendlyByteBuf(outBuf, player.registryAccess())
                regBuf.writeBlockPos(pos)
                regBuf.writeUtf(be.shopName)
                regBuf.writeUtf(be.ownerName)
                regBuf.writeInt(be.offers.size)
                for (offer in be.offers) {
                    net.minecraft.world.item.ItemStack.OPTIONAL_STREAM_CODEC.encode(regBuf, offer.resultItem)
                    regBuf.writeLong(offer.priceCCC)
                    net.minecraft.world.item.ItemStack.OPTIONAL_STREAM_CODEC.encode(regBuf, offer.costItem)
                    regBuf.writeInt(offer.costCount)
                }
                regBuf.writeLong(be.pendingEarningsCCC)
            }
        } else {
            dev.architectury.registry.menu.MenuRegistry.openExtendedMenu(
                player,
                object : net.minecraft.world.MenuProvider {
                    override fun getDisplayName() = net.minecraft.network.chat.Component.literal(be.shopName)
                    override fun createMenu(id: Int, inv: net.minecraft.world.entity.player.Inventory, p: Player) =
                        com.howlite.menu.PlayerShopMenu(id, inv, pos, be.shopName, be.ownerName, be.offers.mapIndexed { idx, offer ->
                            com.howlite.menu.PlayerShopMenu.ShopOfferView(
                                offer.resultItem.copy(), offer.priceCCC,
                                offer.costItem.copy(), offer.costCount,
                                be.availableStockFor(offer)
                            )
                        })
                }
            ) { outBuf ->
                val regBuf = net.minecraft.network.RegistryFriendlyByteBuf(outBuf, player.registryAccess())
                regBuf.writeBlockPos(pos)
                regBuf.writeUtf(be.shopName)
                regBuf.writeUtf(be.ownerName)
                regBuf.writeInt(be.offers.size)
                for (offer in be.offers) {
                    net.minecraft.world.item.ItemStack.OPTIONAL_STREAM_CODEC.encode(regBuf, offer.resultItem)
                    regBuf.writeLong(offer.priceCCC)
                    net.minecraft.world.item.ItemStack.OPTIONAL_STREAM_CODEC.encode(regBuf, offer.costItem)
                    regBuf.writeInt(offer.costCount)
                    regBuf.writeInt(be.availableStockFor(offer))
                }
            }
        }

        return InteractionResult.SUCCESS
    }

    override fun playerWillDestroy(level: Level, pos: BlockPos, state: BlockState, player: Player): BlockState {
        if (!level.isClientSide) {
            val be = level.getBlockEntity(pos) as? PlayerShopBlockEntity
            if (be != null) {
                val isAllowed = be.isOwner(player.uuid) || player.hasPermissions(2)
                if (!isAllowed) {
                    // Block the break — place the block back
                    level.setBlock(pos, state, 3)
                    player.sendSystemMessage(
                        net.minecraft.network.chat.Component.translatable("cobblemongymodyssey.player_shop.msg.not_owner")
                    )
                    return state
                }
                // Drop the stock back to the owner if online, or at block position
                for (stack in be.stockItems) {
                    if (!stack.isEmpty) {
                        net.minecraft.world.entity.item.ItemEntity(
                            level,
                            pos.x + 0.5, pos.y + 1.0, pos.z + 0.5,
                            stack.copy()
                        ).also { level.addFreshEntity(it) }
                    }
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player)
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        if (state.block != newState.block) {
            level.removeBlockEntity(pos)
        }
    }
}
