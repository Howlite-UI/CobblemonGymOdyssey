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
import com.howlite.events.EconomyEventHandler
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
        EconomyEventHandler.register()

        com.cobblemon.mod.common.Cobblemon.statProvider = AltarStatProvider(com.cobblemon.mod.common.Cobblemon.statProvider)

        try {
            com.cobblemon.mod.common.Cobblemon.config.maxPokemonLevel = 300
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Bloquer le spawn naturel de Pokémon sauvages dans la dimension d'arènes
        com.cobblemon.mod.common.api.events.CobblemonEvents.POKEMON_ENTITY_SPAWN.subscribe { event ->
            val entity = event.entity
            val level = entity.level()
            if (level.isClientSide) return@subscribe
            if (level.dimension().location().toString() == "cobblemongymodyssey:gym_dimension") {
                val nick = entity.pokemon.nickname?.string ?: ""
                val isBoss = nick.startsWith("§c[Boss]")
                val isPlayerOwned = entity.pokemon.getOwnerUUID() != null
                if (!isBoss && !isPlayerOwned) {
                    event.cancel()
                }
            }
        }

        // Bloquer le chargement de Pokémon sauvages ou de templates (qui n'ont pas le préfixe [Boss]) dans la dimension d'arènes
        com.cobblemon.mod.common.api.events.CobblemonEvents.POKEMON_ENTITY_LOAD.subscribe { event ->
            val entity = event.pokemonEntity
            val level = entity.level()
            if (level.isClientSide) return@subscribe
            if (level.dimension().location().toString() == "cobblemongymodyssey:gym_dimension") {
                val nick = entity.pokemon.nickname?.string ?: ""
                val isBoss = nick.startsWith("§c[Boss]")
                val isPlayerOwned = entity.pokemon.getOwnerUUID() != null
                if (!isBoss && !isPlayerOwned) {
                    event.cancel()
                }
            }
        }


        // Bloquer tout autre spawn ou ajout de Pokémon sauvage (non-boss et non-possédé par un joueur) dans la dimension d'arènes
        dev.architectury.event.events.common.EntityEvent.ADD.register { entity, level ->
            if (level.isClientSide) return@register dev.architectury.event.EventResult.pass()
            if (level.dimension().location().toString() == "cobblemongymodyssey:gym_dimension") {
                if (entity is com.cobblemon.mod.common.entity.pokemon.PokemonEntity) {
                    val isBoss = entity.pokemon.nickname?.string?.startsWith("§c[Boss]") == true
                    val isPlayerOwned = entity.pokemon.getOwnerUUID() != null
                    if (!isBoss && !isPlayerOwned) {
                        return@register dev.architectury.event.EventResult.interruptFalse()
                    }
                }
            }
            dev.architectury.event.EventResult.pass()
        }


        // Bloquer la casse de blocs dans la dimension d'arènes pour les joueurs en survie/aventure
        dev.architectury.event.events.common.BlockEvent.BREAK.register { level, pos, state, player, xp ->
            if (level.dimension().location().toString() == "cobblemongymodyssey:gym_dimension") {
                if (player != null && !player.isCreative()) {
                    return@register dev.architectury.event.EventResult.interruptFalse()
                }
            }
            dev.architectury.event.EventResult.pass()
        }

        // Bloquer la pose de blocs dans la dimension d'arènes pour les joueurs en survie/aventure
        dev.architectury.event.events.common.BlockEvent.PLACE.register { level, pos, state, entity ->
            if (level.dimension().location().toString() == "cobblemongymodyssey:gym_dimension") {
                if (entity is net.minecraft.world.entity.player.Player && !entity.isCreative()) {
                    return@register dev.architectury.event.EventResult.interruptFalse()
                }
            }
            dev.architectury.event.EventResult.pass()
        }

        // Bloquer les interactions indésirables avec des blocs (seaux, briquet, etc.) dans la dimension d'arènes
        dev.architectury.event.events.common.InteractionEvent.RIGHT_CLICK_BLOCK.register { player, hand, pos, direction ->
            if (player.level().dimension().location().toString() == "cobblemongymodyssey:gym_dimension") {
                if (!player.isCreative()) {
                    val stack = player.getItemInHand(hand)
                    val item = stack.item
                    if (item is net.minecraft.world.item.BucketItem ||
                        item is net.minecraft.world.item.BlockItem ||
                        item is net.minecraft.world.item.FlintAndSteelItem ||
                        item is net.minecraft.world.item.FireChargeItem ||
                        item is net.minecraft.world.item.SpawnEggItem ||
                        item is net.minecraft.world.item.HoeItem ||
                        item is net.minecraft.world.item.ShovelItem ||
                        item is net.minecraft.world.item.AxeItem
                    ) {
                        return@register dev.architectury.event.EventResult.interruptFalse()
                    }
                }
            }
            dev.architectury.event.EventResult.pass()
        }
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
                    com.howlite.items.BadgeCaseHelper.openMenu(player)
                }
            }
        }

        // Register Claim Daily Allowance Network Packet Receiver (Client -> Server)
        NetworkManager.registerReceiver(
            NetworkManager.Side.C2S,
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "claim_daily_allowance")
        ) { buf, context ->
            val regionName = buf.readUtf()
            context.queue {
                val player = context.player
                if (player !is ServerPlayer) return@queue

                val progress = com.howlite.api.PlayerProgressApi.get(player)
                val todayStr = java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString()

                if (progress.dailyAllowanceClaims[regionName] == todayStr) {
                    player.sendSystemMessage(
                        Component.translatable("cobblemongymodyssey.daily.msg.already_claimed")
                    )
                    return@queue
                }

                val region = com.howlite.screen.BadgeCaseScreen.Region.entries.find { it.name == regionName }
                if (region == null) return@queue

                // Server-side validation: must have completed the region (beaten all gym leaders)
                val isCompleted = region.badges.isNotEmpty() && region.badges.all { progress.hasBadge(it) }
                if (!isCompleted) {
                    player.sendSystemMessage(
                        Component.translatable("cobblemongymodyssey.daily.msg.not_completed")
                    )
                    return@queue
                }

                val unlockedInRegion = region.badges.count { progress.hasBadge(it) }
                val base = (region.ordinal + 1) * 2_000L
                val bonus = unlockedInRegion * (region.ordinal + 1) * 500L
                val total = base + bonus

                progress.claimDailyAllowance(regionName, todayStr)
                com.howlite.api.PlayerProgressApi.markDirty(player)

                val wallet = com.howlite.wallet.WalletManager.get(player)
                wallet.balanceCCC += total
                com.howlite.wallet.WalletNetwork.syncToClient(player, wallet)

                val coinStr = com.howlite.wallet.WalletManager.formatCCC(total)
                player.sendSystemMessage(
                    Component.translatable("cobblemongymodyssey.economy.daily_allowance", coinStr)
                )

                com.howlite.items.BadgeCaseHelper.openMenu(player)
            }
        }

        // Register Claim All Daily Allowances Network Packet Receiver (Client -> Server)
        NetworkManager.registerReceiver(
            NetworkManager.Side.C2S,
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "claim_all_daily_allowances")
        ) { buf, context ->
            context.queue {
                val player = context.player
                if (player !is ServerPlayer) return@queue

                val progress = com.howlite.api.PlayerProgressApi.get(player)
                val todayStr = java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString()

                var totalReward = 0L
                var claimedAny = false

                com.howlite.screen.BadgeCaseScreen.Region.entries.forEach { region ->
                    val isCompleted = region.badges.isNotEmpty() && region.badges.all { progress.hasBadge(it) }
                    if (isCompleted && progress.dailyAllowanceClaims[region.name] != todayStr) {
                        val unlockedInRegion = region.badges.count { progress.hasBadge(it) }
                        val base = (region.ordinal + 1) * 2_000L
                        val bonus = unlockedInRegion * (region.ordinal + 1) * 500L
                        val total = base + bonus

                        progress.claimDailyAllowance(region.name, todayStr)
                        totalReward += total
                        claimedAny = true
                    }
                }

                if (claimedAny) {
                    com.howlite.api.PlayerProgressApi.markDirty(player)

                    val wallet = com.howlite.wallet.WalletManager.get(player)
                    wallet.balanceCCC += totalReward
                    com.howlite.wallet.WalletNetwork.syncToClient(player, wallet)

                    val coinStr = com.howlite.wallet.WalletManager.formatCCC(totalReward)
                    player.sendSystemMessage(
                        Component.translatable("cobblemongymodyssey.economy.daily_allowance", coinStr)
                    )
                } else {
                    player.sendSystemMessage(
                        Component.translatable("cobblemongymodyssey.daily.msg.already_claimed")
                    )
                }

                com.howlite.items.BadgeCaseHelper.openMenu(player)
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

                // 3.5. Daily limit check (5 fights per region per day)
                val fightsToday = progress.getAltarFightsToday(regionName)
                if (fightsToday >= 5) {
                    player.sendSystemMessage(
                        net.minecraft.network.chat.Component.translatable("cobblemongymodyssey.altar.msg.limit_reached")
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
                progress.incrementAltarFights(regionName)
                progress.activeAltarBet = betCCC
                progress.activeAltarDifficulty = difficulty
                com.howlite.api.PlayerProgressApi.markDirty(player)

                // --- Spawn the boss wild battle ---
                com.howlite.events.AltarBossSpawner.startAltarBattle(player, regionName, difficulty)
            }
        }
    }
}

class AltarStatProvider(val delegate: com.cobblemon.mod.common.api.pokemon.stats.StatProvider) : com.cobblemon.mod.common.api.pokemon.stats.StatProvider by delegate {
    override fun getStatForPokemon(pokemon: com.cobblemon.mod.common.pokemon.Pokemon, stat: com.cobblemon.mod.common.api.pokemon.stats.Stat): Int {
        val baseValue = delegate.getStatForPokemon(pokemon, stat)
        val nick = pokemon.nickname?.string ?: ""
        if (nick.startsWith("§c[Boss]")) {
            if (stat == com.cobblemon.mod.common.api.pokemon.stats.Stats.HP) return baseValue
            val multiplier = when (pokemon.level) {
                150 -> 1.2
                200 -> 1.6
                300 -> 2.0
                else -> 1.0
            }
            return (baseValue * multiplier).toInt()
        }
        return baseValue
    }
}
