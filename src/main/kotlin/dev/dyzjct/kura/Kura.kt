package dev.dyzjct.kura

import base.utils.threads.BackgroundScope
import dev.dyzjct.kura.command.CommandManager
import dev.dyzjct.kura.event.eventbus.AlwaysListening
import dev.dyzjct.kura.manager.*
import dev.dyzjct.kura.module.ModuleManager
import dev.dyzjct.kura.module.modules.client.ClickGui
import dev.dyzjct.kura.module.modules.client.HUDEditor
import dev.dyzjct.kura.setting.StringSetting
import dev.dyzjct.kura.utils.math.LagCompensator
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import kotlin.io.path.Path

class Kura : AlwaysListening {

    companion object {
        const val MOD_NAME = "Kura"
        const val VERSION = "Community Edition v1.0.4"
        var logger: Logger = LogManager.getLogger("Kura")
        var commandPrefix = StringSetting("CommandPrefix", null, ".")
        var DISPLAY_NAME = "$MOD_NAME-$VERSION | Have a nice day!"
        var TICK_TIMER = 1f

        // Root Dir Save
        val DIRECTORY = Path("$MOD_NAME/")

        @get:JvmName("isReady")
        private var ready = false
        private var hasPostInit = false
        var hasInit = false


        fun onManagersInit() {
            if (hasInit) return
            Thread.currentThread().priority = Thread.MAX_PRIORITY
            LagCompensator.call()
            EventListenerManager.call()
            ModuleManager.init()
            CommandManager.onInit()
            RotationManager.onInit()
            FileManager.onInit()
            FileManager.loadAll()
            InventoryTaskManager.onInit()
            CrystalManager.onInit()
            HotbarManager.onInit()
            EntityManager.onInit()
            MovementManager.onInit()
            CombatManager.onInit()
            HoleManager.onInit()
            WorldManager.onInit()
            BlockFinderManager.onInit()
            SphereCalculatorManager.onInit()
            DisablerManager.onInit()
            BackgroundScope.start()

            ClickGui.disable()
            HUDEditor.disable()
            hasInit = true
        }

        fun onPostInit() {
            if (hasPostInit) return
            for (module in ModuleManager.getToggleList()) {
                if (module.isDisabled) continue
                module.disable()
            }
            ready = true
            System.gc()
            hasPostInit = true
        }
    }
}