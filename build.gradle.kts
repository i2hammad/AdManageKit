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