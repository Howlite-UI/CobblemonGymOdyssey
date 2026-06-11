package com.howlite.fabric

import com.howlite.CobblemonGymOdyssey
import com.howlite.api.PlayerProgressApi
import com.howlite.fabric.data.FabricComponents
import com.howlite.fabric.data.FabricPlayerProgressProvider
import net.fabricmc.api.ModInitializer
import net.minecraft.server.level.ServerPlayer
import org.ladysnake.cca.api.v3.entity.EntityComponentFactoryRegistry
import org.ladysnake.cca.api.v3.entity.EntityComponentInitializer
import org.ladysnake.cca.api.v3.entity.RespawnCopyStrategy

/**
 * Point d'entrée Fabric du mod.
 *
 * Responsabilités :
 * 1. Enregistrer le composant CCA sur le [ServerPlayer] (via [EntityComponentInitializer]).
 * 2. Injecter le [FabricPlayerProgressProvider] comme implémentation de l'API commune.
 * 3. Déléguer l'initialisation des événements Cobblemon à [CobblemonGymOdyssey.init].
 *
 * Note : Cette classe implémente [EntityComponentInitializer] pour enregistrer
 * le composant CCA de façon propre. L'entry point "cardinal-components" doit
 * être déclaré dans fabric.mod.json (voir ci-dessous).
 */
class ExampleModFabric : ModInitializer, EntityComponentInitializer {

    override fun onInitialize() {
        // Brancher le provider Fabric dans l'API commune (injection de dépendance)
        PlayerProgressApi.provider = object : com.howlite.api.PlayerProgressProvider {
            override fun getProgress(player: ServerPlayer) =
                FabricComponents.PLAYER_PROGRESS.get(player).getProgress(player)

            override fun markDirty(player: ServerPlayer) =
                FabricComponents.PLAYER_PROGRESS.get(player).markDirty(player)
        }

        // Initialiser les listeners d'événements Cobblemon (Common)
        CobblemonGymOdyssey.init()
    }

    /**
     * Attache le composant CCA à chaque joueur serveur.
     *
     * [RespawnCopyStrategy.ALWAYS_COPY] garantit que les badges et le Level Cap
     * persistent après la mort du joueur.
     */
    override fun registerEntityComponentFactories(registry: EntityComponentFactoryRegistry) {
        registry.registerForPlayers(
            FabricComponents.PLAYER_PROGRESS,
            { _ -> FabricPlayerProgressProvider() },
            RespawnCopyStrategy.ALWAYS_COPY
        )
    }
}
