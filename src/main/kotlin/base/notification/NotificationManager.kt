package base.notification

import dev.dyzjct.kura.module.hud.NotificationHUD
import dev.dyzjct.kura.utils.TimerUtils
import dev.dyzjct.kura.utils.animations.Easing
import java.util.concurrent.CopyOnWriteArrayList

object NotificationManager {
    val taskList = CopyOnWriteArrayList<NewNotification>()

    fun addNotification(message: String) {
        taskList.add(NewNotification(message))
    }

    class NewNotification(val message: String) {
        private var startTime = System.currentTimeMillis()
        private var noResetTime = System.currentTimeMillis()
        private var timer = TimerUtils()
        var reversed = false
        val length = NotificationHUD.animationLength * 100
        val animation
            get() = if (!timer.passed(length / 2f)) {
                Easing.IN_OUT_EXPO.dec(Easing.toDelta(startTime, length / NotificationHUD.fadeFraction))
                    .coerceIn(0f, 1f)
            } else {
                if (!reversed) {
                    startTime = System.currentTimeMillis()
                    reversed = true
                }
                Easing.IN_OUT_CIRC.inc(Easing.toDelta(startTime, length / NotificationHUD.fadeFraction))
            }.coerceIn(0f, 1f)
        val animationNoReset
            get() = Easing.OUT_SINE.dec(Easing.toDelta(noResetTime, length))
                .coerceIn(0f, 1f)
    }
}