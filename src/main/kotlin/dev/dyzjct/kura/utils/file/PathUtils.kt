package dev.dyzjct.kura.utils.file

import java.nio.file.Path

infix fun Path.resolve(path: Path) = this.resolve(path)

infix fun Path.resolve(path: String) = this.resolve(path)
