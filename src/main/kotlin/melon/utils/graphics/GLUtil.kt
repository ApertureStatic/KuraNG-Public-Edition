package melon.utils.graphics

import com.mojang.blaze3d.platform.GlStateManager._activeTexture
import org.lwjgl.opengl.GL11

object GLUtil {
    private val textureState = arrayOf<TextureState>()
    private val lightingState = BooleanState(2896)
    private val alphaState = AlphaState()
    private var activeTextureUnit = 0
    var defaultTexUnit = 33984

    init {
        for (i in 0..7) {
            textureState[i] = TextureState()
        }
    }

    fun disableAlpha() {
        alphaState.alphaTest.setDisabled()
    }

    fun enableAlpha() {
        alphaState.alphaTest.setEnabled()
    }

    fun alphaFunc(func: Int, ref: Float) {
        if (func != alphaState.func || ref != alphaState.ref) {
            alphaState.func = func
            alphaState.ref = ref
            GL11.glAlphaFunc(func, ref)
        }
    }

    fun enableLighting() {
        lightingState.setEnabled()
    }

    fun disableLighting() {
        lightingState.setDisabled()
    }

    fun setActiveTexture(texture: Int) {
        if (activeTextureUnit != texture - defaultTexUnit) {
            activeTextureUnit = texture - defaultTexUnit
            _activeTexture(texture)
        }
    }

    fun enableTexture2D() {
        textureState[activeTextureUnit].texture2DState.setEnabled()
    }

    fun disableTexture2D() {
        textureState[activeTextureUnit].texture2DState.setDisabled()
    }

    class TextureState {
        var texture2DState = BooleanState(3553)
        var textureName = 0
    }

    class BooleanState(private val capability: Int) {
        private var currentState = false
        fun setDisabled() {
            setState(false)
        }

        fun setEnabled() {
            setState(true)
        }

        fun setState(state: Boolean) {
            if (state != currentState) {
                currentState = state
                if (state) {
                    GL11.glEnable(capability)
                } else {
                    GL11.glDisable(capability)
                }
            }
        }
    }

    class AlphaState {
        var alphaTest = BooleanState(3008)
        var func = 519
        var ref = -1.0f
    }

}