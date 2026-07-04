package com.howlite.mixin;

import com.howlite.client.render.TeleportAnimationClient;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin sur net.minecraft.client.gui.Gui.
 * Pendant l'animation de teleportation :
 *  - Annule le rendu COMPLET du HUD (hotbar, vie, XP, party Cobblemon, crosshair, etc.)
 *  - Rend uniquement l'overlay de teleportation par-dessus la scene 3D.
 */
@Mixin(Gui.class)
public abstract class GuiMixin {

    /**
     * Intercepte Gui.render() au HEAD pour bloquer tout le HUD pendant l'animation
     * et dessiner uniquement notre overlay de teleportation.
     */
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (TeleportAnimationClient.INSTANCE.getPhase() != TeleportAnimationClient.TeleportPhase.NONE) {
            // Dessine notre overlay (lignes de vitesse, flash blanc, etc.)
            TeleportAnimationClient.INSTANCE.renderOverlay(guiGraphics);
            // Annule tout le reste du rendu HUD
            ci.cancel();
        }
    }
}