package com.howlite.blocks

import com.mojang.serialization.MapCodec
import com.necro.raid.dens.common.blocks.block.RaidCrystalBlock
import com.necro.raid.dens.common.data.raid.RaidCycleMode
import com.necro.raid.dens.common.data.raid.RaidTier
import com.necro.raid.dens.common.data.raid.RaidType
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import java.util.UUID
import net.minecraft.world.InteractionResult
import net.minecraft.world.ItemInteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.InteractionHand
import dev.architectury.networking.NetworkManager
import io.netty.buffer.Unpooled
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import com.howlite.CobblemonGymOdyssey


import net.minecraft.world.level.BlockGetter
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape

class ConsumableRaidBlock(val tier: RaidTier, properties: Properties) : RaidCrystalBlock(properties) {

    private val blockCodec: MapCodec<ConsumableRaidBlock> = simpleCodec { ConsumableRaidBlock(tier, it) }

    init {
        registerDefaultState(
            stateDefinition.any()
                .setValue(ACTIVE, true)
                .setValue(CAN_RESET, false)
                .setValue(CYCLE_MODE, RaidCycleMode.NONE)
                .setValue(IS_NATURAL, false)
                .setValue(RAID_TIER, tier)
                .setValue(RAID_TYPE, RaidType.NONE)
        )
    }

    override fun codec(): MapCodec<ConsumableRaidBlock> = blockCodec

    override fun getShape(
        state: BlockState,
        level: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape {
        return box(0.0, 0.0, 0.0, 16.0, 18.0, 16.0)
    }

    override fun getCollisionShape(
        state: BlockState,
        level: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape {
        return box(0.0, 0.0, 0.0, 16.0, 18.0, 16.0)
    }

    override fun getRenderShape(state: BlockState): RenderShape {
        return RenderShape.MODEL
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return ConsumableRaidBlockEntity(pos, state)
    }

    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        return createTickerHelper(
            type,
            GymBlocks.CONSUMABLE_RAID_BLOCK_ENTITY.get(),
            BlockEntityTicker { l, p, s, be ->
                if (be is ConsumableRaidBlockEntity) {
                    be.tick(l, p, s)
                }
            }
        )
    }

    override fun setPlacedBy(
        level: Level,
        pos: BlockPos,
        state: BlockState,
        placer: LivingEntity?,
        stack: ItemStack
    ) {
        super.setPlacedBy(level, pos, state, placer, stack)
        
        if (!level.isClientSide) {
            // Choose a random elemental type for the raid (excluding NONE)
            val types = RaidType.values().filter { it != RaidType.NONE }
            val randomType = types[level.random.nextInt(types.size)]
            
            val newState = state
                .setValue(ACTIVE, true)
                .setValue(RAID_TIER, this.tier)
                .setValue(RAID_TYPE, randomType)
            
            level.setBlock(pos, newState, 3)
            
            // Get block entity and force-generate a boss!
            val blockEntity = level.getBlockEntity(pos)
            if (blockEntity is ConsumableRaidBlockEntity) {
                blockEntity.setUuid(UUID.randomUUID())
                blockEntity.setOpen()
                blockEntity.generateRaidBoss(level, pos, newState)
            }
        }
    }

    fun startOrJoinRaidPublic(player: Player, state: BlockState, level: Level, pos: BlockPos) {
        super.useWithoutItem(
            state,
            level,
            pos,
            player,
            net.minecraft.world.phys.BlockHitResult.miss(
                net.minecraft.world.phys.Vec3.ZERO,
                net.minecraft.core.Direction.UP,
                pos
            )
        )
    }

    override fun useWithoutItem(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hitResult: BlockHitResult
    ): InteractionResult {
        if (!level.isClientSide && player is ServerPlayer) {
            val be = level.getBlockEntity(pos) as? ConsumableRaidBlockEntity
            if (be != null && be.isActive(state)) {
                val raidBoss = be.getRaidBoss()
                if (raidBoss != null) {
                    val buf = net.minecraft.network.RegistryFriendlyByteBuf(
                        Unpooled.buffer(),
                        player.level().registryAccess()
                    )
                    buf.writeBlockPos(pos)
                    NetworkManager.sendToPlayer(
                        player,
                        ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "open_consumable_raid_gui"),
                        buf
                    )
                    return InteractionResult.SUCCESS
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide)
    }

    override fun useItemOn(
        stack: ItemStack,
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hitResult: BlockHitResult
    ): ItemInteractionResult {
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        if (state.block != newState.block) {
            super.onRemove(state, level, pos, newState, isMoving)
        }
    }
}
