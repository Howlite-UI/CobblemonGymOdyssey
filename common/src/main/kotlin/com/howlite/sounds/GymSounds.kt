package com.howlite.sounds

import com.howlite.CobblemonGymOdyssey
import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent

object GymSounds {
    private val SOUNDS: DeferredRegister<SoundEvent> =
        DeferredRegister.create(CobblemonGymOdyssey.MOD_ID, Registries.SOUND_EVENT)

    val PORTAL_OPEN: RegistrySupplier<SoundEvent> = SOUNDS.register("portal_open") {
        SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(
                CobblemonGymOdyssey.MOD_ID,
                "portal_open"
            )
        )
    }

    fun register() {
        SOUNDS.register()
    }
}
