package com.howlite.fabric.data

import net.minecraft.resources.ResourceLocation
import org.ladysnake.cca.api.v3.component.ComponentKey
import org.ladysnake.cca.api.v3.component.ComponentRegistry

/**
 * Définit et enregistre la clé de composant Cardinal Components (CCA) pour
 * les données de progression joueur.
 *
 * La clé [PLAYER_PROGRESS] est le handle global utilisé pour récupérer le
 * composant depuis n'importe quelle entité [PlayerEntity].
 */
object FabricComponents {

    val PLAYER_PROGRESS: ComponentKey<FabricPlayerProgressProvider> =
        ComponentRegistry.getOrCreate(
            ResourceLocation.fromNamespaceAndPath("cobblemongymodyssey", "player_progress"),
            FabricPlayerProgressProvider::class.java
        )
}
