package com.howlite.blocks

import com.necro.raid.dens.common.blocks.entity.RaidCrystalBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState

class ConsumableRaidBlockEntity(pos: BlockPos, state: BlockState) :
    RaidCrystalBlockEntity(GymBlocks.CONSUMABLE_RAID_BLOCK_ENTITY.get(), pos, state) {

    override fun closeRaid() {
        super.closeRaid()
        // Destroy the block and replace it with air
        level?.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 3)
    }
}
