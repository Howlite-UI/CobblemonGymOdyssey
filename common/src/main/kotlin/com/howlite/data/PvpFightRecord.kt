package com.howlite.data

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.nbt.CompoundTag

/**
 * Représente l'historique de combat d'un joueur contre un adversaire spécifique.
 */
data class PvpFightRecord(
    val lastFightDate: String,
    val consecutiveDays: Int,
    val wins: Int = 0,
    val losses: Int = 0
) {
    companion object {
        val CODEC: Codec<PvpFightRecord> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("lastFightDate").forGetter { it.lastFightDate },
                Codec.INT.fieldOf("consecutiveDays").forGetter { it.consecutiveDays },
                Codec.INT.fieldOf("wins").orElse(0).forGetter { it.wins },
                Codec.INT.fieldOf("losses").orElse(0).forGetter { it.losses }
            ).apply(instance) { date, days, w, l -> PvpFightRecord(date, days, w, l) }
        }

        fun fromNbt(tag: CompoundTag): PvpFightRecord {
            return PvpFightRecord(
                lastFightDate = tag.getString("LastFightDate"),
                consecutiveDays = tag.getInt("ConsecutiveDays"),
                wins = if (tag.contains("Wins")) tag.getInt("Wins") else 0,
                losses = if (tag.contains("Losses")) tag.getInt("Losses") else 0
            )
        }

        fun writeToNbt(record: PvpFightRecord, tag: CompoundTag) {
            tag.putString("LastFightDate", record.lastFightDate)
            tag.putInt("ConsecutiveDays", record.consecutiveDays)
            tag.putInt("Wins", record.wins)
            tag.putInt("Losses", record.losses)
        }
    }
}
