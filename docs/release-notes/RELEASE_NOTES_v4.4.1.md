# Release Notes — v4.4.1

**Release Date:** 2026-07-29

## Overview

v4.4.1 is a patch release with one dependency update and a set of documentation-delivery repairs. **No AdManageKit API changed** — no signature was added, removed, or altered.

The dependency update moves the **Google Mobile Ads Next-Gen SDK from 1.2.1 to 1.3.0**. Since the SDK is an `api` dependency, this is what your app resolves transitively.

The rest of the release fixes everything *around* the library — the API documentation site and the MCP documentation server that AI coding assistants read. Three delivery channels had silently gone stale, each in a way that made the library look older than it is:

- **The API docs site** had not regenerated since the Dokka 2.x upgrade. Every release since then, including 4.4.0, failed its documentation workflow, so [the published KDoc](https://i2hammad.github.io/AdManageKit/) never gained any 4.x types.
- **The MCP server on npm** was stuck at 1.0.0 (published 2026-01-26). Assistants consuming it were being told about a pre-Next-Gen-SDK, pre-Billing-9 library.
- **The MCP server's own tool schemas** hardcoded which versions and classes existed, so even a fresh publish would have rejected `4.4.0` as an unknown version and reported February 2026 as the latest release.

All modules are bumped to **4.4.1**. The MCP server is published separately as **1.2.0**.

---

## Changed

### Google Mobile Ads Next-Gen SDK 1.2.1 → 1.3.0

`gradle/libs.versions.toml` moves `adsMobileSdk` to **1.3.0**. AdManageKit exposes the SDK as an `api` dependency, so apps on 4.4.1 resolve 1.3.0 transitively unless they pin it themselves.

No AdManageKit code needed to change for this — the library was already on Next-Gen SDK patterns throughout (explicit `MobileAds.initialize()`, main-thread marshalling of background callbacks, Next-Gen `AdKit*` type aliases). `assembleDebug` and the full unit test suite (**168 tests, 0 failures**, forced re-run) pass against 1.3.0.

Consult [Google's Next-Gen SDK release notes](https://developers.google.com/admob/android/migration) for what 1.3.0 changes on the SDK side. If your app references the SDK's own types directly (rather than only AdManageKit's `AdKitError` / `AdKitLoadError` / `AdKitValue` aliases), review those call sites — but nothing in AdManageKit's surface requires action.

---

## Fixed

### API documentation generation (Dokka)

The Deploy API Documentation workflow failed on every release since the Dokka 2.x upgrade. On an Android module, Dokka registers a source set per variant (`debug`, `release`, plus test/androidTest/testFixtures) *and* a generic `main`. Since `main`, `debug` and `release` all cover `src/main`, this tripped Dokka's duplicate-source-root check:

```
Pre-generation validity check failed: Source sets 'androidJvm' and 'release'
have the common source roots: .../src/main/kotlin, .../src/main/java
```

All five modules were affected; CI only reported two because the build stopped early. Dokka is now configured to document the **release variant only** — matching what each module actually publishes via `components["release"]` — and to suppress the rest. That also keeps test and androidTest sources out of the public API docs, which they were never meant to be eligible for.

The configuration lives in the root build rather than being repeated across five modules, so any new module is covered automatically.

`dokkaGeneratePublicationHtml` now emits **1244 pages** across all five modules, including the 4.4.0 billing types (`BillingPeriod`, `OneTimeOfferInfo`, `ProductDetailsListener`, `InAppMessageListener`) and the new `AppPurchase` members. No test-source pages are emitted.

### MCP server npm publishing

The `publish-npm` job ran checkout, setup-node, download-artifact and `npm publish` with **no dependency install**. Because `package.json` declares `"prepublishOnly": "npm run build"`, `npm publish` re-ran `tsc` in a job with no `node_modules` and died with `TS2307 Cannot find module` on every import. The build job's uploaded artifact could not help — `prepublishOnly` rebuilds from source regardless.

- `npm ci` added to `publish-npm` so `prepublishOnly` can build. The redundant `download-artifact` step is dropped; the build job stays as a verification gate.
- **`zod` declared as a direct dependency.** It is imported by `tools/code-generation.ts` and `tools/documentation.ts` but appeared in neither `dependencies` nor `devDependencies`, resolving only because npm hoisted it out of `@modelcontextprotocol/sdk`. An SDK release nesting zod differently would have broken the build with no source change. The range mirrors the SDK's own (`^3.25 || ^4.0`) so npm can dedupe.

### MCP tool schemas drifting from bundled docs

Publishing current documentation was not enough — the code gating access to it was stale:

- `get_release_notes` accepted only 17 versions while **38** release-notes files were bundled. Every version from 3.4.0 onward, including 4.4.0, was rejected at schema validation, and `"latest"` was hardcoded to 3.3.8.
- `get_migration_guide` omitted **4.2.0**, the Next-Gen SDK migration — the one most consumers need.
- `get_api_reference` listed 16 classes against 34 documented sections, hiding `OfferInfo`, `BillingPeriod`, `PurchaseResult` and others.
- `TOPIC_MAP` had no entry for the `Subscription-Offers` wiki page.

These lists are now **derived at startup from the bundled content**, so shipping a doc is sufficient to make it reachable: `discoverReleaseVersions()` scans `docs/release-notes/` (sorted newest-first by numeric version, with `"latest"` resolving to the newest file actually present), `discoverMigrationVersions()` scans the README for `### Migrating to X`, and `discoverApiClassNames()` reuses the `API_REFERENCE.md` section parser. `list_documentation` renders from those and appends any `TOPIC_MAP` key the hand-written grouping missed, so a registered topic can never be undiscoverable. The hardcoded `RELEASE_VERSIONS`, `MIGRATION_VERSIONS` and `API_CLASS_NAMES` constants are gone, leaving nothing to drift.

`z.enum()` requires a non-empty tuple, so discovery degrades to a plain string when it finds nothing, rather than throwing at registration and taking the server down.

### MCP code-generation templates emitting obsolete APIs

The generator templates are hardcoded TypeScript, so unlike the bundled docs they gained nothing from republishing. They emitted code against APIs two major versions old — including one case that does not work at all.

- **`generate_config` emitted no `MobileAds.initialize()` call.** Since v4.2.0 AdManageKit runs on the Next-Gen SDK, which removed the legacy SDK's silent lazy-init, so the generated `Application` class produced an app that never loads ads. Both the Kotlin and Java templates now initialize explicitly, off the main thread (`initialize()` blocks and can ANR), read the application id from the manifest, and construct `AppOpenManager` only after initialization returns — otherwise its lifecycle observer can fire a load the SDK rejects as "not initialized". This mirrors the sample app's `MyApplication.kt`.
- **Billing `setup`** now emits `setDebugMode(BuildConfig.DEBUG)` (a library AAR always has `BuildConfig.DEBUG = false`, so the host app must inject its build state), obfuscated account ids, and the `ProductDetailsListener` for diagnosing an empty paywall.
- **Billing `subscribe`** showed only `subscribe(activity, id)`, which resolves the offer itself and on a multi-offer product can charge for a plan the user did not pick. It now shows the offer-explicit form and explains why.
- **New `offers` scenario** — multi-offer paywalls: enumerating offers, buying a specific one, savings badges, per-month normalization, trial eligibility, one-time product offers.
- **New `account_hold` scenario** — client-side hold detection, Play's payment recovery flow, pending plan changes.
- `subscription_management` and `subscribe` now handle `SubscriptionState.ON_HOLD`, which 4.4.0 made reachable.
- The **Java template** was a stub covering only `setup` and `purchase`; it now mirrors the Kotlin coverage.
- **`FRESH_WITH_CACHE_FALLBACK` was missing from every loading-strategy enum**, and native ads were restricted to two of the four values. All four fields now use a shared `AD_LOADING_STRATEGIES` constant mirroring `AdLoadingStrategy.kt`.

---

## Verification

- `assembleDebug` succeeds and `testDebugUnitTest --rerun-tasks` passes **168 tests with 0 failures** against Next-Gen SDK 1.3.0 (forced re-run, not an up-to-date cache hit).
- `dokkaGeneratePublicationHtml` succeeds across all five modules (1244 pages, no test-source pages).
- `npm publish --dry-run` exercises the previously failing `prepublishOnly` path: builds clean, produces a 103-file tarball carrying 58 docs and 19 wiki pages.
- A live MCP stdio session against real docs resolves 38 release versions (4.4.0 newest, `"latest"` → v4.4.0), 5 migration guides including 4.2.0, 33 API classes including `OfferInfo` and `BillingPeriod`, and 24 topics including `subscription-offers`.
- Against a docs-less root, the server still starts, schemas degrade with no enum, and handlers return their normal "not found" messages.
- Every symbol emitted by the refreshed templates was checked against library source: 25 `AppPurchase` methods, 15 `OfferInfo`/`OneTimeOfferInfo` properties, `SubscriptionState.ON_HOLD`, `BillingPeriod.formatOf`, and the `InAppMessageListener` / `ProductDetailsListener` method names all resolve. All nine billing scenarios generate in both languages.

---

## Upgrading

Drop-in from 4.4.0 — no AdManageKit API changed, so no source edits are required.

The one thing to be aware of is the transitive **Next-Gen SDK bump to 1.3.0**. If your app pins `com.google.android.libraries.ads.mobile.sdk` itself, either raise your pin to 1.3.0 or expect Gradle's conflict resolution to pick the higher version anyway. If your app calls the SDK's own types directly — rather than only AdManageKit's `AdKitError` / `AdKitLoadError` / `AdKitValue` aliases — give those call sites a compile pass.

```groovy
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v4.4.1'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-billing:v4.4.1'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-core:v4.4.1'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-compose:v4.4.1'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-yandex:v4.4.1'
```

For the MCP server, `npx @i2hammad/admanagekit-mcp@latest` now resolves to 1.2.0.
