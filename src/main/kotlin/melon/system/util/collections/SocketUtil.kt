package melon.system.util.collections

import java.net.Socket

val Socket.isNotClosed: Boolean get() = !this.isClosed