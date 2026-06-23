package com.howlite.menu

import com.howlite.CobblemonGymOdyssey
import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
import net.minecraft.core.registries.Registries
import net.minecraft.world.flag.FeatureFlags
import net.minecraft.world.inventory.MenuType

/**
 * Enregistrement du [MenuType] de la Boîte à Badges via Architectury DeferredRegister.
 *
 * Le [MenuType] est partagé Common (Fabric + NeoForge).
 * L'enregistrement côté client du Screen se fait dans chaque module plateforme.
 */
object BadgeCaseMenus {

    val MENUS: DeferredRegister<MenuType<*>> =
        DeferredRegister.create(CobblemonGymOdyssey.MOD_ID, Registries.MENU)

    /**
     * Le [MenuType] de la Boîte à Badges.
     *
     * Le constructeur client reçoit uniquement le syncId et un inventaire vide,
     * les données réelles (badges débloqués) arrivent via [FriendlyByteBuf]
     * dans [BadgeCaseMenu].
     */
    val BADGE_CASE_MENU_TYPE: RegistrySupplier<MenuType<BadgeCaseMenu>> =
        MENUS.register("badge_case") {
            dev.architectury.registry.menu.MenuRegistry.ofExtended { syncId, _, buf ->
                BadgeCaseMenu(syncId, buf)
            }
        }

    val GYM_SHOP_MENU_TYPE: RegistrySupplier<MenuType<GymShopMenu>> =
        MENUS.register("gym_shop") {
            dev.architectury.registry.menu.MenuRegistry.ofExtended { syncId, inv, buf ->
                GymShopMenu(syncId, inv, buf)
            }
        }

    val GYM_SHOP_EDIT_MENU_TYPE: RegistrySupplier<MenuType<GymShopEditMenu>> =
        MENUS.register("gym_shop_edit") {
            dev.architectury.registry.menu.MenuRegistry.ofExtended { syncId, inv, buf ->
                GymShopEditMenu(syncId, inv, buf)
            }
        }

    /** Appelé depuis [com.howlite.CobblemonGymOdyssey.init]. */
    fun register() {
        MENUS.register()
    }
}
