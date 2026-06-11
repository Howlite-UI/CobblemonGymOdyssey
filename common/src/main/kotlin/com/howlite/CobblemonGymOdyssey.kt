package com.howlite

import com.howlite.commands.GymTestCommand
import com.howlite.events.GymBattleEventHandler
import com.howlite.events.LevelCapEventHandler
import com.howlite.items.GymBadgeItems

object CobblemonGymOdyssey {
    const val MOD_ID = "cobblemongymodyssey"

    @JvmStatic
    fun init() {
        GymBadgeItems.register()
        LevelCapEventHandler.register()
        GymBattleEventHandler.register()
        GymTestCommand.register()
    }
}
