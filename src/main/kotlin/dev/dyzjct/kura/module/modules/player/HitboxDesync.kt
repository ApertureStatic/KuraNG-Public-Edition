package dev.dyzjct.kura.module.modules.player

import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import kotlin.math.abs

object HitboxDesync : Module(name = "HitboxDesync", langName = "卡墙", category = Category.PLAYER) {
    private const val MAGIC_OFFSET = 0.20000996883537

    init {
        onMotion {
            val f = player.horizontalFacing
            val bb = player.boundingBox
            val center: Vec3d = bb.center
            val offset = Vec3d(f.unitVector)
            val fin = merge(Vec3d.of(BlockPos.ofFloored(center)).add(.5, 0.0, .5).add(offset.multiply(MAGIC_OFFSET)), f)
            player.setPosition(
                if (fin.x == 0.0) player.x else fin.x,
                player.y,
                if (fin.z == 0.0) player.z else fin.z
            )
            disable()
        }
    }

    private fun merge(a: Vec3d, facing: Direction): Vec3d {
        return Vec3d(
            a.x * abs(facing.unitVector.x()),
            a.y * abs(facing.unitVector.y()),
            a.z * abs(facing.unitVector.z())
        )
    }
}