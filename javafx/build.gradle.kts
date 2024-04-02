plugins {
    application
    kotlin("jvm") version "1.9.20"
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("org.beryx.runtime") version "1.12.7"
    id("com.github.ben-manes.versions") version "0.51.0"
    id("com.jaredsburrows.license")
    id("com.github.gmazzo.buildconfig") version "5.3.5"
}

group = "app.paletti.javafx"
version = "2023.12"

repositories {
    mavenCentral()
}

javafx {
    version = "22"
    modules("javafx.controls", "javafx.fxml", "javafx.swing")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:1.7.3")
    implementation("org.xerial:sqlite-jdbc:3.45.2.0")
    implementation("net.harawata:appdirs:1.2.2")
    implementation("org.slf4j:slf4j-simple:2.0.9")
}

tasks {
    compileJava {
        targetCompatibility = JavaVersion.VERSION_20.toString()
    }
    compileKotlin {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_20.toString()
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_20.toString()
    }
    jpackage {
        finalizedBy("copyDlls")
    }
}

tasks.register<Copy>("copyDlls") {
    from(".")
    include("*.dll")
    into(file(layout.buildDirectory.dir("jpackage/Paletti")))
}

buildConfig {
    useKotlinOutput()
    packageName("app.paletti")

    buildConfigField("String", "APP_NAME", "\"${project.name}\"")
    buildConfigField("String", "APP_VERSION", "\"${project.version}\"")
    buildConfigField("String", "APP_COPYRIGHT", "\"Copyright © 2022-2023\"")
    buildConfigField("String", "APP_LICENSE", "\"${project.name} (c) 2022-2023 Eroica\"")
}

application {
    mainClass.set("MainKt")
}

runtime {
    options.set(listOf("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages"))
    launcher {
        noConsole = true
    }
    jpackage {
        imageName = "Paletti"
        skipInstaller = true
        imageOptions = listOf(
            "--icon", "src/main/resources/Paletti.ico",
            "--copyright", "2022-2023",
            "--vendor", "GROUNDCTRL"
        )
    }
}
