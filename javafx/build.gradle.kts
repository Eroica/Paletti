import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    kotlin("jvm") version "1.5.10"
    id("org.openjfx.javafxplugin") version "0.0.10"
    id("com.github.johnrengelman.shadow") version "7.0.0"
    id("org.beryx.runtime") version "1.12.5"
}

group = "app.paletti.javafx"
version = "2.0.1"

repositories {
    mavenCentral()
}

javafx {
    version = "16"
    modules("javafx.controls", "javafx.fxml", "javafx.swing")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    implementation("io.reactivex.rxjava2:rxjava:2.2.21")
    implementation("io.reactivex.rxjava2:rxjavafx:2.2.2")
    implementation("org.xerial:sqlite-jdbc:3.34.0")
    implementation("net.harawata:appdirs:1.2.1")
    implementation("org.slf4j:slf4j-simple:1.7.30")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    jpackage {
        finalizedBy("copyDlls")
    }
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        useIR = true
    }
}

tasks.register<Copy>("copyDlls") {
    from("/libs")
    include("*.dll")
    into(file(layout.buildDirectory.dir("jpackage/Paletti")))
}

application {
    mainClass.set("MainKt")
}

runtime {
    jpackage {
        imageName = "Paletti"
        skipInstaller = true
        imageOptions = listOf(
            "--icon", "src/main/resources/Paletti.ico",
            "--copyright", "2021",
			"--vendor", "Paletti"
        )
    }
}
