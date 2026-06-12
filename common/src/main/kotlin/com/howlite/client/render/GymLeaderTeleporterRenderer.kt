package com.howlite.client.render

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.howlite.CobblemonGymOdyssey
import com.howlite.blocks.GymLeaderTeleporterBlock
import com.howlite.blocks.GymLeaderTeleporterBlockEntity
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.client.renderer.LightTexture
import net.minecraft.resources.ResourceLocation
import net.minecraft.core.Direction
import net.minecraft.client.gui.Font
import org.joml.Matrix3f
import org.joml.Matrix4f
import kotlin.math.sin

class GymLeaderTeleporterRenderer(val context: BlockEntityRendererProvider.Context) :
    BlockEntityRenderer<GymLeaderTeleporterBlockEntity> {

    companion object {
        val PORTAL_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CobblemonGymOdyssey.MOD_ID,
            "textures/block/gym_portal.png"
        )
        val PORTAL_SIDE_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CobblemonGymOdyssey.MOD_ID,
            "textures/block/gym_portal_side.png"
        )
    }

    override fun render(
        blockEntity: GymLeaderTeleporterBlockEntity,
        partialTick: Float,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        packedLight: Int,
        packedOverlay: Int
    ) {
        val state = blockEntity.blockState
        if (!state.getValue(GymLeaderTeleporterBlock.PORTAL_OPEN)) return

        // Calculer l'âge de l'animation en prenant en compte la fermeture
        val portalTicks = blockEntity.portalTicks
        val animationAge: Float
        if (portalTicks <= 15) {
            // Phase de fermeture (compte à rebours inversé)
            animationAge = Math.max(0f, portalTicks - partialTick)
        } else {
            // Phase d'ouverture / stabilisation
            animationAge = blockEntity.clientTicks + partialTick
        }

        // Calculer les échelles X et Y selon les 3 phases de l'animation de type TVA
        val scaleX: Float
        val scaleY: Float
        if (animationAge < 5f) {
            // Phase 1 : Ligne verticale fine apparaissant instantanément
            scaleY = animationAge / 5f
            scaleX = 0.01f
        } else if (animationAge < 15f) {
            // Phase 2 : Expansion horizontale (Slash)
            scaleY = 1.0f
            val t = (animationAge - 5f) / 10f
            scaleX = 0.01f + t * 0.99f
        } else {
            // Phase 3 : Stabilisation fixe avec léger mouvement organique
            scaleY = 1.0f
            scaleX = 1.0f + (sin(animationAge * 0.2f) * 0.02f)
        }

        // Rendu du Timer au-dessus du portail
        if (portalTicks > 0) {
            val remainingSeconds = (portalTicks + 19) / 20
            val text = "$remainingSeconds s"
            
            poseStack.pushPose()
            // Placé au-dessus du portail (élevé de 2.5m par rapport à la base)
            poseStack.translate(0.5, 2.5, 0.5)
            
            // Face the camera (billboard)
            val camera = net.minecraft.client.Minecraft.getInstance().entityRenderDispatcher.camera
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-camera.yRot))
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(camera.xRot))
            
            // Scale text to world coordinates (negative scale to invert Y)
            poseStack.scale(-0.025f, -0.025f, 0.025f)
            
            val font = context.font
            val textWidth = font.width(text)
            val matrix = poseStack.last().pose()
            val xOffset = -textWidth / 2f
            
            font.drawInBatch(
                text,
                xOffset,
                0f,
                0xFFFFA800.toInt(), // Beautiful gold color matching the portal
                true, // Draw with shadow
                matrix,
                bufferSource,
                Font.DisplayMode.NORMAL,
                0, // Transparent background
                packedLight
            )
            poseStack.popPose()
        }

        poseStack.pushPose()
        
        // Placer au centre du bloc, élevé de 5 pixels (3px base + 2px tweak = 5px = 0.3125)
        poseStack.translate(0.5, 0.3125, 0.5)

        // Obtenir l'orientation fixe depuis le BlockState (au lieu du billboarding)
        val facing = state.getValue(GymLeaderTeleporterBlock.FACING)
        val rotation = when (facing) {
            Direction.NORTH -> 180f
            Direction.SOUTH -> 0f
            Direction.EAST -> 270f
            Direction.WEST -> 90f
            else -> 0f
        }
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rotation))

        // Échelle du portail (taille humaine : 1.2m de large, 2.0m de haut, épaisseur de 6 pixels = 0.375f)
        val width = 1.2f
        val height = 2.0f
        val thickness = 0.375f
        poseStack.scale(scaleX * width, scaleY * height, scaleX * thickness)

        val last = poseStack.last()
        val pose = last.pose()
        val light = LightTexture.FULL_BRIGHT

        // Définir les dimensions de la boîte unité (-0.5 à 0.5 en X et Z, 0.0 à 1.0 en Y)
        val minX = -0.5f
        val maxX = 0.5f
        val minY = 0.0f
        val maxY = 1.0f
        val minZ = -0.5f
        val maxZ = 0.5f

        // 1. Récupérer le VertexConsumer principal et dessiner les faces avant/arrière
        val consumerMain = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(PORTAL_TEXTURE))

        // Face avant (facing +Z)
        vertex(consumerMain, pose, last, minX, minY, maxZ, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, light)
        vertex(consumerMain, pose, last, maxX, minY, maxZ, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, light)
        vertex(consumerMain, pose, last, maxX, maxY, maxZ, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f, light)
        vertex(consumerMain, pose, last, minX, maxY, maxZ, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, light)

        // Face arrière (facing -Z)
        vertex(consumerMain, pose, last, maxX, minY, minZ, 0.0f, 1.0f, 0.0f, 0.0f, -1.0f, light)
        vertex(consumerMain, pose, last, minX, minY, minZ, 1.0f, 1.0f, 0.0f, 0.0f, -1.0f, light)
        vertex(consumerMain, pose, last, minX, maxY, minZ, 1.0f, 0.0f, 0.0f, 0.0f, -1.0f, light)
        vertex(consumerMain, pose, last, maxX, maxY, minZ, 0.0f, 0.0f, 0.0f, 0.0f, -1.0f, light)

        // 2. Récupérer le VertexConsumer secondaire pour les tranches et les dessiner
        val consumerSide = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(PORTAL_SIDE_TEXTURE))

        // Face gauche (facing -X)
        vertex(consumerSide, pose, last, minX, minY, minZ, 0.0f, 1.0f, -1.0f, 0.0f, 0.0f, light)
        vertex(consumerSide, pose, last, minX, minY, maxZ, 1.0f, 1.0f, -1.0f, 0.0f, 0.0f, light)
        vertex(consumerSide, pose, last, minX, maxY, maxZ, 1.0f, 0.0f, -1.0f, 0.0f, 0.0f, light)
        vertex(consumerSide, pose, last, minX, maxY, minZ, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, light)

        // Face droite (facing +X)
        vertex(consumerSide, pose, last, maxX, minY, maxZ, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, light)
        vertex(consumerSide, pose, last, maxX, minY, minZ, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, light)
        vertex(consumerSide, pose, last, maxX, maxY, minZ, 1.0f, 0.0f, 1.0f, 0.0f, 0.0f, light)
        vertex(consumerSide, pose, last, maxX, maxY, maxZ, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, light)

        // Face supérieure (facing +Y)
        vertex(consumerSide, pose, last, minX, maxY, maxZ, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f, light)
        vertex(consumerSide, pose, last, maxX, maxY, maxZ, 1.0f, 1.0f, 0.0f, 1.0f, 0.0f, light)
        vertex(consumerSide, pose, last, maxX, maxY, minZ, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, light)
        vertex(consumerSide, pose, last, minX, maxY, minZ, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, light)

        // Face inférieure (facing -Y)
        vertex(consumerSide, pose, last, minX, minY, minZ, 0.0f, 1.0f, 0.0f, -1.0f, 0.0f, light)
        vertex(consumerSide, pose, last, maxX, minY, minZ, 1.0f, 1.0f, 0.0f, -1.0f, 0.0f, light)
        vertex(consumerSide, pose, last, maxX, minY, maxZ, 1.0f, 0.0f, 0.0f, -1.0f, 0.0f, light)
        vertex(consumerSide, pose, last, minX, minY, maxZ, 0.0f, 0.0f, 0.0f, -1.0f, 0.0f, light)

        poseStack.popPose()
    }

    private fun vertex(
        consumer: VertexConsumer,
        pose: Matrix4f,
        poseEntry: PoseStack.Pose,
        x: Float, y: Float, z: Float,
        u: Float, v: Float,
        nx: Float, ny: Float, nz: Float,
        light: Int
    ) {
        consumer.addVertex(pose, x, y, z)
            .setColor(255, 255, 255, 255)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(poseEntry, nx, ny, nz)
    }
}
