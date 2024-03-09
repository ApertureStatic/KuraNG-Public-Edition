package base.system.render.shader

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.gl.GlUniform
import net.minecraft.client.gl.ShaderProgram
import net.minecraft.client.render.VertexFormat
import net.minecraft.resource.ResourceFactory
import net.minecraft.util.Identifier
import net.minecraft.util.Pair
import org.jetbrains.annotations.ApiStatus
import java.io.IOException
import java.util.function.Consumer
import java.util.function.Function

open class GlProgram(id: Identifier, vertexFormat: VertexFormat) {
    private var backingProgram: ShaderProgram? = null

    init {
        REGISTERED_PROGRAMS.add(Pair(
            Function { resourceFactory: ResourceFactory ->
                try {
                    return@Function OwoShaderProgram(resourceFactory, id.toString(), vertexFormat)
                } catch (e: IOException) {
                    throw RuntimeException("Failed to initialized shader program", e)
                }
            },
            Consumer { program: ShaderProgram? ->
                backingProgram = program
                setup()
            }
        ))
    }

    open fun use() {
        RenderSystem.setShader { backingProgram }
    }

    protected open fun setup() {}
    protected fun findUniform(name: String): GlUniform {
        return backingProgram!!.loadedUniforms[name]!!
    }

    class OwoShaderProgram(factory: ResourceFactory, name: String, format: VertexFormat) :
        ShaderProgram(factory, name, format)

    companion object {
        private val REGISTERED_PROGRAMS: MutableList<Pair<Function<ResourceFactory, ShaderProgram>, Consumer<ShaderProgram>>> =
            ArrayList()

        @ApiStatus.Internal
        fun forEachProgram(loader: Consumer<Pair<Function<ResourceFactory, ShaderProgram>, Consumer<ShaderProgram>>>?) {
            REGISTERED_PROGRAMS.forEach(loader)
        }
    }
}