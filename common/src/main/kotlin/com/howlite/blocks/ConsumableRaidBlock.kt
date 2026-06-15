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

class ConsumableRaidBlock(properties: Properties) : RaidCrystalBlock(properties) {

    companion object {
        val CODEC: MapCodec<ConsumableRaidBlock> = simpleCodec { ConsumableRaidBlock(it) }
    }

    init {
        registerDefaultState(
            stateDefinition.any()
                .setValue(ACTIVE, true)
                .setValue(CAN_RESET, false)
                .setValue(CYCLE_MODE, RaidCycleMode.NONE)
                .setValue(IS_NATURAL, false)
                .setValue(RAID_TIER, RaidTier.TIER_FIVE)
                .setValue(RAID_TYPE, RaidType.NONE)
        )
    }

    override fun codec(): MapCodec<ConsumableRaidBlock> = CODEC

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
            // Select random tier between 5, 6, 7 stars
            val tiers = listOf(RaidTier.TIER_FIVE, RaidTier.TIER_SIX, RaidTier.TIER_SEVEN)
            val randomTier = tiers[level.random.nextInt(tiers.size)]
            
            // Choose a random elemental type for the raid (excluding NONE)
            val types = RaidType.values().filter { it != RaidType.NONE }
            val randomType = types[level.random.nextInt(types.size)]
            
            val newState = state
                .setValue(ACTIVE, true)
                .setValue(RAID_TIER, randomTier)
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

    override fun useWithoutItem(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hitResult: BlockHitResult
    ): InteractionResult {
        if (!level.isClientSide) {
            println("[GymOdyssey] useWithoutItem called at $pos for player ${player.name.string}")
            val be = level.getBlockEntity(pos)
            println("[GymOdyssey] BlockEntity at $pos is: ${be?.javaClass?.name ?: "null"}")
            if (be is ConsumableRaidBlockEntity) {
                println("[GymOdyssey] BlockEntity raidBoss: ${be.raidBossLocation}")
                println("[GymOdyssey] BlockEntity uuid: ${be.uuid}")
                println("[GymOdyssey] BlockEntity isOpen: ${be.isOpen}")
                println("[GymOdyssey] BlockEntity isActive: ${be.isActive(state)}")
            }
        }
        return super.useWithoutItem(state, level, pos, player, hitResult)
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
        if (!level.isClientSide) {
            println("[GymOdyssey] useItemOn called at $pos with stack $stack")
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult)
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        if (state.block != newState.block) {
            super.onRemove(state, level, pos, newState, isMoving)
        }
    }
}
