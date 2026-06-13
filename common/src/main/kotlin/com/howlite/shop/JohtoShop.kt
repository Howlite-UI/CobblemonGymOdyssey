package com.howlite.shop

import com.howlite.data.GymBadge
import com.howlite.api.PlayerProgressApi
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
import java.util.Optional

class JohtoVirtualMerchant(private val player: Player) : Merchant {
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

object JohtoShop {

    // Monnaie par défaut : Émeraude
    val CURRENCY_ITEM: Item = Items.EMERALD

    private fun getCobblemonItem(id: String, fallback: Item): Item {
        val loc = ResourceLocation.fromNamespaceAndPath("cobblemon", id)
        val item = BuiltInRegistries.ITEM.get(loc)
        return if (item == Items.AIR) fallback else item
    }

    private fun createOffer(costCount: Int, result: ItemStack): MerchantOffer {
        val cost = ItemCost(CURRENCY_ITEM, costCount)
        return MerchantOffer(
            cost,
            Optional.empty(),
            result,
            999, // maxUses
            0, // merchantXp
            0f // priceMultiplier
        )
    }

    fun openShop(player: ServerPlayer) {
        val progress = PlayerProgressApi.get(player)
        val offers = MerchantOffers()

        // 1. Badge Zéphyr (zephyr_badge) -> Poké Balls et Potions
        if (progress.hasBadge(GymBadge.ZEPHYR_BADGE)) {
            offers.add(createOffer(1, ItemStack(getCobblemonItem("poke_ball", Items.SNOWBALL), 4)))
            offers.add(createOffer(2, ItemStack(getCobblemonItem("potion", Items.HONEY_BOTTLE), 2)))
        }

        // 2. Badge Essaim (hive_badge) -> Great Balls, Super Potions, Total Soin
        if (progress.hasBadge(GymBadge.HIVE_BADGE)) {
            offers.add(createOffer(2, ItemStack(getCobblemonItem("great_ball", Items.ENDER_PEARL), 4)))
            offers.add(createOffer(3, ItemStack(getCobblemonItem("super_potion", Items.POTION), 2)))
            offers.add(createOffer(2, ItemStack(getCobblemonItem("full_heal", Items.MILK_BUCKET), 2)))
        }

        // 3. Badge Plaine (plain_badge) -> Pierres Évolutives
        if (progress.hasBadge(GymBadge.PLAIN_BADGE)) {
            offers.add(createOffer(8, ItemStack(getCobblemonItem("fire_stone", Items.FLINT_AND_STEEL))))
            offers.add(createOffer(8, ItemStack(getCobblemonItem("water_stone", Items.WATER_BUCKET))))
            offers.add(createOffer(8, ItemStack(getCobblemonItem("thunder_stone", Items.LIGHTNING_ROD))))
            offers.add(createOffer(8, ItemStack(getCobblemonItem("leaf_stone", Items.OAK_SAPLING))))
        }

        // 4. Badge Brouillard (fog_badge) -> Ultra Balls, Hyper Potions
        if (progress.hasBadge(GymBadge.FOG_BADGE)) {
            offers.add(createOffer(4, ItemStack(getCobblemonItem("ultra_ball", Items.ENDER_EYE), 4)))
            offers.add(createOffer(5, ItemStack(getCobblemonItem("hyper_potion", Items.POTION), 2)))
        }

        // 5. Badge Choc (storm_badge) -> Super Bonbons
        if (progress.hasBadge(GymBadge.STORM_BADGE)) {
            offers.add(createOffer(12, ItemStack(getCobblemonItem("rare_candy", Items.GOLDEN_APPLE))))
        }

        // 6. Badge Minéral (mineral_badge) -> Vitamines (Zinc, Carbone, Calcium...)
        if (progress.hasBadge(GymBadge.MINERAL_BADGE)) {
            offers.add(createOffer(15, ItemStack(getCobblemonItem("zinc", Items.COPPER_INGOT), 2)))
            offers.add(createOffer(15, ItemStack(getCobblemonItem("protein", Items.IRON_INGOT), 2)))
            offers.add(createOffer(15, ItemStack(getCobblemonItem("calcium", Items.GOLD_INGOT), 2)))
        }

        // 7. Badge Glacier (glacier_badge) -> Pilule Talent & Capsules d'Argent
        if (progress.hasBadge(GymBadge.GLACIER_BADGE)) {
            offers.add(createOffer(20, ItemStack(getCobblemonItem("ability_capsule", Items.AMETHYST_SHARD))))
            offers.add(createOffer(16, ItemStack(getCobblemonItem("bottle_cap", Items.IRON_NUGGET))))
        }

        // 8. Badge Lever (rising_badge) -> Patch Talent & Master Ball
        if (progress.hasBadge(GymBadge.RISING_BADGE)) {
            offers.add(createOffer(64, ItemStack(getCobblemonItem("ability_patch", Items.NETHER_STAR))))
            offers.add(createOffer(64, ItemStack(getCobblemonItem("master_ball", Items.HEART_OF_THE_SEA))))
        }

        if (offers.isEmpty()) return

        val merchant = JohtoVirtualMerchant(player)
        merchant.overrideOffers(offers)

        player.openMenu(object : MenuProvider {
            override fun getDisplayName(): Component =
                Component.translatable("cobblemongymodyssey.shop.johto.title")

            override fun createMenu(syncId: Int, inv: Inventory, p: Player): AbstractContainerMenu {
                return MerchantMenu(syncId, inv, merchant)
            }
        })
    }
}
