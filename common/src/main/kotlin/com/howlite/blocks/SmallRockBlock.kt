package com.howlite.blocks

import net.minecraft.core.BlockPos
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape

class SmallRockBlock(properties: Properties) : Block(properties) {
    private val SHAPE: VoxelShape = Block.box(2.0, 0.0, 0.0, 15.0, 10.0, 15.0)

    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        return SHAPE
    }
}
