package dev.dyzjct.kura.mixin.render;

import base.system.render.shader.GlProgram;
import net.minecraft.client.gl.ShaderProgram;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ShaderProgram.class)
public class MixinShaderProgram {
    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Identifier;<init>(Ljava/lang/String;)V"), require = 0)
    private String fixIdentifier(String id) {
        if (!((Object) this instanceof GlProgram.OwoShaderProgram)) return id;

        var splitName = id.split(":");
        if (splitName.length != 2 || !splitName[0].startsWith("shaders/")) return id;

        return splitName[0].replace("shaders/", "") + ":" + "shaders/core/" + splitName[1];
    }
}
