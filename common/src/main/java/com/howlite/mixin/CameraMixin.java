package com.howlite.mixin;

import com.howlite.client.render.TeleportAnimationClient;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin client sur Camera.
 *
 * 1) onSetup : deplace la camera verticalement et modifie son pitch pendant l animation.
 * 2) onIsDetached : renvoie true pendant l animation pour :
 *    - rendre le corps du joueur visible depuis le dessus,
 *    - supprimer le rendu de la main en vue a la premiere personne.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Shadow
    public abstract Vec3 getPosition();

    @Shadow
    public abstract float getXRot();

    @Shadow
    public abstract float getYRot();

    @Inject(method = "setup", at = @At("TAIL"))
    private void onSetup(BlockGetter level, Entity entity, boolean thirdPerson,
                         boolean inverseView, float partialTicks, CallbackInfo ci) {

        if (TeleportAnimationClient.INSTANCE.getPhase() == TeleportAnimationClient.TeleportPhase.NONE) {
            return;
        }

        Vec3 currentPos = this.getPosition();
        double[] pos = { currentPos.x, currentPos.y, currentPos.z };
        float[] rot = { this.getYRot(), this.getXRot() };

        if (TeleportAnimationClient.INSTANCE.calculateCameraModifiers(pos, rot, partialTicks)) {
            this.setPosition(pos[0], pos[1], pos[2]);
            this.setRotation(rot[0], rot[1]);
        }
    }

    /**
     * Override isDetached() to return true during the animation so that:
     *  - The local player body is rendered (visible from above).
     *  - The first-person arm/hand is suppressed.
     */
    @Inject(method = "isDetached", at = @At("RETURN"), cancellable = true)
    private void onIsDetached(CallbackInfoReturnable<Boolean> cir) {
        if (TeleportAnimationClient.INSTANCE.getPhase() != TeleportAnimationClient.TeleportPhase.NONE) {
            cir.setReturnValue(true);
        }
    }
}