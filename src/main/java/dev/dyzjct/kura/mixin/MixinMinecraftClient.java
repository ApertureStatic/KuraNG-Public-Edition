package dev.dyzjct.kura.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.dyzjct.kura.Kura;
import dev.dyzjct.kura.event.events.RunGameLoopEvent;
import dev.dyzjct.kura.event.events.TickEvent;
import dev.dyzjct.kura.event.events.screen.GuiScreenEvent;
import dev.dyzjct.kura.gui.screen.MainMenuScreen;
import dev.dyzjct.kura.manager.FileManager;
import dev.dyzjct.kura.module.AbstractModule;
import dev.dyzjct.kura.system.render.newfont.FontRenderers;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.Icons;
import net.minecraft.client.util.Window;
import net.minecraft.resource.ResourcePack;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.Nullables;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Mixin(MinecraftClient.class)
public abstract class MixinMinecraftClient {
    @Shadow
    static MinecraftClient instance;
    @Shadow
    public ClientPlayerEntity player;
    @Shadow
    public Screen currentScreen;
    @Shadow
    private IntegratedServer server;

    @Shadow
    public ClientPlayNetworkHandler getNetworkHandler() {
        return this.player == null ? null : this.player.networkHandler;
    }

    @Shadow
    public ServerInfo getCurrentServerEntry() {
        return Nullables.map(this.getNetworkHandler(), ClientPlayNetworkHandler::getServerInfo);
    }

    @Shadow
    public abstract void setScreen(@Nullable Screen screen);

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/session/telemetry/GameLoadTimeEvent;startTimer(Lnet/minecraft/client/session/telemetry/TelemetryEventProperty;)V", shift = At.Shift.AFTER))
    public void onInit(RunArgs args, CallbackInfo ci) {
//        BuildersKt.launch(CoroutineUtilsKt.getIOScope(), CoroutineUtilsKt.getIOScope().getCoroutineContext(), CoroutineStart.DEFAULT, (coroutineScope, continuation) -> Kura.Companion);
        Kura.Companion.onManagersInit();
    }

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    public void onPostInit(RunArgs args, CallbackInfo ci) {
        Kura.Companion.onPostInit();
    }

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/session/telemetry/GameLoadTimeEvent;startTimer(Lnet/minecraft/client/session/telemetry/TelemetryEventProperty;)V"))
    public void onFontLoad(RunArgs args, CallbackInfo ci) {
        if (!Kura.Companion.getHasInit()) {
            try {
                FontRenderers.INSTANCE.setDefault(FontRenderers.INSTANCE.createDefault(16f, "comfortaa"));
                FontRenderers.INSTANCE.setCn(FontRenderers.INSTANCE.createDefault(16f, "chinese"));
                FontRenderers.INSTANCE.setLexend(FontRenderers.INSTANCE.createDefault(16f, "lexenddeca-regular"));
                FontRenderers.INSTANCE.setJbMono(FontRenderers.INSTANCE.createDefault(72f, "JetBrainsMono-Bold"));
                FontRenderers.INSTANCE.setNever(FontRenderers.INSTANCE.createDefault(72f, "DancingScript-Medium"));
                FontRenderers.INSTANCE.setBig_default(FontRenderers.INSTANCE.createDefault(72f, "comfortaa"));
                FontRenderers.INSTANCE.setIcons(FontRenderers.INSTANCE.createIcons(20));
                FontRenderers.INSTANCE.setMid_icons(FontRenderers.INSTANCE.createIcons(46f));
                FontRenderers.INSTANCE.setBig_icons(FontRenderers.INSTANCE.createIcons(72f));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Inject(method = "getWindowTitle", at = @At("HEAD"), cancellable = true)
    public void setCustomTitile(CallbackInfoReturnable<String> cir) {
        StringBuilder stringBuilder = new StringBuilder(Kura.Companion.getDISPLAY_NAME() + " ");
        if (MinecraftClient.getModStatus().isModded()) {
            stringBuilder.append("*");
        }
        stringBuilder.append(" ");
        stringBuilder.append(SharedConstants.getGameVersion().getName());
        ClientPlayNetworkHandler clientPlayNetworkHandler = this.getNetworkHandler();
        if (clientPlayNetworkHandler != null && clientPlayNetworkHandler.getConnection().isOpen()) {
            stringBuilder.append(" - ");
            ServerInfo serverInfo = this.getCurrentServerEntry();
            if (this.server != null && !this.server.isRemote()) {
                stringBuilder.append(I18n.translate("title.singleplayer"));
            } else if (serverInfo != null && serverInfo.isRealm()) {
                stringBuilder.append(I18n.translate("title.multiplayer.realms"));
            } else if (this.server != null || this.getCurrentServerEntry() != null && this.getCurrentServerEntry().isLocal()) {
                stringBuilder.append(I18n.translate("title.multiplayer.lan"));
            } else {
                stringBuilder.append(I18n.translate("title.multiplayer.other"));
            }
        }
        cir.setReturnValue(stringBuilder.toString());
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/Window;setIcon(Lnet/minecraft/resource/ResourcePack;Lnet/minecraft/client/util/Icons;)V"))
    private void onChangeIcon(Window window, ResourcePack resourcePack, Icons icons) {
        if (MinecraftClient.IS_SYSTEM_MAC) return;
        RenderSystem.assertInInitPhase();

        try (MemoryStack memorystack = MemoryStack.stackPush()) {
            GLFWImage.Buffer buffer = GLFWImage.malloc(2, memorystack);
            List<InputStream> imgList = List.of(Objects.requireNonNull(Kura.class.getResourceAsStream("/assets/kura/logo/logo.png")), Objects.requireNonNull(Kura.class.getResourceAsStream("/assets/kura/logo/logo.png")));
            List<ByteBuffer> buffers = new ArrayList<>();

            for (int i = 0; i < imgList.size(); i++) {
                NativeImage nativeImage = NativeImage.read(imgList.get(i));
                ByteBuffer bytebuffer = MemoryUtil.memAlloc(nativeImage.getWidth() * nativeImage.getHeight() * 4);

                bytebuffer.asIntBuffer().put(nativeImage.copyPixelsRgba());
                buffer.position(i);
                buffer.width(nativeImage.getWidth());
                buffer.height(nativeImage.getHeight());
                buffer.pixels(bytebuffer);

                buffers.add(bytebuffer);
            }

            GLFW.glfwSetWindowIcon(instance.getWindow().getHandle(), buffer);
            buffers.forEach(MemoryUtil::memFree);
        } catch (IOException ignored) {
        }
    }

    /**
     * @author nmsl mojang
     * @reason fix inGameHud's Bug.
     */
    @Overwrite
    public static boolean isFabulousGraphicsOrBetter() {
        return !instance.gameRenderer.isRenderingPanorama() && instance.options.getGraphicsMode().getValue().getId() >= GraphicsMode.FABULOUS.getId() && !Kura.Companion.getOnDrawInGameHUD();
    }


    @Inject(method = "setScreen", at = @At("HEAD"))
    public void onPreSetScreen(Screen screen, CallbackInfo ci) {
        if (screen != null) {
            new GuiScreenEvent.Display(screen).post();
        }
    }

    @Inject(method = "setScreen", at = @At("RETURN"))
    public void onPostSetScreen(Screen screen, CallbackInfo ci) {
        if (screen != null) {
            new GuiScreenEvent.Displayed(screen).post();
        }
        AbstractModule.Companion.setLaoded(true);

        if (screen instanceof TitleScreen) {
            this.setScreen(new MainMenuScreen());
        }
//        if (screen != verScreen) { //&& Kura.Companion.getId().equals(SocketConnection.INSTANCE.getTaskID())) {
//            setScreen(verScreen);
//        }
    }

    @Inject(method = "setScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;removed()V", shift = At.Shift.BEFORE))
    public void onScreenRemove(Screen screen, CallbackInfo ci) {
        if (screen != null) {
            new GuiScreenEvent.Close(screen).post();
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void onTickHead(CallbackInfo ci) {
        TickEvent.Pre.INSTANCE.post();
    }

    @Inject(method = "tick", at = @At("RETURN"))
    public void onTickReturn(CallbackInfo ci) {
        TickEvent.Post.INSTANCE.post();
    }

    @Inject(method = "run", at = @At("RETURN"))
    public void shutdown(CallbackInfo info) {
        Kura.Companion.getLogger().warn("Saving Kura configuration please wait...");
        FileManager.saveAll();
        Kura.Companion.getLogger().warn("Configuration saved!");
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/RenderTickCounter;beginRenderTick(J)I", shift = At.Shift.BEFORE))
    public void render$Inject$INVOKE$updateTimer(boolean tick, CallbackInfo ci) {
        instance.getProfiler().push("kuraRunGameLoop");
        RunGameLoopEvent.Start.INSTANCE.post();
        instance.getProfiler().pop();
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiler/Profiler;pop()V", ordinal = 0, shift = At.Shift.AFTER))
    public void renderTick(boolean tick, CallbackInfo ci) {
        instance.getProfiler().push("kuraRunGameLoop");
        RunGameLoopEvent.Tick.INSTANCE.post();
        instance.getProfiler().pop();
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Mouse;updateMouse()V", shift = At.Shift.BEFORE))
    public void render$Inject$INVOKE$endStartSection(boolean tick, CallbackInfo ci) {
        instance.getProfiler().push("kuraRunGameLoop");
        RunGameLoopEvent.Render.INSTANCE.post();
        instance.getProfiler().pop();
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiler/Profiler;pop()V", ordinal = 7, shift = At.Shift.BEFORE))
    public void render$Inject$INVOKE$isFramerateLimitBelowMax(boolean tick, CallbackInfo ci) {
        instance.getProfiler().push("kuraRunGameLoop");
        RunGameLoopEvent.End.INSTANCE.post();
        instance.getProfiler().pop();
    }
}
