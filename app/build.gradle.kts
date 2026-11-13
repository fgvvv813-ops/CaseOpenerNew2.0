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
        // Nonsecret Firebase Android client configuration, injected at APK build time.
        // Private Firebase SERVICE ACCOUNT credentials must NEVER be put in the APK.
        buildConfigField("String", "FIREBASE_PROJECT_ID",
            "\\"" + providers.environmentVariable("FIREBASE_PROJECT_ID").orElse("").get() + "\\"")
        buildConfigField("String", "FIREBASE_SENDER_ID",
            "\\"" + providers.environmentVariable("FIREBASE_SENDER_ID").orElse("").get() + "\\"")
        buildConfigField("String", "FIREBASE_APPLICATION_ID",
            "\\"" + providers.environmentVariable("FIREBASE_APPLICATION_ID").orElse("").get() + "\\"")
        buildConfigField("String", "FIREBASE_API_KEY",
            "\\"" + providers.environmentVariable("FIREBASE_API_KEY").orElse("").get() + "\\"")

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
    // Firebase Cloud Messaging: supports terminated/background Android app.
    implementation("com.google.firebase:firebase-messaging:25.1.3")
}
