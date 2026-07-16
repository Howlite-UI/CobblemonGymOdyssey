package com.howlite.client

import com.howlite.CobblemonGymOdyssey
import com.howlite.blocks.GymBlocks
import com.howlite.blocks.ConsumableRaidBlockEntity
import com.howlite.blocks.UnownStoneActivatedBlockEntity
import com.howlite.client.render.GymLeaderTeleporterRenderer
import com.howlite.client.render.ConsumableRaidBlockEntityRenderer
import com.howlite.client.render.UnownStoneActivatedRenderer
import com.howlite.client.render.PlayerShopBlockEntityRenderer
import com.howlite.client.screen.ConsumableRaidScreen
import dev.architectury.networking.NetworkManager
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry
import net.minecraft.client.Minecraft
import net.minecraft.resources.ResourceLocation
import com.howlite.moon.MoonNetwork

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

        BlockEntityRendererRegistry.register(GymBlocks.PLAYER_SHOP_BLOCK_ENTITY.get()) { context ->
            PlayerShopBlockEntityRenderer(context)
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

        // Register S2C packet to sync Player Shop stock count in real-time
        NetworkManager.registerReceiver(
            NetworkManager.Side.S2C,
            ResourceLocation.fromNamespaceAndPath(CobblemonGymOdyssey.MOD_ID, "player_shop_sync_stock")
        ) { buf, context ->
            val offerIdx = buf.readInt()
            val newStock = buf.readInt()
            context.queue {
                val mc = Minecraft.getInstance()
                val menu = mc.player?.containerMenu as? com.howlite.menu.PlayerShopMenu
                if (menu != null && offerIdx in menu.offers.indices) {
                    menu.offers[offerIdx].availableStock = newStock
                }
            }
        }
        // Register S2C packets for the teleport animation (Zoom Up / Zoom Down)
        com.howlite.events.TeleportAnimationNetwork.registerClientReceivers()

        // ── Phases lunaires ────────────────────────────────────────────
        // Enregistrer le receiver S2C sync_moon_phase
        MoonNetwork.registerClientReceivers()

        // Receiver S2C pour ouvrir le GUI de l'Observatoire Céleste
        NetworkManager.registerReceiver(
            NetworkManager.Side.S2C,
            com.howlite.blocks.CelestialObservatoryBlock.OPEN_GUI_PACKET
        ) { buf, context ->
            buf.readBlockPos() // Lire la position (non utilisée directement)
            context.queue {
                Minecraft.getInstance().setScreen(
                    com.howlite.client.screen.CelestialObservatoryScreen()
                )
            }
        }
    }
}
