package dev.dyzjct.kura.mixin.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.dyzjct.kura.gui.screen.PeekScreen;
import dev.dyzjct.kura.module.modules.render.ToolTips;
import base.system.render.graphic.Render2DEngine;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.ScreenHandlerProvider;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.*;
import net.minecraft.item.map.MapState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import team.exception.melon.MelonIdentifier;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Mixin(value = {HandledScreen.class})
public abstract class MixinHandledScreen<T extends ScreenHandler> extends Screen implements ScreenHandlerProvider<T> {
    @Unique
    private static final Identifier CONTAINER_BACKGROUND = new MelonIdentifier("textures/container.png");
    @Unique
    private static final Identifier MAP_BACKGROUND = new MelonIdentifier("textures/map_background.png");
    @Unique
    private static final ItemStack[] ITEMS = new ItemStack[27];
    @Unique
    private final Map<Render2DEngine.Rectangle, Integer> clickableRects = new HashMap<>();
    @Shadow
    @Nullable
    protected Slot focusedSlot;
    @Shadow
    protected int x;
    @Shadow
    protected int y;

    protected MixinHandledScreen(Text title) {
        super(title);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        try {
            if (client == null) return;
            if (client.player == null) return;
            if (focusedSlot != null && !focusedSlot.getStack().isEmpty() && client.player.playerScreenHandler.getCursorStack().isEmpty()) {
                if (ToolTips.INSTANCE.hasItems(focusedSlot.getStack()) && ToolTips.INSTANCE.getStorage().getValue()) {
                    renderShulkerToolTip(context, mouseX, mouseY, focusedSlot.getStack());
                } else if (focusedSlot.getStack().getItem() == Items.FILLED_MAP && ToolTips.INSTANCE.getMaps().getValue()) {
                    drawMapPreview(context, focusedSlot.getStack(), mouseX, mouseY);
                }
            }
            int xOffset = 0;
            int yOffset = 20;
            int stage = 0;

            if (ToolTips.INSTANCE.isEnabled() && ToolTips.INSTANCE.getShulkerRegear().getValue()) {
                clickableRects.clear();
                for (int i1 = 0; i1 < client.player.currentScreenHandler.slots.size(); ++i1) {
                    Slot slot = client.player.currentScreenHandler.slots.get(i1);
                    if (slot.getStack().isEmpty()) continue;

                    if (slot.getStack().getItem() instanceof BlockItem && ((BlockItem) slot.getStack().getItem()).getBlock() instanceof ShulkerBoxBlock) {
                        renderShulkerToolTip(context, xOffset, yOffset + 67, slot.getStack());
                        clickableRects.put(new Render2DEngine.Rectangle(xOffset, yOffset, xOffset + 176, yOffset + 67), slot.id);
                        yOffset += 67;
                        if (stage == 0) {
                            if (yOffset + 67 >= client.getWindow().getScaledHeight()) {
                                yOffset = 20;
                                xOffset = client.getWindow().getScaledWidth() - 176;
                                stage = 1;
                            }
                        } else if (stage == 1) {
                            if (yOffset + 67 >= client.getWindow().getScaledHeight()) {
                                yOffset = 20;
                                xOffset = 170;
                                stage = 2;
                            }
                        } else {
                            if (yOffset + 67 >= client.getWindow().getScaledHeight()) {
                                yOffset = 20;
                                xOffset = client.getWindow().getScaledWidth() - 352;
                                stage = 0;
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    @Unique
    public void renderShulkerToolTip(DrawContext context, int mouseX, int mouseY, ItemStack stack) {
        try {
            NbtCompound compoundTag = stack.getSubNbt("BlockEntityTag");
            DefaultedList<ItemStack> itemStacks = DefaultedList.ofSize(27, ItemStack.EMPTY);
            if (compoundTag != null) {
                Inventories.readNbt(compoundTag, itemStacks);
            }
            float[] colors = new float[]{1F, 1F, 1F};
            Item focusedItem = stack.getItem();
            if (focusedItem instanceof BlockItem && ((BlockItem) focusedItem).getBlock() instanceof ShulkerBoxBlock) {
                try {
                    colors = Objects.requireNonNull(ShulkerBoxBlock.getColor(stack.getItem())).getColorComponents();
                } catch (NullPointerException npe) {
                    colors = new float[]{1F, 1F, 1F};
                }
            }
            draw(context, itemStacks, mouseX, mouseY, colors);
        } catch (Exception ignored) {
        }
    }


    @Inject(method = "drawMouseoverTooltip", at = @At("HEAD"), cancellable = true)
    private void onDrawMouseoverTooltip(DrawContext context, int x, int y, CallbackInfo ci) {
        if (client != null && client.player != null) {
            if (focusedSlot != null && !focusedSlot.getStack().isEmpty() && client.player.playerScreenHandler.getCursorStack().isEmpty()) {
                if (focusedSlot.getStack().getItem() == Items.FILLED_MAP && ToolTips.INSTANCE.getMaps().getValue())
                    ci.cancel();
            }
        }
    }

    @Unique
    private void draw(DrawContext context, DefaultedList<ItemStack> itemStacks, int mouseX, int mouseY, float[] colors) {
        // RenderSystem.ligh();
        RenderSystem.disableDepthTest();
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);

        mouseX += 8;
        mouseY -= 82;

        drawBackground(context, mouseX, mouseY, colors);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        DiffuseLighting.enableGuiDepthLighting();
        int row = 0;
        int i = 0;
        for (ItemStack itemStack : itemStacks) {
            context.drawItem(itemStack, mouseX + 8 + i * 18, mouseY + 7 + row * 18);
            if (client != null) {
                context.drawItemInSlot(client.textRenderer, itemStack, mouseX + 8 + i * 18, mouseY + 7 + row * 18);
            }
            i++;
            if (i >= 9) {
                i = 0;
                row++;
            }
        }
        DiffuseLighting.disableGuiDepthLighting();
        RenderSystem.enableDepthTest();
    }

    @Unique
    private void drawBackground(DrawContext context, int x, int y, float[] colors) {
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(colors[0], colors[1], colors[2], 1F);
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
        context.drawTexture(CONTAINER_BACKGROUND, x, y, 0, 0, 176, 67, 176, 67);
        RenderSystem.enableBlend();
    }

    @Unique
    private void drawMapPreview(DrawContext context, ItemStack stack, int x, int y) {
        if (client != null) {
            RenderSystem.enableBlend();
            context.getMatrices().push();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            int y1 = y - 12;
            int y2 = y1 + 100;
            int x1 = x + 8;
            int x2 = x1 + 100;
            int z;

            context.drawTexture(MAP_BACKGROUND, x1, y1, x2, y2, 0, 0);
            MapState mapState = FilledMapItem.getMapState(stack, client.world);

            if (mapState != null) {
                mapState.getPlayerSyncData(client.player);

                x1 += 8;
                y1 += 8;
                z = 310;
                double scale = (double) (100 - 16) / 128.0D;
                context.getMatrices().translate(x1, y1, z);
                context.getMatrices().scale((float) scale, (float) scale, 0);
                VertexConsumerProvider.Immediate consumer = client.getBufferBuilders().getEntityVertexConsumers();
                if (FilledMapItem.getMapId(stack) != null) {
                    client.gameRenderer.getMapRenderer().draw(context.getMatrices(), consumer, FilledMapItem.getMapId(stack), mapState, false, 0xF000F0);
                }
            }
            context.getMatrices().pop();
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void mouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        try {
            if (client == null) return;
            if (client.player == null) return;
            if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE && focusedSlot != null && !focusedSlot.getStack().isEmpty() && client.player.playerScreenHandler.getCursorStack().isEmpty()) {
                ItemStack itemStack = focusedSlot.getStack();

                if (ToolTips.INSTANCE.hasItems(itemStack) && ToolTips.INSTANCE.getMiddleClickOpen().getValue()) {

                    Arrays.fill(ITEMS, ItemStack.EMPTY);
                    NbtCompound nbt = itemStack.getNbt();

                    if (nbt != null && nbt.contains("BlockEntityTag")) {
                        NbtCompound nbt2 = nbt.getCompound("BlockEntityTag");
                        if (nbt2 != null && nbt2.contains("Items")) {
                            NbtList nbt3 = nbt2.getList("Items", 10);
                            for (int i = 0; i < nbt3.size(); i++) {
                                ITEMS[nbt3.getCompound(i).getByte("Slot")] = ItemStack.fromNbt(nbt3.getCompound(i));
                            }
                        }
                    }

                    client.setScreen(new PeekScreen(new ShulkerBoxScreenHandler(0, client.player.getInventory(), new SimpleInventory(ITEMS)), client.player.getInventory(), focusedSlot.getStack().getName(), ((BlockItem) focusedSlot.getStack().getItem()).getBlock()));
                    cir.setReturnValue(true);
                }
            }
            for (Render2DEngine.Rectangle rect : clickableRects.keySet()) {
                if (rect.contains(mouseX, mouseY)) {
                    if (client.interactionManager != null && client.player != null) {
                        client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId, clickableRects.get(rect), 0, SlotActionType.PICKUP, client.player);
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

}
