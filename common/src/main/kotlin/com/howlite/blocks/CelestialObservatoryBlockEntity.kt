package com.howlite.blocks

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

/**
 * Block Entity de l'Observatoire Céleste.
 *
 * Entité minimale — pas d'inventaire, pas de ticker.
 * Sert de marqueur pour le [BlockEntityType] et le futur renderer si nécessaire.
 */
class CelestialObservatoryBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(GymBlocks.CELESTIAL_OBSERVATORY_ENTITY.get(), pos, state)
