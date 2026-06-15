package com.howlite.blocks

import com.howlite.CobblemonGymOdyssey
import com.howlite.items.GymBadgeItems
import dev.architectury.registry.CreativeTabRegistry
import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
import net.minecraft.core.registries.Registries
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour

object GymBlocks {
    private val BLOCKS: DeferredRegister<Block> =
        DeferredRegister.create(CobblemonGymOdyssey.MOD_ID, Registries.BLOCK)

    private val BLOCK_ENTITY_TYPES: DeferredRegister<BlockEntityType<*>> =
        DeferredRegister.create(CobblemonGymOdyssey.MOD_ID, Registries.BLOCK_ENTITY_TYPE)

    private val ITEMS: DeferredRegister<Item> =
        DeferredRegister.create(CobblemonGymOdyssey.MOD_ID, Registries.ITEM)

    val GYM_LEADER_TELEPORTER: RegistrySupplier<Block> = BLOCKS.register("gym_leader_teleporter") {
        GymLeaderTeleporterBlock(BlockBehaviour.Properties.of().strength(3.0f).requiresCorrectToolForDrops())
    }

    val GYM_LEADER_TELEPORTER_ITEM: RegistrySupplier<Item> = ITEMS.register("gym_leader_teleporter") {
        BlockItem(GYM_LEADER_TELEPORTER.get(), Item.Properties())
    }

    val SMALL_ROCK: RegistrySupplier<Block> = BLOCKS.register("small_rock") {
        SmallRockBlock(BlockBehaviour.Properties.of().strength(1.5f).requiresCorrectToolForDrops().noOcclusion())
    }

    val SMALL_ROCK_ITEM: RegistrySupplier<Item> = ITEMS.register("small_rock") {
        BlockItem(SMALL_ROCK.get(), Item.Properties())
    }

    val CONSUMABLE_RAID_BLOCK: RegistrySupplier<Block> = BLOCKS.register("consumable_raid_block") {
        ConsumableRaidBlock(BlockBehaviour.Properties.of().strength(3.0f).requiresCorrectToolForDrops().noOcclusion())
    }

    val CONSUMABLE_RAID_BLOCK_ITEM: RegistrySupplier<Item> = ITEMS.register("consumable_raid_block") {
        BlockItem(CONSUMABLE_RAID_BLOCK.get(), Item.Properties())
    }

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    val GYM_LEADER_TELEPORTER_ENTITY: RegistrySupplier<BlockEntityType<GymLeaderTeleporterBlockEntity>> =
        BLOCK_ENTITY_TYPES.register("gym_leader_teleporter") {
            BlockEntityType.Builder.of(::GymLeaderTeleporterBlockEntity, GYM_LEADER_TELEPORTER.get()).build(null)
        }

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    val CONSUMABLE_RAID_BLOCK_ENTITY: RegistrySupplier<BlockEntityType<ConsumableRaidBlockEntity>> =
        BLOCK_ENTITY_TYPES.register("consumable_raid_block") {
            BlockEntityType.Builder.of(::ConsumableRaidBlockEntity, CONSUMABLE_RAID_BLOCK.get()).build(null)
        }

    fun register() {
        BLOCKS.register()
        BLOCK_ENTITY_TYPES.register()
        ITEMS.register()

        // Append item to the badges creative tab
        CreativeTabRegistry.append(
            GymBadgeItems.GYM_BADGES_TAB,
            GYM_LEADER_TELEPORTER_ITEM,
            SMALL_ROCK_ITEM,
            CONSUMABLE_RAID_BLOCK_ITEM
        )
    }
}
