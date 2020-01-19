plugins {
    kotlin("jvm") version "1.3.61"
    application
    id("org.openjfx.javafxplugin") version "0.0.8"
    id("edu.sc.seis.launch4j") version "2.4.6"
}

group = "com.moebots"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

javafx {
    modules("javafx.controls", "javafx.fxml", "javafx.web")
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

launch4j {
    mainClassName = "MainKt"
    stayAlive = true
}
