package dev.dyzjct.kura.manager

import dev.dyzjct.kura.event.events.player.PlayerMotionEvent

object EventAccessManager {
    private var playerMotion: PlayerMotionEvent? = null

    fun getData(): PlayerMotionEvent? {
        if (playerMotion != null) {
            return playerMotion
        }
        return null
    }

    fun setData(e: PlayerMotionEvent) {
        playerMotion = e
    }
}