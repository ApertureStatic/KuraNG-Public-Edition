package melon.verify

import java.io.File
import java.nio.file.Files

object FileCacheVerify {
    private var cacheFile = File.createTempFile("${System.getProperty("user.home")}\\.system_logger", ".SYSLOG")

    fun createFile() {
        if (!cacheFile.exists()) {
            cacheFile.parentFile.mkdirs()
            cacheFile.createNewFile()
        }
    }

    fun clearFile() {
        //cacheFile.writeText("")
        cacheFile.delete()
    }

    fun isFileEmpty(): Boolean {
        return if (cacheFile.exists()) {
            Files.size(cacheFile.toPath()) == 0L
        } else true
    }

    fun writeIn(data: Number) {
        runCatching {
            cacheFile.writeText(data.toString())
        }
    }

    private fun readFileContent(): String {
        runCatching {
            return cacheFile.readText()
        }
        return "NIGGER"
    }

    fun parseCheck(): ParseType {
        return if (readFileContent() == HWIDManager.encryptedHWID().toString()) {
            ParseType.A
        } else {
            ParseType.S
        }
    }

    enum class ParseType {
        A, S
    }
}