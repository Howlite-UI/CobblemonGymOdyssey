package com.howlite.data

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.nbt.CompoundTag

/**
 * Représente un instantané d'un Pokémon lors d'une victoire d'arène.
 */
data class PokemonSnapshot(
    val species: String,
    val level: Int,
    val isShiny: Boolean,
    val displayName: String
) {
    companion object {
        val CODEC: Codec<PokemonSnapshot> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("species").forGetter { it.species },
                Codec.INT.fieldOf("level").forGetter { it.level },
                Codec.BOOL.fieldOf("isShiny").forGetter { it.isShiny },
                Codec.STRING.fieldOf("displayName").forGetter { it.displayName }
            ).apply(instance) { s, l, sh, d -> PokemonSnapshot(s, l, sh, d) }
        }

        fun fromNbt(tag: CompoundTag): PokemonSnapshot {
            return PokemonSnapshot(
                species = tag.getString("Species"),
                level = tag.getInt("Level"),
                isShiny = tag.getBoolean("IsShiny"),
                displayName = tag.getString("DisplayName")
            )
        }

        fun writeToNbt(snapshot: PokemonSnapshot, tag: CompoundTag) {
            tag.putString("Species", snapshot.species)
            tag.putInt("Level", snapshot.level)
            tag.putBoolean("IsShiny", snapshot.isShiny)
            tag.putString("DisplayName", snapshot.displayName)
        }
    }
}
