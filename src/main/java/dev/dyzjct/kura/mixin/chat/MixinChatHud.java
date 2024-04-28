package dev.dyzjct.kura.mixin.chat;

import dev.dyzjct.kura.mixins.IChatHud;
import dev.dyzjct.kura.mixins.IChatHudLine;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.client.util.ChatMessages;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ChatHud.class)
public abstract class MixinChatHud implements IChatHud {
    @Shadow
    @Final
    private MinecraftClient client;
    @Shadow
    @Final
    private List<ChatHudLine.Visible> visibleMessages;
    @Shadow
    @Final
    private List<ChatHudLine> messages;
    @Shadow
    private int scrolledLines;
    @Shadow
    private boolean hasUnreadNewMessages;
    @Unique
    private int nextId;

    @Shadow
    public abstract void addMessage(Text message);

    @Shadow
    public int getWidth() {
        return 0;
    }

    @Shadow
    public int getHeight() {
        return 0;
    }

    @Shadow
    public double getChatScale() {
        return 0.0;
    }

    @Shadow
    private boolean isChatFocused() {
        return false;
    }

    @Shadow
    public void scroll(int scroll) {
    }

    @Override
    public void kuraAddMessage(Text message, int id) {
        nextId = id;
        addMessage(message);
        nextId = Integer.MIN_VALUE;
    }

    /**
     * @author nigger
     * @reason Fix List Crash Error
     */
    @Overwrite
    private void addMessage(Text message, @Nullable MessageSignatureData signature, int ticks, @Nullable MessageIndicator indicator, boolean refresh) {
        synchronized (visibleMessages) {
            int i = MathHelper.floor((double) this.getWidth() / this.getChatScale());
            if (indicator != null && indicator.icon() != null) {
                i -= indicator.icon().width + 4 + 2;
            }
            List<OrderedText> list = ChatMessages.breakRenderedChatMessageLines(message, i, this.client.textRenderer);
            boolean bl = this.isChatFocused();
            for (int j = 0; j < list.size(); ++j) {
                OrderedText orderedText = list.get(j);
                if (bl && this.scrolledLines > 0) {
                    this.hasUnreadNewMessages = true;
                    this.scroll(1);
                }
                boolean bl2 = j == list.size() - 1;
                this.visibleMessages.add(0, new ChatHudLine.Visible(ticks, orderedText, indicator, bl2));
            }
            while (this.visibleMessages.size() > 100) {
                this.visibleMessages.remove(this.visibleMessages.size() - 1);
            }
            if (!refresh) {
                this.messages.add(0, new ChatHudLine(ticks, message, signature, indicator));
                while (this.messages.size() > 100) {
                    this.messages.remove(this.messages.size() - 1);
                }
            }
        }
    }

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;ILnet/minecraft/client/gui/hud/MessageIndicator;Z)V", at = @At(value = "INVOKE", target = "Ljava/util/List;add(ILjava/lang/Object;)V", ordinal = 0, shift = At.Shift.AFTER))
    private void onAddMessageAfterNewChatHudLineVisible(Text message, MessageSignatureData signature, int ticks, MessageIndicator indicator, boolean refresh, CallbackInfo info) {
        ((IChatHudLine) (Object) visibleMessages.get(0)).setId(nextId);
    }

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;ILnet/minecraft/client/gui/hud/MessageIndicator;Z)V", at = @At(value = "INVOKE", target = "Ljava/util/List;add(ILjava/lang/Object;)V", ordinal = 1, shift = At.Shift.AFTER))
    private void onAddMessageAfterNewChatHudLine(Text message, MessageSignatureData signature, int ticks, MessageIndicator indicator, boolean refresh, CallbackInfo info) {
        ((IChatHudLine) (Object) messages.get(0)).setId(nextId);
    }

    @Inject(at = @At("HEAD"), method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;ILnet/minecraft/client/gui/hud/MessageIndicator;Z)V")
    private void onAddMessage(Text message, @Nullable MessageSignatureData signature, int ticks, @Nullable MessageIndicator indicator, boolean refresh, CallbackInfo info) {
        try {
            visibleMessages.removeIf(msg -> ((IChatHudLine) (Object) msg).getId() == nextId && nextId != Integer.MIN_VALUE);
            for (int i = messages.size() - 1; i > -1; i--) {
                if (((IChatHudLine) (Object) messages.get(i)).getId() == nextId && nextId != Integer.MIN_VALUE) {
                    messages.remove(i);
                }
            }
        } catch (Exception ignored) {
        }
    }
}
