package com.howlite.blocks

import com.mojang.serialization.MapCodec
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState

class UnownStoneActivatedBlock(properties: Properties) : BaseEntityBlock(properties) {

    override fun codec(): MapCodec<out BaseEntityBlock> = CODEC

    companion object {
        val CODEC: MapCodec<UnownStoneActivatedBlock> =
            simpleCodec(::UnownStoneActivatedBlock)
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return UnownStoneActivatedBlockEntity(pos, state)
    }

    override fun getRenderShape(state: BlockState): RenderShape {
        return RenderShape.MODEL
    }

    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        blockEntityType: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        return if (!level.isClientSide) null
        else createTickerHelper(
            blockEntityType,
            GymBlocks.UNOWN_STONE_ACTIVATED_ENTITY.get(),
            UnownStoneActivatedBlockEntity::clientTick
        )
    }
}
