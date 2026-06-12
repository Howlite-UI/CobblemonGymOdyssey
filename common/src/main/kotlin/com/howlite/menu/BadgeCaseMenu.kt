package com.howlite.menu

import com.howlite.data.GymBadge
import net.minecraft.network.FriendlyByteBuf
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
    val levelCap: Int = 10
) : AbstractContainerMenu(BadgeCaseMenus.BADGE_CASE_MENU_TYPE.get(), syncId) {

    companion object {
        private fun readData(buf: FriendlyByteBuf): Pair<Int, Set<GymBadge>> {
            val levelCap = buf.readInt()
            val badges = buf.readCollection(
                { size -> HashSet(size) },
                { b -> GymBadge.fromId(b.readUtf()) }
            ).filterNotNull().toSet()
            return Pair(levelCap, badges)
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

    private constructor(syncId: Int, data: Pair<Int, Set<GymBadge>>) : this(
        syncId,
        unlockedBadges = data.second,
        levelCap = data.first
    )

    /** Pas de déplacement rapide d'items — menu en lecture seule. */
    override fun quickMoveStack(player: Player, index: Int): ItemStack = ItemStack.EMPTY

    /** Toujours valide : la Boîte à Badges est consultable depuis n'importe où. */
    override fun stillValid(player: Player): Boolean = true
}
