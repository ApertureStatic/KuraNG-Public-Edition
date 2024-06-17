package dev.dyzjct.kura.system.util.io

import java.nio.Buffer

fun Buffer.skip(count: Int) {
    this.position(position() + count)
}