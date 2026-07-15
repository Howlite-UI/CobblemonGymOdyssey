package com.howlite.mixin;

import com.howlite.events.TeleportAnimationServer;
import net.blay09.mods.waystones.api.TeleportDestination;
import net.blay09.mods.waystones.api.WaystoneTeleportContext;
import net.blay09.mods.waystones.core.WaystoneTeleportManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.concurrent.CompletableFuture;

/**
 * Mixin optionnel sur WaystoneTeleportManager.doTeleport.
 *
 * Intercepte la teleportation pour les joueurs et demarre le delay / l animation
 * Pokemon B&W avant d executer la teleportation reelle.
 * Bloque la TP si le joueur n a pas de Pokemon rideable (comme le field move Vol en Gen 5).
 */
@Mixin(WaystoneTeleportManager.class)
public class WaystoneTeleportManagerMixin {

    @Inject(
        method = "tryTeleportAsync(Lnet/blay09/mods/waystones/api/WaystoneTeleportContext;)Ljava/util/concurrent/CompletableFuture;",
        at = @At("HEAD"),
        cancellable = true,
        require = 0,
        remap = false
    )
    private static void onTryTeleportAsync(
        WaystoneTeleportContext context,
        CallbackInfoReturnable<CompletableFuture<net.blay09.mods.waystones.api.WaystoneTeleportResult>> cir
    ) {
        System.out.println("[WaystoneMixin] onTryTeleportAsync called");
        Entity entity = context.getEntity();
        System.out.println("[WaystoneMixin] entity=" + entity);
        if (!(entity instanceof ServerPlayer player)) {
            System.out.println("[WaystoneMixin] NOT a ServerPlayer, skipping");
            return;
        }

        // Si nous sommes deja en train d executer la TP differee, on laisse faire
        if (TeleportAnimationServer.INSTANCE.getExecutingWaystoneTeleports().contains(player.getUUID())) {
            System.out.println("[WaystoneMixin] Already executing waystone TP, letting through");
            return;
        }

        // Resoudre la destination à partir du Waystone cible
        net.blay09.mods.waystones.api.Waystone targetWaystone = context.getTargetWaystone();
        System.out.println("[WaystoneMixin] targetWaystone=" + targetWaystone);
        if (targetWaystone == null) {
            System.out.println("[WaystoneMixin] targetWaystone is null, skipping");
            return;
        }
        java.util.Optional<TeleportDestination> destinationOpt = WaystoneTeleportManager.resolveDefaultDestination(player.serverLevel(), targetWaystone);
        System.out.println("[WaystoneMixin] destinationOpt present=" + destinationOpt.isPresent());
        if (destinationOpt.isEmpty()) {
            System.out.println("[WaystoneMixin] destination empty, skipping");
            return;
        }
        TeleportDestination destination = destinationOpt.get();

        // Verifier si la dimension de destination est eligible
        if (destination.level() instanceof ServerLevel targetLevel) {
            boolean eligible = TeleportAnimationServer.INSTANCE.isDimensionEligible(targetLevel);
            System.out.println("[WaystoneMixin] dimension eligible=" + eligible + " dim=" + targetLevel.dimension().location());
            if (!eligible) {
                return;
            }
        }

        // Si le joueur est deja en train de se teleporter, on ignore
        if (TeleportAnimationServer.INSTANCE.isTeleporting(player.getUUID())) {
            System.out.println("[WaystoneMixin] already teleporting, skipping");
            return;
        }

        // ── Vérifier si le joueur possède un Pokémon rideable ────────────────
        boolean hasRideable = TeleportAnimationServer.INSTANCE.hasRideablePokemon(player);
        System.out.println("[WaystoneMixin] hasRideablePokemon=" + hasRideable);
        if (!hasRideable) {
            Component msg = Component.translatable(
                "cobblemongymodyssey.teleport.requires_rideable"
            ).withStyle(Style.EMPTY
                .withColor(TextColor.fromRgb(0xFF4444))
                .withItalic(true)
            );
            // Annuler la téléportation en renvoyant un résultat d'échec contenant l'erreur
            cir.setReturnValue(CompletableFuture.completedFuture(
                net.blay09.mods.waystones.api.WaystoneTeleportResult.failed(
                    new net.blay09.mods.waystones.api.error.WaystoneTeleportError(msg)
                )
            ));
            cir.cancel();
            return;
        }

        System.out.println("[WaystoneMixin] Starting waystone animation for player " + player.getName().getString());
        // Lancer l animation de teleportation et de zoom-up (Pokemon B&W Vol)
        TeleportAnimationServer.INSTANCE.startWaystoneAnimation(player, context, destination);

        // Annuler la TP immediate et renvoyer un CompletableFuture complété avec résultat vide
        cir.setReturnValue(CompletableFuture.completedFuture(
            new net.blay09.mods.waystones.api.WaystoneTeleportResult(new java.util.ArrayList<>())
        ));
        cir.cancel();
    }
}