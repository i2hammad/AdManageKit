// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.dokka)
}

// Aggregate all module docs into root build/dokka/html/
dependencies {
    dokka(project(":admanagekit-core"))
    dokka(project(":AdManageKit"))
    dokka(project(":admanagekit-billing"))
    dokka(project(":admanagekit-compose"))
    dokka(project(":admanagekit-yandex"))
}

// On an Android module, Dokka registers a source set per variant (`debug`,
// `release`, the test/androidTest/testFixtures variants) *plus* a generic `main`
// — and `main`, `debug` and `release` all cover src/main. Dokka rejects that:
//
//   Pre-generation validity check failed: Source sets 'androidJvm' and 'release'
//   have the common source roots: .../src/main/kotlin, .../src/main/java
//
// ('androidJvm' is how Dokka displays the `main` source set.) Document the
// release variant only — it matches what each module publishes via
// components["release"] — and suppress the rest, which also keeps test and
// androidTest sources out of the public API docs.
//
// Applied here rather than in each module so a new module is covered automatically.
subprojects {
    plugins.withId("org.jetbrains.dokka") {
        extensions.configure<org.jetbrains.dokka.gradle.DokkaExtension> {
            dokkaSourceSets.configureEach {
                suppress.set(name != "release")
            }
        }
    }
}

// Generate documentation: ./gradlew dokkaGeneratePublicationHtml
// Output: build/dokka/html

// Build release with documentation: ./gradlew buildRelease
tasks.register("buildRelease") {
    group = "release"
    description = "Build all release artifacts and generate API documentation"

    dependsOn(
        ":AdManageKit:assembleRelease",
        ":admanagekit-billing:assembleRelease",
        ":admanagekit-core:assembleRelease",
        ":admanagekit-compose:assembleRelease",
        "dokkaGeneratePublicationHtml"
    )

    doLast {
        println("=".repeat(60))
        println("Release build complete!")
        println("=".repeat(60))
        println("AAR files: */build/outputs/aar/")
        println("API Docs:  build/dokka/html/index.html")
        println("=".repeat(60))
    }
}