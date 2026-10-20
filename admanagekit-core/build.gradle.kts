plugins {
    alias(libs.plugins.android.library)  // Changed to library
    alias(libs.plugins.dokka)
    id("maven-publish")
}

android {
    namespace = "com.i2hammad.admanagekit.core"
    compileSdk = 37

    defaultConfig {
        minSdk = 24

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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    publishing {
        // withSourcesJar keeps the -sources.jar the kotlin-android plugin used to add on its own
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {

    // AdUnitMappingTest is a pure JVM test: no Android types are exercised,
    // so JUnit is the only test dependency needed.
    testImplementation(libs.junit)
}


afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["release"])  // Now works correctly
                groupId = "com.github.i2hammad"
                artifactId = "ad-manage-kit-core"
                version = "4.4.8"
            }
        }
    }
}
