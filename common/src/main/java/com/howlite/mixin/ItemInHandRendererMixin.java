package com.howlite.mixin;

import com.howlite.client.render.TeleportAnimationClient;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Supprime le rendu de la main / item en main pendant l animation de teleportation.
 * Completement le camera.isDetached() override pour les cas ou le rendu de la main
 * serait quand meme tente.
 */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

    @Inject(method = "renderHandsWithItems", at = @At("HEAD"), cancellable = true)
    private void onRenderHandsWithItems(
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource.BufferSource bufferSource,
        LocalPlayer localPlayer,
        int combinedLight,
        CallbackInfo ci
    ) {
        if (TeleportAnimationClient.INSTANCE.getPhase() != TeleportAnimationClient.TeleportPhase.NONE) {
            ci.cancel();
        }
    }
}