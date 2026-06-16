package com.howlite.wallet

import net.minecraft.nbt.CompoundTag

/**
 * Type de pièce avec sa valeur en CCC (Copper Cobble Coin).
 */
enum class CoinType(val valueCCC: Long, val itemId: String) {
    COPPER(1L, "cobble_copper_coin") {
        override fun getItem(): net.minecraft.world.item.Item = com.howlite.items.CobbleCoins.COBBLE_COPPER_COIN.get()
    },
    SILVER(100L, "cobble_silver_coin") {
        override fun getItem(): net.minecraft.world.item.Item = com.howlite.items.CobbleCoins.COBBLE_SILVER_COIN.get()
    },
    GOLD(10_000L, "cobble_gold_coin") {
        override fun getItem(): net.minecraft.world.item.Item = com.howlite.items.CobbleCoins.COBBLE_GOLD_COIN.get()
    },
    PLATINUM(1_000_000L, "cobble_platinum_coin") {
        override fun getItem(): net.minecraft.world.item.Item = com.howlite.items.CobbleCoins.COBBLE_PLATINUM_COIN.get()
    };

    abstract fun getItem(): net.minecraft.world.item.Item
}

/**
 * Données de la bourse d'un joueur.
 *
 * La valeur totale est stockée comme un Long en CCC (unité de base).
 * Les valeurs décomposées sont calculées à la demande pour l'affichage.
 *
 * Logique :
 *   100 CCC = 1 CSC
 *   100 CSC = 1 CGC  (= 10 000 CCC)
 *   100 CGC = 1 CPC  (= 1 000 000 CCC)
 *
 * Serialisée en NBT pour Cardinal Components (Fabric) et
 * en Codec pour AttachmentTypes (NeoForge).
 */
class WalletData {

    /** Solde total en Copper Cobble Coins (unité de base). */
    var balanceCCC: Long = 0L

    /** Switch 1 : auto-collecte des pièces au pickup. */
    var autoCollect: Boolean = true

    /** Switch 2 : affichage du HUD de balance en coin d'écran. */
    var hudEnabled: Boolean = false

    // -------------------------------------------------------------------------
    // Décomposition pour affichage
    // -------------------------------------------------------------------------

    val platinum: Long get() = balanceCCC / CoinType.PLATINUM.valueCCC
    val gold: Long     get() = (balanceCCC % CoinType.PLATINUM.valueCCC) / CoinType.GOLD.valueCCC
    val silver: Long   get() = (balanceCCC % CoinType.GOLD.valueCCC) / CoinType.SILVER.valueCCC
    val copper: Long   get() = balanceCCC % CoinType.SILVER.valueCCC

    // -------------------------------------------------------------------------
    // Opérations
    // -------------------------------------------------------------------------

    /**
     * Ajoute des pièces au wallet.
     * @param type Le type de pièce ajoutée.
     * @param amount Le nombre de pièces (toujours positif).
     */
    fun addCoins(type: CoinType, amount: Long) {
        require(amount > 0) { "Amount must be positive" }
        balanceCCC += type.valueCCC * amount
    }

    /**
     * Retire une valeur en CCC du wallet.
     * @return true si le retrait a réussi (solde suffisant), false sinon.
     */
    fun removeCoins(valueCCC: Long): Boolean {
        if (balanceCCC < valueCCC) return false
        balanceCCC -= valueCCC
        return true
    }

    /**
     * Retire un montant exprimé dans un [CoinType] donné.
     * @return true si le retrait a réussi.
     */
    fun removeCoins(type: CoinType, amount: Long): Boolean {
        return removeCoins(type.valueCCC * amount)
    }

    // -------------------------------------------------------------------------
    // Sérialisation NBT (partagée Fabric/NeoForge)
    // -------------------------------------------------------------------------

    fun toNbt(): CompoundTag {
        val tag = CompoundTag()
        tag.putLong("balance_ccc", balanceCCC)
        tag.putBoolean("auto_collect", autoCollect)
        tag.putBoolean("hud_enabled", hudEnabled)
        return tag
    }

    fun fromNbt(tag: CompoundTag) {
        balanceCCC = tag.getLong("balance_ccc")
        autoCollect = if (tag.contains("auto_collect")) tag.getBoolean("auto_collect") else true
        hudEnabled = if (tag.contains("hud_enabled")) tag.getBoolean("hud_enabled") else false
    }

    fun copyFrom(other: WalletData) {
        this.balanceCCC = other.balanceCCC
        this.autoCollect = other.autoCollect
        this.hudEnabled = other.hudEnabled
    }

    override fun toString(): String =
        "WalletData(${platinum}CPC ${gold}CGC ${silver}CSC ${copper}CCC, autoCollect=$autoCollect, hud=$hudEnabled)"
}
