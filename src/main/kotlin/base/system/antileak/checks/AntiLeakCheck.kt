package base.system.antileak.checks

interface AntiLeakCheck {
    fun isSafe(): Boolean
    fun isNotSafe() = !isSafe()
}
