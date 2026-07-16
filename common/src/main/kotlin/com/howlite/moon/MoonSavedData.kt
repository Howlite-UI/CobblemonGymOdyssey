package com.howlite.moon

import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.saveddata.SavedData

/**
 * Persistance serveur du système de phases lunaires.
 * Sauvegardée dans `.minecraft/saves/<monde>/data/moon_manager.dat`.
 */
class MoonSavedData private constructor(
    var phaseIndex: Int = 0,
    var lastCheckedDay: Long = -1L,
    var forcedPhase: String = ""
) : SavedData() {

    override fun save(tag: CompoundTag, registries: net.minecraft.core.HolderLookup.Provider): CompoundTag {
        tag.putInt("phaseIndex", phaseIndex)
        tag.putLong("lastCheckedDay", lastCheckedDay)
        tag.putString("forcedPhase", forcedPhase)
        return tag
    }

    companion object {
        private const val DATA_NAME = "moon_manager"

        private fun create() = MoonSavedData()

        private fun load(tag: CompoundTag, registries: net.minecraft.core.HolderLookup.Provider): MoonSavedData {
            return MoonSavedData(
                phaseIndex     = tag.getInt("phaseIndex"),
                lastCheckedDay = tag.getLong("lastCheckedDay"),
                forcedPhase    = tag.getString("forcedPhase")
            )
        }

        /**
         * Obtient ou crée l'instance [MoonSavedData] pour le niveau donné.
         * Toujours appelé sur l'overworld.
         */
        fun get(level: ServerLevel): MoonSavedData {
            // Factory(creator, reader, dataFixTypes) — dataFixTypes = null pour les data simples
            val factory = Factory(
                { create() },
                { tag, registries -> load(tag, registries) },
                null
            )
            return level.dataStorage.computeIfAbsent(factory, DATA_NAME)
        }
    }
}
