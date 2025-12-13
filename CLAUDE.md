# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

AdManageKit is an Android library for simplifying Google AdMob ads, Google Play Billing, and User Messaging Platform (UMP) consent management. The project consists of multiple modules:

- **AdManageKit** (main module): Core ad management functionality
- **admanagekit-billing**: Google Play Billing integration 
- **admanagekit-core**: Shared core functionality and purchase provider interfaces
- **app**: Sample application demonstrating library usage

## Build System

This is a multi-module Android project using Gradle with Kotlin DSL. Key build commands:

```bash
# Build the entire project
./gradlew build

# Clean build
./gradlew clean build

# Build specific module
./gradlew :AdManageKit:build
./gradlew :admanagekit-billing:build

# Run tests
./gradlew test

# Assemble release
./gradlew assembleRelease
```

Version management is centralized in `gradle/libs.versions.toml` using Gradle version catalogs.

## Key Architecture Components

### Core Module (admanagekit-core)
- `BillingConfig`: Central configuration for purchase providers
- `AppPurchaseProvider`: Interface for purchase status checking
- `NoPurchaseProvider`: Default implementation (no purchases)

### Ad Management (AdManageKit)
- `AdManager`: Singleton for interstitial ad management with time/count-based display logic
- `AppOpenManager`: Lifecycle-aware app open ad management
- `NativeAdManager`: Caching system for native ads with 1-hour expiration
- `BannerAdView`: Custom view for banner ads
- Native ad views: `NativeBannerSmall`, `NativeBannerMedium`, `NativeLarge`

### Billing Module (admanagekit-billing)
- `AppPurchase`: Main billing client wrapper supporting Google Play Billing Library v8
- `BillingPurchaseProvider`: Implementation of purchase provider interface
- Purchase handling with acknowledgment and consumption support

### Key Design Patterns
- **Singleton Pattern**: AdManager, AppPurchase for single instances
- **Strategy Pattern**: Purchase provider interface allows different billing implementations
- **Observer Pattern**: Extensive use of callbacks for ad lifecycle events
- **Caching**: Native ads cached per ad unit ID with expiration

### Firebase Integration
All ad types automatically log Firebase Analytics events for tROAS tracking:
- `ad_impression`: When ads are shown
- `ad_paid_event`: Revenue tracking
- `ad_failed_to_load`: Error tracking

## Development Workflow

### Sample App Testing
The `app` module provides comprehensive examples for:
- Ad loading and display patterns
- Billing integration 
- UMP consent management
- Different ad formats and caching

### Version Management
- Stable: v1.3.2 (Google Play Billing Library < v8)
- Beta: v2.0.0-alpha01 (Google Play Billing Library v8)

### Release Process
Project uses JitPack for distribution. The `jitpack.yml` configures OpenJDK 17 and runs preparation scripts.

## Important Implementation Notes

### Purchase Status Integration
All ad components check purchase status via `BillingConfig.getPurchaseProvider().isPurchased()` to disable ads for purchased users.

### Ad Lifecycle Management
- InterstitialAd: Supports time-based intervals (default 15s) and count-based limits
- AppOpenAd: Lifecycle-aware with activity exclusion support
- Native Ads: Global caching can be enabled/disabled via `NativeAdManager.enableCachingNativeAds`

### Error Handling
Custom error codes for purchase-related ad blocking (error code 1001) to distinguish from standard AdMob errors.

## Testing and Development

### Ad Unit IDs
Sample app uses Google's test ad unit IDs. Replace with production IDs before release.

### Billing Testing  
Use Google Play Console's test tracks and test purchase items for billing integration testing.

### UMP Testing
UMP consent testing requires test device IDs configured in AdMob dashboard.