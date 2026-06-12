package com.howlite.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.howlite.api.PlayerProgressApi
import com.howlite.data.GymBadge
import com.howlite.data.PokemonSnapshot
import com.cobblemon.mod.common.Cobblemon
import dev.architectury.event.events.common.CommandRegistrationEvent
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer

/**
 * Enregistre la commande `/gymtest` avec toutes ses sous-commandes de debug
 * pour tester le système de progression et de Level Cap.
 */
object GymTestCommand {

    fun register() {
        CommandRegistrationEvent.EVENT.register { dispatcher, _, _ ->
            registerCommand(dispatcher)
        }
    }

    private fun registerCommand(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal("gymtest")
                .requires { source -> source.hasPermission(2) } // Requiert les permissions OP (niveau 2)
                .then(
                    Commands.literal("status")
                        .executes { context -> showStatus(context, context.source.playerOrException) }
                        .then(
                            Commands.argument("player", EntityArgument.player())
                                .executes { context -> showStatus(context, EntityArgument.getPlayer(context, "player")) }
                        )
                )
                .then(
                    Commands.literal("addbadge")
                        .then(
                            Commands.argument("badge", StringArgumentType.word())
                                .suggests { _, builder ->
                                    SharedSuggestionProvider.suggest(GymBadge.entries.map { it.id }, builder)
                                }
                                .executes { context -> addBadge(context, StringArgumentType.getString(context, "badge"), context.source.playerOrException) }
                                .then(
                                    Commands.argument("player", EntityArgument.player())
                                        .executes { context -> addBadge(context, StringArgumentType.getString(context, "badge"), EntityArgument.getPlayer(context, "player")) }
                                )
                        )
                )
                .then(
                    Commands.literal("removebadge")
                        .then(
                            Commands.argument("badge", StringArgumentType.word())
                                .suggests { _, builder ->
                                    SharedSuggestionProvider.suggest(GymBadge.entries.map { it.id }, builder)
                                }
                                .executes { context -> removeBadge(context, StringArgumentType.getString(context, "badge"), context.source.playerOrException) }
                                .then(
                                    Commands.argument("player", EntityArgument.player())
                                        .executes { context -> removeBadge(context, StringArgumentType.getString(context, "badge"), EntityArgument.getPlayer(context, "player")) }
                                )
                        )
                )
                .then(
                    Commands.literal("reset")
                        .executes { context -> resetProgress(context, context.source.playerOrException) }
                        .then(
                            Commands.argument("player", EntityArgument.player())
                                .executes { context -> resetProgress(context, EntityArgument.getPlayer(context, "player")) }
                        )
                )
                .then(
                    Commands.literal("setlevelcap")
                        .then(
                            Commands.argument("level", IntegerArgumentType.integer(1, 100))
                                .executes { context -> setLevelCap(context, IntegerArgumentType.getInteger(context, "level"), context.source.playerOrException) }
                                .then(
                                    Commands.argument("player", EntityArgument.player())
                                        .executes { context -> setLevelCap(context, IntegerArgumentType.getInteger(context, "level"), EntityArgument.getPlayer(context, "player")) }
                                )
                        )
                )
        )
    }

    private fun showStatus(context: CommandContext<CommandSourceStack>, player: ServerPlayer): Int {
        val data = PlayerProgressApi.get(player)
        val badgesList = if (data.badges.isEmpty()) "Aucun" else data.badges.joinToString(", ") { it.id }
        context.source.sendSuccess(
            { Component.literal("=== Progression de ${player.scoreboardName} ===\n" +
                               "Level Cap : ${data.levelCap}\n" +
                               "Badges : $badgesList") },
            false
        )
        return 1
    }

    private fun addBadge(context: CommandContext<CommandSourceStack>, badgeId: String, player: ServerPlayer): Int {
        val badge = GymBadge.fromId(badgeId)
        if (badge == null) {
            context.source.sendFailure(Component.literal("Badge inconnu : $badgeId"))
            return 0
        }
        val data = PlayerProgressApi.get(player)
        if (data.hasBadge(badge)) {
            context.source.sendFailure(Component.literal("${player.scoreboardName} possède déjà le badge $badgeId"))
            return 0
        }
        
        // Capturer l'équipe du joueur
        try {
            val party = Cobblemon.storage.getParty(player)
            val snapshots = party.filterNotNull().map { pokemon ->
                PokemonSnapshot(
                    species = pokemon.species.name,
                    level = pokemon.level,
                    isShiny = pokemon.shiny,
                    displayName = pokemon.getDisplayName().string
                )
            }
            data.recordTeam(badge.id, snapshots)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        data.earnBadge(badge)
        PlayerProgressApi.markDirty(player)
        context.source.sendSuccess(
            { Component.literal("Badge $badgeId ajouté à ${player.scoreboardName}. Nouveau Level Cap : ${data.levelCap}") },
            true
        )
        return 1
    }

    private fun removeBadge(context: CommandContext<CommandSourceStack>, badgeId: String, player: ServerPlayer): Int {
        val badge = GymBadge.fromId(badgeId)
        if (badge == null) {
            context.source.sendFailure(Component.literal("Badge inconnu : $badgeId"))
            return 0
        }
        val data = PlayerProgressApi.get(player)
        if (!data.hasBadge(badge)) {
            context.source.sendFailure(Component.literal("${player.scoreboardName} ne possède pas le badge $badgeId"))
            return 0
        }
        data.removeBadge(badge)
        PlayerProgressApi.markDirty(player)
        context.source.sendSuccess(
            { Component.literal("Badge $badgeId retiré de ${player.scoreboardName}. Nouveau Level Cap : ${data.levelCap}") },
            true
        )
        return 1
    }

    private fun resetProgress(context: CommandContext<CommandSourceStack>, player: ServerPlayer): Int {
        val data = PlayerProgressApi.get(player)
        data.reset()
        PlayerProgressApi.markDirty(player)
        context.source.sendSuccess(
            { Component.literal("Progression réinitialisée pour ${player.scoreboardName}. Level Cap : ${data.levelCap}") },
            true
        )
        return 1
    }

    private fun setLevelCap(context: CommandContext<CommandSourceStack>, level: Int, player: ServerPlayer): Int {
        val data = PlayerProgressApi.get(player)
        data.overrideLevelCap(level)
        PlayerProgressApi.markDirty(player)
        context.source.sendSuccess(
            { Component.literal("Level Cap de ${player.scoreboardName} forcé à $level") },
            true
        )
        return 1
    }
}
