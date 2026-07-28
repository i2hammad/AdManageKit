plugins {
    alias(libs.plugins.android.library)  // Changed to library
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.dokka)
    id("maven-publish")
}

android {
    namespace = "com.i2hammad.admanagekit.billing"
    compileSdk = 37

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

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    api(libs.billing)
    api(project(":admanagekit-core"))
    implementation(libs.androidx.constraintlayout) // Use 'api' instead of 'implementation' to expose it
    api(libs.material)

    // These are pure JVM tests — the tested PurchaseResult / OfferInfo /
    // BillingPeriod paths never touch android.* APIs.
    testImplementation(libs.junit)
    // The billing library parses ProductDetails from JSON. The android.jar stub
    // used by unit tests returns null from every org.json method, so tests that
    // build real ProductDetails fixtures need a working implementation ahead of
    // the stub on the classpath.
    testImplementation(libs.json)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["release"])  // Now works correctly
                groupId = "com.github.i2hammad"
                artifactId = "ad-manage-kit-billing"
                version = "4.4.0"
            }
        }
    }
}
