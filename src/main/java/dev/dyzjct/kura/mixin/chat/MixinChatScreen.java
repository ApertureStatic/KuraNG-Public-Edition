package dev.dyzjct.kura.mixin.chat;

import dev.dyzjct.kura.Kura;
import dev.dyzjct.kura.command.CommandManager;
import dev.dyzjct.kura.gui.chat.MelonGuiChat;
import base.events.chat.MessageSentEvent;
import base.utils.Wrapper;
import base.utils.chat.ChatUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.StringHelper;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public class MixinChatScreen extends Screen {
    @Shadow
    public String chatLastMessage;
    @Shadow
    public int messageHistorySize;
    @Shadow
    protected TextFieldWidget chatField;

    protected MixinChatScreen(Text title) {
        super(title);
    }

    @Shadow
    public String normalize(String message) {
        return StringHelper.truncateChat(StringUtils.normalizeSpace(message.trim()));
    }

    @Inject(method = "keyPressed", at = @At(value = "RETURN"), cancellable = true)
    public void onKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        Screen currentScreen = Wrapper.getMinecraft().currentScreen;
        if (currentScreen instanceof ChatScreen && !(currentScreen instanceof MelonGuiChat)) {
            if (chatField.getText().startsWith(Kura.Companion.getCommandPrefix().getValue())) {
                Wrapper.getMinecraft().setScreen(new MelonGuiChat(chatField.getText(), chatLastMessage, messageHistorySize));
                cir.setReturnValue(false);
            }
        }
    }


    /**
     * @author zenhao
     * @reason FUCK EVENT MANAGER
     */
    @Overwrite
    public boolean sendMessage(String message, boolean addToHistory) {
        if (message.startsWith(Kura.Companion.getCommandPrefix().getValue())) {
            //ChatUtil.INSTANCE.sendMessage("Command Debug!");
            try {
                if (MinecraftClient.getInstance().inGameHud != null && MinecraftClient.getInstance().inGameHud.getChatHud() != null) {
                    MinecraftClient.getInstance().inGameHud.getChatHud().addToMessageHistory(message);
                }
                if (message.length() > 1) {
//                    CommandManager.INSTANCE.callCommand(message.substring(Command.getCommandPrefix().length() - 1));
                    String errorMessage = CommandManager.INSTANCE.invoke(message);
                    if (errorMessage != null) {
                        ChatUtil.INSTANCE.sendErrorMessage(errorMessage);
                    }
                } else {
                    ChatUtil.INSTANCE.sendWarnMessage("Please enter a command.");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            chatField.setText("");
            chatField.setFocused(false);
            chatField.setEditable(false);
            return true;
        }
        if ((message = this.normalize(message)).isEmpty()) {
            return true;
        }
        MessageSentEvent messageSentEvent = new MessageSentEvent(message);
        messageSentEvent.post();
        if (messageSentEvent.getCancelled()) {
            return false;
        }
        if (client != null) {
            if (addToHistory && client.inGameHud != null && client.inGameHud.getChatHud() != null) {
                this.client.inGameHud.getChatHud().addToMessageHistory(message);
            }
            if (client.player != null && client.getNetworkHandler() != null) {
                if (messageSentEvent.getMessage().startsWith("/")) {
                    this.client.player.networkHandler.sendChatCommand(message.substring(1));
                } else {
                    this.client.player.networkHandler.sendChatMessage(messageSentEvent.getMessage());
                }
            }
        }
        return true;
    }
}
