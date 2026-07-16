package com.howlite.commands

import com.howlite.moon.MoonManager
import com.howlite.moon.MoonPhase
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import dev.architectury.event.events.common.CommandRegistrationEvent
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.network.chat.Component

/**
 * Commande `/moon` — gestion des phases lunaires depuis le chat.
 *
 * ## Sous-commandes
 *
 * ### `/moon check`
 * Affiche dans le chat de l'exécutant :
 * - La phase lunaire actuellement active.
 * - La prochaine phase prévue.
 * - Les effets de gameplay en cours.
 *
 * ### `/moon set <phase>`
 * Force la phase lunaire immédiatement (persiste jusqu'au prochain changement ou reset).
 * Les phases disponibles sont les entrées de [MoonPhase].
 *
 * **Permission requise** : niveau OP 2 (`/op <joueur>` ou `ops.json`).
 */
object MoonCommand {

    fun register() {
        CommandRegistrationEvent.EVENT.register { dispatcher, _, _ ->
            registerCommand(dispatcher)
        }
    }

    private fun registerCommand(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal("moon")
                .requires { source -> source.hasPermission(2) }

                // /moon check
                .then(
                    Commands.literal("check")
                        .executes { context -> executeCheck(context.source) }
                )

                // /moon set <phase>
                .then(
                    Commands.literal("set")
                        .then(
                            Commands.argument("phase", StringArgumentType.word())
                                .suggests { _, builder ->
                                    SharedSuggestionProvider.suggest(
                                        MoonPhase.entries.map { it.name },
                                        builder
                                    )
                                }
                                .executes { context ->
                                    val phaseName = StringArgumentType.getString(context, "phase")
                                    executeSet(context.source, phaseName)
                                }
                        )
                )
        )
    }

    // ─── Exécution : /moon check ─────────────────────────────────────────

    private fun executeCheck(source: CommandSourceStack): Int {
        val current = MoonManager.currentPhase
        val next    = MoonManager.nextPhase

        source.sendSuccess({
            Component.literal("")
                .append(Component.literal("§e═══════ 🌙 Moon Phase ═══════\n"))
                .append(Component.literal("§7Phase actuelle : "))
                .append(Component.translatable(current.displayName).withStyle(phaseStyle(current)))
                .append(Component.literal("\n§7Effets : "))
                .append(Component.translatable(current.descriptionKey).withStyle { it.withColor(0xAAAAAA) })
                .append(Component.literal("\n§7Prochaine phase : "))
                .append(Component.translatable(next.displayName).withStyle(phaseStyle(next)))
                .append(Component.literal("\n§e═══════════════════════════"))
        }, false)

        return 1
    }

    // ─── Exécution : /moon set <phase> ───────────────────────────────────

    private fun executeSet(source: CommandSourceStack, phaseName: String): Int {
        val phase = MoonPhase.fromName(phaseName)
        val server = source.server

        MoonManager.forcePhase(server, phase)

        val msg = if (phase == MoonPhase.NONE)
            Component.literal("§aPhase lunaire réinitialisée (vanilla).")
        else
            Component.literal("§aPhase lunaire forcée : ")
                .append(Component.translatable(phase.displayName).withStyle(phaseStyle(phase)))

        source.sendSuccess({ msg }, true)
        return 1
    }

    // ─── Utilitaires ─────────────────────────────────────────────────────

    private fun phaseStyle(phase: MoonPhase): (net.minecraft.network.chat.Style) -> net.minecraft.network.chat.Style = { style ->
        val color = when (phase) {
            MoonPhase.BLUE_MOON   -> 0x6AADFF
            MoonPhase.RED_MOON    -> 0xFF3A1A
            MoonPhase.PURPLE_MOON -> 0x9B3FFF
            MoonPhase.FULL_MOON   -> 0xFFFACD
            MoonPhase.NONE        -> 0xFFFFFF
        }
        style.withColor(color).withBold(phase.isSpecial)
    }
}
