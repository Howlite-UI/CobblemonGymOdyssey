package com.howlite.items

import net.minecraft.world.item.Item

/**
 * Item représentant un badge d'Arène dans l'inventaire du joueur.
 *
 * Pour l'instant, c'est un item purement décoratif / collectible.
 * Les badges sont accordés automatiquement par [com.howlite.events.GymBattleEventHandler]
 * lors de la victoire contre un Maître d'Arène — ils ne sont pas directement
 * donnés sous forme d'item au joueur via cet Item.
 *
 * Comportements possibles à ajouter plus tard :
 * - Affichage des badges collectés dans une GUI
 * - Tooltip indiquant le Level Cap débloqué
 * - Utilisation pour débloquer des zones / capacités spéciales
 */
class BadgeItem(properties: Properties) : Item(properties)
