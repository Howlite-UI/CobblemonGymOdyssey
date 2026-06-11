package com.howlite.neoforge.data

import com.howlite.CobblemonGymOdyssey
import com.howlite.data.PlayerProgressData
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.attachment.AttachmentType
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.registries.NeoForgeRegistries
import java.util.function.Supplier

/**
 * Déclare et enregistre le type d'attachement NeoForge pour les données de
 * progression joueur.
 *
 * Le [PLAYER_PROGRESS] est un [AttachmentType] persistant (via [PlayerProgressData.CODEC])
 * qui survit aux rechargements de monde et à la mort du joueur (`.copyOnDeath()`).
 *
 * Usage depuis n'importe quel code NeoForge :
 * ```kotlin
 * val data = player.getData(NeoForgeAttachments.PLAYER_PROGRESS)
 * ```
 */
object NeoForgeAttachments {

    private val REGISTRY: DeferredRegister<AttachmentType<*>> =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, CobblemonGymOdyssey.MOD_ID)

    val PLAYER_PROGRESS: Supplier<AttachmentType<PlayerProgressData>> =
        REGISTRY.register("player_progress", Supplier {
            AttachmentType.builder(::PlayerProgressData)
                .serialize(PlayerProgressData.CODEC)  // active la persistance sur disque
                .copyOnDeath()                         // survit à la mort du joueur
                .build()
        })

    /** Doit être appelé depuis le `modEventBus` dans l'init du mod NeoForge. */
    fun register(eventBus: IEventBus) {
        REGISTRY.register(eventBus)
    }
}
