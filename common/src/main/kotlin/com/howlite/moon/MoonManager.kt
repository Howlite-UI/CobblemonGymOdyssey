package com.howlite.moon

import dev.architectury.event.events.common.PlayerEvent
import dev.architectury.event.events.common.TickEvent
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level

/**
 * Gestionnaire central des phases lunaires personnalisées.
 *
 * ## Responsabilités
 * 1. Calculer la phase active à partir d'un index de cycle (persisté via [MoonSavedData]).
 * 2. Écouter chaque tick serveur pour détecter les transitions jour/nuit.
 * 3. Notifier tous les joueurs au début de chaque nuit spéciale.
 * 4. Synchroniser la phase vers tous les clients via [MoonNetwork].
 * 5. Permettre la manipulation manuelle via [forcePhase].
 *
 * ## Cycle lunaire
 * Sur un cycle de [MoonConfig.cycleLengthDays] jours (défaut : 16) :
 * - Jours  0–11 : NONE
 * - Jour   12   : BLUE_MOON
 * - Jour   13   : RED_MOON
 * - Jour   14   : PURPLE_MOON
 * - Jour   15   : FULL_MOON
 */
object MoonManager {

    @Volatile
    var currentPhase: MoonPhase = MoonPhase.NONE
        private set

    @Volatile
    var nextPhase: MoonPhase = MoonPhase.NONE
        private set

    /** Tick de début de nuit (≈ 13000 sur l'axe 0–24000). */
    private const val NIGHT_START_TICK = 13000L

    /** Séquence : les 4 derniers jours du cycle sont les phases spéciales. */
    private fun phaseForIndex(index: Int): MoonPhase {
        val cycleLen = MoonConfig.instance.cycleLengthDays
        return when {
            cycleLen <= 4 -> MoonPhase.entries[index % MoonPhase.entries.size]
            else -> {
                val specialStart = cycleLen - 4
                when (index % cycleLen) {
                    specialStart     -> MoonPhase.BLUE_MOON
                    specialStart + 1 -> MoonPhase.RED_MOON
                    specialStart + 2 -> MoonPhase.PURPLE_MOON
                    specialStart + 3 -> MoonPhase.FULL_MOON
                    else             -> MoonPhase.NONE
                }
            }
        }
    }

    private var nightEventFiredThisCycle = false
    private var cachedServer: MinecraftServer? = null

    fun register() {
        MoonConfig.load()

        // Tick serveur via Architectury TickEvent
        TickEvent.SERVER_POST.register { server ->
            cachedServer = server
            onServerTick(server)
        }

        // Sync à la connexion
        PlayerEvent.PLAYER_JOIN.register { player ->
            if (player is net.minecraft.server.level.ServerPlayer) {
                MoonNetwork.syncPhaseToPlayer(player, currentPhase)
            }
        }

        println("[GymOdyssey/Moon] MoonManager enregistré.")
    }

    // ─── Tick handler ────────────────────────────────────────────────────────

    private fun onServerTick(server: MinecraftServer) {
        val overworld = server.getLevel(Level.OVERWORLD) ?: return
        val dayTime = overworld.dayTime
        val absoluteDay = dayTime / 24000L
        val timeInDay = dayTime % 24000L

        val data = MoonSavedData.get(overworld)

        // Nouveau jour
        if (absoluteDay > data.lastCheckedDay) {
            data.lastCheckedDay = absoluteDay
            if (data.forcedPhase.isEmpty()) {
                data.phaseIndex = (data.phaseIndex + 1) % MoonConfig.instance.cycleLengthDays
            }
            data.setDirty()
            nightEventFiredThisCycle = false

            if (data.forcedPhase.isEmpty()) {
                val newPhase = phaseForIndex(data.phaseIndex)
                val nextIndex = (data.phaseIndex + 1) % MoonConfig.instance.cycleLengthDays
                nextPhase = phaseForIndex(nextIndex)
                applyPhaseChange(server, newPhase)
            }
        }

        // Détection début de nuit
        if (!nightEventFiredThisCycle && timeInDay in NIGHT_START_TICK..(NIGHT_START_TICK + 20L)) {
            nightEventFiredThisCycle = true
            onNightStart(server)
        }
    }

    private fun onNightStart(server: MinecraftServer) {
        if (!MoonConfig.instance.notifyOnMoonChange) return
        if (!currentPhase.isSpecial) return
        val notifKey = currentPhase.nightNotificationKey ?: return
        val message = Component.translatable(notifKey)
        server.playerList.players.forEach { it.sendSystemMessage(message) }
    }

    // ─── Phase management ────────────────────────────────────────────────────

    fun forcePhase(server: MinecraftServer, phase: MoonPhase) {
        val overworld = server.getLevel(Level.OVERWORLD) ?: return
        val data = MoonSavedData.get(overworld)
        data.forcedPhase = if (phase == MoonPhase.NONE) "" else phase.name
        data.setDirty()
        applyPhaseChange(server, phase)
        println("[GymOdyssey/Moon] Phase forcée : $phase")
    }

    private fun applyPhaseChange(server: MinecraftServer, phase: MoonPhase) {
        if (currentPhase == phase) return
        currentPhase = phase
        MoonNetwork.broadcastPhase(server, phase)
    }

    fun initFromSavedData(server: MinecraftServer) {
        val overworld = server.getLevel(Level.OVERWORLD) ?: return
        val data = MoonSavedData.get(overworld)
        currentPhase = if (data.forcedPhase.isNotEmpty()) {
            MoonPhase.fromName(data.forcedPhase)
        } else {
            phaseForIndex(data.phaseIndex)
        }
        val nextIndex = (data.phaseIndex + 1) % MoonConfig.instance.cycleLengthDays
        nextPhase = phaseForIndex(nextIndex)
        println("[GymOdyssey/Moon] Phase restaurée : $currentPhase (suivante : $nextPhase)")
    }
}
