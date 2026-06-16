package com.howlite.wallet

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
}
