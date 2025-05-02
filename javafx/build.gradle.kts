plugins {
    application
    kotlin("jvm") version "2.1.20"
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("com.dua3.gradle.runtime") version "1.13.1-patch-1"
    id("com.github.ben-manes.versions") version "0.52.0"
    id("com.jaredsburrows.license")
    id("com.github.gmazzo.buildconfig") version "5.6.2"
}

group = "app.paletti.javafx"
version = "2025.05"

repositories {
    mavenCentral()
}

javafx {
    version = "24"
    modules("javafx.controls", "javafx.fxml", "javafx.swing")
}

kotlin {
    jvmToolchain(23)
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:1.10.2")
    implementation("org.xerial:sqlite-jdbc:3.49.1.0")
    implementation("net.harawata:appdirs:1.4.0")
    implementation("org.slf4j:slf4j-simple:2.0.17")
}

tasks {
    jpackage {
        finalizedBy("copyDlls")
    }
}

tasks.register<Copy>("copyDlls") {
    from(".")
    include("*.dll")
    into(file(layout.buildDirectory.dir("jpackage/Paletti")))
}

tasks.named("dependencyUpdates", com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask::class).configure {
    val immaturityLevels = listOf("rc", "cr", "m", "beta", "alpha", "preview")
    val immaturityRegexes = immaturityLevels.map { ".*[.\\-]$it[.\\-\\d]*".toRegex(RegexOption.IGNORE_CASE) }
    fun immaturityLevel(version: String): Int = immaturityRegexes.indexOfLast { version.matches(it) }
    rejectVersionIf { immaturityLevel(candidate.version) > immaturityLevel(currentVersion) }
}

buildConfig {
    useKotlinOutput()
    packageName("app.paletti")

    buildConfigField("String", "APP_NAME", "\"${project.name}\"")
    buildConfigField("String", "APP_VERSION", "\"${project.version}\"")
    buildConfigField("String", "APP_COPYRIGHT", "\"Copyright © 2022-2025\"")
    buildConfigField("String", "APP_LICENSE", "\"${project.name} (c) 2022-2025 Eroica\"")
}

application {
    mainClass.set("MainKt")
}

runtime {
    options.set(listOf("--strip-debug", "--compress", "zip-9", "--no-header-files", "--no-man-pages"))
    modules = listOf(
        "java.sql",
        "java.desktop",
        "java.logging",
        "java.scripting",
        "java.xml",
        "jdk.unsupported",
        "java.datatransfer"
    )
    launcher {
        noConsole = true
    }
    jpackage {
        imageName = "Paletti"
        skipInstaller = true
        imageOptions = listOf(
            "--icon", "src/main/resources/Paletti.ico",
            "--copyright", "2022-2025",
            "--vendor", "GROUNDCTRL"
        )
    }
}
