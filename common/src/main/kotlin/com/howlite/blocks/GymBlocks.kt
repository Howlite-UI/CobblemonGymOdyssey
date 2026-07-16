package com.howlite.blocks

import com.howlite.CobblemonGymOdyssey
import com.howlite.items.GymBadgeItems
import com.necro.raid.dens.common.data.raid.RaidTier
import dev.architectury.registry.CreativeTabRegistry
import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
import net.minecraft.core.registries.Registries
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.SlabBlock
import net.minecraft.world.level.block.StairBlock
import net.minecraft.world.level.block.WallBlock
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
        ConsumableRaidBlock(RaidTier.TIER_FIVE, BlockBehaviour.Properties.of().strength(3.0f).requiresCorrectToolForDrops().noOcclusion())
    }

    val CONSUMABLE_RAID_BLOCK_ITEM: RegistrySupplier<Item> = ITEMS.register("consumable_raid_block") {
        BlockItem(CONSUMABLE_RAID_BLOCK.get(), Item.Properties())
    }

    val CONSUMABLE_RAID_BLOCK_6: RegistrySupplier<Block> = BLOCKS.register("consumable_raid_block_6") {
        ConsumableRaidBlock(RaidTier.TIER_SIX, BlockBehaviour.Properties.of().strength(3.0f).requiresCorrectToolForDrops().noOcclusion())
    }

    val CONSUMABLE_RAID_BLOCK_6_ITEM: RegistrySupplier<Item> = ITEMS.register("consumable_raid_block_6") {
        BlockItem(CONSUMABLE_RAID_BLOCK_6.get(), Item.Properties())
    }

    val CONSUMABLE_RAID_BLOCK_7: RegistrySupplier<Block> = BLOCKS.register("consumable_raid_block_7") {
        ConsumableRaidBlock(RaidTier.TIER_SEVEN, BlockBehaviour.Properties.of().strength(3.0f).requiresCorrectToolForDrops().noOcclusion())
    }

    val CONSUMABLE_RAID_BLOCK_7_ITEM: RegistrySupplier<Item> = ITEMS.register("consumable_raid_block_7") {
        BlockItem(CONSUMABLE_RAID_BLOCK_7.get(), Item.Properties())
    }

    // ── Unown Stone ──────────────────────────────────────────────────────────
    val UNOWN_STONE: RegistrySupplier<Block> = BLOCKS.register("unown_stone") {
        UnownStoneBlock(BlockBehaviour.Properties.of().strength(2.0f).requiresCorrectToolForDrops())
    }

    val UNOWN_STONE_ITEM: RegistrySupplier<Item> = ITEMS.register("unown_stone") {
        BlockItem(UNOWN_STONE.get(), Item.Properties())
    }

    // ── Unown Stone Activated ────────────────────────────────────────────────
    val UNOWN_STONE_SLAB: RegistrySupplier<Block> = BLOCKS.register("unown_stone_slab") {
        SlabBlock(BlockBehaviour.Properties.of().strength(2.0f).requiresCorrectToolForDrops())
    }

    val UNOWN_STONE_SLAB_ITEM: RegistrySupplier<Item> = ITEMS.register("unown_stone_slab") {
        BlockItem(UNOWN_STONE_SLAB.get(), Item.Properties())
    }

    val UNOWN_STONE_STAIRS: RegistrySupplier<Block> = BLOCKS.register("unown_stone_stairs") {
        StairBlock(UNOWN_STONE.get().defaultBlockState(), BlockBehaviour.Properties.of().strength(2.0f).requiresCorrectToolForDrops())
    }

    val UNOWN_STONE_STAIRS_ITEM: RegistrySupplier<Item> = ITEMS.register("unown_stone_stairs") {
        BlockItem(UNOWN_STONE_STAIRS.get(), Item.Properties())
    }

    val UNOWN_STONE_WALL: RegistrySupplier<Block> = BLOCKS.register("unown_stone_wall") {
        WallBlock(BlockBehaviour.Properties.of().strength(2.0f).requiresCorrectToolForDrops())
    }

    val UNOWN_STONE_WALL_ITEM: RegistrySupplier<Item> = ITEMS.register("unown_stone_wall") {
        BlockItem(UNOWN_STONE_WALL.get(), Item.Properties())
    }

    // ── Unown Stone Activated ────────────────────────────────────────────────
    val UNOWN_STONE_ACTIVATED: RegistrySupplier<Block> = BLOCKS.register("unown_stone_activated") {
        UnownStoneActivatedBlock(BlockBehaviour.Properties.of().strength(2.0f).requiresCorrectToolForDrops().noOcclusion())
    }

    val UNOWN_STONE_ACTIVATED_ITEM: RegistrySupplier<Item> = ITEMS.register("unown_stone_activated") {
        BlockItem(UNOWN_STONE_ACTIVATED.get(), Item.Properties())
    }

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    val GYM_LEADER_TELEPORTER_ENTITY: RegistrySupplier<BlockEntityType<GymLeaderTeleporterBlockEntity>> =
        BLOCK_ENTITY_TYPES.register("gym_leader_teleporter") {
            BlockEntityType.Builder.of(::GymLeaderTeleporterBlockEntity, GYM_LEADER_TELEPORTER.get()).build(null)
        }

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    val CONSUMABLE_RAID_BLOCK_ENTITY: RegistrySupplier<BlockEntityType<ConsumableRaidBlockEntity>> =
        BLOCK_ENTITY_TYPES.register("consumable_raid_block") {
            BlockEntityType.Builder.of(
                ::ConsumableRaidBlockEntity,
                CONSUMABLE_RAID_BLOCK.get(),
                CONSUMABLE_RAID_BLOCK_6.get(),
                CONSUMABLE_RAID_BLOCK_7.get()
            ).build(null)
        }

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    val UNOWN_STONE_ACTIVATED_ENTITY: RegistrySupplier<BlockEntityType<UnownStoneActivatedBlockEntity>> =
        BLOCK_ENTITY_TYPES.register("unown_stone_activated") {
            BlockEntityType.Builder.of(::UnownStoneActivatedBlockEntity, UNOWN_STONE_ACTIVATED.get()).build(null)
        }

    // ── Player Shop ──────────────────────────────────────────
    val PLAYER_SHOP: RegistrySupplier<Block> = BLOCKS.register("player_shop") {
        PlayerShopBlock(BlockBehaviour.Properties.of().strength(3.0f).requiresCorrectToolForDrops().noOcclusion())
    }

    val PLAYER_SHOP_ITEM: RegistrySupplier<Item> = ITEMS.register("player_shop") {
        BlockItem(PLAYER_SHOP.get(), Item.Properties())
    }

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    val PLAYER_SHOP_BLOCK_ENTITY: RegistrySupplier<BlockEntityType<PlayerShopBlockEntity>> =
        BLOCK_ENTITY_TYPES.register("player_shop") {
            BlockEntityType.Builder.of(::PlayerShopBlockEntity, PLAYER_SHOP.get()).build(null)
        }

    // ── Celestial Observatory ────────────────────────────────
    val CELESTIAL_OBSERVATORY: RegistrySupplier<Block> = BLOCKS.register("celestial_observatory") {
        CelestialObservatoryBlock(
            BlockBehaviour.Properties.of()
                .strength(3.5f)
                .requiresCorrectToolForDrops()
                .lightLevel { 4 }  // Légère luminescence
        )
    }

    val CELESTIAL_OBSERVATORY_ITEM: RegistrySupplier<Item> = ITEMS.register("celestial_observatory") {
        BlockItem(CELESTIAL_OBSERVATORY.get(), Item.Properties())
    }

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    val CELESTIAL_OBSERVATORY_ENTITY: RegistrySupplier<BlockEntityType<CelestialObservatoryBlockEntity>> =
        BLOCK_ENTITY_TYPES.register("celestial_observatory") {
            BlockEntityType.Builder.of(::CelestialObservatoryBlockEntity, CELESTIAL_OBSERVATORY.get()).build(null)
        }

    fun register() {
        BLOCKS.register()
        BLOCK_ENTITY_TYPES.register()
        ITEMS.register()

        // Append item to the blocks creative tab
        CreativeTabRegistry.append(
            GymBadgeItems.GYM_BLOCKS_TAB,
            GYM_LEADER_TELEPORTER_ITEM,
            SMALL_ROCK_ITEM,
            CONSUMABLE_RAID_BLOCK_ITEM,
            CONSUMABLE_RAID_BLOCK_6_ITEM,
            CONSUMABLE_RAID_BLOCK_7_ITEM,
            UNOWN_STONE_ITEM,
            UNOWN_STONE_ACTIVATED_ITEM,
            UNOWN_STONE_SLAB_ITEM,
            UNOWN_STONE_STAIRS_ITEM,
            UNOWN_STONE_WALL_ITEM,
            PLAYER_SHOP_ITEM,
            CELESTIAL_OBSERVATORY_ITEM
        )
    }
}
