package melon.notification

import dev.dyzjct.kura.utils.TimerUtils
import dev.dyzjct.kura.utils.animations.Easing
import java.util.concurrent.CopyOnWriteArrayList

object NotificationManager {
    var taskList = CopyOnWriteArrayList<Notification>()

    fun addNotification(message: String, renderTime: Long = 2500L) {
        taskList.add(Notification(message, renderTime))
    }

    class Notification(var message: String, private val renderTime: Long) {
        private var startTime = System.currentTimeMillis()
        private var startLayerTime = System.currentTimeMillis()
        private var reversedLayer = false
        private var layerTimer = TimerUtils()
        private var timer = TimerUtils()
        var reversed = false
        var leftSided = true
        val baseAnimation
            get() = if (!timer.passed(renderTime)) {
                if (leftSided) {
                    Easing.IN_OUT_EXPO.inc(Easing.toDelta(startTime, renderTime / 8f))
                } else {
                    Easing.IN_OUT_EXPO.dec(Easing.toDelta(startTime, renderTime / 8f))
                }
            } else {
                if (!reversed) {
                    startTime = System.currentTimeMillis()
                    reversed = true
                }
                if (leftSided) {
                    Easing.IN_OUT_CIRC.dec(Easing.toDelta(startTime, renderTime / 8f))
                } else {
                    Easing.IN_OUT_CIRC.inc(Easing.toDelta(startTime, renderTime / 8f))
                }
            }.coerceIn(0f, 1f)
        val layerAnimation
            get() = if (!layerTimer.passed(renderTime - (renderTime / 10f))) {
                if (leftSided) {
                    Easing.IN_OUT_EXPO.inc(Easing.toDelta(startLayerTime, renderTime / 5.8f))
                } else {
                    Easing.IN_OUT_EXPO.dec(Easing.toDelta(startLayerTime, renderTime / 5.8f))
                }
            } else {
                if (!reversedLayer) {
                    startLayerTime = System.currentTimeMillis()
                    reversedLayer = true
                }
                if (leftSided) {
                    Easing.IN_OUT_CIRC.dec(Easing.toDelta(startLayerTime, renderTime / 4.5f))
                } else {
                    Easing.IN_OUT_CIRC.inc(Easing.toDelta(startLayerTime, renderTime / 4.5f))
                }
            }.coerceIn(0f, 1f)
    }
}