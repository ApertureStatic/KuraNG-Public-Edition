package dev.dyzjct.kura

import dev.dyzjct.kura.command.CommandManager
import dev.dyzjct.kura.manager.*
import dev.dyzjct.kura.gui.clickgui.GUIRender
import dev.dyzjct.kura.gui.clickgui.HUDRender
import dev.dyzjct.kura.module.ModuleManager
import dev.dyzjct.kura.module.modules.client.ClickGui
import dev.dyzjct.kura.module.modules.client.HUDEditor
import dev.dyzjct.kura.setting.StringSetting
import dev.dyzjct.kura.utils.math.LagCompensator
import base.system.event.AlwaysListening
import base.utils.threads.BackgroundScope
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import kotlin.io.path.Path

class Kura : AlwaysListening {
    enum class UserType(val userType: String) {
        User("User"), Beta("Beta"), Nigger("NIGGER")
    }

    companion object {
        const val MOD_NAME = "Kura"
        const val VERSION = "1.0"
        var userState = UserType.Beta
        var logger: Logger = LogManager.getLogger("Kura")
        var commandPrefix = StringSetting("CommandPrefix", null, ".")
        var DISPLAY_NAME = "$MOD_NAME.dev $VERSION (${userState.userType})"
        var TICK_TIMER = 1f

        //如果是Dev版本就改成"正数" User版本就改成负数
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
            GUIRender.onCall()
            HUDRender.onCall()
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