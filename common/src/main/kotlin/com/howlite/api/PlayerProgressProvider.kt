package com.howlite.api

import com.howlite.data.PlayerProgressData
import net.minecraft.server.level.ServerPlayer

/**
 * Abstraction plateforme-agnostique pour accéder aux données de progression
 * d'un joueur. Chaque module platform (Fabric / NeoForge) fournit son
 * implémentation concrète qui est enregistrée dans [PlayerProgressApi].
 */
interface PlayerProgressProvider {
    /**
     * Retourne les données de progression du joueur donné.
     * L'implémentation est responsable de créer une instance par défaut
     * si aucune donnée n'existe encore.
     */
    fun getProgress(player: ServerPlayer): PlayerProgressData

    /**
     * Marque les données comme modifiées pour forcer la sauvegarde ou la
     * synchronisation réseau si nécessaire. Peut être un no-op selon la
     * plateforme.
     */
    fun markDirty(player: ServerPlayer)
}

/**
 * Singleton global servant de point d'accès unique aux données joueur depuis
 * le code Common. Le provider est injecté par le module platform au démarrage.
 *
 * Usage : `PlayerProgressApi.get(player)`
 */
object PlayerProgressApi {
    /**
     * Provider injecté par le module platform au moment de l'initialisation.
     * Toute tentative d'accès avant l'injection lève une exception claire.
     */
    lateinit var provider: PlayerProgressProvider

    /** Raccourci pratique pour récupérer les données d'un joueur. */
    fun get(player: ServerPlayer): PlayerProgressData = provider.getProgress(player)

    /** Raccourci pour marquer les données comme modifiées. */
    fun markDirty(player: ServerPlayer) = provider.markDirty(player)
}
