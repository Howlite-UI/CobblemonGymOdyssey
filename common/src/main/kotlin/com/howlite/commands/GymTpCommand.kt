package com.howlite.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import com.howlite.api.PlayerProgressApi
import com.howlite.world.GymArenaGenerator
import dev.architectury.event.events.common.CommandRegistrationEvent
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation

import com.mojang.brigadier.arguments.StringArgumentType
import com.howlite.data.GymBadge
import net.minecraft.commands.SharedSuggestionProvider

object GymTpCommand {

    fun register() {
        CommandRegistrationEvent.EVENT.register { dispatcher, _, _ ->
            registerCommand(dispatcher)
        }
    }

    private fun registerCommand(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal("gymtp")
                .requires { source -> source.hasPermission(2) }
                .executes { context -> teleportPlayer(context, null) }
                .then(
                    Commands.literal("back")
                        .executes { context -> teleportBack(context) }
                )
                .then(
                    Commands.argument("badge", StringArgumentType.word())
                        .suggests { _, builder ->
                            SharedSuggestionProvider.suggest(GymBadge.entries.map { it.id }, builder)
                        }
                        .executes { context -> teleportPlayer(context, StringArgumentType.getString(context, "badge")) }
                )
        )
    }

    private fun teleportPlayer(context: CommandContext<CommandSourceStack>, targetBadgeId: String?): Int {
        val player = context.source.playerOrException
        val progress = PlayerProgressApi.get(player)

        // Sauvegarder sa position actuelle comme position de retour
        progress.saveReturnPosition(
            player.level().dimension().location().toString(),
            player.x,
            player.y,
            player.z,
            player.yRot,
            player.xRot
        )
        PlayerProgressApi.markDirty(player)

        // Téléporter et générer l'arène spécifique
        GymArenaGenerator.teleportAndGenerate(player, targetBadgeId)

        context.source.sendSuccess(
            { Component.literal("§a[Dev] Arène générée et joueur téléporté. Position de retour enregistrée !") },
            false
        )
        return 1
    }

    private fun teleportBack(context: CommandContext<CommandSourceStack>): Int {
        val player = context.source.playerOrException
        val progress = PlayerProgressApi.get(player)

        val returnDimStr = progress.returnDim
        val returnX = progress.returnX
        val returnY = progress.returnY
        val returnZ = progress.returnZ
        val returnYaw = progress.returnYaw ?: 0f
        val returnPitch = progress.returnPitch ?: 0f

        if (returnDimStr == null || returnX == null || returnY == null || returnZ == null) {
            context.source.sendFailure(Component.literal("§cPas de position de retour enregistrée ! Utilisez d'abord /gymtp."))
            return 0
        }

        val server = player.server
        val returnLevelKey = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.parse(returnDimStr)
        )
        val targetWorld = server.getLevel(returnLevelKey) ?: server.overworld()

        // Effacer la position de retour
        progress.clearReturnPosition()
        PlayerProgressApi.markDirty(player)

        // Téléporter le joueur
        player.teleportTo(targetWorld, returnX, returnY, returnZ, returnYaw, returnPitch)

        context.source.sendSuccess(
            { Component.literal("§a[Dev] Téléporté de retour à la position d'origine.") },
            false
        )
        return 1
    }
}
