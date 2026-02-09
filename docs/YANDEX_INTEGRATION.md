# Yandex Ads Integration

## Overview

The `admanagekit-yandex` module provides Yandex Ads SDK providers for the multi-provider waterfall system. It supports all five ad types: interstitial, banner, native, app open, and rewarded.

## Installation

### 1. Add Dependency

```groovy
dependencies {
    implementation "com.github.i2hammad.AdManageKit:ad-manage-kit:VERSION"
    implementation "com.github.i2hammad.AdManageKit:ad-manage-kit-core:VERSION"
    implementation "com.github.i2hammad.AdManageKit:ad-manage-kit-yandex:VERSION"
}
```

The Yandex Mobile Ads SDK (`com.yandex.android:mobileads`) is included transitively.

### 2. Initialize

In your `Application.onCreate()`:

```kotlin
import com.i2hammad.admanagekit.yandex.YandexProviderRegistration
import com.i2hammad.admanagekit.admob.provider.AdMobProviderRegistration
import com.i2hammad.admanagekit.core.ad.AdProviderConfig
import com.i2hammad.admanagekit.core.ad.AdUnitMapping

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // ... existing AdManageKitConfig setup ...

        // Initialize Yandex SDK
        YandexProviderRegistration.initialize(this)

        // Create providers
        val admob = AdMobProviderRegistration.create()
        val yandex = YandexProviderRegistration.create()

        // Map your AdMob IDs to Yandex equivalents
        AdUnitMapping.register("ca-app-pub-xxx/your-interstitial", mapOf(
            "yandex" to "R-M-XXXXXX-Y"
        ))
        AdUnitMapping.register("ca-app-pub-xxx/your-native", mapOf(
            "yandex" to "R-M-XXXXXX-Y"
        ))
        // ... register all ad units ...

        // Set provider chains (AdMob primary, Yandex fallback)
        AdProviderConfig.setInterstitialChain(listOf(admob.interstitialProvider, yandex.interstitialProvider))
        AdProviderConfig.setBannerChain(listOf(admob.bannerProvider, yandex.bannerProvider))
        AdProviderConfig.setNativeChain(listOf(admob.nativeProvider, yandex.nativeProvider))
        AdProviderConfig.setAppOpenChain(listOf(admob.appOpenProvider, yandex.appOpenProvider))
        AdProviderConfig.setRewardedChain(listOf(admob.rewardedProvider, yandex.rewardedProvider))
    }
}
```

### 3. Done

No other code changes needed. All existing ad loading calls will automatically fall through to Yandex when AdMob fails.

## Yandex Provider Classes

| Ad Type | Provider Class | Description |
|---------|---------------|-------------|
| Interstitial | `YandexInterstitialProvider` | Full-screen interstitial ads |
| Banner | `YandexBannerProvider` | Adaptive banner ads |
| Native | `YandexNativeProvider` | Size-aware native ads (small/medium/large) |
| App Open | `YandexAppOpenProvider` | App open ads on resume |
| Rewarded | `YandexRewardedProvider` | Rewarded video ads with reward callbacks |

## Native Ad Sizes

`YandexNativeProvider` renders different layouts based on the `NativeAdSize` hint from the consumer view:

### SMALL (NativeBannerSmall)

Compact single-row layout:
```
[Icon] [Title + Domain] [CTA Button] [i]
```
- Icon (40dp) + title (single line) + CTA button
- Domain text below title
- Feedback icon inline

### MEDIUM (NativeBannerMedium)

Vertical layout without media:
```
[Icon] [Title        ]
       [Domain       ]
[Body text...        ]
[    CTA Button      ]
[i] [Warning] [Sponsored]
```
- Header row: icon (48dp) + title + domain
- Body text (up to 3 lines)
- Full-width CTA button
- Required Yandex views: feedback, warning, sponsored

### LARGE (NativeLarge / NativeTemplateView)

Full `NativeBannerView` template from Yandex SDK:
- Automatically renders all ad assets including media
- Uses Yandex's built-in template layout
- No custom view binding needed

## Ad Unit IDs

### Test Ad Unit IDs (Yandex Demo)

| Ad Type | Test ID |
|---------|---------|
| Interstitial | `demo-interstitial-yandex` |
| Banner | `demo-banner-yandex` |
| Native | `demo-native-content-yandex` |
| App Open | `demo-appopenad-yandex` |
| Rewarded | `demo-rewarded-yandex` |

### Production Setup

Get your ad unit IDs from the [Yandex Advertising Network](https://partner.yandex.com/) dashboard. Register them in `AdUnitMapping` keyed by your existing AdMob IDs:

```kotlin
AdUnitMapping.register("ca-app-pub-xxx/your-real-interstitial", mapOf(
    "yandex" to "R-M-XXXXXX-1"
))
```

## Revenue Tracking

All Yandex providers automatically report paid events via the waterfall callback system. Revenue data from Yandex's `ImpressionData` is converted to `AdKitAdValue` and logged to Firebase Analytics as `ad_paid_event`, matching the existing AdMob revenue tracking format.

## Yandex SDK Requirements

- **Min SDK**: 21+
- **Yandex SDK Version**: 7.18.1+ (included transitively)
- **Kotlin**: 1.7.10+
- **Hardware acceleration**: Required for video ads

## Troubleshooting

### "No ad unit ID for Yandex Ads, skipping"

Your ad unit ID is not registered in `AdUnitMapping`. Register the Yandex equivalent:

```kotlin
AdUnitMapping.register("your-admob-ad-unit-id", mapOf(
    "yandex" to "your-yandex-ad-unit-id"
))
```

### "Failed to parse server's response"

This is a Yandex server-side issue with demo/test ad units. Ensure you have AdMob as a fallback in your chain:

```kotlin
AdProviderConfig.setInterstitialChain(listOf(
    yandex.interstitialProvider,
    admob.interstitialProvider  // fallback
))
```

### Native ads not showing (SMALL/MEDIUM)

Yandex SDK requires `domain`, `feedback`, `warning`, and `sponsored` views to be bound. If any are missing, `bindNativeAd()` throws and the ad fails to load. This is handled internally by `YandexNativeProvider` -- ensure you're using the latest version.

### "Resource for required view X is not present"

This crash occurs when Yandex's `NativeAdViewBinder` is missing required views. Update to the latest `admanagekit-yandex` version which includes all required views.
