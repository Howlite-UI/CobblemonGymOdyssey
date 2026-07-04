package com.howlite.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Charge dynamiquement les Mixins de Waystones uniquement si le mod Waystones est present.
 * Utilise getResource pour eviter d'appeler Class.forName, ce qui chargerait
 * la classe trop tot et provoquerait un crash MixinTargetAlreadyLoadedException.
 */
public class GymMixinConfigPlugin implements IMixinConfigPlugin {
    private static boolean isWaystonesPresent = false;

    @Override
    public void onLoad(String mixinPackage) {
        // Recherche du fichier de classe sans forcer son chargement dans la JVM
        String resourcePath = "net/blay09/mods/waystones/core/WaystoneTeleportManager.class";
        isWaystonesPresent = this.getClass().getClassLoader().getResource(resourcePath) != null;
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.contains("Waystone")) {
            return isWaystonesPresent;
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}