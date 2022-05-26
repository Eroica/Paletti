pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.jaredsburrows.license") {
                useModule("com.jaredsburrows:gradle-license-plugin:0.9.0")
            }
        }
    }
}

rootProject.name = "Paletti"
