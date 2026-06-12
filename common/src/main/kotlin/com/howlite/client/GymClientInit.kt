package com.howlite.client

import com.howlite.blocks.GymBlocks
import com.howlite.client.render.GymLeaderTeleporterRenderer
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry

object GymClientInit {
    fun init() {
        BlockEntityRendererRegistry.register(GymBlocks.GYM_LEADER_TELEPORTER_ENTITY.get()) { context ->
            GymLeaderTeleporterRenderer(context)
        }
    }
}
