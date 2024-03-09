package melon.system.antileak.checks

import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*

object ProcessCheck : AntiLeakCheck {
    private val bannedProcess get() = listOf("wireshark", "mitmproxy")

    override fun isSafe(): Boolean {
        try {
            val processBuilder = ProcessBuilder()
            processBuilder.command("tasklist.exe")
            val process = processBuilder.start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String
            while (reader.readLine().also { line = it } != null) {
                if (bannedProcess.contains(line.lowercase(Locale.getDefault()))) {
                    return false
                }
            }
        } catch (_: Exception) {}
        return true
    }
}
