package com.howlite

import com.howlite.blocks.GymBlocks
import com.howlite.blocks.ConsumableRaidBlock
import com.howlite.blocks.ConsumableRaidBlockEntity
import com.howlite.commands.GymTestCommand
import com.howlite.commands.GymTpCommand
import com.howlite.events.BattleLevelCapEventHandler
import com.howlite.events.CoinPickupHandler
import com.howlite.events.GymBattleEventHandler
import com.howlite.events.GymBattleReturnHandler
import com.howlite.events.LevelCapEventHandler
import com.howlite.events.PvpBattleEventHandler
import com.howlite.events.AltarBattleEventHandler
import com.howlite.items.CobbleCoins
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
    @Suppress("DEPRECATION")
    fun init() {
        println("[GymOdyssey] CobblemonGymOdyssey.init() common initialization running!")
        GymSounds.register()
        GymBadgeItems.register()
        CobbleCoins.register()
        GymBlocks.register()
        BadgeCaseMenus.register()
        LevelCapEventHandler.register()
        GymBattleEventHandler.register()
        GymBattleReturnHandler.register()
        BattleLevelCapEventHandler.register()
        CoinPickupHandler.register()
        PvpBattleEventHandler.register()
        GymTestCommand.register()
        GymTpCommand.register()
        AltarBattleEventHandler.register()

        // Synchroniser le wallet lors de la connexion du joueur et l'enregistrer dans le tracker PvP
        dev.architectury.event.events.common.PlayerEvent.PLAYER_JOIN.register { player ->
            if (player is ServerPlayer) {
                val server = player.server
                if (server != null) {
                    if (com.howlite.data.PvpPlayerTracker.registeredPlayers.isEmpty()) {
                        com.howlite.data.PvpPlayerTracker.load(server)
                    }
                    com.howlite.data.PvpPlayerTracker.registerPlayer(server, player.uuid, player.scoreboardName)
                }

                val wallet = com.howlite.wallet.WalletManager.get(player)
                com.howlite.wallet.WalletNetwork.syncToClient(player, wallet)

                // Safety return for Altar battles: if the player was in a battle arena, teleport them back
                val progress = com.howlite.api.PlayerProgressApi.get(player)
                if (progress.activeAltarBet > 0L && progress.returnDim != null) {
                    val returnDim = progress.returnDim!!
                    val returnX = progress.returnX ?: 0.0
                    val returnY = progress.returnY ?: 64.0
                    val returnZ = progress.returnZ ?: 0.0
                    val returnYaw = progress.returnYaw ?: 0f
                    val returnPitch = progress.returnPitch ?: 0f

                    // Lose the bet (disconnected = forfeit)
                    progress.activeAltarBet = 0L
                    progress.activeAltarDifficulty = 0
                    progress.clearReturnPosition()
                    com.howlite.api.PlayerProgressApi.markDirty(player)

                    val returnLevelKey = net.minecraft.resources.ResourceKey.create(
                        net.minecraft.core.registries.Registries.DIMENSION,
                        net.minecraft.resources.ResourceLocation.parse(returnDim)
                    )
                    val targetWorld = player.server.getLevel(returnLevelKey) ?: player.server.overworld()
                    player.teleportTo(targetWorld, returnX, returnY, returnZ, returnYaw, returnPitch)
                    player.sendSystemMessage(
                        net.minecraft.network.chat.Component.translatable("cobblemongymodyssey.altar.msg.returned_disconnected")
                    )
                }
            }
        }

        // Register Request PvP Player List Network Packet Receiver (Client -> Server)
        NetworkManager.registerReceiver(
            NetworkManager.Side.C2S,
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "request_pvp_player_list")
        ) { _, context ->
            val player = context.player
            if (player is ServerPlayer) {
                context.queue {
                    val server = player.server ?: return@queue
                    if (com.howlite.data.PvpPlayerTracker.registeredPlayers.isEmpty()) {
                        com.howlite.data.PvpPlayerTracker.load(server)
                    }
                    val playersList = com.howlite.data.PvpPlayerTracker.registeredPlayers.toList()

                    val buf = net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), player.registryAccess())
                    buf.writeInt(playersList.size)
                    for (profile in playersList) {
                        buf.writeUtf(profile.uuid)
                        buf.writeUtf(profile.name)
                        val isOnline = server.playerList.getPlayer(java.util.UUID.fromString(profile.uuid)) != null
                        buf.writeBoolean(isOnline)
                    }
                    NetworkManager.sendToPlayer(
                        player,
                        ResourceLocation.fromNamespaceAndPath(MOD_ID, "sync_pvp_player_list"),
                        buf
                    )
                }
            }
        }

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
                                BadgeCaseMenu(
                                    syncId,
                                    badges,
                                    levelCap,
                                    badgeTeams,
                                    progress.pvpWins,
                                    progress.pvpLosses,
                                    progress.pvpRewardsClaimedToday,
                                    progress.pvpFights
                                )
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
                        // Sync PvP stats to client
                        println("[GymOdyssey] Server: Syncing PvP stats to ${player.scoreboardName} (${player.uuid}) client buffer. Wins: ${progress.pvpWins}, Losses: ${progress.pvpLosses}, Opponent fights: ${progress.pvpFights.size}")
                        buffer.writeInt(progress.pvpWins)
                        buffer.writeInt(progress.pvpLosses)
                        buffer.writeInt(progress.pvpRewardsClaimedToday)
                        buffer.writeInt(progress.pvpFights.size)
                        progress.pvpFights.forEach { (opponentUuid, record) ->
                            buffer.writeUtf(opponentUuid)
                            buffer.writeUtf(record.lastFightDate)
                            buffer.writeInt(record.consecutiveDays)
                            buffer.writeInt(record.wins)
                            buffer.writeInt(record.losses)
                        }
                    }
                }
            }
        }

        // Register Request Altar Challenge Network Packet Receiver (Client -> Server)
        NetworkManager.registerReceiver(
            NetworkManager.Side.C2S,
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "request_altar_challenge")
        ) { buf, context ->
            val betCCC = buf.readLong()
            val difficulty = buf.readInt()
            val regionName = buf.readUtf()

            context.queue {
                val player = context.player
                if (player !is ServerPlayer) return@queue

                val progress = com.howlite.api.PlayerProgressApi.get(player)

                // --- Server-side validations ---
                val wallet = com.howlite.wallet.WalletManager.get(player)

                // 1. Bet > 0
                if (betCCC <= 0L) {
                    player.sendSystemMessage(
                        net.minecraft.network.chat.Component.translatable("cobblemongymodyssey.altar.msg.invalid_bet")
                    )
                    return@queue
                }

                // 2. Balance sufficient
                if (wallet.balanceCCC < betCCC) {
                    player.sendSystemMessage(
                        net.minecraft.network.chat.Component.translatable("cobblemongymodyssey.altar.msg.insufficient_balance")
                    )
                    return@queue
                }

                // 3. Not already in a battle
                if (progress.activeAltarBet > 0L) {
                    player.sendSystemMessage(
                        net.minecraft.network.chat.Component.translatable("cobblemongymodyssey.altar.msg.already_active")
                    )
                    return@queue
                }

                // 4. Team size check
                val maxPokemon = when (difficulty) {
                    1 -> 3; 2 -> 2; 3 -> 1; else -> 3
                }
                val party = com.cobblemon.mod.common.Cobblemon.storage.getParty(player)
                val teamSize = party.filterNotNull().count()
                if (teamSize > maxPokemon) {
                    player.sendSystemMessage(
                        net.minecraft.network.chat.Component.translatable(
                            "cobblemongymodyssey.altar.msg.exceed_team_limit",
                            maxPokemon
                        )
                    )
                    return@queue
                }
                if (teamSize == 0) {
                    player.sendSystemMessage(
                        net.minecraft.network.chat.Component.translatable("cobblemongymodyssey.altar.msg.no_pkm_team")
                    )
                    return@queue
                }

                // --- Deduct bet ---
                val success = com.howlite.wallet.WalletManager.removeAndSync(player, betCCC)
                if (!success) {
                    player.sendSystemMessage(
                        net.minecraft.network.chat.Component.translatable("cobblemongymodyssey.altar.msg.insufficient_balance")
                    )
                    return@queue
                }

                // --- Save return position ---
                val currentDim = player.level().dimension().location().toString()
                progress.saveReturnPosition(
                    currentDim,
                    player.x, player.y, player.z,
                    player.yRot, player.xRot
                )
                progress.activeAltarBet = betCCC
                progress.activeAltarDifficulty = difficulty
                com.howlite.api.PlayerProgressApi.markDirty(player)

                // --- Spawn the boss wild battle ---
                com.howlite.events.AltarBossSpawner.startAltarBattle(player, regionName, difficulty)
            }
        }
    }
}
