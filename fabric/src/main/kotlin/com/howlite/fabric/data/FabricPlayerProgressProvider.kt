package com.howlite.fabric.data

import com.howlite.api.PlayerProgressProvider
import com.howlite.data.PlayerProgressData
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerPlayer
import org.ladysnake.cca.api.v3.component.Component

/**
 * Implémentation Fabric de la persistance des données de progression joueur.
 *
 * Cette classe joue un double rôle :
 * - [Component] (CCA 6.x) : gère la sérialisation/désérialisation NBT automatique
 *   assurée par Cardinal Components lors de la sauvegarde et du chargement du joueur.
 *   Signatures CCA 6.1 : `readFromNbt(CompoundTag, HolderLookup.Provider)` et
 *   `writeToNbt(CompoundTag, HolderLookup.Provider)`.
 * - [PlayerProgressProvider] : expose les données au code Common via [PlayerProgressApi].
 *
 * Chaque instance est attachée à UN joueur spécifique par CCA. La méthode
 * [getProgress] retourne donc toujours les données de ce joueur.
 */
class FabricPlayerProgressProvider : Component, PlayerProgressProvider {

    private val data = PlayerProgressData()

    // -------------------------------------------------------------------------
    // PlayerProgressProvider – API Common
    // -------------------------------------------------------------------------

    override fun getProgress(player: ServerPlayer): PlayerProgressData = data

    /** CCA gère la sauvegarde automatiquement ; pas d'action requise ici. */
    override fun markDirty(player: ServerPlayer) = Unit

    // -------------------------------------------------------------------------
    // Component (CCA 6.1) – Sérialisation NBT
    // Les signatures utilisent HolderLookup.Provider (Minecraft 1.21.1)
    // -------------------------------------------------------------------------

    override fun writeToNbt(tag: CompoundTag, registryLookup: HolderLookup.Provider) {
        data.writeToNbt(tag)
    }

    override fun readFromNbt(tag: CompoundTag, registryLookup: HolderLookup.Provider) {
        data.readFromNbt(tag)
    }
}
