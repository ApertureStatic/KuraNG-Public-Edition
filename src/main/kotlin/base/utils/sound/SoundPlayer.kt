package base.utils.sound

import base.utils.concurrent.threads.ConcurrentScope
import kotlinx.coroutines.launch
import java.io.InputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.FloatControl
import kotlin.math.ln

class SoundPlayer(private val file: InputStream) {
    fun play(volume: Float) {
        ConcurrentScope.launch {
            playSound(volume)
        }
    }

    private fun playSound(volume: Float) {
        try {
            val audioInputStream = AudioSystem.getAudioInputStream(this.file)
            val clip = AudioSystem.getClip()
            clip.open(audioInputStream)

            val gainControl = clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl

            val dB = (ln(volume.toDouble()) / ln(10.0) * 20.0).toFloat()
            gainControl.value = dB

            clip.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}