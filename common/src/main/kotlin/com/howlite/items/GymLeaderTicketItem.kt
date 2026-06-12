package com.howlite.items

import com.howlite.data.GymBadge
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag

/**
 * Item représentant le ticket permettant d'activer le téléporteur d'arène.
 */
class GymLeaderTicketItem(properties: Properties, val targetBadge: GymBadge) : Item(properties) {
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        val badgeKey = "item.cobblemongymodyssey.${targetBadge.id}"
        tooltipComponents.add(
            Component.translatable("item.cobblemongymodyssey.gym_leader_ticket.tooltip", Component.translatable(badgeKey))
        )
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag)
    }
}
