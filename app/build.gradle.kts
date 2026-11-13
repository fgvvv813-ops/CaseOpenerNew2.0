plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.caseopener.newtwo"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.caseopener.newtwo"
        minSdk = 26
        targetSdk = 35
        versionCode = providers.gradleProperty("appVersionCode").get().toInt()
        versionName = providers.gradleProperty("appVersionName").get()
    }

    // Stable test signing so subsequent Android updates retain app data.
    // This key is FOR DEVELOPMENT ONLY; not a production release certificate.
    signingConfigs {
        create("stableDev") {
            storeFile = rootProject.file("signing/caseopener-test.jks")
            storePassword = "caseopener-test"
            keyAlias = "caseopener-test"
            keyPassword = "caseopener-test"
        }
    }
    buildTypes {
        getByName("debug") { signingConfig = signingConfigs.getByName("stableDev") }
        getByName("release") { isMinifyEnabled = false }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
}
