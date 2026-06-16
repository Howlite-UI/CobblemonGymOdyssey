package com.howlite.shop

import com.howlite.data.GymBadge
import com.howlite.data.GymRegion
import com.howlite.api.PlayerProgressApi
import com.google.gson.GsonBuilder
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.MerchantMenu
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.trading.ItemCost
import net.minecraft.world.item.trading.Merchant
import net.minecraft.world.item.trading.MerchantOffer
import net.minecraft.world.item.trading.MerchantOffers
import net.minecraft.world.MenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.AbstractContainerMenu
import java.io.File
import java.util.Optional

class GymVirtualMerchant(private val player: Player) : Merchant {
    private var offers = MerchantOffers()

    override fun setTradingPlayer(p: Player?) {}
    override fun getTradingPlayer(): Player? = player
    override fun getOffers(): MerchantOffers = offers
    override fun overrideOffers(o: MerchantOffers) {
        offers = o
    }
    override fun notifyTrade(offer: MerchantOffer) {}
    override fun notifyTradeUpdated(itemStack: ItemStack) {}
    override fun getVillagerXp(): Int = 0
    override fun overrideXp(i: Int) {}
    override fun showProgressBar(): Boolean = false
    override fun getNotifyTradeSound(): SoundEvent = SoundEvents.VILLAGER_YES
    override fun isClientSide(): Boolean = player.level().isClientSide
}

object GymShop {

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val CONFIG_FILE = File("config/cobblemongymodyssey_shops.json")

    data class ShopItemConfig(
        val requiredBadge: String,
        val costItem: String = "minecraft:emerald",
        val costCount: Int = 1,
        val resultItem: String,
        val resultCount: Int = 1
    )

    data class RegionShopConfig(
        val items: List<ShopItemConfig> = emptyList()
    )

    data class ShopsConfig(
        val shops: Map<String, RegionShopConfig> = emptyMap()
    )

    private var loadedConfig: ShopsConfig = ShopsConfig()

    init {
        loadConfig()
    }

    fun loadConfig() {
        try {
            if (!CONFIG_FILE.parentFile.exists()) {
                CONFIG_FILE.parentFile.mkdirs()
            }
            if (!CONFIG_FILE.exists()) {
                loadedConfig = createDefaultConfig()
                val json = gson.toJson(loadedConfig)
                CONFIG_FILE.writeText(json, Charsets.UTF_8)
            } else {
                val json = CONFIG_FILE.readText(Charsets.UTF_8)
                loadedConfig = gson.fromJson(json, ShopsConfig::class.java)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            loadedConfig = createDefaultConfig()
        }
    }

    private fun getItem(id: String, fallback: Item): Item {
        val loc = ResourceLocation.tryParse(id) ?: return fallback
        val item = BuiltInRegistries.ITEM.get(loc)
        return if (item == Items.AIR) fallback else item
    }

    private fun createOffer(costId: String, costCount: Int, resultId: String, resultCount: Int): MerchantOffer {
        val costItem = getItem(costId, Items.EMERALD)
        val resultItem = getItem(resultId, Items.SNOWBALL)
        val cost = ItemCost(costItem, costCount)
        val result = ItemStack(resultItem, resultCount)
        return MerchantOffer(
            cost,
            Optional.empty(),
            result,
            999, // maxUses
            0, // merchantXp
            0f // priceMultiplier
        )
    }

    fun openShop(player: ServerPlayer, region: GymRegion) {
        val progress = PlayerProgressApi.get(player)
        val offers = MerchantOffers()

        loadConfig()

        val regionConfig = loadedConfig.shops[region.name]
        if (regionConfig != null) {
            for (itemConf in regionConfig.items) {
                val badgeOpt = GymBadge.entries.find { it.id == itemConf.requiredBadge }
                val hasBadge = if (badgeOpt != null) {
                    progress.hasBadge(badgeOpt)
                } else {
                    itemConf.requiredBadge.isEmpty()
                }

                if (hasBadge) {
                    offers.add(createOffer(
                        itemConf.costItem,
                        itemConf.costCount,
                        itemConf.resultItem,
                        itemConf.resultCount
                    ))
                }
            }
        }

        if (offers.isEmpty()) return

        val merchant = GymVirtualMerchant(player)
        merchant.overrideOffers(offers)

        val regionLower = region.name.lowercase()
        val containerIdOpt = player.openMenu(object : MenuProvider {
            override fun getDisplayName(): Component =
                Component.translatable("cobblemongymodyssey.shop.$regionLower.title")

            override fun createMenu(syncId: Int, inv: Inventory, p: Player): AbstractContainerMenu {
                return MerchantMenu(syncId, inv, merchant)
            }
        })

        if (containerIdOpt != null && containerIdOpt.isPresent) {
            player.sendMerchantOffers(
                containerIdOpt.getAsInt(),
                offers,
                1,
                0,
                false,
                false
            )
        }
    }

    private fun createDefaultConfig(): ShopsConfig {
        val shopsMap = mutableMapOf<String, RegionShopConfig>()

        // 1. KANTO
        shopsMap["KANTO"] = RegionShopConfig(
            items = listOf(
                ShopItemConfig("boulder_badge", "minecraft:emerald", 1, "cobblemon:poke_ball", 4),
                ShopItemConfig("boulder_badge", "minecraft:emerald", 2, "cobblemon:potion", 2),
                ShopItemConfig("cascade_badge", "minecraft:emerald", 2, "cobblemon:great_ball", 4),
                ShopItemConfig("cascade_badge", "minecraft:emerald", 3, "cobblemon:super_potion", 2),
                ShopItemConfig("thunder_badge", "minecraft:emerald", 4, "cobblemon:ultra_ball", 4),
                ShopItemConfig("thunder_badge", "minecraft:emerald", 5, "cobblemon:hyper_potion", 2),
                ShopItemConfig("rainbow_badge", "minecraft:emerald", 2, "cobblemon:exp_candy_s", 4),
                ShopItemConfig("rainbow_badge", "minecraft:emerald", 4, "cobblemon:exp_candy_m", 2),
                ShopItemConfig("soul_badge", "minecraft:emerald", 12, "cobblemon:rare_candy", 1),
                ShopItemConfig("marsh_badge", "minecraft:emerald", 15, "cobblemon:zinc", 2),
                ShopItemConfig("marsh_badge", "minecraft:emerald", 15, "cobblemon:protein", 2),
                ShopItemConfig("volcano_badge", "minecraft:emerald", 20, "cobblemon:ability_capsule", 1),
                ShopItemConfig("volcano_badge", "minecraft:emerald", 16, "cobblemon:bottle_cap", 1),
                ShopItemConfig("earth_badge", "minecraft:emerald", 64, "cobblemon:ability_patch", 1),
                ShopItemConfig("earth_badge", "minecraft:emerald", 64, "cobblemon:master_ball", 1)
            )
        )

        // 2. JOHTO
        shopsMap["JOHTO"] = RegionShopConfig(
            items = listOf(
                ShopItemConfig("zephyr_badge", "minecraft:emerald", 1, "cobblemon:poke_ball", 4),
                ShopItemConfig("zephyr_badge", "minecraft:emerald", 2, "cobblemon:potion", 2),
                ShopItemConfig("hive_badge", "minecraft:emerald", 2, "cobblemon:great_ball", 4),
                ShopItemConfig("hive_badge", "minecraft:emerald", 3, "cobblemon:super_potion", 2),
                ShopItemConfig("hive_badge", "minecraft:emerald", 2, "cobblemon:full_heal", 2),
                ShopItemConfig("plain_badge", "minecraft:emerald", 8, "cobblemon:fire_stone", 1),
                ShopItemConfig("plain_badge", "minecraft:emerald", 8, "cobblemon:water_stone", 1),
                ShopItemConfig("plain_badge", "minecraft:emerald", 8, "cobblemon:thunder_stone", 1),
                ShopItemConfig("plain_badge", "minecraft:emerald", 8, "cobblemon:leaf_stone", 1),
                ShopItemConfig("plain_badge", "minecraft:emerald", 8, "cobblemon:moon_stone", 1),
                ShopItemConfig("plain_badge", "minecraft:emerald", 8, "cobblemon:sun_stone", 1),
                ShopItemConfig("plain_badge", "minecraft:emerald", 8, "cobblemon:ice_stone", 1),
                ShopItemConfig("plain_badge", "minecraft:emerald", 5, "cobblemon:everstone", 1),
                ShopItemConfig("plain_badge", "minecraft:emerald", 2, "cobblemon:exp_candy_s", 4),
                ShopItemConfig("fog_badge", "minecraft:emerald", 4, "cobblemon:ultra_ball", 4),
                ShopItemConfig("fog_badge", "minecraft:emerald", 5, "cobblemon:hyper_potion", 2),
                ShopItemConfig("fog_badge", "minecraft:emerald", 10, "cobblemon:dusk_stone", 1),
                ShopItemConfig("fog_badge", "minecraft:emerald", 10, "cobblemon:shiny_stone", 1),
                ShopItemConfig("fog_badge", "minecraft:emerald", 10, "cobblemon:dawn_stone", 1),
                ShopItemConfig("fog_badge", "minecraft:emerald", 3, "cobblemon:dusk_ball", 4),
                ShopItemConfig("fog_badge", "minecraft:emerald", 4, "cobblemon:quick_ball", 2),
                ShopItemConfig("fog_badge", "minecraft:emerald", 4, "cobblemon:exp_candy_m", 2),
                ShopItemConfig("storm_badge", "minecraft:emerald", 12, "cobblemon:rare_candy", 1),
                ShopItemConfig("storm_badge", "minecraft:emerald", 4, "cobblemon:timer_ball", 2),
                ShopItemConfig("storm_badge", "minecraft:emerald", 15, "cobblemon:destiny_knot", 1),
                ShopItemConfig("storm_badge", "minecraft:emerald", 8, "cobblemon:exp_candy_l", 1),
                ShopItemConfig("mineral_badge", "minecraft:emerald", 15, "cobblemon:zinc", 2),
                ShopItemConfig("mineral_badge", "minecraft:emerald", 15, "cobblemon:protein", 2),
                ShopItemConfig("mineral_badge", "minecraft:emerald", 15, "cobblemon:calcium", 2),
                ShopItemConfig("glacier_badge", "minecraft:emerald", 20, "cobblemon:ability_capsule", 1),
                ShopItemConfig("glacier_badge", "minecraft:emerald", 16, "cobblemon:bottle_cap", 1),
                ShopItemConfig("rising_badge", "minecraft:emerald", 64, "cobblemon:ability_patch", 1),
                ShopItemConfig("rising_badge", "minecraft:emerald", 64, "cobblemon:master_ball", 1)
            )
        )

        // 3. HOENN
        shopsMap["HOENN"] = RegionShopConfig(
            items = listOf(
                ShopItemConfig("stone_badge", "minecraft:emerald", 1, "cobblemon:poke_ball", 4),
                ShopItemConfig("stone_badge", "minecraft:emerald", 2, "cobblemon:potion", 2),
                ShopItemConfig("knuckle_badge", "minecraft:emerald", 2, "cobblemon:great_ball", 4),
                ShopItemConfig("knuckle_badge", "minecraft:emerald", 3, "cobblemon:super_potion", 2),
                ShopItemConfig("dynamo_badge", "minecraft:emerald", 4, "cobblemon:quick_ball", 2),
                ShopItemConfig("heat_badge", "minecraft:emerald", 5, "cobblemon:charcoal", 1),
                ShopItemConfig("balance_badge", "minecraft:emerald", 12, "cobblemon:rare_candy", 1),
                ShopItemConfig("feather_badge", "minecraft:emerald", 15, "cobblemon:carbos", 2),
                ShopItemConfig("mind_badge", "minecraft:emerald", 25, "cobblemon:choice_band", 1),
                ShopItemConfig("mind_badge", "minecraft:emerald", 25, "cobblemon:choice_specs", 1),
                ShopItemConfig("mind_badge", "minecraft:emerald", 25, "cobblemon:choice_scarf", 1),
                ShopItemConfig("rain_badge", "minecraft:emerald", 64, "cobblemon:master_ball", 1)
            )
        )

        // 4. SINNOH
        shopsMap["SINNOH"] = RegionShopConfig(
            items = listOf(
                ShopItemConfig("coal_badge", "minecraft:emerald", 1, "cobblemon:poke_ball", 4),
                ShopItemConfig("forest_badge", "minecraft:emerald", 8, "cobblemon:leaf_stone", 1),
                ShopItemConfig("cobble_badge", "minecraft:emerald", 15, "cobblemon:rocky_helmet", 1),
                ShopItemConfig("fen_badge", "minecraft:emerald", 10, "cobblemon:damp_rock", 1),
                ShopItemConfig("relic_badge", "minecraft:emerald", 10, "cobblemon:spell_tag", 1),
                ShopItemConfig("mine_badge", "minecraft:emerald", 12, "cobblemon:metal_coat", 1),
                ShopItemConfig("icicle_badge", "minecraft:emerald", 10, "cobblemon:never_melt_ice", 1),
                ShopItemConfig("beacon_badge", "minecraft:emerald", 64, "cobblemon:master_ball", 1)
            )
        )

        // 5. UNOVA
        shopsMap["UNOVA"] = RegionShopConfig(
            items = listOf(
                ShopItemConfig("toxic_badge", "minecraft:emerald", 1, "cobblemon:poke_ball", 4),
                ShopItemConfig("basic_badge", "minecraft:emerald", 2, "cobblemon:potion", 2),
                ShopItemConfig("insect_badge", "minecraft:emerald", 5, "cobblemon:silver_powder", 1),
                ShopItemConfig("bolt_badge", "minecraft:emerald", 5, "cobblemon:magnet", 1),
                ShopItemConfig("quake_badge", "minecraft:emerald", 5, "cobblemon:soft_sand", 1),
                ShopItemConfig("jet_badge", "minecraft:emerald", 5, "cobblemon:sharp_beak", 1),
                ShopItemConfig("legend_badge", "minecraft:emerald", 10, "cobblemon:dragon_fang", 1),
                ShopItemConfig("wave_badge", "minecraft:emerald", 64, "cobblemon:master_ball", 1)
            )
        )

        // 6. KALOS
        shopsMap["KALOS"] = RegionShopConfig(
            items = listOf(
                ShopItemConfig("bug_badge", "minecraft:emerald", 1, "cobblemon:poke_ball", 4),
                ShopItemConfig("cliff_badge", "minecraft:emerald", 10, "cobblemon:hard_stone", 1),
                ShopItemConfig("rumble_badge", "minecraft:emerald", 15, "cobblemon:power_weight", 1),
                ShopItemConfig("rumble_badge", "minecraft:emerald", 15, "cobblemon:power_belt", 1),
                ShopItemConfig("plant_badge", "minecraft:emerald", 15, "cobblemon:power_lens", 1),
                ShopItemConfig("plant_badge", "minecraft:emerald", 15, "cobblemon:power_band", 1),
                ShopItemConfig("voltage_badge", "minecraft:emerald", 15, "cobblemon:power_anklet", 1),
                ShopItemConfig("voltage_badge", "minecraft:emerald", 15, "cobblemon:power_bracer", 1),
                ShopItemConfig("kalos_fairy_badge", "minecraft:emerald", 15, "cobblemon:destiny_knot", 1),
                ShopItemConfig("kalos_fairy_badge", "minecraft:emerald", 5, "cobblemon:everstone", 1),
                ShopItemConfig("psychic_badge", "minecraft:emerald", 10, "cobblemon:luxury_ball", 4),
                ShopItemConfig("iceberg_badge", "minecraft:emerald", 64, "cobblemon:master_ball", 1)
            )
        )

        // 7. ALOLA
        shopsMap["ALOLA"] = RegionShopConfig(
            items = listOf(
                ShopItemConfig("melemele_stamp", "minecraft:emerald", 1, "cobblemon:poke_ball", 4),
                ShopItemConfig("melemele_stamp", "minecraft:emerald", 3, "cobblemon:adrenaline_orb", 1),
                ShopItemConfig("akala_stamp", "minecraft:emerald", 2, "cobblemon:great_ball", 4),
                ShopItemConfig("akala_stamp", "minecraft:emerald", 5, "cobblemon:float_stone", 1),
                ShopItemConfig("ulaula_stamp", "minecraft:emerald", 4, "cobblemon:ultra_ball", 4),
                ShopItemConfig("ulaula_stamp", "minecraft:emerald", 10, "cobblemon:leftovers", 1),
                ShopItemConfig("poni_stamp", "minecraft:emerald", 30, "cobblemon:gold_bottle_cap", 1),
                ShopItemConfig("poni_stamp", "minecraft:emerald", 64, "cobblemon:master_ball", 1)
            )
        )

        // 8. GALAR
        shopsMap["GALAR"] = RegionShopConfig(
            items = listOf(
                ShopItemConfig("grass_badge", "minecraft:emerald", 5, "cobblemon:miracle_seed", 1),
                ShopItemConfig("water_badge", "minecraft:emerald", 5, "cobblemon:mystic_water", 1),
                ShopItemConfig("fire_badge", "minecraft:emerald", 5, "cobblemon:charcoal", 1),
                ShopItemConfig("fighting_badge", "minecraft:emerald", 5, "cobblemon:black_belt", 1),
                ShopItemConfig("galar_fairy_badge", "minecraft:emerald", 15, "cobblemon:destiny_knot", 1),
                ShopItemConfig("rock_badge", "minecraft:emerald", 5, "cobblemon:hard_stone", 1),
                ShopItemConfig("dark_badge", "minecraft:emerald", 5, "cobblemon:black_glasses", 1),
                ShopItemConfig("dragon_badge", "minecraft:emerald", 64, "cobblemon:master_ball", 1)
            )
        )

        // 9. PALDEA
        shopsMap["PALDEA"] = RegionShopConfig(
            items = listOf(
                ShopItemConfig("cortondo_badge", "minecraft:emerald", 1, "cobblemon:poke_ball", 4),
                ShopItemConfig("artazon_badge", "minecraft:emerald", 8, "cobblemon:leaf_stone", 1),
                ShopItemConfig("levincia_badge", "minecraft:emerald", 4, "cobblemon:cell_battery", 1),
                ShopItemConfig("cascarrafa_badge", "minecraft:emerald", 2, "minecraft:water_bucket", 1),
                ShopItemConfig("medali_badge", "minecraft:emerald", 10, "cobblemon:leftovers", 1),
                ShopItemConfig("montenevera_badge", "minecraft:emerald", 10, "cobblemon:spell_tag", 1),
                ShopItemConfig("alfornada_badge", "minecraft:emerald", 5, "cobblemon:psychic_seed", 1),
                ShopItemConfig("glaseado_badge", "minecraft:emerald", 64, "cobblemon:master_ball", 1)
            )
        )

        return ShopsConfig(shopsMap)
    }
}
