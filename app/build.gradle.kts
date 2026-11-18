plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.lucy.cookienotes"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.lucy.cookienotes"
        minSdk = 23
        targetSdk = 35
        versionCode = (System.getenv("APP_VERSION_CODE") ?: "1").toInt()
        versionName = "1.0"
    }
    signingConfigs {
        getByName("debug") {
            // Keystore tetap, bukan dibuat ulang tiap build, supaya sertifikat tanda tangan sama
            // setiap rilis dan APK baru bisa menimpa yang lama tanpa perlu uninstall / hilang data.
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }
    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
