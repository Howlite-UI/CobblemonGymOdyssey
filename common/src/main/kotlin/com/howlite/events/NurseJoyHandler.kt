package com.howlite.events

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.entity.npc.NPCEntity
import dev.architectury.event.EventResult
import dev.architectury.event.events.common.EntityEvent
import dev.architectury.event.events.common.InteractionEvent
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal
import net.minecraft.world.entity.player.Player

import com.howlite.mixin.MobAccessor

object NurseJoyHandler {

    fun register() {
        // Prevent movement and make Nurse Joy invulnerable when added to the world
        EntityEvent.ADD.register { entity, level ->
            if (entity is NPCEntity) {
                if (entity.npc.id.toString() == "cobblemongymodyssey:nurse_joy") {
                    entity.isInvulnerable = true
                    entity.setNoAi(false)

                    // Set movement speed to 0.0 and knockback resistance to 1.0 to keep her stationary
                    entity.getAttribute(Attributes.MOVEMENT_SPEED)?.baseValue = 0.0
                    entity.getAttribute(Attributes.KNOCKBACK_RESISTANCE)?.baseValue = 1.0

                    // Use Mixin Accessor to get goalSelector (cast through Any to satisfy Kotlin compiler)
                    val goalSelector = (entity as Any as MobAccessor).goalSelector

                    // Check if look goals are already present to prevent duplicates on reload
                    val hasLookGoal = goalSelector.getAvailableGoals().any { it.goal is LookAtPlayerGoal }
                    if (!hasLookGoal) {
                        goalSelector.addGoal(1, LookAtPlayerGoal(entity, Player::class.java, 8.0f))
                        goalSelector.addGoal(2, RandomLookAroundGoal(entity))
                    }
                }
            }
            EventResult.pass()
        }

        // Heal the player's Pokémon party on right-click interaction
        InteractionEvent.INTERACT_ENTITY.register { player, entity, hand ->
            if (entity is NPCEntity) {
                if (entity.npc.id.toString() == "cobblemongymodyssey:nurse_joy") {
                    if (player is ServerPlayer) {
                        val party = Cobblemon.storage.getParty(player)
                        party.forEach { it?.heal() }

                        player.serverLevel().playSound(
                            null,
                            entity.x, entity.y, entity.z,
                            SoundEvents.BEACON_ACTIVATE,
                            SoundSource.NEUTRAL,
                            1.0f,
                            1.0f
                        )

                        player.sendSystemMessage(
                            Component.translatable("cobblemongymodyssey.nurse_joy.healed")
                        )
                    }
                    return@register EventResult.interruptFalse()
                }
            }
            EventResult.pass()
        }
    }
}
