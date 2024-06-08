package dev.dyzjct.kura.manager

import base.utils.chat.ChatUtil
import base.utils.concurrent.threads.IOScope
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import dev.dyzjct.kura.Kura
import dev.dyzjct.kura.gui.clickgui.ClickGuiScreen
import dev.dyzjct.kura.gui.clickgui.HudEditorScreen
import dev.dyzjct.kura.gui.clickgui.component.Panel
import dev.dyzjct.kura.gui.gui.GUIRender
import dev.dyzjct.kura.gui.gui.HUDRender
import dev.dyzjct.kura.module.AbstractModule
import dev.dyzjct.kura.module.HUDModule
import dev.dyzjct.kura.module.ModuleManager.getHUDByName
import dev.dyzjct.kura.module.ModuleManager.getModuleByName
import dev.dyzjct.kura.module.ModuleManager.getModules
import dev.dyzjct.kura.module.ModuleManager.hUDModules
import dev.dyzjct.kura.module.modules.client.CombatSystem
import dev.dyzjct.kura.module.hud.NullHUD
import dev.dyzjct.kura.setting.*
import kotlinx.coroutines.launch
import java.awt.Color
import java.io.*
import java.nio.charset.StandardCharsets
import java.util.concurrent.CopyOnWriteArrayList

object FileManager {
    private val initializedConfig = CopyOnWriteArrayList<File>()
    private var CLIENT_FILE: File? = null
    private var FRIEND_FILE: File? = null
    private var GUI_FILE: File? = null
    private var HUD_FILE: File? = null

    var changing = false

    fun onInit() {
        if (!tryLoad()) {
            deleteFiles()
        }
        checkPath(File(NOTEBOT_PATH))
        checkPath(File(BACKGROUND_PATH))
    }

    private fun deleteFiles() {
        try {
            initializedConfig.forEach { it?.delete() }
            Kura.logger.info("All config files deleted successfully!\n")
        } catch (e: Exception) {
            Kura.logger.error("Error while deleting config files!")
            e.printStackTrace()
        }
    }

    private fun saveClient() {
        try {
            checkFile(CLIENT_FILE)
            val father = JsonObject()
            val stuff = JsonObject()
            stuff.addProperty("CommandPrefix", Kura.commandPrefix.value)
            father.add("Client", stuff)
            val saveJson = PrintWriter(OutputStreamWriter(FileOutputStream(CLIENT_CONFIG), StandardCharsets.UTF_8))
            saveJson.println(gsonPretty!!.toJson(father))
            saveJson.close()
        } catch (e: Exception) {
            Kura.logger.error("Error while saving Client stuff!")
            e.printStackTrace()
        }
    }

    private fun saveFriend() {
        try {
            FRIEND_FILE?.let {
                if (!it.exists()) {
                    it.parentFile.mkdirs()
                    try {
                        it.createNewFile()
                    } catch (ignored: Exception) {
                    }
                }
                val father = JsonObject()
                for (friend in FriendManager.friends) {
                    val stuff = JsonObject()
                    stuff.addProperty("isFriend", friend.isFriend)
                    father.add(friend.name, stuff)
                }
                val saveJSon = PrintWriter(OutputStreamWriter(FileOutputStream(it), StandardCharsets.UTF_8))
                saveJSon.println(gsonPretty!!.toJson(father))
                saveJSon.close()
            }
        } catch (e: Exception) {
            Kura.logger.error("Error while saving friends!")
            e.printStackTrace()
        }
    }

    private fun loadNewUiConfig() {
        val file = File(NEW_UI_CONFIG_FILE_NAME)

        if (file.exists()) {
            val json = file.readText()
            val jsonObject = gsonPretty.fromJson(json, JsonObject::class.java)

            (ClickGuiScreen.elements + HudEditorScreen.elements).mapNotNull { it as? Panel }
                .forEach {
                    val panelJsonObject = jsonObject.getAsJsonObject(it.category.name)

                    if (panelJsonObject != null) {
                        it.x = panelJsonObject["x"].asFloat
                        it.y = panelJsonObject["y"].asFloat
                        it.isOpened = panelJsonObject["isOpened"].asBoolean
                    }

                    it.rearrange()
                }
        }
    }

    private fun saveNewUiConfig() {
        val jsonObject = JsonObject()
        val file = File(NEW_UI_CONFIG_FILE_NAME)

        (ClickGuiScreen.elements + HudEditorScreen.elements).mapNotNull { it as? Panel }
            .forEach {
                val panelJsonObject = JsonObject()
                panelJsonObject.addProperty("x", it.x)
                panelJsonObject.addProperty("y", it.y)
                panelJsonObject.addProperty("isOpened", it.isOpened)
                jsonObject.add(it.category.name, panelJsonObject)
            }

        val json = gsonPretty.toJson(jsonObject)
        file.writeText(json)
    }

    private fun saveGUI() {
        try {
            checkFile(GUI_FILE)
            var jsonGui: JsonObject
            val father = JsonObject()
            for (panel in GUIRender.panels) {
                jsonGui = JsonObject()
                jsonGui.addProperty("X", panel.x)
                jsonGui.addProperty("Y", panel.y)
                jsonGui.addProperty("Extended", panel.extended)
                father.add(panel.category.name, jsonGui)
            }
            for (panel in HUDRender.panels) {
                jsonGui = JsonObject()
                jsonGui.addProperty("X", panel.x)
                jsonGui.addProperty("Y", panel.y)
                jsonGui.addProperty("Extended", panel.extended)
                father.add(panel.category.name, jsonGui)
            }
            val saveJSon = PrintWriter(OutputStreamWriter(FileOutputStream(GUI_CONFIG), StandardCharsets.UTF_8))
            saveJSon.println(gsonPretty!!.toJson(father))
            saveJSon.close()
        } catch (e: Exception) {
            Kura.logger.error("Error while saving GUI config!")
            e.printStackTrace()
        }
    }

    private fun saveHUD() {
        try {
            checkFile(HUD_FILE)
            val father = JsonObject()
            for (module in hUDModules) {
                val jsonModule = JsonObject()
                jsonModule.addProperty("Enable", module.isEnabled)
                jsonModule.addProperty("HUDPosX", module.x)
                jsonModule.addProperty("HUDPosY", module.y)
                jsonModule.addProperty("Bind", module.bind)
                if (module.settingList.isNotEmpty()) {
                    for (setting in module.settingList) {
                        when (setting) {
                            is StringSetting -> jsonModule.addProperty(setting.name, setting.value)
                            is BooleanSetting -> jsonModule.addProperty(setting.name, setting.value)
                            is IntegerSetting -> jsonModule.addProperty(setting.name, setting.value)
                            is FloatSetting -> jsonModule.addProperty(setting.name, setting.value)
                            is DoubleSetting -> jsonModule.addProperty(setting.name, setting.value)
                            is ColorSetting -> jsonModule.addProperty(setting.name, setting.value.rgb)
                            is ModeSetting<*> -> jsonModule.addProperty(setting.name, setting.valueAsString)
                        }
                    }
                }
                module.onConfigSave()
                father.add(module.moduleName, jsonModule)
            }
            val saveJSon = PrintWriter(OutputStreamWriter(FileOutputStream(HUD_CONFIG), StandardCharsets.UTF_8))
            saveJSon.println(gsonPretty!!.toJson(father))
            saveJSon.close()
        } catch (e: Exception) {
            Kura.logger.error("Error while saving HUD config!")
            e.printStackTrace()
        }
    }

    private fun saveCombatSystem() {
        try {
            val father = JsonObject()
            val module = CombatSystem
            val moduleFile = File("Kura/config/" + module.moduleName + ".json")
            checkFile(moduleFile)
            val jsonModule = JsonObject()
            jsonModule.addProperty("Enable", module.isEnabled)
            jsonModule.addProperty("Visible", module.isVisible)
            jsonModule.addProperty("HoldEnable", module.holdToEnable)
            jsonModule.addProperty("Bind", module.bind)
            if (module.settingList.isNotEmpty()) {
                for (setting in module.settingList) {
                    when (setting) {
                        is StringSetting -> jsonModule.addProperty(setting.name, setting.value)
                        is BooleanSetting -> jsonModule.addProperty(setting.name, setting.value)
                        is IntegerSetting -> jsonModule.addProperty(setting.name, setting.value)
                        is FloatSetting -> jsonModule.addProperty(setting.name, setting.value)
                        is DoubleSetting -> jsonModule.addProperty(setting.name, setting.value)
                        is ColorSetting -> jsonModule.addProperty(setting.name, setting.value.rgb)
                        is ModeSetting<*> -> jsonModule.addProperty(setting.name, setting.valueAsString)
                    }
                }
            }
            val saveJSon = PrintWriter(OutputStreamWriter(FileOutputStream(moduleFile), StandardCharsets.UTF_8))
            saveJSon.println(gsonPretty!!.toJson(jsonModule))
            saveJSon.close()
            module.onConfigSave()
            father.add(module.moduleName, jsonModule)

        } catch (e: Exception) {
            Kura.logger.error("Error while saving module config!")
            e.printStackTrace()
        }
    }

    private fun saveModule(combatMode: String) {
        try {
            val father = JsonObject()
            for (module in getModules()) {
                if (module == CombatSystem) continue
                val moduleFile = File("Kura/config/" + combatMode + "/modules/" + module.moduleName + ".json")
                checkFile(moduleFile)
                val jsonModule = JsonObject()
                jsonModule.addProperty("Enable", module.isEnabled)
                jsonModule.addProperty("Visible", module.isVisible)
                jsonModule.addProperty("HoldEnable", module.holdToEnable)
                jsonModule.addProperty("Bind", module.bind)
                if (module.settingList.isNotEmpty()) {
                    for (setting in module.settingList) {
                        when (setting) {
                            is StringSetting -> jsonModule.addProperty(setting.name, setting.value)
                            is BooleanSetting -> jsonModule.addProperty(setting.name, setting.value)
                            is IntegerSetting -> jsonModule.addProperty(setting.name, setting.value)
                            is FloatSetting -> jsonModule.addProperty(setting.name, setting.value)
                            is DoubleSetting -> jsonModule.addProperty(setting.name, setting.value)
                            is ColorSetting -> jsonModule.addProperty(setting.name, setting.value.rgb)
                            is ModeSetting<*> -> jsonModule.addProperty(setting.name, setting.valueAsString)
                        }
                    }
                }
                val saveJSon = PrintWriter(OutputStreamWriter(FileOutputStream(moduleFile), StandardCharsets.UTF_8))
                saveJSon.println(gsonPretty!!.toJson(jsonModule))
                saveJSon.close()
                module.onConfigSave()
                father.add(module.moduleName, jsonModule)
            }
            if (CombatSystem.debug) ChatUtil.sendMessage("Saving Modules")
        } catch (e: Exception) {
            Kura.logger.error("Error while saving module config!")
            e.printStackTrace()
        }
    }

    private fun loadClient() {
        CLIENT_FILE?.let {
            if (it.exists()) {
                try {
                    val loadJson = BufferedReader(InputStreamReader(FileInputStream(it), StandardCharsets.UTF_8))
                    val guiJason = jsonParser.parse(loadJson) as JsonObject
                    loadJson.close()
                    for ((key, value) in guiJason.entrySet()) {
                        if (key != "Client") continue
                        val json = value as JsonObject
                        trySetClient(json)
                    }
                } catch (e: IOException) {
                    Kura.logger.error("Error while loading Client stuff!")
                    e.printStackTrace()
                }
            }
        }
    }

    private fun loadFriend() {
        FRIEND_FILE?.let {
            if (it.exists()) {
                try {
                    val loadJson = BufferedReader(InputStreamReader(FileInputStream(it), StandardCharsets.UTF_8))
                    val friendJson = jsonParser.parse(loadJson) as JsonObject
                    loadJson.close()
                    FriendManager.friends.clear()
                    for ((name, value) in friendJson.entrySet()) {
                        if (name == null) continue
                        val nmsl = value as JsonObject
                        var isFriend = false
                        try {
                            isFriend = nmsl["isFriend"].asBoolean
                        } catch (e: Exception) {
                            Kura.logger.error("Can't set friend value for $name, unfriended!")
                        }
                        FriendManager.friends.add(FriendManager.Friend(name, isFriend))
                    }
                } catch (e: IOException) {
                    Kura.logger.error("Error while loading friends!")
                    e.printStackTrace()
                }
            }
        }
    }

    private fun loadGUI() {
        GUI_FILE?.let {
            if (it.exists()) {
                try {
                    val loadJson = BufferedReader(InputStreamReader(FileInputStream(it), StandardCharsets.UTF_8))
                    val guiJson = jsonParser.parse(loadJson) as JsonObject
                    loadJson.close()
                    for ((key, value) in guiJson.entrySet()) {
                        var panel = GUIRender.getPanelByName(key as String)
                        if (panel == null) {
                            panel = HUDRender.getPanelByName(key)
                        }
                        if (panel == null) continue
                        val jsonGui = value as JsonObject
                        panel.x = jsonGui["X"].asDouble
                        panel.y = jsonGui["Y"].asDouble
                        panel.extended = jsonGui["Extended"].asBoolean
                    }
                } catch (e: IOException) {
                    Kura.logger.error("Error while loading GUI config!")
                    e.printStackTrace()
                }
            }
        }
    }

    private fun loadHUD() {
        val hudFile = HUD_FILE.takeIf { it?.exists() == true } ?: return
        IOScope.launch {
            val loadJson =
                hudFile.bufferedReader(StandardCharsets.UTF_8).use { JsonParser.parseReader(it) } as? JsonObject
                    ?: return@launch
            for ((key, value) in loadJson.entrySet()) {
                val module = getHUDByName(key as String)
                if (module == NullHUD) {
                    Kura.logger.warn("HUD with name $key not found.")
                    break
                }
                launch {
                    val jsonMod = value as JsonObject
                    val enabled = jsonMod["Enable"].asBoolean
                    if (module.isEnabled && !enabled) {
                        module.disable()
                    }
                    if (module.isDisabled && enabled) {
                        module.enable()
                    }
                    module.x = jsonMod["HUDPosX"].asFloat
                    module.y = jsonMod["HUDPosY"].asFloat
                    if (module.settingList.isNotEmpty()) {
                        trySet(module, jsonMod)
                    }
                    module.onConfigLoad()
                    module.bind = jsonMod["Bind"].asInt
                }
            }
        }
    }


    fun loadCombatSystem() {
        changing = true
        val module = CombatSystem
        IOScope.launch {
            val modulefile =
                File("Kura/config/" + module.moduleName + ".json").takeIf { it.exists() } ?: return@launch
            val moduleJason =
                modulefile.bufferedReader(StandardCharsets.UTF_8).use { JsonParser.parseReader(it) } as? JsonObject
                    ?: return@launch
            launch {
                runCatching {
                    if (moduleJason["Visible"] == null) {
                        moduleJason.addProperty("Visible", module.isVisible)
                    }
                    if (moduleJason["HoldEnable"] == null) {
                        moduleJason.addProperty("HoldEnable", module.holdToEnable)
                    }
                    val jsonModule = getModuleByName(module.moduleName)
                    val enabled = moduleJason["Enable"].asBoolean
                    val visible = moduleJason["Visible"].asBoolean
                    if (jsonModule.isEnabled && !enabled) {
                        jsonModule.disable()
                    }
                    if (jsonModule.isDisabled && enabled) {
                        jsonModule.enable()
                    }
                    jsonModule.holdToEnable = moduleJason["HoldEnable"].asBoolean
                    jsonModule.isVisible = visible
                    if (jsonModule.settingList.isNotEmpty()) {
                        trySet(jsonModule, moduleJason)
                    }
                    jsonModule.onConfigLoad()
                    jsonModule.bind = moduleJason["Bind"].asInt
                }
            }
        }
        if (CombatSystem.debug) ChatUtil.sendMessage("Loaded CombatSystem")
        changing = false
    }


    private fun loadModule(combatMode: String) {
        changing = true
        for (module in CopyOnWriteArrayList(getModules())) {
            if (module == CombatSystem) continue
            IOScope.launch {
                val modulefile =
                    File("Kura/config/" + combatMode + "/modules/" + module.moduleName + ".json").takeIf { it.exists() }
                        ?: return@launch
                val moduleJason =
                    modulefile.bufferedReader(StandardCharsets.UTF_8).use { JsonParser.parseReader(it) } as? JsonObject
                        ?: return@launch
                launch {
                    runCatching {
                        if (moduleJason["Visible"] == null) {
                            moduleJason.addProperty("Visible", module.isVisible)
                        }
                        if (moduleJason["HoldEnable"] == null) {
                            moduleJason.addProperty("HoldEnable", module.holdToEnable)
                        }
                        val jsonModule = getModuleByName(module.moduleName)
                        val enabled = moduleJason["Enable"].asBoolean
                        val visible = moduleJason["Visible"].asBoolean
                        if (jsonModule.isEnabled && !enabled) {
                            jsonModule.disable()
                        }
                        if (jsonModule.isDisabled && enabled) {
                            jsonModule.enable()
                        }
                        jsonModule.holdToEnable = moduleJason["HoldEnable"].asBoolean
                        jsonModule.isVisible = visible
                        if (jsonModule.settingList.isNotEmpty()) {
                            trySet(jsonModule, moduleJason)
                        }
                        jsonModule.onConfigLoad()
                        jsonModule.bind = moduleJason["Bind"].asInt
                        if (CombatSystem.debug) ChatUtil.sendMessage("Loading Modules")
                    }
                }
            }
        }
        changing = false
    }

    private fun tryLoad(): Boolean {
        try {
            CLIENT_FILE = File(CLIENT_CONFIG)
            initializedConfig.add(CLIENT_FILE)
            FRIEND_FILE = File(FRIEND_CONFIG)
            initializedConfig.add(FRIEND_FILE)
            GUI_FILE = File(GUI_CONFIG)
            initializedConfig.add(GUI_FILE)
            HUD_FILE = File(HUD_CONFIG)
            initializedConfig.add(HUD_FILE)
        } catch (e: Exception) {
            Kura.logger.error("Config files aren't exist or are broken!")
            return false
        }
        return true
    }

    private fun checkFile(file: File?) {
        try {
            file?.let {
                if (!it.exists()) {
                    it.parentFile.mkdirs()
                    it.createNewFile()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkPath(path: File) {
        try {
            if (!path.exists()) {
                path.mkdirs()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun trySet(mods: AbstractModule, jsonMod: JsonObject) {
        try {
            for (value in mods.settingList) {
                tryValue(mods.moduleName, value, jsonMod)
            }
        } catch (e: Exception) {
            Kura.logger.error("Cant set value for " + (if (mods is HUDModule) "HUD " else " module ") + mods.moduleName + "!")
        }
    }

    private fun tryValue(name: String, setting: Setting<*>, jsonMod: JsonObject) {
        try {
            if (setting is StringSetting) {
                val sValue = jsonMod[setting.name].asString
                setting.value = sValue
            }
            if (setting is ColorSetting) {
                val rgba = jsonMod[setting.name].asInt
                setting.value = Color(rgba, true)
            }
            if (setting is BooleanSetting) {
                val bValue = jsonMod[setting.name].asBoolean
                setting.value = bValue
            }
            if (setting is DoubleSetting) {
                val dValue = jsonMod[setting.name].asDouble
                setting.value = dValue
            }
            if (setting is IntegerSetting) {
                val iValue = jsonMod[setting.name].asInt
                setting.value = iValue
            }
            if (setting is FloatSetting) {
                val fValue = jsonMod[setting.name].asFloat
                setting.value = fValue
            }
            if (setting is ModeSetting<*>) {
                setting.setValueByString(jsonMod[setting.name].asString)
            }
        } catch (e: Exception) {
            Kura.logger.error("Cant set value for " + name + ",loaded default! Value name: " + setting.name)
        }
    }

    private fun trySetClient(json: JsonObject) {
        try {
            Kura.commandPrefix.value = json["CommandPrefix"].asString
        } catch (e: Exception) {
            Kura.logger.error("Error while setting Client!")
        }
    }

    // Default
    private const val CONFIG_PATH = "${Kura.MOD_NAME}/config/"
    private const val HUD_CONFIG = CONFIG_PATH + "/" + Kura.MOD_NAME + "-HUDModule.json"
    private const val NOTEBOT_PATH = "${Kura.MOD_NAME}/notebot/"
    private const val BACKGROUND_PATH = "${Kura.MOD_NAME}/background/"
    private const val CLIENT_CONFIG = Kura.MOD_NAME + "/" + Kura.MOD_NAME + "-Client.json"
    private const val FRIEND_CONFIG = Kura.MOD_NAME + "/" + Kura.MOD_NAME + "-Friend.json"
    private const val GUI_CONFIG = Kura.MOD_NAME + "/" + Kura.MOD_NAME + "-Gui.json"
    private const val NEW_UI_CONFIG_FILE_NAME = "${Kura.MOD_NAME}/${Kura.MOD_NAME}-NewUi.json"
    private var gsonPretty = GsonBuilder().setPrettyPrinting().create()
    private var jsonParser = JsonParser()


    @JvmStatic
    fun saveAll(combatMode: String) {
        saveClient()
        saveFriend()
        saveGUI()
        saveHUD()
        saveCombatSystem()
        saveModule(combatMode)
        saveNewUiConfig()
        if (CombatSystem.debug) ChatUtil.sendMessage("Saved All")
    }

    @JvmStatic
    fun loadAll(combatMode: String) {
        loadClient()
        loadFriend()
        loadGUI()
        loadHUD()
        loadModule(combatMode)
        loadNewUiConfig()
        if (CombatSystem.debug) ChatUtil.sendMessage("Loaded All")
    }
}
