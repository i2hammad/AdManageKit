plugins {
    alias(libs.plugins.android.library)  // Changed to library
    alias(libs.plugins.jetbrains.kotlin.android)
    id("maven-publish")
}

android {
    namespace = "com.i2hammad.admanagekit.billing"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
//        targetSdk = 35
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    api(libs.billing)
    api(libs.google.material)
    api(project(":admanagekit-core")) // Use 'api' instead of 'implementation' to expose it

}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["release"])  // Now works correctly
                groupId = "com.github.i2hammad"
                artifactId = "ad-manage-kit-billing"
                version = "1.1.8"
            }
        }
    }
}