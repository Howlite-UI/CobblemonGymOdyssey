package com.howlite.wallet

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player

/**
 * API commune pour accéder et modifier le [WalletData] d'un joueur.
 *
 * L'implémentation concrète de [get] est fournie par chaque plateforme :
 *  - Fabric : [FabricWalletProvider] (Cardinal Components)
 *  - NeoForge : [NeoForgeWalletProvider] (AttachmentTypes)
 *
 * Toutes les modifications passent par [addAndSync] ou [removeAndSync]
 * pour garantir la synchronisation S2C immédiate.
 */
object WalletManager {

    /** Provider injecté au démarrage par chaque plateforme. */
    var provider: WalletProvider? = null

    /**
     * Récupère le [WalletData] d'un joueur (server ou client via cache).
     * Throw si le provider n'est pas initialisé.
     */
    fun get(player: Player): WalletData {
        return checkNotNull(provider) {
            "[GymOdyssey] WalletManager.provider not initialized!"
        }.getWallet(player)
    }

    /**
     * Ajoute des pièces au wallet d'un joueur et synchronise le client.
     * À appeler UNIQUEMENT côté serveur.
     */
    fun addAndSync(player: ServerPlayer, type: CoinType, amount: Long) {
        val wallet = get(player)
        wallet.addCoins(type, amount)
        WalletNetwork.syncToClient(player, wallet)
    }

    /**
     * Retire une valeur (en CCC) du wallet et synchronise.
     * @return true si le retrait a réussi (solde suffisant).
     */
    fun removeAndSync(player: ServerPlayer, valueCCC: Long): Boolean {
        val wallet = get(player)
        val success = wallet.removeCoins(valueCCC)
        if (success) WalletNetwork.syncToClient(player, wallet)
        return success
    }

    /**
     * Met à jour les settings (autoCollect / hudEnabled) et synchronise.
     */
    fun updateSettingsAndSync(player: ServerPlayer, autoCollect: Boolean, hudEnabled: Boolean) {
        val wallet = get(player)
        wallet.autoCollect = autoCollect
        wallet.hudEnabled = hudEnabled
        WalletNetwork.syncToClient(player, wallet)
    }

    /**
     * Formate un montant en CCC sous forme de chaîne de caractères décomposée (CPC, CGC, CSC, CCC).
     */
    fun formatCCC(ccc: Long): String {
        val p = ccc / CoinType.PLATINUM.valueCCC
        val remP = ccc % CoinType.PLATINUM.valueCCC
        val g = remP / CoinType.GOLD.valueCCC
        val remG = remP % CoinType.GOLD.valueCCC
        val s = remG / CoinType.SILVER.valueCCC
        val cu = remG % CoinType.SILVER.valueCCC
        
        val parts = mutableListOf<String>()
        if (p > 0) parts += "${p}CPC"
        if (g > 0) parts += "${g}CGC"
        if (s > 0) parts += "${s}CSC"
        if (cu > 0) parts += "${cu}CCC"
        return if (parts.isEmpty()) "0 CCC" else parts.joinToString(" ")
    }
}

/**
 * Interface implémentée par chaque plateforme pour accéder aux données wallet.
 */
fun interface WalletProvider {
    fun getWallet(player: net.minecraft.world.entity.player.Player): WalletData
}
