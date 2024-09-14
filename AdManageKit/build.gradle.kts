plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("maven-publish")

}

android {
    namespace = "com.i2hammad.admanagekit"
    compileSdk = 35

    defaultConfig {
        minSdk = 21

        consumerProguardFiles("consumer-rules.pro")
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
    buildFeatures {
        viewBinding = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.billing)
    implementation(libs.user.messaging.platform)
    implementation(libs.play.services.ads)
    implementation(libs.shimmer)
    implementation(libs.androidx.lifecycle.process)
//    implementation (libs.firebase.analytics)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)


}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["release"])
                groupId = "com.github.i2hammad"
                artifactId = "ad-manage-kit"
                version = "1.1.2"
            }


        }
    }
}






