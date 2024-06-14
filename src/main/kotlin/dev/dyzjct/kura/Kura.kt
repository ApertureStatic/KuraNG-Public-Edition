package dev.dyzjct.kura

import base.system.event.AlwaysListening
import base.utils.threads.BackgroundScope
import dev.dyzjct.kura.command.CommandManager
import dev.dyzjct.kura.manager.*
import dev.dyzjct.kura.module.ModuleManager
import dev.dyzjct.kura.module.modules.client.ClickGui
import dev.dyzjct.kura.module.modules.client.CombatSystem
import dev.dyzjct.kura.module.modules.client.HUDEditor
import dev.dyzjct.kura.setting.StringSetting
import dev.dyzjct.kura.utils.math.LagCompensator
import helper.kura.socket.SocketManager
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import kotlin.io.path.Path
class Kura : AlwaysListening {

    companion object {
        const val MOD_NAME = "Kura"
        const val VERSION = "Community Edition v1.0.1"
        var logger: Logger = LogManager.getLogger("Kura")
        var commandPrefix = StringSetting("CommandPrefix", null, ".")
        var DISPLAY_NAME = "$MOD_NAME-$VERSION | Have a nice day!"
        var TICK_TIMER = 1f

        var ircSocket = SocketManager()

        // 如果是Dev版本就改成"正数" User版本就改成负数
        // 考虑对接SDK
        var verifiedState = 1

        // Root Dir Save
        val DIRECTORY = Path("$MOD_NAME/")

        @get:JvmName("isReady")
        var ready = false; private set
        var hasInit = false
        var hasPostInit = false
        var called = false
        var id = ""


        fun onManagersInit() {
            if (hasInit) return
//            defaultScope.launch {
//                dumpJar()
//            }
            Thread.currentThread().priority = Thread.MAX_PRIORITY
            LagCompensator.call()
            EventListenerManager.call()
            ModuleManager.init()
            CommandManager.onInit()
            RotationManager.onInit()
            FileManager.onInit()
            FileManager.loadCombatSystem()
            FileManager.loadAll(CombatSystem.combatMode.value.name)
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