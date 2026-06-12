package com.howlite.blocks

import com.mojang.serialization.MapCodec
import com.howlite.api.PlayerProgressApi
import com.howlite.items.GymBadgeItems
import com.howlite.items.GymLeaderTicketItem
import com.howlite.world.GymArenaGenerator
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.ItemInteractionResult
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape

class GymLeaderTeleporterBlock(properties: Properties) : BaseEntityBlock(properties) {

    override fun codec(): MapCodec<out BaseEntityBlock> =
        simpleCodec(::GymLeaderTeleporterBlock)

    private val SHAPE: VoxelShape = Block.box(0.0, 0.0, 0.0, 16.0, 3.0, 16.0)

    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        return SHAPE
    }

    init {
        registerDefaultState(stateDefinition.any().setValue(PORTAL_OPEN, false))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(PORTAL_OPEN)
    }

    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        GymLeaderTeleporterBlockEntity(pos, state)

    override fun useItemOn(
        stack: ItemStack,
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hitResult: BlockHitResult
    ): ItemInteractionResult {
        val item = stack.item
        if (item is GymLeaderTicketItem) {
            if (!state.getValue(PORTAL_OPEN)) {
                if (!level.isClientSide) {
                    val blockEntity = level.getBlockEntity(pos) as? GymLeaderTeleporterBlockEntity
                    if (blockEntity != null) {
                        // Consommer le ticket
                        if (!player.abilities.instabuild) {
                            stack.shrink(1)
                        }

                        // Sauvegarder la position du joueur dans le BlockEntity
                        blockEntity.returnDim = level.dimension().location().toString()
                        blockEntity.returnX = player.x
                        blockEntity.returnY = player.y
                        blockEntity.returnZ = player.z
                        blockEntity.returnYaw = player.yRot
                        blockEntity.returnPitch = player.xRot
                        blockEntity.portalTicks = 200 // 10 secondes (20 ticks/sec)
                        blockEntity.activatedByPlayer = player.uuid
                        blockEntity.targetBadgeId = item.targetBadge.id
                        blockEntity.setChanged()

                        // Sauvegarder également dans le PlayerProgressData (Common) pour plus de résilience
                        if (player is ServerPlayer) {
                            val data = PlayerProgressApi.get(player)
                            data.saveReturnPosition(
                                blockEntity.returnDim,
                                blockEntity.returnX,
                                blockEntity.returnY,
                                blockEntity.returnZ,
                                blockEntity.returnYaw,
                                blockEntity.returnPitch
                            )
                            PlayerProgressApi.markDirty(player)
                        }

                        // Activer le portail dans le blockstate
                        level.setBlock(pos, state.setValue(PORTAL_OPEN, true), 3)

                        // Jouer des sons
                        level.playSound(
                            null,
                            pos,
                            SoundEvents.END_PORTAL_SPAWN,
                            SoundSource.BLOCKS,
                            1.0f,
                            1.0f
                        )
                    }
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide)
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
    }

    override fun stepOn(level: Level, pos: BlockPos, state: BlockState, entity: Entity) {
        if (!level.isClientSide && state.getValue(PORTAL_OPEN) && entity is ServerPlayer) {
            val blockEntity = level.getBlockEntity(pos) as? GymLeaderTeleporterBlockEntity
            // Téléporte si c'est le joueur qui a activé le portail
            if (blockEntity != null && (blockEntity.activatedByPlayer == null || blockEntity.activatedByPlayer == entity.uuid)) {
                // Lancer la téléportation
                GymArenaGenerator.teleportAndGenerate(entity, blockEntity.targetBadgeId)
                
                // Fermer immédiatement le portail
                level.setBlock(pos, state.setValue(PORTAL_OPEN, false), 3)
                blockEntity.portalTicks = 0
                blockEntity.setChanged()
            }
        }
        super.stepOn(level, pos, state, entity)
    }

    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        return createTickerHelper(
            type,
            GymBlocks.GYM_LEADER_TELEPORTER_ENTITY.get(),
            if (level.isClientSide) GymLeaderTeleporterBlockEntity::clientTick else GymLeaderTeleporterBlockEntity::serverTick
        )
    }

    companion object {
        val PORTAL_OPEN: BooleanProperty = BooleanProperty.create("portal_open")
    }
}
