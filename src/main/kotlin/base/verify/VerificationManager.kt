package base.verify

import base.system.event.AlwaysListening

object VerificationManager : AlwaysListening {
    init {/*
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
        }*/
    }
}