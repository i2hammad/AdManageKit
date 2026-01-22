plugins {
    alias(libs.plugins.android.library)  // Changed to library
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.dokka)
    id("maven-publish")
}

android {
    namespace = "com.i2hammad.admanagekit.billing"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
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
    api(project(":admanagekit-core"))
    implementation(libs.androidx.constraintlayout) // Use 'api' instead of 'implementation' to expose it
    api(libs.material)

}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["release"])  // Now works correctly
                groupId = "com.github.i2hammad"
                artifactId = "ad-manage-kit-billing"
                version = "3.3.5"
            }
        }
    }
}
