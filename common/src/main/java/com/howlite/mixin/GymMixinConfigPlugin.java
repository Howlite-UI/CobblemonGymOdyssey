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

    private static boolean checkWaystones() {
        // Class-load check for Waystones API (extremely robust and works across all loaders)
        try {
            Class.forName("net.blay09.mods.waystones.api.WaystoneTeleportContext");
            return true;
        } catch (Throwable ignored) {}

        // Check Fabric
        try {
            Class<?> fabricLoaderClass = Class.forName("net.fabricmc.loader.api.FabricLoader");
            Object fabricLoader = fabricLoaderClass.getMethod("getInstance").invoke(null);
            boolean isLoaded = (boolean) fabricLoaderClass.getMethod("isModLoaded", String.class).invoke(fabricLoader, "waystones");
            if (isLoaded) return true;
        } catch (Throwable ignored) {}

        // Check NeoForge / Forge
        try {
            Class<?> fmlLoaderClass = Class.forName("net.neoforged.fml.loading.FMLLoader");
            Object loadingModList = fmlLoaderClass.getMethod("getLoadingModList").invoke(null);
            Object modFile = loadingModList.getClass().getMethod("getModFileById", String.class).invoke(loadingModList, "waystones");
            if (modFile != null) return true;
        } catch (Throwable ignored) {}

        try {
            Class<?> modListClass = Class.forName("net.minecraftforge.fml.ModList");
            Object modList = modListClass.getMethod("get").invoke(null);
            boolean isLoaded = (boolean) modListClass.getMethod("isLoaded", String.class).invoke(modList, "waystones");
            if (isLoaded) return true;
        } catch (Throwable ignored) {}

        // Fallback to getResource check
        try {
            String resourcePath = "net/blay09/mods/waystones/core/WaystoneTeleportManager.class";
            if (GymMixinConfigPlugin.class.getClassLoader().getResource(resourcePath) != null) {
                return true;
            }
        } catch (Throwable ignored) {}

        return false;
    }

    @Override
    public void onLoad(String mixinPackage) {
        isWaystonesPresent = checkWaystones();
        System.out.println("[CobblemonGymOdyssey MixinConfig] onLoad: isWaystonesPresent=" + isWaystonesPresent);
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.contains("Waystone")) {
            boolean apply = isWaystonesPresent;
            System.out.println("[CobblemonGymOdyssey MixinConfig] shouldApplyMixin: target=" + targetClassName + " mixin=" + mixinClassName + " apply=" + apply);
            return apply;
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