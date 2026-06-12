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
import net.minecraft.world.level.block.state.properties.DirectionProperty
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.core.Direction
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape

class GymLeaderTeleporterBlock(properties: Properties) : BaseEntityBlock(properties) {

    override fun codec(): MapCodec<out BaseEntityBlock> =
        simpleCodec(::GymLeaderTeleporterBlock)

    private val SHAPE: VoxelShape = Block.box(0.0, 0.0, 0.0, 16.0, 3.0, 16.0)
    private val COLLISION_SHAPE_OPEN: VoxelShape = Block.box(0.0, 0.0, 0.0, 16.0, 2.5, 16.0)

    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        return SHAPE
    }

    override fun getCollisionShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        return if (state.getValue(PORTAL_OPEN)) COLLISION_SHAPE_OPEN else SHAPE
    }

    init {
        registerDefaultState(stateDefinition.any().setValue(PORTAL_OPEN, false).setValue(FACING, Direction.NORTH))
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
        return defaultBlockState().setValue(FACING, context.horizontalDirection.opposite)
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(PORTAL_OPEN, FACING)
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
                        blockEntity.portalTicks = 600 // 30 secondes (20 ticks/sec)
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

                        // Activer le portail dans le blockstate et forcer la synchro client
                        val newState = state.setValue(PORTAL_OPEN, true)
                        level.setBlock(pos, newState, 3)
                        level.sendBlockUpdated(pos, state, newState, 3)
                    }
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide)
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
    }

    override fun entityInside(state: BlockState, level: Level, pos: BlockPos, entity: Entity) {
        if (!level.isClientSide && state.getValue(PORTAL_OPEN) && entity is ServerPlayer) {
            val blockEntity = level.getBlockEntity(pos) as? GymLeaderTeleporterBlockEntity
            // Téléporte si c'est le joueur qui a activé le portail
            if (blockEntity != null && (blockEntity.activatedByPlayer == null || blockEntity.activatedByPlayer == entity.uuid)) {
                // Lancer la téléportation
                GymArenaGenerator.teleportAndGenerate(entity, blockEntity.targetBadgeId)
                
                // Fermer immédiatement le portail et forcer la synchro client
                val newState = state.setValue(PORTAL_OPEN, false)
                level.setBlock(pos, newState, 3)
                blockEntity.portalTicks = 0
                blockEntity.setChanged()
                level.sendBlockUpdated(pos, state, newState, 3)
            }
        }
        super.entityInside(state, level, pos, entity)
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
        val FACING: DirectionProperty = BlockStateProperties.HORIZONTAL_FACING
    }
}
