package com.howlite.data

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.nbt.CompoundTag

/**
 * Représente l'historique de combat d'un joueur contre un adversaire spécifique.
 */
data class PvpFightRecord(
    val lastFightDate: String,
    val consecutiveDays: Int
) {
    companion object {
        val CODEC: Codec<PvpFightRecord> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("lastFightDate").forGetter { it.lastFightDate },
                Codec.INT.fieldOf("consecutiveDays").forGetter { it.consecutiveDays }
            ).apply(instance) { date, days -> PvpFightRecord(date, days) }
        }

        fun fromNbt(tag: CompoundTag): PvpFightRecord {
            return PvpFightRecord(
                lastFightDate = tag.getString("LastFightDate"),
                consecutiveDays = tag.getInt("ConsecutiveDays")
            )
        }

        fun writeToNbt(record: PvpFightRecord, tag: CompoundTag) {
            tag.putString("LastFightDate", record.lastFightDate)
            tag.putInt("ConsecutiveDays", record.consecutiveDays)
        }
    }
}
