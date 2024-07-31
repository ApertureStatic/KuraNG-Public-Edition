package dev.dyzjct.kura.module

import base.utils.concurrent.threads.IOScope
import base.utils.math.DamageCalculator
import dev.dyzjct.kura.Kura
import dev.dyzjct.kura.event.eventbus.AlwaysListening
import dev.dyzjct.kura.event.eventbus.listener
import dev.dyzjct.kura.event.events.TickEvent
import dev.dyzjct.kura.event.events.input.BindEvent
import dev.dyzjct.kura.module.hud.*
import dev.dyzjct.kura.module.modules.client.*
import dev.dyzjct.kura.module.modules.combat.*
import dev.dyzjct.kura.module.modules.crystal.AutoCrystal
import dev.dyzjct.kura.module.modules.misc.*
import dev.dyzjct.kura.module.modules.movement.*
import dev.dyzjct.kura.module.modules.player.*
import dev.dyzjct.kura.module.modules.render.*
import dev.dyzjct.kura.system.render.newfont.FontRenderers
import kotlinx.coroutines.async
import net.minecraft.client.gui.DrawContext
import java.util.concurrent.CopyOnWriteArrayList
import java.util.stream.Collectors

object ModuleManager : AlwaysListening {
    private var sortedModules = CopyOnWriteArrayList<AbstractModule>()
    val moduleList = CopyOnWriteArrayList<AbstractModule>()

    init {
        listener<TickEvent.Pre>(true) {
            sortModules()
            moduleList.forEach {
                if (it.bind == -1) {
                    it.bind = 0
                }
            }
        }
    }


    fun init() {
        loadModules()
        loadHUDs()
        moduleList.sortWith(Comparator.comparing { it.moduleName })
        Kura.logger.info("Module Initialised")
    }

    fun getToggleList(): ArrayList<Module> {
        val toggleList = ArrayList<Module>()
        toggleList.add(FakePlayer)
        toggleList.add(PortalESP)
        toggleList.add(Xray)
        return toggleList
    }

    private fun loadCategoryClient() {
        registerModule(ClickGui)
        registerModule(HUDEditor)
        registerModule(Colors)
        registerModule(UiSetting)
        registerModule(Cape)
        registerModule(LoadingMenu)
        registerModule(Sound)
        registerModule(CombatSystem)
    }

    private fun loadCategoryCombat() {
        registerModule(AutoReplenish)
        registerModule(AutoOffHand)
        registerModule(Surround)
        registerModule(HoleFiller)
        registerModule(AnchorAura)
        registerModule(AutoTrap)
        registerModule(FastUse)
        registerModule(HoleSnap)
        registerModule(SelfTrap)
        registerModule(AutoEXP)
        registerModule(KillAura)
        registerModule(BedAura)
        registerModule(ManualCev)
        registerModule(AutoWeb)
        registerModule(HolePush)
        registerModule(Burrow)
        registerModule(HeadTrap)
        registerModule(InfiniteAura)
        registerModule(HoleMiner)
        registerModule(CrystalBasePlacer)
        registerModule(AutoPot)
        registerModule(PearlFucker)
        registerModule(AnchorHelper)
        registerModule(AutoHitCrystal)
        registerModule(MainHandTotem)
    }

    private fun loadCategoryMisc() {
        registerModule(FakePlayer)
        registerModule(AutoReconnect)
        registerModule(AutoRespawn)
        registerModule(MCF)
        registerModule(MCP)
        registerModule(TotemPopCounter)
        registerModule(AirPlace)
        registerModule(AutoCraftBed)
        registerModule(ChatSuffix)
        registerModule(PearlClip)
        registerModule(Spammer)
        registerModule(PacketLogger)
        registerModule(Clip)
        registerModule(AutoDupe)
    }

    private fun loadCategoryMovement() {
        registerModule(Velocity)
        registerModule(Strafe)
        registerModule(Speed)
        registerModule(Sprint)
        registerModule(Step)
        registerModule(GUIMove)
        registerModule(NoSlowDown)
        registerModule(ElytraFly)
        registerModule(FastWeb)
        registerModule(Blink)
        registerModule(Flight)
        registerModule(ControlElytraFly)
    }

    private fun loadCategoryPlayer() {
        registerModule(PacketMine)
        registerModule(AutoArmor)
        registerModule(NoEntityTrace)
        registerModule(NoRotate)
        registerModule(Timer)
        registerModule(Reach)
        registerModule(BetterEat)
        registerModule(Scaffold)
        registerModule(NoFall)
        registerModule(FreeCam)
        registerModule(HitboxDesync)
        registerModule(AntiMinePlace)
        registerModule(AntiHunger)
        registerModule(HotbarSwapper)
    }

    private fun loadCategoryRender() {
        registerModule(BlockHighlight)
        registerModule(NoRender)
        registerModule(CustomFov)
        registerModule(Brightness)
        registerModule(GameAnimation)
        registerModule(NameTags)
        registerModule(HoleESP)
        registerModule(ToolTips)
        registerModule(PortalESP)
        registerModule(CrystalRender)
        registerModule(ChestESP)
        registerModule(LogESP)
        registerModule(Chams)
        registerModule(CameraClip)
        registerModule(HandView)
        registerModule(PlaceRender)
        registerModule(BreakESP)
        registerModule(Aspect)
        registerModule(Xray)
        registerModule(PopChams)
        registerModule(Zoom)
        registerModule(AnimationRemover)
    }

    private fun loadModules() {
        loadCategoryCombat()
        loadCategoryClient()
        loadCategoryMisc()
        loadCategoryMovement()
        loadCategoryRender()
        loadCategoryPlayer()
        registerModule(AutoCrystal)
        DamageCalculator
        getModules().sortedWith(Comparator.comparing { it.moduleName })
    }

    private fun loadHUDs() {
        registerModule(ArmorHUD)
        registerModule(CoordsHUD)
        registerModule(ModuleListHUD)
        registerModule(FpsHUD)
        registerModule(FriendListHUD)
        registerModule(PingHUD)
        registerModule(RamHUD)
        registerModule(ServerHUD)
        registerModule(SpeedHUD)
        registerModule(PlayerListHUD)
        registerModule(TpsHUD)
        registerModule(WaterMarkHUD)
        registerModule(Image)
        registerModule(NotificationHUD)
        registerModule(TargetHUD)
        getModules().sortedWith(Comparator.comparing { it.moduleName })
    }

    fun onKey(event: BindEvent) {
        moduleList.forEach {
            if (it.isEnabled) {
                it.onKey(event)
            }
        }
    }

    private fun registerModule(module: AbstractModule) = runCatching {
        IOScope.async { moduleList.add(module) }
    }

    @JvmStatic
    fun getModules(): List<AbstractModule> {
        return moduleList.stream().filter { it is Module }.collect(Collectors.toList())
    }

    private fun sortModules() {
        sortedModules = CopyOnWriteArrayList(
            getEnabledModules().stream()
                .sorted(Comparator.comparing { FontRenderers.lexend.getStringWidth(it.getArrayList()) * -1 })
                .collect(Collectors.toList())
        )
    }

    private fun getEnabledModules(): ArrayList<AbstractModule> {
        val enabledModules = ArrayList<AbstractModule>()
        for (module in moduleList) {
            if (!module.isEnabled) continue
            enabledModules.add(module)
        }
        return enabledModules
    }

    @JvmStatic
    val hudModules: List<HUDModule>
        get() = moduleList.filterIsInstance<HUDModule>().toList()

    @JvmStatic
    fun getModuleByName(targetName: String?): Module {
        for (iModule in moduleList.filterIsInstance<Module>()) {
            if (!iModule.moduleName.equals(targetName, ignoreCase = true)) continue
            return iModule
        }
        return NullModule
    }

    @JvmStatic
    fun getModuleByClass(targetName: Class<out Module>): Module {
        for (iModule in moduleList.filterIsInstance<Module>()) {
            if (iModule.javaClass != targetName) continue
            return iModule
        }
        return NullModule
    }

    @JvmStatic
    fun getHUDByName(targetName: String?): HUDModule {
        for (iModule in hudModules) {
            if (!iModule.moduleName.equals(targetName, ignoreCase = true)) continue
            return iModule
        }
        return NullHUD
    }

    fun onKeyPressed(key: Int, action: Int): Boolean {
        if (key == 0) {
            return false
        }

        moduleList.forEach {
            if (it.bind == key) {
                if (action == 1) {
                    if (it.holdToEnable) {
                        it.enable()
                    } else {
                        it.toggle()
                    }
                } else if (it.holdToEnable && action == 0) {
                    it.disable()
                }

                if (it is ClickGui || it is HUDEditor) {
                    return true
                }
            }
        }

        return false
    }

    fun onLogout() {
        moduleList.forEach {
            if (it.isEnabled) {
                it.onLogout()
            }
        }
    }

    fun onRenderHUD(context: DrawContext) {
        hudModules.forEach {
            if (it.isEnabled) {
                it.renderDelegateOnGame(context)
            }
        }
    }
}