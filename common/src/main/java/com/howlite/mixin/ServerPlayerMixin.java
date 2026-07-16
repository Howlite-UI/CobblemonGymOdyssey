package com.howlite.mixin;

import com.howlite.events.TeleportAnimationServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.RelativeMovement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

/**
 * Intercepte ServerPlayer.teleportTo(ServerLevel, double, double, double, Set, float, float)
 * (la surcharge booleenne, propre a ServerPlayer) pour declencher l animation de teleportation.
 *
 * La surcharge void teleportTo(ServerLevel, x, y, z, yaw, pitch) est definie
 * sur Entity (pas ServerPlayer) et est interceptee dans EntityMixin.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    @Inject(
        method = "teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FF)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onTeleportTo(
        ServerLevel targetLevel,
        double x, double y, double z,
        Set<RelativeMovement> relativeMovements,
        float yaw, float pitch,
        CallbackInfoReturnable<Boolean> cir
    ) {
        ServerPlayer self = (ServerPlayer) (Object) this;

        if (TeleportAnimationServer.INSTANCE.getExecutingDelayedTeleports().contains(self.getUUID())) return;
        if (!TeleportAnimationServer.INSTANCE.isDimensionEligible(targetLevel)) return;
        if (TeleportAnimationServer.INSTANCE.isTeleporting(self.getUUID())) return;

        // ── Filtre Elevator : deplacement purement vertical (meme X et Z, meme dimension) ──
        // L'elevator de OpenBlocks ne change que le Y, jamais X ou Z.
        boolean sameDimension = self.level().dimension().equals(targetLevel.dimension());
        double dx = Math.abs(x - self.getX());
        double dz = Math.abs(z - self.getZ());
        if (sameDimension && dx < 0.5 && dz < 0.5) return;

        // ── Filtre Hypertube : teleportation intra-dimension sur courte distance ──
        // Create Hypertubes deplacent le joueur en petites etapes rapides.
        // On ignore les TP intra-dimension sous 20 blocs de distance horizontale.
        double horizontalDistSq = dx * dx + dz * dz;
        if (sameDimension && horizontalDistSq < 400.0) return;

        TeleportAnimationServer.INSTANCE.startAnimation(self, targetLevel, x, y, z, yaw, pitch);
        cir.setReturnValue(false);
        cir.cancel();
    }
}