package dev.dyzjct.kura.gui.chat

import com.mojang.blaze3d.systems.RenderSystem
import dev.dyzjct.kura.Kura
import dev.dyzjct.kura.command.CommandManager
import base.system.render.graphic.Render2DEngine
import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ChatScreen
import net.minecraft.client.render.*
import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11.GL_LINE_SMOOTH
import java.awt.Color
import java.io.IOException
import java.util.*

open class MelonGuiChat(startString: String, historybuffer: String, sentHistoryCursor: Int) : ChatScreen(startString) {

    private var cursor = 0
    private var bestMatchingString = ""
    private var completeStringList: List<String> = emptyList()

    private var lastArgumentSize = 0

    private val textInput: String
        get() = chatField.text

    init {
        if (startString != Kura.commandPrefix.value) {
            calculateCompleteStringList(startString)
        }
        chatLastMessage = historybuffer
        cursor = sentHistoryCursor
    }

    override fun charTyped(typedChar: Char, keyCode: Int): Boolean {
        this.messageHistorySize = cursor
        super.charTyped(typedChar, keyCode)
        cursor = this.messageHistorySize

        if (!textInput.startsWith(Kura.commandPrefix.value)) {
            val newGUI: ChatScreen = object : ChatScreen(textInput) {
                var cursor: Int = this@MelonGuiChat.cursor

                @Throws(IOException::class)
                override fun charTyped(typedChar: Char, keyCode: Int): Boolean {
                    this.messageHistorySize = cursor
                    super.charTyped(typedChar, keyCode)
                    cursor = this.messageHistorySize
                    return true
                }
            }
            newGUI.chatLastMessage = this.chatLastMessage
            MinecraftClient.getInstance().setScreen(newGUI)
            return true
        }

        calculateCompleteStringList(textInput)

        return true
    }

    private fun calculateCompleteStringList(line: String) {
        val args = line.removePrefix(".").split(" ")

        lastArgumentSize = args.size
        completeStringList = CommandManager.complete(args)

        if (completeStringList.isNotEmpty()) {
            bestMatchingString = completeStringList.first()
        }
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (keyCode == GLFW.GLFW_KEY_TAB) {
            val splits = chatField.text.removePrefix(".").split(" ")
            val lastStr = splits.last()

            if (bestMatchingString.startsWith(lastStr, ignoreCase = true)) {
                val newText = splits.dropLast(1)
                    .joinToString(" ") + if (splits.size > 1) " $bestMatchingString" else bestMatchingString
                chatField.text = ".$newText"
            }

            return true
        }

        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, partialTicks: Float) {
        Render2DEngine.drawQuad(context.matrices, 2f, height - 14f, width - 2f, height - 2f, Color(Int.MIN_VALUE))

        val text = this.chatField.text ?: ""
        val textX = (this.chatField.textRenderer.getWidth(text) + 4).toFloat()
        val textY = this.chatField.y.toFloat()

        val filteredCompleteList = if (completeStringList.size > 10) {
            completeStringList.filterIndexed { index, str ->
                index <= 10
            }
        } else {
            completeStringList
        }

        val completeListMaxWidth = filteredCompleteList.maxOfOrNull { chatField.textRenderer.getWidth(it) } ?: 2

        val completeBorderY =
            textY - (filteredCompleteList.size * (this.chatField.textRenderer.fontHeight + 2)).toFloat()

        Render2DEngine.drawQuad(
            context.matrices,
            textX - 1,
            completeBorderY - 1,
            textX + completeListMaxWidth + 1,
            textY,
            Color(Int.MIN_VALUE)
        )

        filteredCompleteList.forEachIndexed { index, s ->
            chatField.textRenderer.draw(
                s,
                textX,
                completeBorderY + (chatField.textRenderer.fontHeight + 2) * index,
                0xFFFFFF,
                false,
                context.matrices.peek().positionMatrix,
                context.vertexConsumers,
                TextRenderer.TextLayerType.SEE_THROUGH,
                0,
                0
            )
        }

//        this.chatField.textRenderer.draw(
//            Text.literal(" $bestMatchingString"),
//            textX,
//            textY,
//            0x666666,
//            false,
//            context.matrices.peek().positionMatrix,
//            context.vertexConsumers,
//            TextRenderer.TextLayerType.SEE_THROUGH,
//            0,
//            0
//        )

        this.chatField.render(context, mouseX, mouseY, partialTicks)
        MinecraftClient.getInstance().inGameHud?.let {
            val itextcomponent = it.chatHud?.getTextStyleAt(
                MinecraftClient.getInstance().mouse.x, MinecraftClient.getInstance().mouse.y
            )
            if (itextcomponent != null && itextcomponent.hoverEvent != null) {
                this.hoveredElement(mouseX.toDouble(), mouseY.toDouble())
            }
        }
        RenderSystem.setShader { GameRenderer.getPositionColorProgram() }
        val tessellator = Tessellator.getInstance()
        val bufferBuilder = tessellator.buffer
        GL11.glEnable(GL_LINE_SMOOTH)
        bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR)
        RenderSystem.lineWidth(1f)
        Render2DEngine.setupRender()
        //RENDER TASK
        bufferBuilder.vertex(chatField.x - 2.0, chatField.y - 2.0, 0.0).color(0.8f, 0.1f, 0f, 1f).next()
        bufferBuilder.vertex(chatField.x + chatField.width - 2.0, chatField.y - 2.0, 0.0).color(0.8f, 0.1f, 0f, 1f)
            .next()

        bufferBuilder.vertex(chatField.x + this.chatField.width - 2.0, chatField.y - 2.0, 0.0).color(0.8f, 0.1f, 0f, 1f)
            .next()
        bufferBuilder.vertex(
            chatField.x + chatField.width - 2.0, chatField.y + chatField.height - 2.0, 0.0
        ).color(0.8f, 0.1f, 0f, 1f).next()

        bufferBuilder.vertex(
            chatField.x + chatField.width - 2.0, chatField.y + chatField.height - 2.0, 0.0
        ).color(0.8f, 0.1f, 0f, 1f).next()
        bufferBuilder.vertex(chatField.x - 2.0, chatField.y + this.chatField.height - 2.0, 0.0)
            .color(0.8f, 0.1f, 0f, 1f).next()

        bufferBuilder.vertex(chatField.x - 2.0, chatField.y + this.chatField.height - 2.0, 0.0)
            .color(0.8f, 0.1f, 0f, 1f).next()
        bufferBuilder.vertex(chatField.x - 2.0, chatField.y - 2.0, 0.0).color(0.8f, 0.1f, 0f, 1f).next()
        //END RENDER TASK
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end())
        GL11.glDisable(GL_LINE_SMOOTH)
        RenderSystem.lineWidth(1f)
        Render2DEngine.endRender()
    }

    override fun close() {
        super.close()
        lastArgumentSize = 0
    }
}