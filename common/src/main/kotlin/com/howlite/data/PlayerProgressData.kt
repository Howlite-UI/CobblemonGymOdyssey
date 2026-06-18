package com.howlite.data

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.StringTag
import net.minecraft.nbt.Tag
import java.util.Optional

/**
 * Données de progression d'un joueur : badges obtenus et Level Cap actuel.
 *
 * Cette classe est partagée entre les deux plateformes. La sérialisation NBT
 * est utilisée par Cardinal Components (Fabric) et la Codec par les Data
 * Attachments NeoForge.
 *
 * Le [levelCap] initial de 10 force le joueur à battre le premier arène
 * avant que ses Pokémon ne puissent dépasser le niveau 10.
 */
class PlayerProgressData {

    private val _badges: MutableSet<GymBadge> = mutableSetOf()
    val badges: Set<GymBadge> get() = _badges.toSet()

    /** Niveau maximum que peuvent atteindre les Pokémon de ce joueur. */
    var levelCap: Int = INITIAL_LEVEL_CAP
        private set

    private val _badgeTeams: MutableMap<String, List<PokemonSnapshot>> = mutableMapOf()
    val badgeTeams: Map<String, List<PokemonSnapshot>> get() = _badgeTeams

    private val _pvpFights: MutableMap<String, PvpFightRecord> = mutableMapOf()
    val pvpFights: Map<String, PvpFightRecord> get() = _pvpFights

    var lastPvpResetDate: String? = null
    var pvpRewardsClaimedToday: Int = 0

    fun recordPvpFight(opponentUuid: String, record: PvpFightRecord) {
        _pvpFights[opponentUuid] = record
    }

    var arenaIndex: Int? = null
    var returnDim: String? = null
    var returnX: Double? = null
    var returnY: Double? = null
    var returnZ: Double? = null
    var returnYaw: Float? = null
    var returnPitch: Float? = null

    fun saveReturnPosition(dim: String, x: Double, y: Double, z: Double, yaw: Float, pitch: Float) {
        this.returnDim = dim
        this.returnX = x
        this.returnY = y
        this.returnZ = z
        this.returnYaw = yaw
        this.returnPitch = pitch
    }

    fun clearReturnPosition() {
        this.returnDim = null
        this.returnX = null
        this.returnY = null
        this.returnZ = null
        this.returnYaw = null
        this.returnPitch = null
    }

    fun recordTeam(badgeId: String, team: List<PokemonSnapshot>) {
        _badgeTeams[badgeId] = team
    }

    /** Vérifie si le joueur possède déjà le badge donné. */
    fun hasBadge(badge: GymBadge): Boolean = badge in _badges

    /**
     * Ajoute un badge à la collection du joueur et recalcule le [levelCap]
     * au maximum de tous les badges Kanto détenus.
     */
    fun earnBadge(badge: GymBadge) {
        _badges.add(badge)
        recalculateLevelCap()
    }

    /**
     * Supprime un badge de la collection du joueur et recalcule le [levelCap].
     */
    fun removeBadge(badge: GymBadge) {
        _badges.remove(badge)
        _badgeTeams.remove(badge.id)
        recalculateLevelCap()
    }

    private fun recalculateLevelCap() {
        val kantoBadges = _badges.filter { it.region == GymRegion.KANTO }
        levelCap = if (kantoBadges.isEmpty()) INITIAL_LEVEL_CAP else kantoBadges.maxOf { it.levelCap }
    }

    /**
     * Réinitialise toute la progression (badges et level cap).
     */
    fun reset() {
        _badges.clear()
        _badgeTeams.clear()
        _pvpFights.clear()
        lastPvpResetDate = null
        pvpRewardsClaimedToday = 0
        levelCap = INITIAL_LEVEL_CAP
    }

    /**
     * Force une valeur spécifique de Level Cap (utilisé principalement pour le debug).
     */
    fun overrideLevelCap(cap: Int) {
        levelCap = cap
    }

    // -------------------------------------------------------------------------
    // Sérialisation NBT (utilisée par Cardinal Components côté Fabric)
    // -------------------------------------------------------------------------

    fun writeToNbt(tag: CompoundTag) {
        tag.putInt(KEY_LEVEL_CAP, levelCap)
        val badgeList = ListTag()
        _badges.forEach { badgeList.add(StringTag.valueOf(it.id)) }
        tag.put(KEY_BADGES, badgeList)

        val teamsTag = CompoundTag()
        _badgeTeams.forEach { (badgeId, team) ->
            val teamList = ListTag()
            team.forEach { snapshot ->
                val pokemonTag = CompoundTag()
                PokemonSnapshot.writeToNbt(snapshot, pokemonTag)
                teamList.add(pokemonTag)
            }
            teamsTag.put(badgeId, teamList)
        }
        tag.put(KEY_BADGE_TEAMS, teamsTag)

        arenaIndex?.let { tag.putInt("ArenaIndex", it) }
        returnDim?.let { tag.putString("ReturnDim", it) }
        returnX?.let { tag.putDouble("ReturnX", it) }
        returnY?.let { tag.putDouble("ReturnY", it) }
        returnZ?.let { tag.putDouble("ReturnZ", it) }
        returnYaw?.let { tag.putFloat("ReturnYaw", it) }
        returnPitch?.let { tag.putFloat("ReturnPitch", it) }

        val pvpFightsTag = CompoundTag()
        _pvpFights.forEach { (opponentUuid, record) ->
            val recordTag = CompoundTag()
            PvpFightRecord.writeToNbt(record, recordTag)
            pvpFightsTag.put(opponentUuid, recordTag)
        }
        tag.put(KEY_PVP_FIGHTS, pvpFightsTag)
        lastPvpResetDate?.let { tag.putString(KEY_LAST_PVP_RESET, it) }
        tag.putInt(KEY_PVP_REWARDS_CLAIMED, pvpRewardsClaimedToday)
    }

    fun readFromNbt(tag: CompoundTag) {
        levelCap = tag.getInt(KEY_LEVEL_CAP).takeIf { it > 0 } ?: INITIAL_LEVEL_CAP
        _badges.clear()
        tag.getList(KEY_BADGES, Tag.TAG_STRING.toInt()).forEach { nbt ->
            GymBadge.fromId(nbt.asString)?.let { _badges.add(it) }
        }

        _badgeTeams.clear()
        if (tag.contains(KEY_BADGE_TEAMS)) {
            val teamsTag = tag.getCompound(KEY_BADGE_TEAMS)
            teamsTag.allKeys.forEach { badgeId ->
                val teamList = teamsTag.getList(badgeId, Tag.TAG_COMPOUND.toInt())
                val snapshots = mutableListOf<PokemonSnapshot>()
                for (i in 0 until teamList.size) {
                    snapshots.add(PokemonSnapshot.fromNbt(teamList.getCompound(i)))
                }
                _badgeTeams[badgeId] = snapshots
            }
        }

        arenaIndex = if (tag.contains("ArenaIndex")) tag.getInt("ArenaIndex") else null
        returnDim = if (tag.contains("ReturnDim")) tag.getString("ReturnDim") else null
        returnX = if (tag.contains("ReturnX")) tag.getDouble("ReturnX") else null
        returnY = if (tag.contains("ReturnY")) tag.getDouble("ReturnY") else null
        returnZ = if (tag.contains("ReturnZ")) tag.getDouble("ReturnZ") else null
        returnYaw = if (tag.contains("ReturnYaw")) tag.getFloat("ReturnYaw") else null
        returnPitch = if (tag.contains("ReturnPitch")) tag.getFloat("ReturnPitch") else null

        _pvpFights.clear()
        if (tag.contains(KEY_PVP_FIGHTS)) {
            val pvpFightsTag = tag.getCompound(KEY_PVP_FIGHTS)
            pvpFightsTag.allKeys.forEach { opponentUuid ->
                _pvpFights[opponentUuid] = PvpFightRecord.fromNbt(pvpFightsTag.getCompound(opponentUuid))
            }
        }
        lastPvpResetDate = if (tag.contains(KEY_LAST_PVP_RESET)) tag.getString(KEY_LAST_PVP_RESET) else null
        pvpRewardsClaimedToday = if (tag.contains(KEY_PVP_REWARDS_CLAIMED)) tag.getInt(KEY_PVP_REWARDS_CLAIMED) else 0
    }

    companion object {
        private const val KEY_LEVEL_CAP = "LevelCap"
        private const val KEY_BADGES = "Badges"
        private const val KEY_BADGE_TEAMS = "BadgeTeams"
        private const val KEY_PVP_FIGHTS = "PvpFights"
        private const val KEY_LAST_PVP_RESET = "LastPvpResetDate"
        private const val KEY_PVP_REWARDS_CLAIMED = "PvpRewardsClaimedToday"
        const val INITIAL_LEVEL_CAP = 10

        // -------------------------------------------------------------------------
        // Codec (utilisé par NeoForge Data Attachments)
        // -------------------------------------------------------------------------

        val CODEC: Codec<PlayerProgressData> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.INT.fieldOf(KEY_LEVEL_CAP)
                    .orElse(INITIAL_LEVEL_CAP)
                    .forGetter { it.levelCap },
                Codec.STRING.listOf()
                    .fieldOf(KEY_BADGES)
                    .orElse(emptyList())
                    .forGetter { data -> data._badges.map { it.id } },
                Codec.unboundedMap(Codec.STRING, PokemonSnapshot.CODEC.listOf())
                    .fieldOf(KEY_BADGE_TEAMS)
                    .orElse(emptyMap())
                    .forGetter { it._badgeTeams },
                Codec.INT.optionalFieldOf("arena_index").forGetter { Optional.ofNullable(it.arenaIndex) },
                Codec.STRING.optionalFieldOf("return_dim").forGetter { Optional.ofNullable(it.returnDim) },
                Codec.DOUBLE.optionalFieldOf("return_x").forGetter { Optional.ofNullable(it.returnX) },
                Codec.DOUBLE.optionalFieldOf("return_y").forGetter { Optional.ofNullable(it.returnY) },
                Codec.DOUBLE.optionalFieldOf("return_z").forGetter { Optional.ofNullable(it.returnZ) },
                Codec.FLOAT.optionalFieldOf("return_yaw").forGetter { Optional.ofNullable(it.returnYaw) },
                Codec.FLOAT.optionalFieldOf("return_pitch").forGetter { Optional.ofNullable(it.returnPitch) },
                Codec.unboundedMap(Codec.STRING, PvpFightRecord.CODEC)
                    .fieldOf("pvp_fights")
                    .orElse(emptyMap())
                    .forGetter { it.pvpFights },
                Codec.STRING.optionalFieldOf("last_pvp_reset_date").forGetter { Optional.ofNullable(it.lastPvpResetDate) },
                Codec.INT.fieldOf("pvp_rewards_claimed_today").orElse(0).forGetter { it.pvpRewardsClaimedToday }
            ).apply(instance) { cap, badgeIds, teams, arenaIndexOpt, returnDimOpt, returnXOpt, returnYOpt, returnZOpt, returnYawOpt, returnPitchOpt, pvpFights, lastPvpResetDateOpt, pvpRewardsClaimedToday ->
                PlayerProgressData().also { d ->
                    d.levelCap = cap
                    badgeIds.mapNotNull { GymBadge.fromId(it) }.forEach { d._badges.add(it) }
                    d._badgeTeams.putAll(teams)
                    d.arenaIndex = arenaIndexOpt.orElse(null)
                    d.returnDim = returnDimOpt.orElse(null)
                    d.returnX = returnXOpt.orElse(null)
                    d.returnY = returnYOpt.orElse(null)
                    d.returnZ = returnZOpt.orElse(null)
                    d.returnYaw = returnYawOpt.orElse(null)
                    d.returnPitch = returnPitchOpt.orElse(null)
                    d._pvpFights.putAll(pvpFights)
                    d.lastPvpResetDate = lastPvpResetDateOpt.orElse(null)
                    d.pvpRewardsClaimedToday = pvpRewardsClaimedToday
                }
            }
        }
    }
}
