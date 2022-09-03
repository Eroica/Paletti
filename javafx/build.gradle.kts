plugins {
    application
    kotlin("jvm") version "1.7.0"
    id("org.openjfx.javafxplugin") version "0.0.13"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("org.beryx.runtime") version "1.12.7"
    id("com.github.ben-manes.versions") version "0.42.0"
    id("com.jaredsburrows.license")
    id("com.github.gmazzo.buildconfig") version "3.1.0"
}

group = "app.paletti.javafx"
version = "2022.09"

repositories {
    mavenCentral()
}

javafx {
    version = "18"
    modules("javafx.controls", "javafx.fxml", "javafx.swing")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("io.reactivex.rxjava3:rxjava:3.1.5")
    implementation("org.xerial:sqlite-jdbc:3.39.2.1")
    implementation("net.harawata:appdirs:1.2.1")
    implementation("org.slf4j:slf4j-simple:2.0.0")
}

tasks {
    compileJava {
        targetCompatibility = JavaVersion.VERSION_18.toString()
    }
    compileKotlin {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_18.toString()
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_18.toString()
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
    buildConfigField("String", "APP_COPYRIGHT", "\"Copyright © 2022\"")
    buildConfigField("String", "APP_LICENSE", "\"${project.name} (c) 2022 Eroica\"")
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
            "--copyright", "2022",
			"--vendor", "GROUNDCTRL"
        )
    }
}
