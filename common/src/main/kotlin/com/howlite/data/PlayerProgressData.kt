package com.howlite.data

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.StringTag
import net.minecraft.nbt.Tag

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

    fun recordTeam(badgeId: String, team: List<PokemonSnapshot>) {
        _badgeTeams[badgeId] = team
    }

    /** Vérifie si le joueur possède déjà le badge donné. */
    fun hasBadge(badge: GymBadge): Boolean = badge in _badges

    /**
     * Ajoute un badge à la collection du joueur et recalcule le [levelCap]
     * au maximum de tous les badges détenus.
     */
    fun earnBadge(badge: GymBadge) {
        _badges.add(badge)
        levelCap = _badges.maxOf { it.levelCap }
    }

    /**
     * Supprime un badge de la collection du joueur et recalcule le [levelCap].
     */
    fun removeBadge(badge: GymBadge) {
        _badges.remove(badge)
        _badgeTeams.remove(badge.id)
        levelCap = if (_badges.isEmpty()) INITIAL_LEVEL_CAP else _badges.maxOf { it.levelCap }
    }

    /**
     * Réinitialise toute la progression (badges et level cap).
     */
    fun reset() {
        _badges.clear()
        _badgeTeams.clear()
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
    }

    companion object {
        private const val KEY_LEVEL_CAP = "LevelCap"
        private const val KEY_BADGES = "Badges"
        private const val KEY_BADGE_TEAMS = "BadgeTeams"
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
                    .forGetter { it._badgeTeams }
            ).apply(instance) { cap, badgeIds, teams ->
                PlayerProgressData().also { d ->
                    d.levelCap = cap
                    badgeIds.mapNotNull { GymBadge.fromId(it) }.forEach { d._badges.add(it) }
                    d._badgeTeams.putAll(teams)
                }
            }
        }
    }
}
