package com.howlite.neoforge.data

import com.howlite.api.PlayerProgressProvider
import com.howlite.data.PlayerProgressData
import net.minecraft.server.level.ServerPlayer

/**
 * Implémentation NeoForge du [PlayerProgressProvider] utilisant le système
 * de Data Attachments de NeoForge.
 *
 * NeoForge gère automatiquement la sérialisation et la synchronisation des
 * données via le Codec défini dans [PlayerProgressData.CODEC]. Aucune action
 * manuelle n'est nécessaire dans [markDirty].
 */
class NeoForgePlayerProgressProvider : PlayerProgressProvider {

    /**
     * Récupère les données de progression attachées au joueur.
     * Si aucune donnée n'existe encore, NeoForge crée une instance par défaut
     * via le constructeur fourni dans [NeoForgeAttachments.PLAYER_PROGRESS].
     */
    override fun getProgress(player: ServerPlayer): PlayerProgressData =
        player.getData(NeoForgeAttachments.PLAYER_PROGRESS)

    /**
     * NeoForge Data Attachments nécessite de ré-appliquer les données modifiées
     * via [ServerPlayer.setData] pour notifier la sauvegarde.
     */
    override fun markDirty(player: ServerPlayer) {
        player.setData(NeoForgeAttachments.PLAYER_PROGRESS, getProgress(player))
    }
}
