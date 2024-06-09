package dev.dyzjct.kura.mixin.gui;

import dev.dyzjct.kura.module.modules.misc.AutoReconnect;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.GameModeSelectionScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(DisconnectedScreen.class)
public abstract class MixinDisconnectScreen extends Screen {
    @Unique
    private final GameModeSelectionScreen.ButtonWidget reconnectBtn;

    protected MixinDisconnectScreen(Text title, GameModeSelectionScreen.ButtonWidget reconnectBtn) {
        super(title);
        this.reconnectBtn = reconnectBtn;
    }

    @Override
    public void tick() {
        if (AutoReconnect.INSTANCE.isDisabled() || !AutoReconnect.INSTANCE.isReconnecting()) return;
        if (reconnectBtn != null) reconnectBtn.setMessage(Text.literal(AutoReconnect.INSTANCE.getText()));
    }
}