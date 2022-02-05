plugins {
    application
    kotlin("jvm") version "1.6.10"
    id("org.openjfx.javafxplugin") version "0.0.11"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("org.beryx.runtime") version "1.12.7"
    id("com.github.ben-manes.versions") version "0.42.0"
}

group = "app.paletti.javafx"
version = "2022.02"

repositories {
    mavenCentral()
}

javafx {
    version = "17"
    modules("javafx.controls", "javafx.fxml", "javafx.swing")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("io.reactivex.rxjava3:rxjava:3.1.3")
    implementation("org.xerial:sqlite-jdbc:3.36.0.3")
    implementation("net.harawata:appdirs:1.2.1")
    implementation("org.slf4j:slf4j-simple:1.7.35")
}

tasks {
    compileJava {
        targetCompatibility = JavaVersion.VERSION_17.toString()
    }
    compileKotlin {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_17.toString()
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_17.toString()
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
            "--copyright", "2022",
			"--vendor", "Paletti"
        )
    }
}
