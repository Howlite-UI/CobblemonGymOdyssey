package com.howlite.items

import com.howlite.CobblemonGymOdyssey
import com.howlite.data.GymBadge
import dev.architectury.registry.CreativeTabRegistry
import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack

/**
 * Enregistrement cross-platform des 8 Items Badge via Architectury.
 *
 * - Les items sont enregistrés via [DeferredRegister] (Fabric + NeoForge).
 * - L'onglet créatif est créé via [DeferredRegister] pour les onglets créatifs.
 */
object GymBadgeItems {

    private val ITEMS: DeferredRegister<Item> =
        DeferredRegister.create(CobblemonGymOdyssey.MOD_ID, Registries.ITEM)

    private val TABS: DeferredRegister<CreativeModeTab> =
        DeferredRegister.create(CobblemonGymOdyssey.MOD_ID, Registries.CREATIVE_MODE_TAB)

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

    /** La Boîte à Badges — s'ouvre en clic droit pour afficher les badges du joueur. */
    val BADGE_CASE: RegistrySupplier<Item> = ITEMS.register("badge_case") {
        BadgeCaseItem(Item.Properties().stacksTo(1))
    }

    // -------------------------------------------------------------------------
    // Tickets d'Arène spécifiques
    // -------------------------------------------------------------------------
    val BOULDER_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_boulder_badge") {
        GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.BOULDER_BADGE)
    }
    val CASCADE_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_cascade_badge") {
        GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.CASCADE_BADGE)
    }
    val THUNDER_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_thunder_badge") {
        GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.THUNDER_BADGE)
    }
    val RAINBOW_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_rainbow_badge") {
        GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.RAINBOW_BADGE)
    }
    val SOUL_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_soul_badge") {
        GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.SOUL_BADGE)
    }
    val MARSH_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_marsh_badge") {
        GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.MARSH_BADGE)
    }
    val VOLCANO_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_volcano_badge") {
        GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.VOLCANO_BADGE)
    }
    val EARTH_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_earth_badge") {
        GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.EARTH_BADGE)
    }

    // -------------------------------------------------------------------------
    // Onglet créatif "Gym Badges"
    // -------------------------------------------------------------------------

    val GYM_BADGES_TAB: RegistrySupplier<CreativeModeTab> = TABS.register("gym_badges") {
        CreativeTabRegistry.create(
            Component.translatable("itemGroup.cobblemongymodyssey.gym_badges")
        ) { ItemStack(BOULDER_BADGE.get()) }
    }

    /**
     * Retourne l'Item correspondant à un badge donné.
     */
    fun getItemForBadge(badge: GymBadge): Item {
        return when (badge) {
            GymBadge.BOULDER_BADGE -> BOULDER_BADGE.get()
            GymBadge.CASCADE_BADGE -> CASCADE_BADGE.get()
            GymBadge.THUNDER_BADGE -> THUNDER_BADGE.get()
            GymBadge.RAINBOW_BADGE -> RAINBOW_BADGE.get()
            GymBadge.SOUL_BADGE -> SOUL_BADGE.get()
            GymBadge.MARSH_BADGE -> MARSH_BADGE.get()
            GymBadge.VOLCANO_BADGE -> VOLCANO_BADGE.get()
            GymBadge.EARTH_BADGE -> EARTH_BADGE.get()
        }
    }

    // -------------------------------------------------------------------------
    // Point d'entrée appelé depuis CobblemonGymOdyssey.init()
    // -------------------------------------------------------------------------

    fun register() {
        ITEMS.register()
        TABS.register()

        // Ajouter les badges, la boîte et les tickets à l'onglet
        CreativeTabRegistry.append(
            GYM_BADGES_TAB,
            BOULDER_BADGE, CASCADE_BADGE, THUNDER_BADGE, RAINBOW_BADGE,
            SOUL_BADGE, MARSH_BADGE, VOLCANO_BADGE, EARTH_BADGE,
            BADGE_CASE,
            BOULDER_TICKET, CASCADE_TICKET, THUNDER_TICKET, RAINBOW_TICKET,
            SOUL_TICKET, MARSH_TICKET, VOLCANO_TICKET, EARTH_TICKET
        )
    }
}
