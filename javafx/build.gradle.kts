plugins {
    application
    kotlin("jvm") version "1.5.10"
    id("org.openjfx.javafxplugin") version "0.0.10"
    id("com.github.johnrengelman.shadow") version "7.0.0"
    id("org.beryx.runtime") version "1.12.5"
    id("com.github.ben-manes.versions") version "0.39.0"
}

group = "app.paletti.javafx"
version = "2021.08"

repositories {
    mavenCentral()
}

javafx {
    version = "16"
    modules("javafx.controls", "javafx.fxml", "javafx.swing")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("io.reactivex.rxjava3:rxjava:3.0.13")
    implementation("org.xerial:sqlite-jdbc:3.36.0.1")
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

tasks.register<Copy>("copyDlls") {
    from("/libs")
    include("*.dll")
    into(file(layout.buildDirectory.dir("jpackage/Paletti")))
}

application {
    mainClass.set("MainKt")
}

runtime {
    options.set(listOf("--strip-debug", "--no-header-files", "--no-man-pages"))
    launcher {
        noConsole = true
    }
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
