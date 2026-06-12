package com.howlite

import com.howlite.blocks.GymBlocks
import com.howlite.commands.GymTestCommand
import com.howlite.commands.GymTpCommand
import com.howlite.events.BattleLevelCapEventHandler
import com.howlite.events.GymBattleEventHandler
import com.howlite.events.GymBattleReturnHandler
import com.howlite.events.LevelCapEventHandler
import com.howlite.items.GymBadgeItems
import com.howlite.menu.BadgeCaseMenus

object CobblemonGymOdyssey {
    const val MOD_ID = "cobblemongymodyssey"

    @JvmStatic
    fun init() {
        GymBadgeItems.register()
        GymBlocks.register()
        BadgeCaseMenus.register()
        LevelCapEventHandler.register()
        GymBattleEventHandler.register()
        GymBattleReturnHandler.register()
        BattleLevelCapEventHandler.register()
        GymTestCommand.register()
        GymTpCommand.register()
    }
}
