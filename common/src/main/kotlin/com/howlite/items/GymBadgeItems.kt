package com.howlite.items

import com.howlite.CobblemonGymOdyssey
import dev.architectury.registry.CreativeTabRegistry
import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.DeferredSupplier
import dev.architectury.registry.registries.RegistrySupplier
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack

/**
 * Enregistrement cross-platform des 8 Items Badge via Architectury.
 *
 * - Les items sont enregistrés via [DeferredRegister] (Fabric + NeoForge).
 * - L'onglet créatif est créé via [CreativeTabRegistry.create] — l'API
 *   Architectury dédiée aux creative tabs, qui évite les problèmes de
 *   compatibilité SAM Kotlin/Java avec MC 1.21.1.
 *
 * ## Commandes utiles en jeu
 * `/give @p cobblemongymodyssey:boulder_badge`
 */
object GymBadgeItems {

    private val ITEMS: DeferredRegister<Item> =
        DeferredRegister.create(CobblemonGymOdyssey.MOD_ID, Registries.ITEM)

    // -------------------------------------------------------------------------
    // Items Badge
    // -------------------------------------------------------------------------

    val BOULDER_BADGE: RegistrySupplier<Item> = ITEMS.register("boulder_badge") {
        BadgeItem(Item.Properties().stacksTo(1))
    }
    val CASCADE_BADGE: RegistrySupplier<Item> = ITEMS.register("cascade_badge") {
        BadgeItem(Item.Properties().stacksTo(1))
    }
    val THUNDER_BADGE: RegistrySupplier<Item> = ITEMS.register("thunder_badge") {
        BadgeItem(Item.Properties().stacksTo(1))
    }
    val RAINBOW_BADGE: RegistrySupplier<Item> = ITEMS.register("rainbow_badge") {
        BadgeItem(Item.Properties().stacksTo(1))
    }
    val SOUL_BADGE: RegistrySupplier<Item> = ITEMS.register("soul_badge") {
        BadgeItem(Item.Properties().stacksTo(1))
    }
    val MARSH_BADGE: RegistrySupplier<Item> = ITEMS.register("marsh_badge") {
        BadgeItem(Item.Properties().stacksTo(1))
    }
    val VOLCANO_BADGE: RegistrySupplier<Item> = ITEMS.register("volcano_badge") {
        BadgeItem(Item.Properties().stacksTo(1))
    }
    val EARTH_BADGE: RegistrySupplier<Item> = ITEMS.register("earth_badge") {
        BadgeItem(Item.Properties().stacksTo(1))
    }

    // -------------------------------------------------------------------------
    // Onglet créatif "Gym Badges" — créé via CreativeTabRegistry Architectury
    // -------------------------------------------------------------------------

    /**
     * Référence lazy à l'onglet créatif (DeferredSupplier).
     * Initialisée dans [register].
     */
    lateinit var GYM_BADGES_TAB: DeferredSupplier<CreativeModeTab>
        private set

    // -------------------------------------------------------------------------
    // Point d'entrée appelé depuis CobblemonGymOdyssey.init()
    // -------------------------------------------------------------------------

    fun register() {
        ITEMS.register()

        // Référence différée sur l'onglet (par ResourceLocation)
        GYM_BADGES_TAB = CreativeTabRegistry.defer(
            ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "gym_badges")
        )

        // Créer l'onglet créatif via l'API Architectury (cross-platform)
        CreativeTabRegistry.create { builder: CreativeModeTab.Builder ->
            builder
                .title(Component.translatable("itemGroup.cobblemongymodyssey.gym_badges"))
                .icon { ItemStack(BOULDER_BADGE.get()) }
        }

        // Ajouter les 8 badges à l'onglet
        CreativeTabRegistry.append(
            GYM_BADGES_TAB,
            BOULDER_BADGE, CASCADE_BADGE, THUNDER_BADGE, RAINBOW_BADGE,
            SOUL_BADGE, MARSH_BADGE, VOLCANO_BADGE, EARTH_BADGE
        )
    }
}
