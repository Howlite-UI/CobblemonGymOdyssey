package com.howlite.client.render

import com.howlite.blocks.PlayerShopBlockEntity
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.world.item.ItemDisplayContext

class PlayerShopBlockEntityRenderer(context: BlockEntityRendererProvider.Context) : BlockEntityRenderer<PlayerShopBlockEntity> {
    private val itemRenderer = context.itemRenderer

    override fun render(
        blockEntity: PlayerShopBlockEntity,
        partialTick: Float,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        packedLight: Int,
        packedOverlay: Int
    ) {
        val offers = blockEntity.offers
        if (offers.isEmpty()) return

        val level = blockEntity.level ?: return

        // Cycle through offers every 40 ticks (2 seconds)
        val gameTime = level.gameTime
        val index = ((gameTime / 40) % offers.size).toInt()
        val offer = offers.getOrNull(index) ?: return
        val stack = offer.resultItem
        if (stack.isEmpty) return

        poseStack.pushPose()

        // Float up/down and rotate
        val time = gameTime + partialTick
        val hover = kotlin.math.sin(time * 0.1f) * 0.05f

        // Position above the block table (height of the block is 0.75, so float it at 1.15)
        poseStack.translate(0.5, 1.15 + hover, 0.5)

        // Rotate
        val rotation = time * 2.0f
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation))

        // Scale the item preview
        poseStack.scale(0.85f, 0.85f, 0.85f)

        // Render the item
        itemRenderer.renderStatic(
            stack,
            ItemDisplayContext.GROUND,
            packedLight,
            packedOverlay,
            poseStack,
            bufferSource,
            level,
            0
        )

        poseStack.popPose()
    }
}
