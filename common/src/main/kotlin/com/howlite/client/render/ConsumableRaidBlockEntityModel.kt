package com.howlite.client.render

import com.howlite.blocks.ConsumableRaidBlockEntity
import com.necro.raid.dens.common.blocks.block.RaidCrystalBlock
import com.necro.raid.dens.common.data.raid.RaidType
import net.minecraft.resources.ResourceLocation
import software.bernie.geckolib.model.GeoModel

@Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
class ConsumableRaidBlockEntityModel : GeoModel<ConsumableRaidBlockEntity>() {
    override fun getModelResource(animatable: ConsumableRaidBlockEntity): ResourceLocation {
        return ResourceLocation.fromNamespaceAndPath(
            "cobblemongymodyssey",
            "geo/raid_crystal_block.geo.json"
        )
    }

    override fun getTextureResource(animatable: ConsumableRaidBlockEntity): ResourceLocation {
        val blockState = animatable.blockState
        val type = if (blockState.hasProperty(RaidCrystalBlock.RAID_TYPE)) {
            blockState.getValue(RaidCrystalBlock.RAID_TYPE)
        } else {
            RaidType.NONE
        }

        val typeName = type.name.lowercase()
        val texturePath = if (type == RaidType.NONE) {
            "textures/block/raidcrystal/raid_crystal_block.png"
        } else {
            "textures/block/raidcrystal/raid_crystal_block_$typeName.png"
        }
        return ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", texturePath)
    }

    override fun getAnimationResource(animatable: ConsumableRaidBlockEntity): ResourceLocation? {
        return null // Floating and spinning are handled programmatically in the renderer
    }
}
