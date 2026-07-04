package com.howlite.mixin;

import com.howlite.events.TeleportAnimationServer;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

/**
 * Injecte un tick dans MinecraftServer.tickChildren pour mettre a jour
 * les animations de teleportation en attente de maniere synchrone.
 */
@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {

    @Inject(method = "tickChildren", at = @At("TAIL"))
    private void onTickChildren(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        TeleportAnimationServer.INSTANCE.tick((MinecraftServer) (Object) this);
    }
}