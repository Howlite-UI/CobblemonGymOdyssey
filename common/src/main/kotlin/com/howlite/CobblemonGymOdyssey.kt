package com.howlite

import com.howlite.blocks.GymBlocks
import com.howlite.blocks.ConsumableRaidBlock
import com.howlite.blocks.ConsumableRaidBlockEntity
import com.howlite.commands.GymTestCommand
import com.howlite.commands.GymTpCommand
import com.howlite.events.BattleLevelCapEventHandler
import com.howlite.events.GymBattleEventHandler
import com.howlite.events.GymBattleReturnHandler
import com.howlite.events.LevelCapEventHandler
import com.howlite.items.GymBadgeItems
import com.howlite.menu.BadgeCaseMenu
import com.howlite.menu.BadgeCaseMenus
import com.howlite.sounds.GymSounds
import dev.architectury.networking.NetworkManager
import dev.architectury.registry.menu.MenuRegistry
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.MenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu

object CobblemonGymOdyssey {
    const val MOD_ID = "cobblemongymodyssey"

    @JvmStatic
    fun init() {
        println("[GymOdyssey] CobblemonGymOdyssey.init() common initialization running!")
        GymSounds.register()
        GymBadgeItems.register()
        GymBlocks.register()
        BadgeCaseMenus.register()
        LevelCapEventHandler.register()
        GymBattleEventHandler.register()
        GymBattleReturnHandler.register()
        BattleLevelCapEventHandler.register()
        GymTestCommand.register()
        GymTpCommand.register()

        // Register Enter Consumable Raid Network Packet Receiver (Client -> Server)
        NetworkManager.registerReceiver(
            NetworkManager.Side.C2S,
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "enter_consumable_raid")
        ) { buf, context ->
            val player = context.player
            val pos = buf.readBlockPos()
            context.queue {
                if (player is ServerPlayer) {
                    val level = player.serverLevel()
                    // Sanity check to prevent players from entering raids from too far
                    if (player.distanceToSqr(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) < 64.0) {
                        val state = level.getBlockState(pos)
                        val be = level.getBlockEntity(pos) as? ConsumableRaidBlockEntity
                        val block = state.block as? ConsumableRaidBlock
                        if (be != null && block != null && be.isActive(state)) {
                            block.startOrJoinRaidPublic(player, state, level, pos)
                        }
                    }
                }
            }
        }

        // Register Open Badge Case Network Packet Receiver (Client -> Server)
        NetworkManager.registerReceiver(
            NetworkManager.Side.C2S,
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "open_badge_case")
        ) { _, context ->
            val player = context.player
            if (player is ServerPlayer) {
                context.queue {
                    val progress = com.howlite.api.PlayerProgressApi.get(player)
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
                    ) { buffer ->
                        buffer.writeInt(levelCap)
                        buffer.writeCollection(badges) { b, badge -> b.writeUtf(badge.id) }
                        buffer.writeInt(badgeTeams.size)
                        badgeTeams.forEach { (badgeId, team) ->
                            buffer.writeUtf(badgeId)
                            buffer.writeCollection(team) { b, pokemon ->
                                b.writeUtf(pokemon.species)
                                b.writeInt(pokemon.level)
                                b.writeBoolean(pokemon.isShiny)
                                b.writeUtf(pokemon.displayName)
                            }
                        }
                    }
                }
            }
        }
    }
}
