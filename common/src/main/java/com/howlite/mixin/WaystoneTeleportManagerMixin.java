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

        // ── Vérifier si le joueur possède un Pokémon rideable ────────────────
        // Comme dans Pokémon B&W, seuls les dresseurs ayant un Pokémon capable
        // de voler (rideable) peuvent utiliser les Waystones pour se téléporter.
        if (!TeleportAnimationServer.INSTANCE.hasRideablePokemon(player)) {
            Component msg = Component.translatable(
                "cobblemongymodyssey.teleport.requires_rideable"
            ).withStyle(Style.EMPTY
                .withColor(TextColor.fromRgb(0xFF4444))
                .withItalic(true)
            );
            player.sendSystemMessage(msg);
            // Annuler la téléportation sans effet de bord
            cir.setReturnValue(com.mojang.datafixers.util.Either.left(new java.util.ArrayList<>()));
            cir.cancel();
            return;
        }

        // Lancer l animation de teleportation et de zoom-up (Pokemon B&W Vol)
        TeleportAnimationServer.INSTANCE.startWaystoneAnimation(player, context, destination);

        // Annuler la TP immediate et renvoyer une reussite fictive
        cir.setReturnValue(com.mojang.datafixers.util.Either.left(new java.util.ArrayList<>()));
        cir.cancel();
    }
}