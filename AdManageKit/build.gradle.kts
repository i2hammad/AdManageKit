plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.dokka)
    id("maven-publish")
}

android {
    namespace = "com.i2hammad.admanagekit"
    compileSdk = 36

    defaultConfig {
        minSdk = 23

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
    api(libs.material)
    api(libs.user.messaging.platform)
    api(libs.play.services.ads)
    api(libs.shimmer)
    implementation(libs.androidx.lifecycle.process)
    api(platform(libs.firebase.bom))
    api(libs.firebase.analytics)
    api(project(":admanagekit-core"))
    implementation(libs.androidx.work.runtime)


}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["release"])
                groupId = "com.github.i2hammad"
                artifactId = "ad-manage-kit"
                version = "2.9.0"
            }


        }
    }
}





