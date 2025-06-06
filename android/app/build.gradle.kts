plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    kotlin("kapt")
    kotlin("plugin.serialization") version "2.1.20"
    id("com.jaredsburrows.license") version "0.9.8"
    id("com.github.ben-manes.versions") version "0.52.0"
}

android {
    namespace = "app.paletti.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "app.paletti.android"
        minSdk = 29
        targetSdk = 34
        versionCode = 2
        versionName = "v2025.06"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        externalNativeBuild {
            cmake {
                arguments += listOf("-DLEPT_SRC_DIR=")
            }
        }
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }
    buildFeatures {
        dataBinding = true
        buildConfig = true
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

tasks.named("dependencyUpdates", com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask::class).configure {
    val immaturityLevels = listOf("rc", "cr", "m", "beta", "alpha", "preview")
    val immaturityRegexes = immaturityLevels.map { ".*[.\\-]$it[.\\-\\d]*".toRegex(RegexOption.IGNORE_CASE) }
    fun immaturityLevel(version: String): Int = immaturityRegexes.indexOfLast { version.matches(it) }
    rejectVersionIf { immaturityLevel(candidate.version) > immaturityLevel(currentVersion) }
}

dependencies {
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.work:work-runtime-ktx:2.10.1")
    implementation("androidx.fragment:fragment-ktx:1.8.8")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("com.google.android.material:material:1.12.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")

    implementation("org.kodein.di:kodein-di-jvm:7.26.1")
    implementation("org.kodein.di:kodein-di-conf:7.26.1")
}

kapt {
    correctErrorTypes = true
}
