package com.howlite.client.render

import com.howlite.blocks.ConsumableRaidBlockEntity
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import software.bernie.geckolib.renderer.GeoBlockRenderer

class ConsumableRaidBlockEntityRenderer(context: BlockEntityRendererProvider.Context) :
    GeoBlockRenderer<ConsumableRaidBlockEntity>(ConsumableRaidBlockEntityModel()) {

    override fun render(
        blockEntity: ConsumableRaidBlockEntity,
        partialTick: Float,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        packedLight: Int,
        packedOverlay: Int
    ) {
        poseStack.pushPose()

        // Slowly float the model up and down
        val gameTime = blockEntity.level?.gameTime ?: 0L
        val time = gameTime + partialTick
        val floatOffset = kotlin.math.sin(time * 0.1f) * 0.05f
        poseStack.translate(0.0, floatOffset.toDouble(), 0.0)

        // Slowly rotate and scale the model around the center of the block
        val rotation = time * 2.0f
        poseStack.translate(0.5, 0.5, 0.5)
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rotation))
        poseStack.scale(0.55f, 0.55f, 0.55f)
        poseStack.translate(-0.5, -0.5, -0.5)

        super.render(blockEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay)

        poseStack.popPose()
    }
}
