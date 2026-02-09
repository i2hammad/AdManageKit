# Repository Guidelines

## Project Structure & Module Organization
- `AdManageKit/` holds the primary AdMob + UMP SDK integrations (view-based UI, caching, loaders).
- `admanagekit-core/` contains shared config, models, and low-level helpers used by all other modules.
- `admanagekit-billing/` adds Google Play Billing flows and purchase helpers.
- `admanagekit-compose/` wraps AdManageKit features for Jetpack Compose.
- `app/` is the sample app; use it to manual-test features before release.
- `docs/` and `wiki/` contain public-facing guides; `build/dokka/htmlMultiModule/` is generated API docs.

## Build, Test, and Development Commands
- `./gradlew assembleDebug` builds the sample app for quick validation.
- `./gradlew :AdManageKit:assembleRelease` (or module-specific `:admanagekit-*:assembleRelease`) produces AARs under `*/build/outputs/aar/`.
- `./gradlew buildRelease` runs all release builds plus Dokka multi-module docs in one step.
- `./gradlew dokkaGenerateHtml` regenerates API docs locally (output in `build/dokka/html`).
- `./gradlew test` runs JVM unit tests; `./gradlew connectedAndroidTest` runs device/emulator tests per module.

## Coding Style & Naming Conventions
- Kotlin-first codebase (Kotlin 2.1, Java/Kotlin target 17 for libraries); use 4-space indentation and idiomatic null-safety.
- Packages stay under `com.i2hammad.admanagekit.*`; avoid introducing new roots.
- Class/object names in PascalCase; functions/fields in camelCase; XML resources in `snake_case`.
- Compose APIs should prefer stateless functions with `@Composable` previews named `<Component>Preview`.
- Keep public API changes documented in `docs/` and ensure module `api` vs `implementation` visibility is deliberate.

## Testing Guidelines
- Place unit tests in `module/src/test/java` and instrumented tests in `module/src/androidTest/java`; name files `*Test` (unit) and `*InstrumentedTest` where UI/device-only.
- Use fake ad/billing clients or Google test ad unit IDs to keep runs deterministic and policy-safe.
- Cover new caching/strategy branches when adding ad-loading logic; add Compose UI tests for new composables where feasible.
- Run `./gradlew test` before opening a PR; add `connectedAndroidTest` results when device-dependent behavior is touched.

## Commit & Pull Request Guidelines
- Follow the existing style seen in history (`Docs: …`, `Release vX.Y.Z`, `Feature: …`); keep summaries under ~72 chars.
- Prefer topic branches (`feature/<scope>`, `fix/<issue>`) and rebase regularly.
- PRs should list modules touched, testing performed, and screenshots/GIFs for UI-visible changes (sample app screenshots are acceptable).
- Link issues or release notes when bumping versions; note any API or behavior changes in PR descriptions and `README`/`docs` as needed.

## Security & Configuration Tips
- Do not commit real AdMob IDs, signing keys, or `local.properties`; keep secrets in your local environment/CI variables.
- Use test device IDs and sample ad units while developing; switch to production units only in release builds.
- Regenerate `consumer-proguard-rules` carefully and keep ProGuard/R8 rules minimal to avoid stripping required ad/billing classes.
