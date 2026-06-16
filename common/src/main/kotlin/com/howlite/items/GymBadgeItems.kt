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

    // Johto Badges
    val ZEPHYR_BADGE: RegistrySupplier<Item> = ITEMS.register("zephyr_badge") {
        BadgeItem(Item.Properties().stacksTo(1))
    }
    val HIVE_BADGE: RegistrySupplier<Item> = ITEMS.register("hive_badge") {
        BadgeItem(Item.Properties().stacksTo(1))
    }
    val PLAIN_BADGE: RegistrySupplier<Item> = ITEMS.register("plain_badge") {
        BadgeItem(Item.Properties().stacksTo(1))
    }
    val FOG_BADGE: RegistrySupplier<Item> = ITEMS.register("fog_badge") {
        BadgeItem(Item.Properties().stacksTo(1))
    }
    val STORM_BADGE: RegistrySupplier<Item> = ITEMS.register("storm_badge") {
        BadgeItem(Item.Properties().stacksTo(1))
    }
    val MINERAL_BADGE: RegistrySupplier<Item> = ITEMS.register("mineral_badge") {
        BadgeItem(Item.Properties().stacksTo(1))
    }
    val GLACIER_BADGE: RegistrySupplier<Item> = ITEMS.register("glacier_badge") {
        BadgeItem(Item.Properties().stacksTo(1))
    }
    val RISING_BADGE: RegistrySupplier<Item> = ITEMS.register("rising_badge") {
        BadgeItem(Item.Properties().stacksTo(1))
    }

    // Hoenn Badges
    val STONE_BADGE: RegistrySupplier<Item> = ITEMS.register("stone_badge") { BadgeItem(Item.Properties().stacksTo(1)) }
    val KNUCKLE_BADGE: RegistrySupplier<Item> = ITEMS.register("knuckle_badge") { BadgeItem(Item.Properties().stacksTo(1)) }
    val DYNAMO_BADGE: RegistrySupplier<Item> = ITEMS.register("dynamo_badge") { BadgeItem(Item.Properties().stacksTo(1)) }
    val HEAT_BADGE: RegistrySupplier<Item> = ITEMS.register("heat_badge") { BadgeItem(Item.Properties().stacksTo(1)) }
    val BALANCE_BADGE: RegistrySupplier<Item> = ITEMS.register("balance_badge") { BadgeItem(Item.Properties().stacksTo(1)) }
    val FEATHER_BADGE: RegistrySupplier<Item> = ITEMS.register("feather_badge") { BadgeItem(Item.Properties().stacksTo(1)) }
    val MIND_BADGE: RegistrySupplier<Item> = ITEMS.register("mind_badge") { BadgeItem(Item.Properties().stacksTo(1)) }
    val RAIN_BADGE: RegistrySupplier<Item> = ITEMS.register("rain_badge") { BadgeItem(Item.Properties().stacksTo(1)) }

    // Sinnoh Badges
    val COAL_BADGE: RegistrySupplier<Item> = ITEMS.register("coal_badge") { BadgeItem(Item.Properties().stacksTo(1)) }
    val FOREST_BADGE: RegistrySupplier<Item> = ITEMS.register("forest_badge") { BadgeItem(Item.Properties().stacksTo(1)) }
    val COBBLE_BADGE: RegistrySupplier<Item> = ITEMS.register("cobble_badge") { BadgeItem(Item.Properties().stacksTo(1)) }
    val FEN_BADGE: RegistrySupplier<Item> = ITEMS.register("fen_badge") { BadgeItem(Item.Properties().stacksTo(1)) }
    val RELIC_BADGE: RegistrySupplier<Item> = ITEMS.register("relic_badge") { BadgeItem(Item.Properties().stacksTo(1)) }
    val MINE_BADGE: RegistrySupplier<Item> = ITEMS.register("mine_badge") { BadgeItem(Item.Properties().stacksTo(1)) }
    val ICICLE_BADGE: RegistrySupplier<Item> = ITEMS.register("icicle_badge") { BadgeItem(Item.Properties().stacksTo(1)) }
    val BEACON_BADGE: RegistrySupplier<Item> = ITEMS.register("beacon_badge") { BadgeItem(Item.Properties().stacksTo(1)) }

    // Unova Badges
    val TRIO_BADGE: RegistrySupplier<Item> = ITEMS.register("trio_badge") { BadgeItem(Item.Properties().stacksTo(1)) }
    val BASIC_BADGE: RegistrySupplier<Item> = ITEMS.register("basic_badge") { BadgeItem(Item.Properties().stacksTo(1)) }
    val TOXIC_BADGE: RegistrySupplier<Item> = ITEMS.register("toxic_badge") { BadgeItem(Item.Properties().stacksTo(1)) }
    val INSECT_BADGE: RegistrySupplier<Item> = ITEMS.register("insect_badge") { BadgeItem(Item.Properties().stacksTo(1)) }
    val BOLT_BADGE: RegistrySupplier<Item> = ITEMS.register("bolt_badge") { BadgeItem(Item.Properties().stacksTo(1)) }
    val QUAKE_BADGE: RegistrySupplier<Item> = ITEMS.register("quake_badge") { BadgeItem(Item.Properties().stacksTo(1)) }
    val JET_BADGE: RegistrySupplier<Item> = ITEMS.register("jet_badge") { BadgeItem(Item.Properties().stacksTo(1)) }
    val FREEZE_BADGE: RegistrySupplier<Item> = ITEMS.register("freeze_badge") { BadgeItem(Item.Properties().stacksTo(1)) }
    val LEGEND_BADGE: RegistrySupplier<Item> = ITEMS.register("legend_badge") { BadgeItem(Item.Properties().stacksTo(1)) }
    val WAVE_BADGE: RegistrySupplier<Item> = ITEMS.register("wave_badge") { BadgeItem(Item.Properties().stacksTo(1)) }

    // Kalos Badges
    val BUG_BADGE: RegistrySupplier<Item> = ITEMS.register("bug_badge") { BadgeItem(Item.Properties().stacksTo(1)) }
    val CLIFF_BADGE: RegistrySupplier<Item> = ITEMS.register("cliff_badge") { BadgeItem(Item.Properties().stacksTo(1)) }
    val RUMBLE_BADGE: RegistrySupplier<Item> = ITEMS.register("rumble_badge") { BadgeItem(Item.Properties().stacksTo(1)) }
    val PLANT_BADGE: RegistrySupplier<Item> = ITEMS.register("plant_badge") { BadgeItem(Item.Properties().stacksTo(1)) }
    val VOLTAGE_BADGE: RegistrySupplier<Item> = ITEMS.register("voltage_badge") { BadgeItem(Item.Properties().stacksTo(1)) }
    val KALOS_FAIRY_BADGE: RegistrySupplier<Item> = ITEMS.register("kalos_fairy_badge") { BadgeItem(Item.Properties().stacksTo(1)) }
    val PSYCHIC_BADGE: RegistrySupplier<Item> = ITEMS.register("psychic_badge") { BadgeItem(Item.Properties().stacksTo(1)) }
    val ICEBERG_BADGE: RegistrySupplier<Item> = ITEMS.register("iceberg_badge") { BadgeItem(Item.Properties().stacksTo(1)) }

    // Alola Grand Trials Stamps
    val MELEMELE_STAMP: RegistrySupplier<Item> = ITEMS.register("melemele_stamp") { BadgeItem(Item.Properties().stacksTo(1)) }
    val AKALA_STAMP: RegistrySupplier<Item> = ITEMS.register("akala_stamp") { BadgeItem(Item.Properties().stacksTo(1)) }
    val ULAULA_STAMP: RegistrySupplier<Item> = ITEMS.register("ulaula_stamp") { BadgeItem(Item.Properties().stacksTo(1)) }
    val PONI_STAMP: RegistrySupplier<Item> = ITEMS.register("poni_stamp") { BadgeItem(Item.Properties().stacksTo(1)) }

    // Galar Badges
    val GRASS_BADGE: RegistrySupplier<Item> = ITEMS.register("grass_badge") { BadgeItem(Item.Properties().stacksTo(1)) }
    val WATER_BADGE: RegistrySupplier<Item> = ITEMS.register("water_badge") { BadgeItem(Item.Properties().stacksTo(1)) }
    val FIRE_BADGE: RegistrySupplier<Item> = ITEMS.register("fire_badge") { BadgeItem(Item.Properties().stacksTo(1)) }
    val FIGHTING_BADGE: RegistrySupplier<Item> = ITEMS.register("fighting_badge") { BadgeItem(Item.Properties().stacksTo(1)) }
    val GHOST_BADGE: RegistrySupplier<Item> = ITEMS.register("ghost_badge") { BadgeItem(Item.Properties().stacksTo(1)) }
    val GALAR_FAIRY_BADGE: RegistrySupplier<Item> = ITEMS.register("galar_fairy_badge") { BadgeItem(Item.Properties().stacksTo(1)) }
    val ROCK_BADGE: RegistrySupplier<Item> = ITEMS.register("rock_badge") { BadgeItem(Item.Properties().stacksTo(1)) }
    val ICE_BADGE: RegistrySupplier<Item> = ITEMS.register("ice_badge") { BadgeItem(Item.Properties().stacksTo(1)) }
    val DARK_BADGE: RegistrySupplier<Item> = ITEMS.register("dark_badge") { BadgeItem(Item.Properties().stacksTo(1)) }
    val DRAGON_BADGE: RegistrySupplier<Item> = ITEMS.register("dragon_badge") { BadgeItem(Item.Properties().stacksTo(1)) }

    // Paldea Badges
    val CORTONDO_BADGE: RegistrySupplier<Item> = ITEMS.register("cortondo_badge") { BadgeItem(Item.Properties().stacksTo(1)) }
    val ARTAZON_BADGE: RegistrySupplier<Item> = ITEMS.register("artazon_badge") { BadgeItem(Item.Properties().stacksTo(1)) }
    val LEVINCIA_BADGE: RegistrySupplier<Item> = ITEMS.register("levincia_badge") { BadgeItem(Item.Properties().stacksTo(1)) }
    val CASCARRAFA_BADGE: RegistrySupplier<Item> = ITEMS.register("cascarrafa_badge") { BadgeItem(Item.Properties().stacksTo(1)) }
    val MEDALI_BADGE: RegistrySupplier<Item> = ITEMS.register("medali_badge") { BadgeItem(Item.Properties().stacksTo(1)) }
    val MONTENEVERA_BADGE: RegistrySupplier<Item> = ITEMS.register("montenevera_badge") { BadgeItem(Item.Properties().stacksTo(1)) }
    val ALFORNADA_BADGE: RegistrySupplier<Item> = ITEMS.register("alfornada_badge") { BadgeItem(Item.Properties().stacksTo(1)) }
    val GLASEADO_BADGE: RegistrySupplier<Item> = ITEMS.register("glaseado_badge") { BadgeItem(Item.Properties().stacksTo(1)) }

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

    // Johto Tickets
    val ZEPHYR_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_johto_zephyr_badge") {
        GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.ZEPHYR_BADGE)
    }
    val HIVE_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_johto_hive_badge") {
        GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.HIVE_BADGE)
    }
    val PLAIN_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_johto_plain_badge") {
        GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.PLAIN_BADGE)
    }
    val FOG_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_johto_fog_badge") {
        GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.FOG_BADGE)
    }
    val STORM_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_johto_storm_badge") {
        GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.STORM_BADGE)
    }
    val MINERAL_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_johto_mineral_badge") {
        GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.MINERAL_BADGE)
    }
    val GLACIER_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_johto_glacier_badge") {
        GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.GLACIER_BADGE)
    }
    val RISING_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_johto_rising_badge") {
        GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.RISING_BADGE)
    }

    // Hoenn Tickets
    val STONE_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_hoenn_stone_badge") { GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.STONE_BADGE) }
    val KNUCKLE_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_hoenn_knuckle_badge") { GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.KNUCKLE_BADGE) }
    val DYNAMO_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_hoenn_dynamo_badge") { GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.DYNAMO_BADGE) }
    val HEAT_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_hoenn_heat_badge") { GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.HEAT_BADGE) }
    val BALANCE_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_hoenn_balance_badge") { GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.BALANCE_BADGE) }
    val FEATHER_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_hoenn_feather_badge") { GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.FEATHER_BADGE) }
    val MIND_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_hoenn_mind_badge") { GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.MIND_BADGE) }
    val RAIN_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_hoenn_rain_badge") { GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.RAIN_BADGE) }

    // Sinnoh Tickets
    val COAL_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_sinnoh_coal_badge") { GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.COAL_BADGE) }
    val FOREST_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_sinnoh_forest_badge") { GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.FOREST_BADGE) }
    val COBBLE_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_sinnoh_cobble_badge") { GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.COBBLE_BADGE) }
    val FEN_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_sinnoh_fen_badge") { GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.FEN_BADGE) }
    val RELIC_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_sinnoh_relic_badge") { GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.RELIC_BADGE) }
    val MINE_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_sinnoh_mine_badge") { GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.MINE_BADGE) }
    val ICICLE_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_sinnoh_icicle_badge") { GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.ICICLE_BADGE) }
    val BEACON_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_sinnoh_beacon_badge") { GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.BEACON_BADGE) }

    // Unova Tickets
    val TRIO_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_unova_trio_badge") { GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.TRIO_BADGE) }
    val BASIC_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_unova_basic_badge") { GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.BASIC_BADGE) }
    val TOXIC_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_unova_toxic_badge") { GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.TOXIC_BADGE) }
    val INSECT_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_unova_insect_badge") { GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.INSECT_BADGE) }
    val BOLT_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_unova_bolt_badge") { GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.BOLT_BADGE) }
    val QUAKE_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_unova_quake_badge") { GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.QUAKE_BADGE) }
    val JET_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_unova_jet_badge") { GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.JET_BADGE) }
    val FREEZE_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_unova_freeze_badge") { GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.FREEZE_BADGE) }
    val LEGEND_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_unova_legend_badge") { GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.LEGEND_BADGE) }
    val WAVE_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_unova_wave_badge") { GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.WAVE_BADGE) }

    // Kalos Tickets
    val BUG_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_kalos_bug_badge") { GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.BUG_BADGE) }
    val CLIFF_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_kalos_cliff_badge") { GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.CLIFF_BADGE) }
    val RUMBLE_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_kalos_rumble_badge") { GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.RUMBLE_BADGE) }
    val PLANT_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_kalos_plant_badge") { GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.PLANT_BADGE) }
    val VOLTAGE_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_kalos_voltage_badge") { GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.VOLTAGE_BADGE) }
    val KALOS_FAIRY_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_kalos_fairy_badge") { GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.KALOS_FAIRY_BADGE) }
    val PSYCHIC_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_kalos_psychic_badge") { GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.PSYCHIC_BADGE) }
    val ICEBERG_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_kalos_iceberg_badge") { GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.ICEBERG_BADGE) }

    // Alola Tickets
    val MELEMELE_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_alola_melemele_stamp") { GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.MELEMELE_STAMP) }
    val AKALA_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_alola_akala_stamp") { GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.AKALA_STAMP) }
    val ULAULA_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_alola_ulaula_stamp") { GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.ULAULA_STAMP) }
    val PONI_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_alola_poni_stamp") { GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.PONI_STAMP) }

    // Galar Tickets
    val GRASS_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_galar_grass_badge") { GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.GRASS_BADGE) }
    val WATER_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_galar_water_badge") { GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.WATER_BADGE) }
    val FIRE_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_galar_fire_badge") { GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.FIRE_BADGE) }
    val FIGHTING_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_galar_fighting_badge") { GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.FIGHTING_BADGE) }
    val GHOST_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_galar_ghost_badge") { GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.GHOST_BADGE) }
    val GALAR_FAIRY_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_galar_fairy_badge") { GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.GALAR_FAIRY_BADGE) }
    val ROCK_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_galar_rock_badge") { GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.ROCK_BADGE) }
    val ICE_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_galar_ice_badge") { GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.ICE_BADGE) }
    val DARK_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_galar_dark_badge") { GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.DARK_BADGE) }
    val DRAGON_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_galar_dragon_badge") { GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.DRAGON_BADGE) }

    // Paldea Tickets
    val CORTONDO_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_paldea_cortondo_badge") { GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.CORTONDO_BADGE) }
    val ARTAZON_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_paldea_artazon_badge") { GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.ARTAZON_BADGE) }
    val LEVINCIA_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_paldea_levincia_badge") { GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.LEVINCIA_BADGE) }
    val CASCARRAFA_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_paldea_cascarrafa_badge") { GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.CASCARRAFA_BADGE) }
    val MEDALI_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_paldea_medali_badge") { GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.MEDALI_BADGE) }
    val MONTENEVERA_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_paldea_montenevera_badge") { GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.MONTENEVERA_BADGE) }
    val ALFORNADA_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_paldea_alfornada_badge") { GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.ALFORNADA_BADGE) }
    val GLASEADO_TICKET: RegistrySupplier<Item> = ITEMS.register("gym_ticket_paldea_glaseado_badge") { GymLeaderTicketItem(Item.Properties().stacksTo(64), GymBadge.GLASEADO_BADGE) }

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
            GymBadge.ZEPHYR_BADGE -> ZEPHYR_BADGE.get()
            GymBadge.HIVE_BADGE -> HIVE_BADGE.get()
            GymBadge.PLAIN_BADGE -> PLAIN_BADGE.get()
            GymBadge.FOG_BADGE -> FOG_BADGE.get()
            GymBadge.STORM_BADGE -> STORM_BADGE.get()
            GymBadge.MINERAL_BADGE -> MINERAL_BADGE.get()
            GymBadge.GLACIER_BADGE -> GLACIER_BADGE.get()
            GymBadge.RISING_BADGE -> RISING_BADGE.get()
            GymBadge.STONE_BADGE -> STONE_BADGE.get()
            GymBadge.KNUCKLE_BADGE -> KNUCKLE_BADGE.get()
            GymBadge.DYNAMO_BADGE -> DYNAMO_BADGE.get()
            GymBadge.HEAT_BADGE -> HEAT_BADGE.get()
            GymBadge.BALANCE_BADGE -> BALANCE_BADGE.get()
            GymBadge.FEATHER_BADGE -> FEATHER_BADGE.get()
            GymBadge.MIND_BADGE -> MIND_BADGE.get()
            GymBadge.RAIN_BADGE -> RAIN_BADGE.get()
            GymBadge.COAL_BADGE -> COAL_BADGE.get()
            GymBadge.FOREST_BADGE -> FOREST_BADGE.get()
            GymBadge.COBBLE_BADGE -> COBBLE_BADGE.get()
            GymBadge.FEN_BADGE -> FEN_BADGE.get()
            GymBadge.RELIC_BADGE -> RELIC_BADGE.get()
            GymBadge.MINE_BADGE -> MINE_BADGE.get()
            GymBadge.ICICLE_BADGE -> ICICLE_BADGE.get()
            GymBadge.BEACON_BADGE -> BEACON_BADGE.get()
            GymBadge.TRIO_BADGE -> TRIO_BADGE.get()
            GymBadge.BASIC_BADGE -> BASIC_BADGE.get()
            GymBadge.TOXIC_BADGE -> TOXIC_BADGE.get()
            GymBadge.INSECT_BADGE -> INSECT_BADGE.get()
            GymBadge.BOLT_BADGE -> BOLT_BADGE.get()
            GymBadge.QUAKE_BADGE -> QUAKE_BADGE.get()
            GymBadge.JET_BADGE -> JET_BADGE.get()
            GymBadge.FREEZE_BADGE -> FREEZE_BADGE.get()
            GymBadge.LEGEND_BADGE -> LEGEND_BADGE.get()
            GymBadge.WAVE_BADGE -> WAVE_BADGE.get()
            GymBadge.BUG_BADGE -> BUG_BADGE.get()
            GymBadge.CLIFF_BADGE -> CLIFF_BADGE.get()
            GymBadge.RUMBLE_BADGE -> RUMBLE_BADGE.get()
            GymBadge.PLANT_BADGE -> PLANT_BADGE.get()
            GymBadge.VOLTAGE_BADGE -> VOLTAGE_BADGE.get()
            GymBadge.KALOS_FAIRY_BADGE -> KALOS_FAIRY_BADGE.get()
            GymBadge.PSYCHIC_BADGE -> PSYCHIC_BADGE.get()
            GymBadge.ICEBERG_BADGE -> ICEBERG_BADGE.get()
            GymBadge.MELEMELE_STAMP -> MELEMELE_STAMP.get()
            GymBadge.AKALA_STAMP -> AKALA_STAMP.get()
            GymBadge.ULAULA_STAMP -> ULAULA_STAMP.get()
            GymBadge.PONI_STAMP -> PONI_STAMP.get()
            GymBadge.GRASS_BADGE -> GRASS_BADGE.get()
            GymBadge.WATER_BADGE -> WATER_BADGE.get()
            GymBadge.FIRE_BADGE -> FIRE_BADGE.get()
            GymBadge.FIGHTING_BADGE -> FIGHTING_BADGE.get()
            GymBadge.GHOST_BADGE -> GHOST_BADGE.get()
            GymBadge.GALAR_FAIRY_BADGE -> GALAR_FAIRY_BADGE.get()
            GymBadge.ROCK_BADGE -> ROCK_BADGE.get()
            GymBadge.ICE_BADGE -> ICE_BADGE.get()
            GymBadge.DARK_BADGE -> DARK_BADGE.get()
            GymBadge.DRAGON_BADGE -> DRAGON_BADGE.get()
            GymBadge.CORTONDO_BADGE -> CORTONDO_BADGE.get()
            GymBadge.ARTAZON_BADGE -> ARTAZON_BADGE.get()
            GymBadge.LEVINCIA_BADGE -> LEVINCIA_BADGE.get()
            GymBadge.CASCARRAFA_BADGE -> CASCARRAFA_BADGE.get()
            GymBadge.MEDALI_BADGE -> MEDALI_BADGE.get()
            GymBadge.MONTENEVERA_BADGE -> MONTENEVERA_BADGE.get()
            GymBadge.ALFORNADA_BADGE -> ALFORNADA_BADGE.get()
            GymBadge.GLASEADO_BADGE -> GLASEADO_BADGE.get()
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
            ZEPHYR_BADGE, HIVE_BADGE, PLAIN_BADGE, FOG_BADGE,
            STORM_BADGE, MINERAL_BADGE, GLACIER_BADGE, RISING_BADGE,
            STONE_BADGE, KNUCKLE_BADGE, DYNAMO_BADGE, HEAT_BADGE,
            BALANCE_BADGE, FEATHER_BADGE, MIND_BADGE, RAIN_BADGE,
            COAL_BADGE, FOREST_BADGE, COBBLE_BADGE, FEN_BADGE,
            RELIC_BADGE, MINE_BADGE, ICICLE_BADGE, BEACON_BADGE,
            TRIO_BADGE, BASIC_BADGE, TOXIC_BADGE, INSECT_BADGE,
            BOLT_BADGE, QUAKE_BADGE, JET_BADGE, FREEZE_BADGE,
            LEGEND_BADGE, WAVE_BADGE, BUG_BADGE, CLIFF_BADGE,
            RUMBLE_BADGE, PLANT_BADGE, VOLTAGE_BADGE, KALOS_FAIRY_BADGE,
            PSYCHIC_BADGE, ICEBERG_BADGE, MELEMELE_STAMP, AKALA_STAMP,
            ULAULA_STAMP, PONI_STAMP, GRASS_BADGE, WATER_BADGE,
            FIRE_BADGE, FIGHTING_BADGE, GHOST_BADGE, GALAR_FAIRY_BADGE,
            ROCK_BADGE, ICE_BADGE, DARK_BADGE, DRAGON_BADGE,
            CORTONDO_BADGE, ARTAZON_BADGE, LEVINCIA_BADGE, CASCARRAFA_BADGE,
            MEDALI_BADGE, MONTENEVERA_BADGE, ALFORNADA_BADGE, GLASEADO_BADGE,
            BADGE_CASE,
            BOULDER_TICKET, CASCADE_TICKET, THUNDER_TICKET, RAINBOW_TICKET,
            SOUL_TICKET, MARSH_TICKET, VOLCANO_TICKET, EARTH_TICKET,
            ZEPHYR_TICKET, HIVE_TICKET, PLAIN_TICKET, FOG_TICKET,
            STORM_TICKET, MINERAL_TICKET, GLACIER_TICKET, RISING_TICKET,
            STONE_TICKET, KNUCKLE_TICKET, DYNAMO_TICKET, HEAT_TICKET,
            BALANCE_TICKET, FEATHER_TICKET, MIND_TICKET, RAIN_TICKET,
            COAL_TICKET, FOREST_TICKET, COBBLE_TICKET, FEN_TICKET,
            RELIC_TICKET, MINE_TICKET, ICICLE_TICKET, BEACON_TICKET,
            TRIO_TICKET, BASIC_TICKET, TOXIC_TICKET, INSECT_TICKET,
            BOLT_TICKET, QUAKE_TICKET, JET_TICKET, FREEZE_TICKET,
            LEGEND_TICKET, WAVE_TICKET, BUG_TICKET, CLIFF_TICKET,
            RUMBLE_TICKET, PLANT_TICKET, VOLTAGE_TICKET, KALOS_FAIRY_TICKET,
            PSYCHIC_TICKET, ICEBERG_TICKET, MELEMELE_TICKET, AKALA_TICKET,
            ULAULA_TICKET, PONI_TICKET, GRASS_TICKET, WATER_TICKET,
            FIRE_TICKET, FIGHTING_TICKET, GHOST_TICKET, GALAR_FAIRY_TICKET,
            ROCK_TICKET, ICE_TICKET, DARK_TICKET, DRAGON_TICKET,
            CORTONDO_TICKET, ARTAZON_TICKET, LEVINCIA_TICKET, CASCARRAFA_TICKET,
            MEDALI_TICKET, MONTENEVERA_TICKET, ALFORNADA_TICKET, GLASEADO_TICKET
        )
    }
}
