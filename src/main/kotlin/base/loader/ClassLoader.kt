package base.loader

import net.minecraft.client.MinecraftClient
import java.lang.ClassLoader

object ClassLoader: ClassLoader(MinecraftClient::class.java.classLoader) {
    fun load(array: ByteArray, name: String) {
        defineClass(name, array, 0, array.size)
    }
}