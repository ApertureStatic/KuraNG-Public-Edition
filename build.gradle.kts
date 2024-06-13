plugins {
    java
    kotlin("jvm") version "2.0.0-Beta4"
    id("fabric-loom") version "1.6-SNAPSHOT"
}

repositories {
    mavenLocal()
    mavenCentral()
}

val library by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings("net.fabricmc:yarn:${property("yarn_mappings")}:v2")

    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-renderer-indigo:${property("fabric_version")}")

    library(kotlin("stdlib"))
    library("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
}

loom {
    accessWidenerPath.assign(file("src/main/resources/kura.accesswidener"))
}

kotlin {
    jvmToolchain(17)
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(17)
    }

    compileKotlin {
        kotlinOptions {
            freeCompilerArgs = listOf(
                "-opt-in=kotlin.RequiresOptIn",
                "-opt-in=kotlin.contracts.ExperimentalContracts",
                "-Xlambdas=indy",
                "-Xjvm-default=all",
                "-Xbackend-threads=0",
                "-Xno-source-debug-extension"
            )
        }
        compilerOptions {
            languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
            apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
        }
    }

    processResources {
        inputs.property("version", project.version)
        filesMatching("fabric.mod.json") {
            expand(getProperties())
            expand(mutableMapOf("version" to project.version))
        }
    }

    jar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        from(
            library.map {
                if (it.isDirectory) {
                    it
                } else {
                    zipTree(it)
                }
            }
        )

        exclude("META-INF/*.RSA", "META-INF/*.DSA", "META-INF/*.SF")
    }

    fun registerAutoBuildTask(name: String, targetFolder: String) {
        register<Copy>(name) {
            group = "auto build"
            dependsOn("build")

            if (file("$targetFolder/Kura-nG*.jar").exists()) {
                delete {
                    fileTree("$targetFolder/").matching {
                        include("Kura-NG*.jar")
                    }
                }
            }

            from("build/libs/")
            include("Kura-NG*.jar")
            into("$targetFolder/")
        }
    }
    registerAutoBuildTask("dyzjct", "E:\\KuraNG\\.minecraft\\mods")
    registerAutoBuildTask("dyz-new", "C:\\Minecraft\\Plain Craft Launcher\\.minecraft\\versions\\1.20.4-Fabric 0.15.11\\mods")
}