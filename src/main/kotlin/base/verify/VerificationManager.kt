package base.verify

import base.events.client.VerificationEvent
import base.system.event.AlwaysListening
import base.system.event.listener
import base.verify.SocketConnection.taskID
import com.mojang.blaze3d.systems.RenderSystem
import dev.dyzjct.kura.Kura.Companion.id
import dev.dyzjct.kura.utils.math.RandomUtil

object VerificationManager : AlwaysListening {
    init {
        listener<VerificationEvent.DrawTessellator>(true) {
            if (id != taskID) {
                RenderSystem.assertOnRenderThread()
                RenderSystem.getProjectionMatrix().scale(RandomUtil.nextFloat(0.01f, 0.8f))
                RenderSystem.getProjectionMatrix().rotate(
                    RandomUtil.nextFloat(0.01f, 0.8f),
                    RandomUtil.nextFloat(0.01f, 0.8f),
                    RandomUtil.nextFloat(0.01f, 0.8f),
                    RandomUtil.nextFloat(0.01f, 0.8f)
                )
            }
        }

        listener<VerificationEvent.DrawBuffer>(true) {
            if (id != taskID) {
                RenderSystem.assertOnRenderThread()
                RenderSystem.getProjectionMatrix().scale(RandomUtil.nextFloat(0.01f, 0.8f))
                RenderSystem.getProjectionMatrix().rotate(
                    RandomUtil.nextFloat(0.01f, 0.8f),
                    RandomUtil.nextFloat(0.01f, 0.8f),
                    RandomUtil.nextFloat(0.01f, 0.8f),
                    RandomUtil.nextFloat(0.01f, 0.8f)
                )
            }
        }
    }
}