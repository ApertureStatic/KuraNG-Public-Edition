package base.notification

import dev.dyzjct.kura.utils.TimerUtils
import dev.dyzjct.kura.utils.animations.Easing
import java.awt.Color
import java.util.concurrent.CopyOnWriteArrayList

object NotificationManager {
    val taskList = CopyOnWriteArrayList<NewNotification>()

    fun addNotification(message: String, mode: NotiMode) {
        taskList.add(NewNotification(message, mode))
    }

    class NewNotification(val message: String, mode: NotiMode) {
        private var startTime = System.currentTimeMillis()
        private var timer = TimerUtils()
        val color = mode.color
        var reversed = false
        val length = 1500L
        val animation
            get() = if (!timer.passed(length)) {
                Easing.IN_OUT_EXPO.dec(Easing.toDelta(startTime, length / 8f))
            } else {
                if (!reversed) {
                    startTime = System.currentTimeMillis()
                    reversed = true
                }
                Easing.IN_OUT_CIRC.inc(Easing.toDelta(startTime, length / 8f))
            }.coerceIn(0f, 1f)
    }

    enum class NotiMode(val color: Color) {
        EnableModule(Color(72, 255, 72)),
        DisableModule(Color(255, 72, 72)),
        TotemPop(Color(76, 78, 215)),
        Warning(Color(255, 255, 72)),
        Error(Color(150, 25, 100))
    }
}