package com.howlite.menu

import com.howlite.data.GymBadge
import com.howlite.data.PokemonSnapshot
import net.minecraft.network.FriendlyByteBuf
import com.howlite.data.GymRegion
import com.howlite.shop.GymShop
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.ItemStack

/**
 * Menu (côté serveur + côté client) de la Boîte à Badges.
 *
 * Ce menu est **en lecture seule** : il n'expose aucun slot d'inventaire Vanilla.
 * Son rôle est uniquement de transporter la liste des [unlockedBadges] du serveur
 * vers le client afin que [com.howlite.screen.BadgeCaseScreen] puisse afficher
 * les badges correctement.
 *
 * ## Flux de création
 * - **Serveur** : [com.howlite.items.BadgeCaseItem.use] → `NetworkHooks.openScreen`
 *   ou `player.openMenu` avec un [net.minecraft.world.MenuProvider] qui écrit les
 *   badges dans le [FriendlyByteBuf].
 * - **Client** : Factory dans [BadgeCaseMenus.BADGE_CASE_MENU_TYPE] lit le buf
 *   et recrée ce menu avec les badges reçus.
 *
 * @param syncId     identifiant de synchronisation fourni par Minecraft.
 * @param unlockedBadges ensemble des badges débloqués par le joueur.
 */
class BadgeCaseMenu(
    syncId: Int,
    val unlockedBadges: Set<GymBadge>,
    val levelCap: Int = 10,
    val badgeTeams: Map<String, List<PokemonSnapshot>> = emptyMap(),
    val pvpWins: Int = 0,
    val pvpLosses: Int = 0,
    val pvpRewardsClaimedToday: Int = 0,
    val pvpFights: Map<String, com.howlite.data.PvpFightRecord> = emptyMap(),
    val altarFightsToday: Map<String, Int> = emptyMap(),
    val dailyAllowanceClaims: Map<String, String> = emptyMap()
) : AbstractContainerMenu(BadgeCaseMenus.BADGE_CASE_MENU_TYPE.get(), syncId) {

    override fun clickMenuButton(player: Player, id: Int): Boolean {
        if (id in 0 until GymRegion.entries.size) {
            if (player is ServerPlayer) {
                val region = GymRegion.entries[id]
                val progress = com.howlite.api.PlayerProgressApi.get(player)
                val hasAnyBadge = GymBadge.entries.filter { it.region == region }.any { progress.hasBadge(it) }
                if (hasAnyBadge) {
                    GymShop.openShop(player, region)
                    return true
                }
            }
        }
        return false
    }

    class MenuData(
        val levelCap: Int,
        val badges: Set<GymBadge>,
        val badgeTeams: Map<String, List<PokemonSnapshot>>,
        val pvpWins: Int,
        val pvpLosses: Int,
        val pvpRewardsClaimedToday: Int,
        val pvpFights: Map<String, com.howlite.data.PvpFightRecord>,
        val altarFightsToday: Map<String, Int>,
        val dailyAllowanceClaims: Map<String, String>
    )

    companion object {
        private fun readMenuData(buf: FriendlyByteBuf): MenuData {
            val levelCap = buf.readInt()
            val badges = buf.readCollection(
                { size -> HashSet(size) },
                { b -> GymBadge.fromId(b.readUtf()) }
            ).filterNotNull().toSet()

            val badgeTeams = mutableMapOf<String, List<PokemonSnapshot>>()
            if (buf.readableBytes() > 0) {
                val size = buf.readInt()
                for (i in 0 until size) {
                    val badgeId = buf.readUtf()
                    val team = buf.readCollection(
                        { s -> ArrayList<PokemonSnapshot>(s) },
                        { b ->
                            PokemonSnapshot(
                                species = b.readUtf(),
                                level = b.readInt(),
                                isShiny = b.readBoolean(),
                                displayName = b.readUtf()
                            )
                        }
                    )
                    badgeTeams[badgeId] = team
                }
            }

            var pvpWins = 0
            var pvpLosses = 0
            var pvpRewardsClaimedToday = 0
            val pvpFights = mutableMapOf<String, com.howlite.data.PvpFightRecord>()

            if (buf.readableBytes() > 0) {
                pvpWins = buf.readInt()
                pvpLosses = buf.readInt()
                pvpRewardsClaimedToday = buf.readInt()
                val pvpFightsSize = buf.readInt()
                for (i in 0 until pvpFightsSize) {
                    val opponentUuid = buf.readUtf()
                    val lastFightDate = buf.readUtf()
                    val consecutiveDays = buf.readInt()
                    val wins = buf.readInt()
                    val losses = buf.readInt()
                    pvpFights[opponentUuid] = com.howlite.data.PvpFightRecord(lastFightDate, consecutiveDays, wins, losses)
                }
            }

            val altarFightsToday = mutableMapOf<String, Int>()
            if (buf.readableBytes() > 0) {
                val size = buf.readInt()
                for (i in 0 until size) {
                    altarFightsToday[buf.readUtf()] = buf.readInt()
                }
            }

            val dailyAllowanceClaims = mutableMapOf<String, String>()
            if (buf.readableBytes() > 0) {
                val size = buf.readInt()
                for (i in 0 until size) {
                    dailyAllowanceClaims[buf.readUtf()] = buf.readUtf()
                }
            }

            return MenuData(levelCap, badges, badgeTeams, pvpWins, pvpLosses, pvpRewardsClaimedToday, pvpFights, altarFightsToday, dailyAllowanceClaims)
        }
    }

    /**
     * Constructeur secondaire utilisé côté **client** via la factory du [MenuType].
     * Lit les données depuis le [FriendlyByteBuf] envoyé par le serveur.
     */
    constructor(syncId: Int, buf: FriendlyByteBuf) : this(
        syncId,
        readMenuData(buf)
    )

    private constructor(syncId: Int, data: MenuData) : this(
        syncId,
        unlockedBadges = data.badges,
        levelCap = data.levelCap,
        badgeTeams = data.badgeTeams,
        pvpWins = data.pvpWins,
        pvpLosses = data.pvpLosses,
        pvpRewardsClaimedToday = data.pvpRewardsClaimedToday,
        pvpFights = data.pvpFights,
        altarFightsToday = data.altarFightsToday,
        dailyAllowanceClaims = data.dailyAllowanceClaims
    )

    /** Pas de déplacement rapide d'items — menu en lecture seule. */
    override fun quickMoveStack(player: Player, index: Int): ItemStack = ItemStack.EMPTY

    /** Toujours valide : la Boîte à Badges est consultable depuis n'importe où. */
    override fun stillValid(player: Player): Boolean = true
}
