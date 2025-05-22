# AdManageKit
[![](https://jitpack.io/v/i2hammad/AdManageKit.svg)](https://jitpack.io/#i2hammad/AdManageKit)

AdManageKit is an Android library designed to simplify the management of Google AdMob ads, billing
using the Google Play Billing Library, and User Messaging Platform (UMP) consent. This library
provides a streamlined approach to integrating ads and handling user consent for privacy compliance,
while also offering a sample project to demonstrate its usage.

## Features

- **AdMob Ads Management**: Easily integrate and manage AdMob ads in your Android applications,
  including banner, interstitial, and native ads.
- **Firebase auto log tracking event, tROAS**: This app can track tROAS,
  including banner, interstitial, and native ads.
- **Billing Management (Separate Module)**: Seamless integration with the Google Play Billing Library to handle in-app purchases and subscriptions.
- **UMP Consent Management**: Manage user consent using Google's User Messaging Platform (UMP) to
  comply with privacy regulations like GDPR and CCPA.
- **Sample Project**: A fully functional sample project to demonstrate how to use the library
  effectively in your own apps.

## Getting Started

### Installation

1. **Add the library to your project**:

   Add it in your root build.gradle at the end of repositories:

   ```groovy
  	dependencyResolutionManagement {
		repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
		repositories {
			mavenCentral()
			maven { url 'https://jitpack.io' }
		}
	}
   ```

   Add the following to your `build.gradle` file in the `dependencies` section: Latest Version [![](https://jitpack.io/v/i2hammad/AdManageKit.svg)](https://jitpack.io/#i2hammad/AdManageKit)
   ```groovy
   implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v1.3.1'
   implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-billing:v1.3.1'
   ```

2. **Sync your project** with Gradle files.

### Usage

#### Initializing the Library

The `AdsConsentManager` should be initialized in the first activity of your application to ensure
that the consent form is displayed to the user as required.

```java
public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize the AdsConsentManager
        AdsConsentManager adsConsentManager =   AdsConsentManager.getInstance(this);
        adsConsentManager.requestUMP(this, new UMPResultListener() {
            @Override
            public void onCheckUMPSuccess(boolean isConsentGiven) {
                if (isConsentGiven) {
                    // Initialize Ads here
                }else {
                    // not given
                }

                if (adsConsentManager.canRequestAds()) {
                    // moveToNext
                    // onNextActionCalled()
                }
            }
        });
    }
}
```

#### Managing AdMob Ads

For displaying banner ad include following code in xml

```xml

<com.i2hammad.admanagekit.admob.BannerAdView 
    android:id="@+id/bannerAdView"
    android:layout_height="wrap_content" 
    android:layout_width="match_parent" />
```

Use following code to load banner ad:

```kotlin
bannerAdView.loadBanner(this, "ca-app-pub-3940256099942544/9214589741")

// for Collapsible Banner Ad
bannerAdView.loadCollapsibleBanner(this, "ca-app-pub-3940256099942544/2014213617", true)
```

Similarly for NativeBannerMedium, NativeBannerSmall, NativeLarge

```xml

<com.i2hammad.admanagekit.admob.NativeBannerSmall 
    android:id="@+id/nativeBannerSmall"
    android:layout_width="match_parent" 
    android:layout_height="wrap_content">
</com.i2hammad.admanagekit.admob.NativeBannerSmall>
```

```kotlin
nativeBannerSmall.loadNativeBannerAd(this, "ca-app-pub-3940256099942544/2247696110")

// Load a cached ad (if available and not expired)
nativeBanner.loadNativeBannerAd(activity, "your-ad-unit-id", useCachedAd = true)

// or 

nativeBanner.loadNativeBannerAd(activity, "your-ad-unit-id", useCachedAd = false, object : AdLoadCallback {
    override fun onAdLoaded() { Log.d("Ad", "Ad loaded") }
    override fun onFailedToLoad(adError: LoadAdError) { Log.d("Ad", "Ad failed: ${adError.message}") }
    override fun onAdImpression() { Log.d("Ad", "Ad impression") }
    override fun onAdClicked() { Log.d("Ad", "Ad clicked") }
    override fun onAdClosed() { Log.d("Ad", "Ad closed") }
})
```

```xml

<com.i2hammad.admanagekit.admob.NativeBannerMedium 
    android:id="@+id/nativeBannerLarge"
    android:layout_height="wrap_content" 
    android:layout_width="match_parent">

</com.i2hammad.admanagekit.admob.NativeBannerMedium>
```

```kotlin
nativeBannerMedium.loadNativeBannerAd(this, "ca-app-pub-3940256099942544/2247696110")

// Load cached ad if available and not expired
nativeBannerMedium.loadNativeBannerAd(activity, "your-ad-unit-id", useCachedAd = true)
// or
nativeBannerMedium.loadNativeBannerAd(activity, "your-ad-unit-id", useCachedAd = false, object : AdLoadCallback {
    override fun onAdLoaded() { Log.d("Ad", "Medium Ad loaded") }
    override fun onFailedToLoad(adError: LoadAdError) { Log.d("Ad", "Medium Ad failed: ${adError.message}") }
    override fun onAdImpression() { Log.d("Ad", "Medium Ad impression") }
    override fun onAdClicked() { Log.d("Ad", "Medium Ad clicked") }
    override fun onAdClosed() { Log.d("Ad", "Medium Ad closed") }
    override fun onAdOpened() { Log.d("Ad", "Medium Ad opened") }
})

```

```xml
<com.i2hammad.admanagekit.admob.NativeLarge
    android:id="@+id/nativeBannerLarge"
    android:layout_width="match_parent" 
    android:layout_height="wrap_content">
</com.i2hammad.admanagekit.admob.NativeLarge>
```

```kotlin
nativeLarge.loadNativeAds(this, "ca-app-pub-3940256099942544/2247696110")

//or

// Load cached ad if available and not expired
nativeLarge.loadNativeAds(activity, "your-ad-unit-id", useCachedAd = true)

//or

nativeLarge.loadNativeAds(activity, "your-ad-unit-id", useCachedAd = false, object : AdLoadCallback {
    override fun onAdLoaded() { Log.d("Ad", "Large Ad loaded") }
    override fun onFailedToLoad(adError: LoadAdError) { Log.d("Ad", "Large Ad failed: ${adError.message}") }
    override fun onAdImpression() { Log.d("Ad", "Large Ad impression") }
    override fun onAdClicked() { Log.d("Ad", "Large Ad clicked") }
    override fun onAdClosed() { Log.d("Ad", "Large Ad closed") }
    override fun onAdOpened() { Log.d("Ad", "Large Ad opened") }
})
```

To load interstitial ads use following code.. it will cache ad for later use

```kotlin
AdManager.getInstance().loadInterstitialAd(this, "ca-app-pub-3940256099942544/1033173712")
```

for displaying interstitial ad use following code. onNextAction will be called always even when ad
is not loaded or user has purchased app. Auto reload next ad for later use

```kotlin
AdManager.getInstance().forceShowInterstitial(this, object : AdManagerCallback() {
    override fun onNextAction() {
        val nativeAdsIntent = Intent(this@InterstitialActivity, MainActivity::class.java)
        startActivity(nativeAdsIntent)
    }
})
```

## Usage

### Initializing the Library

#### **Set Up Purchase Provider Globally**
Breaking change if you are using old library version. In your main application class (`MyApp.kt`), set the **purchase provider** globally so that all ads automatically  the user's purchase status:

```kotlin
import com.i2hammad.admanagekit.core.BillingConfig
import com.i2hammad.admanagekit.billing.BillingPurchaseProvider

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Register the billing provider globally
        BillingConfig.setPurchaseProvider(BillingPurchaseProvider())
    }
}
```

#### Handling In-App Purchases

AdManageKit simplifies the process of handling in-app purchases and subscriptions using the Google
Play Billing Library. Follow these steps to set up in-app purchases:

1. **Configure your products** in the Google Play Console:
    - Create in-app products or subscriptions with unique product IDs.
    - Ensure that your app is linked to a payment account.

2. **Initialize the Billing Client**:

```java
    AppPurchase.getInstance().initBilling(getApplication(),Arrays.asList(new PurchaseItem("your_product_id", AppPurchase.TYPE_IAP.PURCHASE)));
```

3. **Start a Purchase Flow**:

To initiate a purchase flow, use the `AppPurchase` class:

```java
    AppPurchase.getInstance().purchase(activity, "your_product_id");
```

4. **Handle Purchase Results**:

Implement a `PurchaseListener` to handle the results of purchase transactions:

```java
AppPurchase.getInstance().setPurchaseListener(new PurchaseListener() {
    @Override
    public void onProductPurchased (String orderId, String originalJson){
        // Handle successful purchase
    }

    @Override
    public void displayErrorMessage (String errorMessage){
        // Handle error in purchase
    }

    @Override
    public void onUserCancelBilling () {
        // Handle user cancellation
    }
});
```

5. **Consume Purchases** (if needed):

If your product is consumable, you can consume the purchase to allow it to be bought again:

```java
    AppPurchase.getInstance().consumePurchase("your_product_id");
```

#### User Messaging Platform (UMP) Consent

Request user consent using the following method:

```java
AdsConsentManager.getInstance(this).requestUMP(this,true,"TEST_DEVICE_ID",false,new UMPResultListener() {
    @Override
    public void onCheckUMPSuccess ( boolean isConsentGiven){
        if (isConsentGiven) {
            // consent given 
        }else {
            // not shown but may request ads (as may be already requested)
        }

        if (adsConsentManager.canRequestAds()) {
            // moveToNext
            // onNextActionCalled()
        }
        
    }
});
```

### Sample Project

A sample project is included in the `app` directory. It demonstrates how to use AdManageKit to
manage ads, purchases, and user consent. Follow these steps to run the sample project:

1. Clone the repository:

   ```bash
   git clone https://github.com/i2hammad/AdManageKit.git
   ```

2. Open the sample project in Android Studio.

3. Replace placeholders with your own AdMob IDs and configure your app in the Google Play Console
   for in-app purchases.

4. Run the project on an Android device or emulator.

### Contributing

Contributions are welcome! If you'd like to contribute to this project, please follow these steps:

1. Fork the repository.
2. Create a new branch (`git checkout -b feature/YourFeature`).
3. Commit your changes (`git commit -m 'Add YourFeature'`).
4. Push to the branch (`git push origin feature/YourFeature`).
5. Open a Pull Request.

### License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

### Support

If you find AdManageKit valuable and wish to support its ongoing development, consider buying me a
cup of tea.  
[Buy me a coffee](https://buymeacoffee.com/i2hammad)

For any questions or issues, please open an issue in this repository or contact me
at [hammadmughal0001@gmail.com](mailto:hammadmughal0001@gmail.com).