package com.howlite.mixin;

import com.howlite.events.TeleportAnimationServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepte les methodes de teleportation sur Entity pour couvrir les cas
 * non captures par ServerPlayerMixin (notamment Waystones/Balm) :
 *
 *  1) teleportTo(ServerLevel, double, double, double, float, float) ? surcharge void
 *     presente sur ServerPlayer mais sans mapping SRG/intermediary sur Entity.
 *     require=0 : optionnel ? si introuvable au runtime, ignore silencieusement.
 *
 *  2) teleportTo(double, double, double) ? teleportation intra-dimension simple
 *     avec seuil de distance pour ignorer les corrections physiques mineures.
 */
@Mixin(Entity.class)
public abstract class EntityMixin {

    // ----------------------------------------------------------------
    // Surcharge void avec ServerLevel (Waystones/Balm code path)
    // require=0 pour etre tolerant si le remapper ne trouve pas la methode
    // ----------------------------------------------------------------
    @Inject(
        method = "teleportTo(Lnet/minecraft/server/level/ServerLevel;DDFF)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void onTeleportToServerLevel(
        ServerLevel targetLevel,
        double x, double y, double z,
        float yaw, float pitch,
        CallbackInfo ci
    ) {
        Object self = this;
        if (!(self instanceof ServerPlayer player)) return;
        if (!shouldAnimate(player, targetLevel)) return;

        TeleportAnimationServer.INSTANCE.startAnimation(player, targetLevel, x, y, z, yaw, pitch);
        ci.cancel();
    }

    // ----------------------------------------------------------------
    // Surcharge simple sans dimension (teleportation intra-dimension)
    // ----------------------------------------------------------------
    @Inject(method = "teleportTo(DDD)V", at = @At("HEAD"), cancellable = true)
    private void onTeleportToSimple(double x, double y, double z, CallbackInfo ci) {
        Object self = this;
        if (!(self instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel serverLevel)) return;
        if (!TeleportAnimationServer.INSTANCE.isDimensionEligible(serverLevel)) return;

        // Ignore les corrections de position serveur courtes (<= 20 blocs)
        if (TeleportAnimationServer.INSTANCE.getExecutingDelayedTeleports().contains(player.getUUID())) return;
        if (TeleportAnimationServer.INSTANCE.isTeleporting(player.getUUID())) return;

        double dx = x - player.getX();
        double dz = z - player.getZ();
        if (dx * dx + dz * dz <= 400.0) return;

        TeleportAnimationServer.INSTANCE.startAnimationFromEntity(player, x, y, z);
        ci.cancel();
    }

    // ----------------------------------------------------------------
    // Helper commun
    // ----------------------------------------------------------------
    private static boolean shouldAnimate(ServerPlayer player, ServerLevel targetLevel) {
        if (TeleportAnimationServer.INSTANCE.getExecutingDelayedTeleports().contains(player.getUUID())) return false;
        if (!TeleportAnimationServer.INSTANCE.isDimensionEligible(targetLevel)) return false;
        if (TeleportAnimationServer.INSTANCE.isTeleporting(player.getUUID())) return false;
        return true;
    }
}