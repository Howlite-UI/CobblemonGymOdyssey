package com.howlite.blocks

import com.necro.raid.dens.common.blocks.entity.RaidCrystalBlockEntity
import com.necro.raid.dens.common.blocks.block.RaidCrystalBlock
import com.necro.raid.dens.common.data.raid.RaidTier
import com.necro.raid.dens.common.data.raid.RaidBoss
import com.necro.raid.dens.common.registry.RaidRegistry
import com.necro.raid.dens.common.CobblemonRaidDens
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState

class ConsumableRaidBlockEntity(pos: BlockPos, state: BlockState) :
    RaidCrystalBlockEntity(GymBlocks.CONSUMABLE_RAID_BLOCK_ENTITY.get(), pos, state) {

    private var clientInitialized = false

    override fun registerControllers(controllers: software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar) {
        // Do not register default controllers from parent class to avoid searching for non-existent "animation.raid_den.sparkle"
    }

    override fun tick(level: Level, pos: BlockPos, state: BlockState) {
        super.tick(level, pos, state)
        if (level.isClientSide && !clientInitialized) {
            if (getRaidBoss() != null) {
                clientInitialized = true
            }
        }
    }

    override fun closeRaid() {
        super.closeRaid()
        // Destroy the block and replace it with air
        level?.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 3)
    }

    override fun generateRaidBoss(level: Level, pos: BlockPos, state: BlockState) {
        val tier = state.getValue(RaidCrystalBlock.RAID_TIER)
        if (tier == RaidTier.TIER_SIX || tier == RaidTier.TIER_SEVEN) {
            // Temporarily use TIER_FIVE for generation to fallback to a valid boss species/moveset
            val tempState = state.setValue(RaidCrystalBlock.RAID_TIER, RaidTier.TIER_FIVE)
            super.generateRaidBoss(level, pos, tempState)
            // Restore the correct blockstate tier and active state
            level.setBlock(pos, state.setValue(RaidCrystalBlock.ACTIVE, true), 3)
        } else {
            super.generateRaidBoss(level, pos, state)
        }
    }

    override fun getRaidBoss(): RaidBoss? {
        val baseBoss = super.getRaidBoss() ?: return null
        val state = level?.getBlockState(blockPos) ?: return baseBoss
        if (state.hasProperty(RaidCrystalBlock.RAID_TIER)) {
            val blockTier = state.getValue(RaidCrystalBlock.RAID_TIER)
            if (blockTier != baseBoss.tier) {
                val copiedBoss = baseBoss.copy()
                val customId = ResourceLocation.fromNamespaceAndPath(
                    "cobblemongymodyssey",
                    "${baseBoss.id.path}_tier_${blockTier.name.lowercase()}"
                )
                copiedBoss.id = customId
                copiedBoss.setTier(blockTier)
                copiedBoss.createDisplayAspects()

                // Dynamically scale tier-specific properties to match the configured defaults of the new tier config
                val tierConfig = CobblemonRaidDens.TIER_CONFIG[blockTier]
                val oldConfig = CobblemonRaidDens.TIER_CONFIG[baseBoss.tier]
                if (tierConfig != null && oldConfig != null) {
                    if (copiedBoss.maxPlayers == oldConfig.maxPlayers()) copiedBoss.maxPlayers = tierConfig.maxPlayers()
                    if (copiedBoss.maxClears == oldConfig.maxClears()) copiedBoss.maxClears = tierConfig.maxClears()
                    if (copiedBoss.haRate == oldConfig.haRate()) copiedBoss.haRate = tierConfig.haRate()
                    if (copiedBoss.maxCheers == oldConfig.maxCheers()) copiedBoss.maxCheers = tierConfig.maxCheers()
                    if (copiedBoss.raidPartySize == oldConfig.raidPartySize()) copiedBoss.raidPartySize = tierConfig.raidPartySize()
                    if (copiedBoss.healthMulti == oldConfig.healthMultiplier()) copiedBoss.healthMulti = tierConfig.healthMultiplier()
                    if (copiedBoss.multiplayerHealthMulti == oldConfig.multiplayerHealthMultiplier()) copiedBoss.multiplayerHealthMulti = tierConfig.multiplayerHealthMultiplier()
                    if (copiedBoss.shinyRate == oldConfig.shinyRate()) copiedBoss.shinyRate = tierConfig.shinyRate()
                    if (copiedBoss.currency == oldConfig.currency()) copiedBoss.currency = tierConfig.currency()
                    if (copiedBoss.maxCatches == oldConfig.maxCatches()) copiedBoss.maxCatches = tierConfig.maxCatches()
                    if (copiedBoss.lives == oldConfig.lives()) copiedBoss.lives = tierConfig.lives()
                    if (copiedBoss.playersShareLives == oldConfig.playersShareLives()) copiedBoss.playersShareLives = tierConfig.playersShareLives()
                    if (copiedBoss.energy == oldConfig.energy()) copiedBoss.energy = tierConfig.energy()
                    if (copiedBoss.requiredDamage == oldConfig.requiredDamage()) copiedBoss.requiredDamage = tierConfig.requiredDamage()
                    if (copiedBoss.catchRate == oldConfig.catchRate()) copiedBoss.catchRate = tierConfig.catchRate()
                }

                // Register dynamically in both client and server lookup maps
                if (!RaidRegistry.exists(customId)) {
                    RaidRegistry.register(copiedBoss)
                }

                return copiedBoss
            }
        }
        return baseBoss
    }
}

