package com.howlite.blocks

import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import java.util.UUID

class GymLeaderTeleporterBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(GymBlocks.GYM_LEADER_TELEPORTER_ENTITY.get(), pos, state) {

    var returnDim: String = ""
    var returnX: Double = 0.0
    var returnY: Double = 0.0
    var returnZ: Double = 0.0
    var returnYaw: Float = 0f
    var returnPitch: Float = 0f
    var portalTicks: Int = 0
    var activatedByPlayer: UUID? = null
    var targetBadgeId: String = ""
    var clientTicks: Int = 0

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        returnDim = tag.getString("ReturnDim")
        returnX = tag.getDouble("ReturnX")
        returnY = tag.getDouble("ReturnY")
        returnZ = tag.getDouble("ReturnZ")
        returnYaw = tag.getFloat("ReturnYaw")
        returnPitch = tag.getFloat("ReturnPitch")
        portalTicks = tag.getInt("PortalTicks")
        targetBadgeId = tag.getString("TargetBadgeId")
        if (tag.hasUUID("ActivatedByPlayer")) {
            activatedByPlayer = tag.getUUID("ActivatedByPlayer")
        }
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        tag.putString("ReturnDim", returnDim)
        tag.putDouble("ReturnX", returnX)
        tag.putDouble("ReturnY", returnY)
        tag.putDouble("ReturnZ", returnZ)
        tag.putFloat("ReturnYaw", returnYaw)
        tag.putFloat("ReturnPitch", returnPitch)
        tag.putInt("PortalTicks", portalTicks)
        tag.putString("TargetBadgeId", targetBadgeId)
        activatedByPlayer?.let { tag.putUUID("ActivatedByPlayer", it) }
    }

    override fun getUpdatePacket(): net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener>? {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this)
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        return saveWithoutMetadata(registries)
    }

    companion object {
        fun serverTick(level: Level, pos: BlockPos, state: BlockState, blockEntity: GymLeaderTeleporterBlockEntity) {
            if (blockEntity.portalTicks > 0) {
                blockEntity.portalTicks--
                blockEntity.setChanged()
                
                if (blockEntity.portalTicks == 0) {
                    val newState = state.setValue(GymLeaderTeleporterBlock.PORTAL_OPEN, false)
                    level.setBlock(pos, newState, 3)
                    level.sendBlockUpdated(pos, state, newState, 3)
                }
            }
        }

        fun clientTick(level: Level, pos: BlockPos, state: BlockState, blockEntity: GymLeaderTeleporterBlockEntity) {
            if (state.getValue(GymLeaderTeleporterBlock.PORTAL_OPEN)) {
                blockEntity.clientTicks++
                if (blockEntity.portalTicks > 0) {
                    blockEntity.portalTicks--
                }
            } else {
                blockEntity.clientTicks = 0
                blockEntity.portalTicks = 0
            }
        }
    }
}

