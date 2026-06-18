package com.howlite.client

import com.howlite.CobblemonGymOdyssey
import com.howlite.blocks.GymBlocks
import com.howlite.blocks.ConsumableRaidBlockEntity
import com.howlite.blocks.UnownStoneActivatedBlockEntity
import com.howlite.client.render.GymLeaderTeleporterRenderer
import com.howlite.client.render.ConsumableRaidBlockEntityRenderer
import com.howlite.client.render.UnownStoneActivatedRenderer
import com.howlite.client.screen.ConsumableRaidScreen
import dev.architectury.networking.NetworkManager
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry
import net.minecraft.client.Minecraft
import net.minecraft.resources.ResourceLocation

object GymClientInit {
    @Suppress("DEPRECATION")
    fun init() {
        BlockEntityRendererRegistry.register(GymBlocks.GYM_LEADER_TELEPORTER_ENTITY.get()) { context ->
            GymLeaderTeleporterRenderer(context)
        }

        BlockEntityRendererRegistry.register(GymBlocks.CONSUMABLE_RAID_BLOCK_ENTITY.get()) { context ->
            ConsumableRaidBlockEntityRenderer(context)
        }

        BlockEntityRendererRegistry.register(GymBlocks.UNOWN_STONE_ACTIVATED_ENTITY.get()) { context ->
            UnownStoneActivatedRenderer(context)
        }

        // Register S2C packet to open Consumable Raid GUI
        NetworkManager.registerReceiver(
            NetworkManager.Side.S2C,
            ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "open_consumable_raid_gui")
        ) { buf, context ->
            val pos = buf.readBlockPos()
            context.queue {
                val level = Minecraft.getInstance().level
                val be = level?.getBlockEntity(pos) as? ConsumableRaidBlockEntity
                if (be != null) {
                    val raidBoss = be.getRaidBoss()
                    if (raidBoss != null) {
                        Minecraft.getInstance().setScreen(ConsumableRaidScreen(pos, be, raidBoss))
                    }
                }
            }
        }

        // Register S2C packet to sync PvP player list and open FightScreen
        NetworkManager.registerReceiver(
            NetworkManager.Side.S2C,
            ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "sync_pvp_player_list")
        ) { buf, context ->
            val size = buf.readInt()
            val players = mutableListOf<com.howlite.screen.FightScreen.PvpPlayerEntry>()
            for (i in 0 until size) {
                val uuid = java.util.UUID.fromString(buf.readUtf())
                val name = buf.readUtf()
                val isOnline = buf.readBoolean()
                players.add(com.howlite.screen.FightScreen.PvpPlayerEntry(uuid, name, isOnline))
            }
            context.queue {
                val mc = Minecraft.getInstance()
                val screen = mc.screen
                if (screen is com.howlite.screen.BadgeCaseScreen) {
                    val parentMenu = screen.menu
                    mc.setScreen(com.howlite.screen.FightScreen(parentMenu, players))
                }
            }
        }
    }
}
