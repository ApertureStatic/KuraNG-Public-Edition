package melon.verify

import java.security.MessageDigest

object HWIDManager {
    private fun getHWID(): String {
        return System.getenv("PROCESSOR_IDENTIFIER") +
                System.getenv("COMPUTERNAME") +
                System.getenv("PROCESSOR_ARCHITECTURE") +
                System.getenv("TEMP") +
                System.getenv("COMPUTERNAME") +
                System.getProperty("user.name")
    }

    private fun encryptBySHA512(input: String): ByteArray {
        val messageDigest = MessageDigest.getInstance("SHA-512")
        return messageDigest.digest(input.toByteArray())
    }

    private fun convertByteArrayToInt(byteArray: ByteArray): Int {
        var result = 0
        for (i in byteArray.indices) {
            val byteValue = byteArray[i].toInt() and 0xFF // Convert signed byte to unsigned int
            result = (result shl 8) or byteValue
        }
        return result
    }

    fun encryptedHWID(): Int {
        return convertByteArrayToInt(encryptBySHA512(getHWID()))
    }
}