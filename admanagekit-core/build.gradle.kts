plugins {
    alias(libs.plugins.android.library)  // Changed to library
    alias(libs.plugins.jetbrains.kotlin.android)

}

android {
    namespace = "com.i2hammad.admanagekit.core"
    compileSdk = 35

    defaultConfig {
        minSdk = 21

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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

dependencies {

}