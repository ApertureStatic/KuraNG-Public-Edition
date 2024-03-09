package dev.dyzjct.kura.utils.file

import java.io.File

fun File.notExists() = !this.exists()

fun File.mkdirIfNotExists(): Boolean {
    return if (this.notExists()) {
        this.mkdir()
        true
    } else false
}

fun File.mkdirsIfNotExists(): Boolean {
    return if (this.notExists()) {
        this.mkdirs()
        true
    } else false
}

fun File.createFileIfNotExists(): Boolean {
    return if (this.notExists()) {
        this.parentFile.mkdirsIfNotExists()
        this.createNewFile()
        true
    } else false
}

fun File.deleteIfExists(): Boolean {
    return if (this.exists()) {
        this.delete()
        true
    } else false
}