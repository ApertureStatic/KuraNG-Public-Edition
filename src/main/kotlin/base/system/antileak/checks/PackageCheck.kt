package base.system.antileak.checks

object PackageCheck : AntiLeakCheck {
    private val bannedPackage get() = listOf("lemon")

    override fun isSafe(): Boolean {
        Package.getPackages().forEach {
            for (noAllow in bannedPackage) {
                if (it?.name?.contains(noAllow) == true) {
                    return false
                }
            }
        }
        return true
    }
}
