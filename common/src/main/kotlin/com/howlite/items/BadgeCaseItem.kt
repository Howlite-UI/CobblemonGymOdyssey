package com.howlite.items

import com.howlite.api.PlayerProgressApi
import com.howlite.menu.BadgeCaseMenu
import com.howlite.data.PokemonSnapshot
import dev.architectury.registry.menu.MenuRegistry
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.MenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

/**
 * Item "Boîte à Badges" ([badge_case]).
 *
 * Un clic droit ouvre la [com.howlite.screen.BadgeCaseScreen], affichant les badges
 * débloqués du joueur organisés par région.
 *
 * L'item peut également être équipé dans un slot accessoire via :
 * - **Trinkets** (Fabric) — slot `misc/badge_case`
 * - **Accessories** (NeoForge) — slot `badge_case`
 * Ces intégrations sont des soft-deps : l'item fonctionne sans elles.
 *
 * ## Transfert des données S→C
 * On utilise [MenuRegistry.openExtendedMenu] d'Architectury qui permet d'écrire
 * des données supplémentaires dans un [FriendlyByteBuf] lors de l'ouverture du menu.
 * Côté client, la factory du [com.howlite.menu.BadgeCaseMenus.BADGE_CASE_MENU_TYPE]
 * lit ce buf via le constructeur secondaire de [BadgeCaseMenu].
 */
class BadgeCaseItem(properties: Properties) : Item(properties) {

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(hand)

        // Ouvrir le menu uniquement côté serveur
        if (!level.isClientSide && player is ServerPlayer) {
            val progress = PlayerProgressApi.get(player)
            val badges = progress.badges
            val levelCap = progress.levelCap
            val badgeTeams = progress.badgeTeams

            MenuRegistry.openExtendedMenu(
                player,
                object : MenuProvider {
                    override fun getDisplayName(): Component =
                        Component.translatable("cobblemongymodyssey.badge_case.title")

                    override fun createMenu(syncId: Int, inv: Inventory, p: Player): AbstractContainerMenu =
                        BadgeCaseMenu(syncId, badges, levelCap, badgeTeams)
                }
            ) { buf: FriendlyByteBuf ->
                buf.writeInt(levelCap)
                buf.writeCollection(badges) { b, badge -> b.writeUtf(badge.id) }
                
                // Écrire les équipes gagnantes
                buf.writeInt(badgeTeams.size)
                badgeTeams.forEach { (badgeId, team) ->
                    buf.writeUtf(badgeId)
                    buf.writeCollection(team) { b, pokemon ->
                        b.writeUtf(pokemon.species)
                        b.writeInt(pokemon.level)
                        b.writeBoolean(pokemon.isShiny)
                        b.writeUtf(pokemon.displayName)
                    }
                }
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide)
    }
}
