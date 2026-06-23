package com.howlite.commands

import com.google.gson.GsonBuilder
import com.howlite.menu.GymShopEditMenu
import com.howlite.shop.GymShop
import com.mojang.brigadier.CommandDispatcher
import dev.architectury.event.events.common.CommandRegistrationEvent
import dev.architectury.registry.menu.MenuRegistry
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.MenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu

object GymShopCommand {

    private val gson = GsonBuilder().create()

    fun register() {
        CommandRegistrationEvent.EVENT.register { dispatcher, _, _ ->
            registerCommand(dispatcher)
        }
    }

    private fun registerCommand(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal("gymshop")
                .requires { source -> source.hasPermission(2) } // Requires OP (level 2)
                .then(
                    Commands.literal("edit")
                        .executes { context ->
                            openEditor(context.source.playerOrException)
                            1
                        }
                )
        )
    }

    private fun openEditor(player: ServerPlayer) {
        GymShop.loadConfig()

        MenuRegistry.openExtendedMenu(
            player,
            object : MenuProvider {
                override fun getDisplayName(): Component = Component.literal("Shop Editor")

                override fun createMenu(syncId: Int, inv: Inventory, p: Player): AbstractContainerMenu {
                    return GymShopEditMenu(syncId, inv, GymShop.loadedConfig)
                }
            }
        ) { buf ->
            buf.writeUtf(gson.toJson(GymShop.loadedConfig))
        }
    }
}
