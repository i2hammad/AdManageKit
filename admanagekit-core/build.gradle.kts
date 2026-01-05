plugins {
    alias(libs.plugins.android.library)  // Changed to library
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.dokka)
    id("maven-publish")
}

android {
    namespace = "com.i2hammad.admanagekit.core"
    compileSdk = 36

    defaultConfig {
        minSdk = 24

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


afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["release"])  // Now works correctly
                groupId = "com.github.i2hammad"
                artifactId = "ad-manage-kit-core-nextgen"
                version = "4.1.1"
            }
        }
    }
}
