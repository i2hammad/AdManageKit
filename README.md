# AdManageKit
[![JitPack](https://jitpack.io/v/i2hammad/AdManageKit.svg)](https://jitpack.io/#i2hammad/AdManageKit)

AdManageKit is an Android library designed to simplify the integration and management of Google AdMob ads, Google Play Billing, and User Messaging Platform (UMP) consent. Version `v1.3.2` introduces advanced native ads caching, app open ads via `AppOpenManager`, and enhanced interstitial ad management via `AdManager`. The library includes a sample project with visual demonstrations of its features.

## Features

- **AdMob Ads Management**: Seamlessly integrate banner, interstitial, app open, and native ads (small, medium, large formats).
- **Native Ads Caching**: Cache native ads per ad unit ID with a 1-hour expiration and a `useCachedAd` option for optimized performance.
- **App Open Ads**: Manage app open ads with lifecycle-aware loading and display using `AppOpenManager`.
- **Interstitial Ads**: Flexible interstitial ad loading and display with time/count-based triggers and dialog support.
- **Firebase Auto-Log Tracking, tROAS**: Automatically track tROAS for all ad types via Firebase Analytics.
- **Billing Management (Separate Module)**: Handle in-app purchases and subscriptions using the Google Play Billing Library.
- **UMP Consent Management**: Manage user consent with Google's UMP for GDPR/CCPA compliance.
- **Sample Project**: A fully functional sample project demonstrating ad management, caching, billing, and consent handling.

## Screenshots

Below are screenshots showcasing key features of `AdManageKit`:

| NativeBannerSmall Ad | Interstitial Ad | App Open Ad | UMP Consent Form |
|----------------------|-----------------|-----------------|------------------|
| ![NativeBannerSmall ad displayed in app](docs/assets/native_ad_small_screenshot.png) | ![Interstitial ad with loading dialog](docs/assets/interstitial_ad_screenshot.png) | ![App open ad on app launch](docs/assets/app_open_ad_screenshot.png) | ![UMP consent form](docs/assets/ump_consent_screenshot.png) |

## Demo Video

Watch a short demo of `AdManageKit` in action, showcasing ad loading, caching, and billing:

<video width="100%" controls>
  <source src="docs/assets/demo_video.mp4" type="video/mp4">
  Your browser does not support the video tag.
</video>

[Watch on YouTube](https://youtu.be/VIDEO_ID) <!-- Replace with actual YouTube link -->

## Getting Started

### Installation

1. **Add the library to your project**:

   In your root `build.gradle`, add JitPack to the repositories:

   ```groovy
   dependencyResolutionManagement {
       repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
       repositories {
           mavenCentral()
           maven { url 'https://jitpack.io' }
       }
   }
   ```

   In your app's `build.gradle`, add the dependencies (Latest Version: `v1.3.2`):

   ```groovy
   implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:1.3.2'
   implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-billing:1.3.2'
   ```

2. **Sync your project** with Gradle.

### Usage

#### Initializing the Library

Initialize `AdsConsentManager` in your app's first activity to handle UMP consent:

```java
public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AdsConsentManager adsConsentManager = AdsConsentManager.getInstance(this);
        adsConsentManager.requestUMP(this, true, "TEST_DEVICE_ID", false, new UMPResultListener() {
            @Override
            public void onCheckUMPSuccess(boolean isConsentGiven) {
                if (isConsentGiven && adsConsentManager.canRequestAds()) {
                    // Initialize and load ads
                }
            }
        });
    }
}
```

#### Set Up Purchase Provider Globally
In your `Application` class, configure the billing provider and initialize `AppOpenManager`:

```kotlin
import com.i2hammad.admanagekit.core.BillingConfig
import com.i2hammad.admanagekit.billing.BillingPurchaseProvider
import com.i2hammad.admanagekit.admob.AppOpenManager

class MyApp : Application() {
    private lateinit var appOpenManager: AppOpenManager

    override fun onCreate() {
        super.onCreate()
        BillingConfig.setPurchaseProvider(BillingPurchaseProvider())
        appOpenManager = AppOpenManager(this, "ca-app-pub-3940256099942544/9257395921")
    }
}
```

#### Managing AdMob Ads

##### Banner Ads
Add a banner ad to your layout:

```xml
<com.i2hammad.admanagekit.admob.BannerAdView
    android:id="@+id/bannerAdView"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

Load a banner ad:

```kotlin
bannerAdView.loadBanner(this, "ca-app-pub-3940256099942544/9214589741")
// For collapsible banner ad
bannerAdView.loadCollapsibleBanner(this, "ca-app-pub-3940256099942544/2014213617", true)
```

##### Native Ads with Caching
The library supports caching for `NativeBannerSmall`, `NativeBannerMedium`, and `NativeLarge` ads, with per-ad-unit caching and a 1-hour expiration.

###### NativeBannerSmall
Add to your layout:

```xml
<com.i2hammad.admanagekit.admob.NativeBannerSmall
    android:id="@+id/nativeBannerSmall"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

Load the ad:

```kotlin
nativeBannerSmall.loadNativeBannerAd(this, "ca-app-pub-3940256099942544/2247696110")
// Load cached ad (if available and not expired)
nativeBannerSmall.loadNativeBannerAd(activity, "your-ad-unit-id", useCachedAd = true)
// With callback
nativeBannerSmall.loadNativeBannerAd(activity, "your-ad-unit-id", useCachedAd = false, object : AdLoadCallback {
    override fun onAdLoaded() { Log.d("Ad", "Small Ad loaded") }
    override fun onFailedToLoad(adError: AdError) { Log.d("Ad", "Small Ad failed: ${adError.message}") }
    override fun onAdImpression() { Log.d("Ad", "Small Ad impression") }
    override fun onAdClicked() { Log.d("Ad", "Small Ad clicked") }
    override fun onAdClosed() { Log.d("Ad", "Small Ad closed") }
    override fun onAdOpened() { Log.d("Ad", "Small Ad opened") }
})
```

###### NativeBannerMedium
Add to your layout:

```xml
<com.i2hammad.admanagekit.admob.NativeBannerMedium
    android:id="@+id/nativeBannerMedium"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

Load the ad:

```kotlin
nativeBannerMedium.loadNativeBannerAd(this, "ca-app-pub-3940256099942544/2247696110")
// Load cached ad
nativeBannerMedium.loadNativeBannerAd(activity, "your-ad-unit-id", useCachedAd = true)
// With callback
nativeBannerMedium.loadNativeBannerAd(activity, "your-ad-unit-id", useCachedAd = false, object : AdLoadCallback {
    override fun onAdLoaded() { Log.d("Ad", "Medium Ad loaded") }
    override fun onFailedToLoad(adError: AdError) { Log.d("Ad", "Medium Ad failed: ${adError.message}") }
    override fun onAdImpression() { Log.d("Ad", "Medium Ad impression") }
    override fun onAdClicked() { Log.d("Ad", "Medium Ad clicked") }
    override fun onAdClosed() { Log.d("Ad", "Medium Ad closed") }
    override fun onAdOpened() { Log.d("Ad", "Medium Ad opened") }
})
```

###### NativeLarge
Add to your layout:

```xml
<com.i2hammad.admanagekit.admob.NativeLarge
    android:id="@+id/nativeLarge"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

Load the ad:

```kotlin
nativeLarge.loadNativeAds(this, "ca-app-pub-3940256099942544/2247696110")
// Load cached ad
nativeLarge.loadNativeAds(activity, "your-ad-unit-id", useCachedAd = true)
// With callback
nativeLarge.loadNativeAds(activity, "your-ad-unit-id", useCachedAd = false, object : AdLoadCallback {
    override fun onAdLoaded() { Log.d("Ad", "Large Ad loaded") }
    override fun onFailedToLoad(adError: AdError) { Log.d("Ad", "Large Ad failed: ${adError.message}") }
    override fun onAdImpression() { Log.d("Ad", "Large Ad impression") }
    override fun onAdClicked() { Log.d("Ad", "Large Ad clicked") }
    override fun onAdClosed() { Log.d("Ad", "Large Ad closed") }
    override fun onAdOpened() { Log.d("Ad", "Large Ad opened") }
})
```

###### Managing Native Ads Caching
Control caching via `NativeAdManager`:

```kotlin
// Enable caching (default)
NativeAdManager.enableCachingNativeAds = true
// Disable caching
NativeAdManager.enableCachingNativeAds = false
// Clear cache for a specific ad unit
NativeAdManager.clearCachedAd("your-ad-unit-id")
// Clear all cached ads
NativeAdManager.clearAllCachedAds()
```

For detailed documentation, see the [Native Ads Caching Wiki](docs/native-ads-caching.md).

##### Interstitial Ads
Load an interstitial ad (automatically cached for later use):

```kotlin
AdManager.getInstance().loadInterstitialAd(this, "ca-app-pub-3940256099942544/1033173712")
```

Display the interstitial ad immediately:

```kotlin
AdManager.getInstance().forceShowInterstitial(this, object : AdManagerCallback() {
    override fun onNextAction() {
        val intent = Intent(this@InterstitialActivity, MainActivity::class.java)
        startActivity(intent)
    }
})
```

Display with a loading dialog:

```kotlin
AdManager.getInstance().forceShowInterstitialWithDialog(this, object : AdManagerCallback() {
    override fun onNextAction() {
        val intent = Intent(this@InterstitialActivity, MainActivity::class.java)
        startActivity(intent)
    }
})
```

Display based on time interval (e.g., every 15 seconds):

```kotlin
AdManager.getInstance().showInterstitialAdByTime(this, object : AdManagerCallback() {
    override fun onNextAction() {
        val intent = Intent(this@InterstitialActivity, MainActivity::class.java)
        startActivity(intent)
    }
})
```

Display based on ad display count (e.g., up to 3 times):

```kotlin
AdManager.getInstance().showInterstitialAdByCount(this, object : AdManagerCallback() {
    override fun onNextAction() {
        val intent = Intent(this@InterstitialActivity, MainActivity::class.java)
        startActivity(intent)
    }, maxDisplayCount = 3)
```

For detailed documentation, see the [Interstitial Ads Wiki](docs/interstitial-ads.md).

##### App Open Ads
Initialize `AppOpenManager` in your `Application` class (see above). Exclude specific activities from showing app open ads:

```kotlin
appOpenManager.disableAppOpenWithActivity(MainActivity::class.java)
```

Force show an app open ad:

```kotlin
appOpenManager.forceShowAdIfAvailable(activity, object : AdManagerCallback() {
    override fun onNextAction() {
        Log.d("AppOpenAd", "Ad dismissed or failed, proceed with next action")
    }
    override fun onAdLoaded() {
        Log.d("AppOpenAd", "Ad displayed successfully")
    }
})
```

Skip the next app open ad:

```kotlin
appOpenManager.skipNextAd()
```

For detailed documentation, see the [App Open Ads Wiki](docs/app-open-ads.md).

#### Handling In-App Purchases

1. **Initialize the Billing Client**:

```java
AppPurchase.getInstance().initBilling(getApplication(), Arrays.asList(new PurchaseItem("your_product_id", AppPurchase.TYPE_IAP.PURCHASE)));
```

2. **Start a Purchase Flow**:

```java
AppPurchase.getInstance().purchase(activity, "your_product_id");
```

3. **Handle Purchase Results**:

```java
AppPurchase.getInstance().setPurchaseListener(new PurchaseListener() {
    @Override
    public void onProductPurchased(String orderId, String originalJson) {
        // Handle successful purchase
    }
    @Override
    public void displayErrorMessage(String errorMessage) {
        // Handle error
    }
    @Override
    public void onUserCancelBilling() {
        // Handle cancellation
    }
});
```

4. **Consume Purchases** (if consumable):

```java
AppPurchase.getInstance().consumePurchase("your_product_id");
```

#### User Messaging Platform (UMP) Consent

Request user consent:

```java
AdsConsentManager.getInstance(this).requestUMP(this, true, "TEST_DEVICE_ID", false, new UMPResultListener() {
    @Override
    public void onCheckUMPSuccess(boolean isConsentGiven) {
        if (isConsentGiven && adsConsentManager.canRequestAds()) {
            // Load ads
        }
    }
});
```

### Sample Project

The sample project in the `app` directory demonstrates ad management (banner, interstitial, app open, native with caching), billing, and UMP consent. To run it:

1. Clone the repository:

   ```bash
   git clone https://github.com/i2hammad/AdManageKit.git
   ```

2. Open in Android Studio.

3. Replace placeholder AdMob IDs and configure in-app purchases in the Google Play Console.

4. Run on a device or emulator.

### Contributing

Contributions are welcome! To contribute:

1. Fork the repository.
2. Create a branch (`git checkout -b feature/YourFeature`).
3. Commit changes (`git commit -m 'Add YourFeature'`).
4. Push to the branch (`git push origin feature/YourFeature`).
5. Open a Pull Request.

### License

Licensed under the MIT License. See the [LICENSE](LICENSE) file.

### Support

Support `AdManageKit` development:  
[Buy me a coffee](https://buymeacoffee.com/i2hammad)

For issues or questions, open an issue on GitHub or email [hammadmughal0001@gmail.com](mailto:hammadmughal0001@gmail.com).