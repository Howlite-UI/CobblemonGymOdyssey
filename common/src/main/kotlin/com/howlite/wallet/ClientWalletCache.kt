package com.howlite.wallet

import net.minecraft.client.Minecraft

/**
 * Cache client-side du wallet du joueur local.
 *
 * Mis à jour à chaque réception du packet [WalletNetwork.WALLET_SYNC_ID].
 * Utilisé par [com.howlite.client.screen.WalletOverlay] et
 * [com.howlite.client.screen.WalletHudOverlay] pour l'affichage.
 *
 * N'est jamais modifié directement côté client — toujours via un packet C2S.
 */
object ClientWalletCache {

    var balance: Long = 0L
    var autoCollect: Boolean = true
    var hudEnabled: Boolean = false

    private var lastPlayerInstance: Any? = null

    // Décomposition pour affichage
    val platinum: Long get() = balance / CoinType.PLATINUM.valueCCC
    val gold: Long     get() = (balance % CoinType.PLATINUM.valueCCC) / CoinType.GOLD.valueCCC
    val silver: Long   get() = (balance % CoinType.GOLD.valueCCC) / CoinType.SILVER.valueCCC
    val copper: Long   get() = balance % CoinType.SILVER.valueCCC

    fun reset() {
        balance = 0L
        autoCollect = true
        hudEnabled = false
    }

    /**
     * Vérifie si le joueur local a changé (connexion, déconnexion, changement de monde).
     * Si oui, réinitialise le cache et demande une synchronisation au serveur.
     */
    fun checkSync(mc: Minecraft) {
        val player = mc.player
        if (player != null && lastPlayerInstance !== player) {
            lastPlayerInstance = player
            reset()
            WalletNetwork.sendRequestSync()
        }
    }
}

