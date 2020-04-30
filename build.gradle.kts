plugins {
    kotlin("jvm") version "1.3.71"
    application
    id("org.openjfx.javafxplugin") version "0.0.8"
    id("org.beryx.runtime") version "1.8.0"
}

group = "com.moebots"
version = "1.1"

repositories {
    mavenCentral()
}

javafx {
    version = "14"
    modules("javafx.controls", "javafx.fxml", "javafx.web", "javafx.swing")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}

application {
    mainClassName = "MainKt"
}

runtime {
    jpackage {
        skipInstaller = true
        imageOptions = listOf("--icon", "src/main/resources/Paletti.ico",
            "--copyright", "2020",
            "--vendor", "Moebots")
    }
}
