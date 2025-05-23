package dev.dyzjct.kura.module

import base.utils.Wrapper
import base.utils.chat.ChatUtil
import base.utils.sound.SoundPlayer
import dev.dyzjct.kura.Kura
import dev.dyzjct.kura.event.eventbus.ListenerOwner
import dev.dyzjct.kura.event.eventbus.SafeClientEvent
import dev.dyzjct.kura.event.eventbus.safeConcurrentListener
import dev.dyzjct.kura.event.eventbus.safeEventListener
import dev.dyzjct.kura.event.events.ModuleEvent
import dev.dyzjct.kura.event.events.PacketEvents
import dev.dyzjct.kura.event.events.RunGameLoopEvent
import dev.dyzjct.kura.event.events.TickEvent
import dev.dyzjct.kura.event.events.input.BindEvent
import dev.dyzjct.kura.event.events.player.PlayerMotionEvent
import dev.dyzjct.kura.event.events.render.Render2DEvent
import dev.dyzjct.kura.event.events.render.Render3DEvent
import dev.dyzjct.kura.manager.NotificationManager
import dev.dyzjct.kura.module.hud.NotificationHUD
import dev.dyzjct.kura.module.modules.client.ClickGui
import dev.dyzjct.kura.module.modules.client.Sound
import dev.dyzjct.kura.setting.*
import dev.dyzjct.kura.system.util.IDRegistry
import dev.dyzjct.kura.system.util.color.ColorRGB
import net.minecraft.client.gui.DrawContext
import org.lwjgl.glfw.GLFW
import java.awt.Color
import java.util.*

abstract class AbstractModule : ListenerOwner() {
    val settingList = ArrayList<Setting<*>>()
    var moduleName = ""
    var moduleCName = ""
    var moduleCategory = Category.HIDDEN
    var alwaysEnable = false
    var isEnabled = false

    var isVisible = false
        set(value) {
            field = value
            ModuleEvent.VisibleChange(this).post()
        }
    var description: String = ""

    private val idRegistry = IDRegistry()
    val id = idRegistry.register()
    var holdToEnable = false
    var bind: Int = 0

    init {
        safeConcurrentListener<TickEvent.Pre> {
            if (alwaysEnable) {
                isEnabled = true
            }
        }
    }

    fun enable() {
        if (alwaysEnable) {
            return
        }
        isEnabled = true
        if (laoded) {
            if (ClickGui.chat.value) ChatUtil.sendMessageWithID(
                moduleName + " is " + ChatUtil.GREEN + "Enabled!",
                11451
            )
            if (NotificationHUD.isEnabled) NotificationManager.addNotification(
                (NotificationHUD.defaultFontColor() + "[ " + ChatUtil.RED + if (ClickGui.chinese.value) moduleCName else moduleName) + NotificationHUD.defaultFontColor() + " ] " + "has been " + ChatUtil.GREEN + "Enable!"
            )
            if (Sound.isEnabled) {
                Kura::class.java.getResourceAsStream("/assets/kura/sounds/NeverEnable.wav")?.let { never ->
                    Kura::class.java.getResourceAsStream("/assets/kura/sounds/Sigma_Enable.wav")?.let { sigma ->
                        Kura::class.java.getResourceAsStream("/assets/kura/sounds/ModuleEnable.wav")?.let { fdp ->
                            when (Sound.mode.value) {
                                Sound.SoundMode.Sigma -> {
                                    SoundPlayer(sigma).play(Sound.volume)
                                }

                                Sound.SoundMode.FDP -> {
                                    SoundPlayer(fdp).play(Sound.volume)
                                }

                                Sound.SoundMode.Never -> {
                                    SoundPlayer(never).play(Sound.volume)
                                }
                            }
                        }
                    } ?: run {
                        NotificationManager.addNotification(ChatUtil.YELLOW + "SoundFailed!")
                    }
                } ?: run {
                    NotificationManager.addNotification(ChatUtil.YELLOW + "SoundFailed!")
                }
            }
        }
        ModuleEvent.Toggle(this).post()
        onEnable()
        subscribe()
    }

    fun disable() {
        if (alwaysEnable) {
            return
        }
        isEnabled = false
        if (laoded) {
            if (ClickGui.chat.value) ChatUtil.sendMessageWithID(moduleName + " is " + ChatUtil.RED + "Disabled!", 11451)
            if (NotificationHUD.isEnabled) NotificationManager.addNotification(
                (NotificationHUD.defaultFontColor() + "[ " + ChatUtil.RED + if (ClickGui.chinese.value) moduleCName else moduleName) + NotificationHUD.defaultFontColor() + " ] " + "has been " + ChatUtil.RED + "Disable!"
            )
            if (Sound.isEnabled) {
                Kura::class.java.getResourceAsStream("/assets/kura/sounds/NeverDisable.wav")?.let { never ->
                    Kura::class.java.getResourceAsStream("/assets/kura/sounds/Sigma_Disable.wav")?.let { sigma ->
                        Kura::class.java.getResourceAsStream("/assets/kura/sounds/ModuleDisable.wav")?.let { fdp ->
                            when (Sound.mode.value) {
                                Sound.SoundMode.Sigma -> {
                                    SoundPlayer(sigma).play(Sound.volume)
                                }

                                Sound.SoundMode.FDP -> {
                                    SoundPlayer(fdp).play(Sound.volume)
                                }

                                Sound.SoundMode.Never -> {
                                    SoundPlayer(never).play(Sound.volume)

                                }
                            }
                        }
                    } ?: run {
                        NotificationManager.addNotification(ChatUtil.YELLOW + "SoundFailed!")
                    }
                } ?: run {
                    NotificationManager.addNotification(ChatUtil.YELLOW + "SoundFailed!")
                }
            }
        }
        ModuleEvent.Toggle(this).post()
        onDisable()
        unsubscribe()
    }

    fun safeDisable() {
        if (isEnabled) {
            disable()
        }
    }

    inline fun onRender3D(crossinline safeEvent: SafeClientEvent.(Render3DEvent) -> Unit) {
        safeEventListener<Render3DEvent> { event ->
            if (isEnabled) {
                safeEvent.invoke(this@safeEventListener, event)
            }
        }
    }

    inline fun onRender2D(crossinline safeEvent: SafeClientEvent.(Render2DEvent) -> Unit) {
        safeEventListener<Render2DEvent> { event ->
            if (isEnabled) {
                safeEvent.invoke(this@safeEventListener, event)
            }
        }
    }

    inline fun onPacketSend(crossinline safeEvent: SafeClientEvent.(PacketEvents.Send) -> Unit) {
        safeEventListener<PacketEvents.Send> { event ->
            if (isEnabled) {
                safeEvent.invoke(this@safeEventListener, event)
            }
        }
    }

    inline fun onPacketReceive(crossinline safeEvent: SafeClientEvent.(PacketEvents.Receive) -> Unit) {
        safeEventListener<PacketEvents.Receive> { event ->
            if (isEnabled) {
                safeEvent.invoke(this@safeEventListener, event)
            }
        }
    }

    inline fun onMotion(crossinline safeEvent: SafeClientEvent.(PlayerMotionEvent) -> Unit) {
        safeEventListener<PlayerMotionEvent> { event ->
            if (isEnabled) {
                safeEvent.invoke(this@safeEventListener, event)
            }
        }
    }

    inline fun onLoop(crossinline safeEvent: SafeClientEvent.(RunGameLoopEvent.Tick) -> Unit) {
        safeEventListener<RunGameLoopEvent.Tick> { event ->
            if (isEnabled) {
                safeEvent.invoke(this@safeEventListener, event)
            }
        }
    }

    fun bsetting(name: String, defaultValue: Boolean): BooleanSetting {
        val value = BooleanSetting(name, this, defaultValue)
        settingList.add(value)
        return value
    }

//TODO Integer Settings

    fun isetting(name: String, defaultValue: Int, minValue: Int, maxValue: Int, modifyValue: Int = 0): IntegerSetting {
        val value = IntegerSetting(name, this, defaultValue, minValue, maxValue, modifyValue)
        settingList.add(value)
        return value
    }

//TODO Float Settings

    fun fsetting(
        name: String, defaultValue: Float, minValue: Float, maxValue: Float, modifyValue: Float = 0f
    ): FloatSetting {
        val value = FloatSetting(name, this, defaultValue, minValue, maxValue, modifyValue)
        settingList.add(value)
        return value
    }

    fun fsetting(
        name: String, defaultValue: Float, range: ClosedFloatingPointRange<Float>, modifyValue: Float = 0f
    ): FloatSetting {
        val value = FloatSetting(name, this, defaultValue, range.start, range.endInclusive, modifyValue)
        settingList.add(value)
        return value
    }

//TODO Double Settings

    fun dsetting(
        name: String, defaultValue: Double, minValue: Double, maxValue: Double, modifyValue: Double = 0.0
    ): DoubleSetting {
        val value = DoubleSetting(name, this, defaultValue, minValue, maxValue, modifyValue)
        settingList.add(value)
        return value
    }

    fun dsetting(
        name: String, defaultValue: Double, range: ClosedFloatingPointRange<Double>, modifyValue: Double = 0.0
    ): DoubleSetting {
        val value = DoubleSetting(name, this, defaultValue, range.start, range.endInclusive, modifyValue)
        settingList.add(value)
        return value
    }

    fun msetting(name: String, modes: Enum<*>): ModeSetting<*> {
        val value: ModeSetting<*> = ModeSetting(name, this, modes)
        settingList.add(value)
        return value
    }

    fun ssetting(settingName: String, defaultSettingValue: String): StringSetting {
        val setting = StringSetting(settingName, this, defaultSettingValue)
        settingList.add(setting)
        return setting
    }

    fun csetting(name: String, defaultValue: Color): ColorSetting {
        val value = ColorSetting(name, this, defaultValue)
        settingList.add(value)
        return value
    }

    fun csetting(name: String, defaultValue: ColorRGB): ColorSetting {
        val value = ColorSetting(name, this, Color(defaultValue.r, defaultValue.g, defaultValue.b))
        settingList.add(value)
        return value
    }

    fun getBindName(): String {
        var kn = if (bind in 3..4) {
            when (bind) {
                3 -> "Mouse_4"
                4 -> "Mouse_5"
                else -> "None"
            }
        } else {
            if (this.bind > 0) GLFW.glfwGetKeyName(bind, GLFW.glfwGetKeyScancode(bind)) else "None"
        }
        if (kn == null) {
            try {
                for (declaredField in GLFW::class.java.declaredFields) {
                    if (declaredField.name.startsWith("GLFW_KEY_")) {
                        val a = declaredField[null] as Int
                        if (a == bind) {
                            val nb = declaredField.name.substring("GLFW_KEY_".length)
                            kn = nb.substring(0, 1).uppercase(Locale.getDefault()) + nb.substring(1)
                                .lowercase(Locale.getDefault())
                        }
                    }
                }
            } catch (ignored: Exception) {
                kn = "unknown.$bind"
            }
        }
        return if (bind == -1) "None" else (kn + "").uppercase(Locale.getDefault())
    }

    val isDisabled: Boolean
        get() = !isEnabled

    open fun getHudInfo(): String? = null
    open fun onConfigLoad() {}
    open fun onConfigSave() {}
    open fun onEnable() {}
    open fun onDisable() {}
    open fun onLogout() {}
    open fun onRender(context: DrawContext) {}

    open fun getCName(): String {
        return moduleCName
    }

    open fun getName(): String {
        return if (ClickGui.chinese.value) moduleCName else moduleName
    }

    fun toggle() {
        isEnabled = !isEnabled
        if (isEnabled) {
            enable()
        } else {
            disable()
        }
    }

    @Suppress("UNUSED")
    fun onKey(event: BindEvent) {
    }

    fun setEnable(toggled: Boolean) {
        isEnabled = toggled
    }

    fun isHidden(): Boolean {
        return moduleCategory.isHidden
    }

    fun getArrayList(): String {
        return if (ClickGui.chinese.value) moduleCName else moduleName + if (getHudInfo() == null || getHudInfo() == "") "" else " " + ChatUtil.SECTIONSIGN + "7" + (if (getHudInfo() == "" || getHudInfo() == null) "" else "[") + ChatUtil.SECTIONSIGN + "f" + getHudInfo() + '\u00a7' + "7" + if (getHudInfo() == "") "" else "]"
    }

    companion object {
        @JvmField
        val mc = Wrapper.minecraft
        var laoded = false
    }
}