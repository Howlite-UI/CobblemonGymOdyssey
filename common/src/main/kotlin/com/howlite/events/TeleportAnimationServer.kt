package com.howlite.events

import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import java.util.UUID
import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.CobblemonEntities
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity

/**
 * Gestion serveur de l'animation de téléportation.
 *
 * Fonctionnement :
 *  1. Quand [ServerPlayerMixin] détecte une téléportation vers Overworld ou End, il appelle [startAnimation].
 *  2. Quand [WaystoneTeleportManagerMixin] détecte une téléportation Waystone, il appelle [startWaystoneAnimation].
 *  3. [startAnimation] / [startWaystoneAnimation] envoie le paquet [TeleportAnimationNetwork.START_TP_ANIM_ID] au client
 *     et enregistre la TP correspondante avec un délai de 30 ticks (1,5 s).
 *  4. À chaque tick via [MinecraftServerMixin], [tick] décrémente le compteur des téléportations en attente.
 *     Quand le timer expire, la téléportation physique est exécutée et le paquet de fin est envoyé.
 *  5. Le joueur reste figé pendant le décollage et 20 ticks après l'atterrissage.
 */
object TeleportAnimationServer {

    /** Délai avant la téléportation physique : 40 ticks = 2.0 secondes */
    private const val ZOOM_UP_TICKS = 40

    /** Durée de gel post-TP pendant le zoom-down : 30 ticks = 1.5 secondes */
    private const val ZOOM_DOWN_TICKS = 30

    // ------------------------------------------------------------------
    // État
    // ------------------------------------------------------------------

    data class PendingTeleport(
        val playerUuid: UUID,
        val targetLevel: ServerLevel,
        val targetX: Double,
        val targetY: Double,
        val targetZ: Double,
        val targetYaw: Float,
        val targetPitch: Float,
        val vehicleUuid: UUID?,
        var remainingTicks: Int = ZOOM_UP_TICKS
    )

    data class PendingWaystoneTeleport(
        val playerUuid: UUID,
        val context: Any, // Type Any pour compiler sans Waystones sur Fabric au runtime
        val destination: Any,
        val vehicleUuid: UUID?,
        var remainingTicks: Int = ZOOM_UP_TICKS
    )

    data class FrozenPlayer(
        val uuid: UUID,
        var remainingTicks: Int = ZOOM_DOWN_TICKS
    )

    private val pendingTeleports = mutableListOf<PendingTeleport>()
    private val pendingWaystoneTeleports = mutableListOf<PendingWaystoneTeleport>()
    private val frozenPlayers = mutableListOf<FrozenPlayer>()

    /**
     * UUIDs des joueurs dont la téléportation est en cours d'exécution par nous-mêmes,
     * pour éviter que le Mixin ne ré-intercepte la TP qu'on déclenche nous-mêmes.
     */
    val executingDelayedTeleports: MutableSet<UUID> =
        java.util.Collections.synchronizedSet(mutableSetOf())

    val executingWaystoneTeleports: MutableSet<UUID> =
        java.util.Collections.synchronizedSet(mutableSetOf())

    /** @return true si ce joueur est actuellement en animation de TP ou figé après TP. */
    fun isTeleporting(uuid: UUID): Boolean =
        pendingTeleports.any { it.playerUuid == uuid } || 
        pendingWaystoneTeleports.any { it.playerUuid == uuid } || 
        frozenPlayers.any { it.uuid == uuid }

    // ------------------------------------------------------------------
    // Déclenchement de l'animation
    // ------------------------------------------------------------------

    /**
     * Démarre l'animation de téléportation pour [player] vers la destination indiquée.
     * Doit être appelé depuis le Mixin qui intercepte [ServerPlayer.teleportTo].
     */
    fun startAnimation(
        player: ServerPlayer,
        targetLevel: ServerLevel,
        targetX: Double,
        targetY: Double,
        targetZ: Double,
        targetYaw: Float,
        targetPitch: Float
    ) {
        // Paquet réseau → zoom-up côté client
        TeleportAnimationNetwork.sendStartAnimation(player, player.x, player.y, player.z)

        // Son de décollage (court, non-looping)
        player.level().playSound(
            null, player.blockPosition(),
            SoundEvents.ENDERMAN_TELEPORT,
            SoundSource.PLAYERS,
            1.0f, 0.7f
        )

        val vehicleUuid = player.vehicle?.uuid

        // Enregistrement de la TP en attente
        pendingTeleports.add(
            PendingTeleport(
                playerUuid = player.uuid,
                targetLevel = targetLevel,
                targetX = targetX,
                targetY = targetY,
                targetZ = targetZ,
                targetYaw = targetYaw,
                targetPitch = targetPitch,
                vehicleUuid = vehicleUuid
            )
        )
    }

    /**
     * Démarre l'animation différée pour les Waystones.
     */
    fun startWaystoneAnimation(
        player: ServerPlayer,
        context: Any,
        destination: Any
    ) {
        // Paquet réseau → zoom-up côté client
        TeleportAnimationNetwork.sendStartAnimation(player, player.x, player.y, player.z)

        // Son de décollage
        player.level().playSound(
            null, player.blockPosition(),
            SoundEvents.ENDERMAN_TELEPORT,
            SoundSource.PLAYERS,
            1.0f, 0.7f
        )

        val vehicleUuid = player.vehicle?.uuid

        pendingWaystoneTeleports.add(
            PendingWaystoneTeleport(
                playerUuid = player.uuid,
                context = context,
                destination = destination,
                vehicleUuid = vehicleUuid
            )
        )
    }

    // ------------------------------------------------------------------
    // Tick (appelé par MinecraftServerMixin chaque tick)
    // ------------------------------------------------------------------

    fun tick(server: MinecraftServer) {
        // --- Joueurs en phase de décollage (TP classique) ---
        val tpIter = pendingTeleports.iterator()
        while (tpIter.hasNext()) {
            val pending = tpIter.next()
            val player = server.playerList.getPlayer(pending.playerUuid)

            if (player == null) {
                tpIter.remove()
                continue
            }

            // Figer le joueur en place
            player.deltaMovement = Vec3.ZERO
            player.connection.resetPosition()

            // Figer et maintenir le véhicule (Pokémon, etc.) s'il y en a un
            if (pending.vehicleUuid != null) {
                val vehicle = player.serverLevel().getEntity(pending.vehicleUuid)
                if (vehicle != null) {
                    vehicle.deltaMovement = Vec3.ZERO
                    vehicle.setPos(player.x, player.y, player.z)
                    if (player.vehicle != vehicle) {
                        player.startRiding(vehicle, true)
                    }
                }
            }

            pending.remainingTicks--

            if (pending.remainingTicks <= 0) {
                tpIter.remove()
                executeTeleport(player, pending)
            }
        }

        // --- Joueurs en phase de décollage (Waystones) ---
        val wsIter = pendingWaystoneTeleports.iterator()
        while (wsIter.hasNext()) {
            val pending = wsIter.next()
            val player = server.playerList.getPlayer(pending.playerUuid)

            if (player == null) {
                wsIter.remove()
                continue
            }

            // Figer le joueur en place
            player.deltaMovement = Vec3.ZERO
            player.connection.resetPosition()

            // Figer et maintenir le véhicule s'il y en a un
            if (pending.vehicleUuid != null) {
                val vehicle = player.serverLevel().getEntity(pending.vehicleUuid)
                if (vehicle != null) {
                    vehicle.deltaMovement = Vec3.ZERO
                    vehicle.setPos(player.x, player.y, player.z)
                    if (player.vehicle != vehicle) {
                        player.startRiding(vehicle, true)
                    }
                }
            }

            pending.remainingTicks--

            if (pending.remainingTicks <= 0) {
                wsIter.remove()
                executeWaystoneTeleport(player, pending)
            }
        }

        // --- Joueurs en phase d'atterrissage (gel post-TP) ---
        val freezeIter = frozenPlayers.iterator()
        while (freezeIter.hasNext()) {
            val fp = freezeIter.next()
            val player = server.playerList.getPlayer(fp.uuid)

            if (player == null) {
                freezeIter.remove()
                continue
            }

            player.deltaMovement = Vec3.ZERO
            player.connection.resetPosition()

            fp.remainingTicks--
            if (fp.remainingTicks <= 0) {
                freezeIter.remove()
            }
        }
    }

    // ------------------------------------------------------------------
    // Exécution de la téléportation physique
    // ------------------------------------------------------------------

    private fun executeTeleport(player: ServerPlayer, pending: PendingTeleport) {
        executingDelayedTeleports.add(player.uuid)
        try {
            val vehicle = if (pending.vehicleUuid != null) player.serverLevel().getEntity(pending.vehicleUuid) else null
            if (vehicle != null) {
                player.stopRiding()
                vehicle.teleportTo(
                    pending.targetLevel,
                    pending.targetX,
                    pending.targetY,
                    pending.targetZ,
                    emptySet(),
                    pending.targetYaw,
                    pending.targetPitch
                )
                player.teleportTo(
                    pending.targetLevel,
                    pending.targetX,
                    pending.targetY,
                    pending.targetZ,
                    emptySet(),
                    pending.targetYaw,
                    pending.targetPitch
                )
                player.startRiding(vehicle, true)
            } else {
                player.teleportTo(
                    pending.targetLevel,
                    pending.targetX,
                    pending.targetY,
                    pending.targetZ,
                    emptySet(),
                    pending.targetYaw,
                    pending.targetPitch
                )
            }
        } finally {
            executingDelayedTeleports.remove(player.uuid)
        }

        // Paquet réseau → zoom-down côté client
        TeleportAnimationNetwork.sendEndAnimation(player)

        // Son d'atterrissage
        pending.targetLevel.playSound(
            null,
            net.minecraft.core.BlockPos.containing(pending.targetX, pending.targetY, pending.targetZ),
            SoundEvents.BEACON_ACTIVATE,
            SoundSource.PLAYERS,
            0.8f, 1.2f
        )

        frozenPlayers.add(FrozenPlayer(player.uuid))
    }

    private fun executeWaystoneTeleport(player: ServerPlayer, pending: PendingWaystoneTeleport) {
        executingWaystoneTeleports.add(player.uuid)
        try {
            val vehicle = if (pending.vehicleUuid != null) player.serverLevel().getEntity(pending.vehicleUuid) else null
            if (vehicle != null) {
                player.stopRiding()
            }

            net.blay09.mods.waystones.core.WaystoneTeleportManager.doTeleport(
                pending.context as net.blay09.mods.waystones.api.WaystoneTeleportContext,
                pending.destination as net.blay09.mods.waystones.api.TeleportDestination
            )

            if (vehicle != null) {
                val targetLevel = player.serverLevel()
                vehicle.teleportTo(
                    targetLevel,
                    player.x,
                    player.y,
                    player.z,
                    emptySet(),
                    player.yRot,
                    player.xRot
                )
                player.startRiding(vehicle, true)
            }
        } finally {
            executingWaystoneTeleports.remove(player.uuid)
        }

        // Paquet réseau → zoom-down côté client
        TeleportAnimationNetwork.sendEndAnimation(player)

        // Son d'atterrissage à la nouvelle position du joueur
        player.level().playSound(
            null,
            player.blockPosition(),
            SoundEvents.BEACON_ACTIVATE,
            SoundSource.PLAYERS,
            0.8f, 1.2f
        )

        frozenPlayers.add(FrozenPlayer(player.uuid))
    }

    /**
     * Variante pour les téléportations intra-dimension (ex: Waystones).
     */
    fun startAnimationFromEntity(player: ServerPlayer, x: Double, y: Double, z: Double) {
        val serverLevel = player.level() as? ServerLevel ?: return
        startAnimation(player, serverLevel, x, y, z, player.yRot, player.xRot)
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Détermine si une dimension est éligible à l'animation (Overworld ou End). */
    fun isDimensionEligible(level: ServerLevel): Boolean =
        level.dimension() == Level.OVERWORLD || level.dimension() == Level.END

    /**
     * Retourne true si le joueur possède au moins un Pokémon rideable dans son équipe.
     * Un Pokémon rideable est un Pokémon dont la forme a au moins un comportement de monte.
     */
    fun hasRideablePokemon(player: ServerPlayer): Boolean {
        return try {
            val party = Cobblemon.storage.getParty(player.uuid, player.registryAccess())
            party.filterNotNull().any { pokemon ->
                val behaviours = pokemon.form.riding.behaviours
                behaviours != null && behaviours.isNotEmpty()
            }
        } catch (e: Exception) {
            false
        }
    }
}
