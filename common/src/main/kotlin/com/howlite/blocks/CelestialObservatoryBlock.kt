package com.howlite.blocks

import com.howlite.CobblemonGymOdyssey
import com.mojang.serialization.MapCodec
import dev.architectury.networking.NetworkManager
import io.netty.buffer.Unpooled
import net.minecraft.core.BlockPos
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult

/**
 * Bloc "Observatoire Céleste" — affiche la phase lunaire actuelle et ses effets.
 *
 * Un clic droit ouvre la GUI [CelestialObservatoryScreen] via un paquet S2C.
 */
@Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
class CelestialObservatoryBlock(properties: Properties) : BaseEntityBlock(properties) {

    companion object {
        val OPEN_GUI_PACKET = ResourceLocation.fromNamespaceAndPath(
            CobblemonGymOdyssey.MOD_ID,
            "open_celestial_observatory_gui"
        )
    }

    private val blockCodec: MapCodec<CelestialObservatoryBlock> = simpleCodec { CelestialObservatoryBlock(it) }
    override fun codec(): MapCodec<CelestialObservatoryBlock> = blockCodec

    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        CelestialObservatoryBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        level: Level, state: BlockState, type: BlockEntityType<T>
    ) = null

    override fun useWithoutItem(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hit: BlockHitResult
    ): InteractionResult {
        if (level.isClientSide) return InteractionResult.SUCCESS

        if (player is ServerPlayer) {
            val buf = RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess())
            buf.writeBlockPos(pos)
            NetworkManager.sendToPlayer(player, OPEN_GUI_PACKET, buf)
        }

        return InteractionResult.CONSUME
    }
}
