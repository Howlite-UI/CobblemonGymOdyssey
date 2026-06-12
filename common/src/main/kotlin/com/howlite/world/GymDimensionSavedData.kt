package com.howlite.world

import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.MinecraftServer
import net.minecraft.util.datafix.DataFixTypes
import net.minecraft.world.level.saveddata.SavedData

class GymDimensionSavedData : SavedData() {
    var nextArenaIndex: Int = 0

    override fun save(tag: CompoundTag, registries: HolderLookup.Provider): CompoundTag {
        tag.putInt("nextArenaIndex", nextArenaIndex)
        return tag
    }

    companion object {
        private val factory = Factory(
            { GymDimensionSavedData() },
            { tag, registries ->
                GymDimensionSavedData().apply {
                    nextArenaIndex = tag.getInt("nextArenaIndex")
                }
            },
            DataFixTypes.LEVEL
        )

        fun get(server: MinecraftServer): GymDimensionSavedData {
            val overworld = server.overworld()
            return overworld.dataStorage.computeIfAbsent(factory, "gym_dimension_data")
        }
    }
}
