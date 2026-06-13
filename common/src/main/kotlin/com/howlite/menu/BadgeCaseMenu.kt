package com.howlite.menu

import com.howlite.data.GymBadge
import com.howlite.data.PokemonSnapshot
import net.minecraft.network.FriendlyByteBuf
import com.howlite.data.GymRegion
import com.howlite.shop.JohtoShop
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
    val badgeTeams: Map<String, List<PokemonSnapshot>> = emptyMap()
) : AbstractContainerMenu(BadgeCaseMenus.BADGE_CASE_MENU_TYPE.get(), syncId) {

    override fun clickMenuButton(player: Player, id: Int): Boolean {
        if (id == 0) {
            if (player is ServerPlayer) {
                val progress = com.howlite.api.PlayerProgressApi.get(player)
                val hasAnyJohtoBadge = GymBadge.entries.filter { it.region == GymRegion.JOHTO }.any { progress.hasBadge(it) }
                if (hasAnyJohtoBadge) {
                    JohtoShop.openShop(player)
                    return true
                }
            }
        }
        return false
    }

    companion object {
        private fun readData(buf: FriendlyByteBuf): Triple<Int, Set<GymBadge>, Map<String, List<PokemonSnapshot>>> {
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
            return Triple(levelCap, badges, badgeTeams)
        }
    }

    /**
     * Constructeur secondaire utilisé côté **client** via la factory du [MenuType].
     * Lit les données depuis le [FriendlyByteBuf] envoyé par le serveur.
     */
    constructor(syncId: Int, buf: FriendlyByteBuf) : this(
        syncId,
        readData(buf)
    )

    private constructor(syncId: Int, data: Triple<Int, Set<GymBadge>, Map<String, List<PokemonSnapshot>>>) : this(
        syncId,
        unlockedBadges = data.second,
        levelCap = data.first,
        badgeTeams = data.third
    )

    /** Pas de déplacement rapide d'items — menu en lecture seule. */
    override fun quickMoveStack(player: Player, index: Int): ItemStack = ItemStack.EMPTY

    /** Toujours valide : la Boîte à Badges est consultable depuis n'importe où. */
    override fun stillValid(player: Player): Boolean = true
}
