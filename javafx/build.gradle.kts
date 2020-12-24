plugins {
    application
    kotlin("jvm") version "1.4.21"
    id("org.openjfx.javafxplugin") version "0.0.9"
    id("com.github.johnrengelman.shadow") version "6.1.0"
    id("org.beryx.runtime") version "1.12.1"
}

group = "app.paletti.javafx"
version = "2.0"

repositories {
    mavenCentral()
}

javafx {
    version = "14"
    modules("javafx.controls", "javafx.fxml", "javafx.swing")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:1.4.2")
    implementation("net.harawata:appdirs:1.2.0")
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
        dependsOn(":copyDlls")
    }
}

tasks.register<Copy>("copyDlls") {
        from(file("$projectDir/libs"))
        include("*.dll")
        into(file("$buildDir/jpackage/Paletti"))
}

application {
    mainClassName = "MainKt"
}

runtime {
    jpackage {
        imageName = "Paletti"
        skipInstaller = true
        imageOptions = listOf(
            "--icon", "src/main/resources/Paletti.ico",
            "--copyright", "2020",
			"--vendor", "Paletti"
        )
    }
}
