package com.howlite.blocks

import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

class UnownStoneActivatedBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(GymBlocks.UNOWN_STONE_ACTIVATED_ENTITY.get(), pos, state) {

    /** Temps écoulé (incrémenté côté client à chaque tick). */
    var animationTick: Long = 0L
        private set

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
    }

    companion object {
        @JvmStatic
        fun clientTick(
            level: net.minecraft.world.level.Level,
            pos: BlockPos,
            state: BlockState,
            be: UnownStoneActivatedBlockEntity
        ) {
            be.animationTick++
        }
    }
}

