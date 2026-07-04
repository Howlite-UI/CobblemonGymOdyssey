package com.howlite.mixin;

import com.howlite.events.TeleportAnimationServer;
import net.blay09.mods.waystones.api.TeleportDestination;
import net.blay09.mods.waystones.api.WaystoneTeleportContext;
import net.blay09.mods.waystones.core.WaystoneTeleportManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin optionnel sur WaystoneTeleportManager.doTeleport.
 *
 * Intercepte la teleportation pour les joueurs et demarre le delay / l animation
 * GTA 5 de 1.5s avant d executer la teleportation reelle.
 */
@Mixin(WaystoneTeleportManager.class)
public class WaystoneTeleportManagerMixin {

    @Inject(
        method = "doTeleport(Lnet/blay09/mods/waystones/api/WaystoneTeleportContext;Lnet/blay09/mods/waystones/api/TeleportDestination;)Lcom/mojang/datafixers/util/Either;",
        at = @At("HEAD"),
        cancellable = true,
        require = 0,
        remap = false
    )
    private static void onDoTeleport(
        WaystoneTeleportContext context,
        TeleportDestination destination,
        CallbackInfoReturnable<com.mojang.datafixers.util.Either<?, ?>> cir
    ) {
        Entity entity = context.getEntity();
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }

        // Si nous sommes deja en train d executer la TP differee, on laisse faire
        if (TeleportAnimationServer.INSTANCE.getExecutingWaystoneTeleports().contains(player.getUUID())) {
            return;
        }

        // Verifier si la dimension de destination est eligible
        if (destination.level() instanceof ServerLevel targetLevel) {
            if (!TeleportAnimationServer.INSTANCE.isDimensionEligible(targetLevel)) {
                return;
            }
        }

        // Si le joueur est deja en train de se teleporter, on ignore
        if (TeleportAnimationServer.INSTANCE.isTeleporting(player.getUUID())) {
            return;
        }

        // Lancer l animation de teleportation et de zoom-up (GTA 5 / Pokemon Vol)
        TeleportAnimationServer.INSTANCE.startWaystoneAnimation(player, context, destination);

        // Annuler la TP immediate et renvoyer une reussite fictive (liste vide d entites TP pour l instant)
        cir.setReturnValue(com.mojang.datafixers.util.Either.left(new java.util.ArrayList<>()));
        cir.cancel();
    }
}