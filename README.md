
# AdManageKit

AdManageKit is an Android library designed to simplify the management of Google AdMob ads, billing using the Google Play Billing Library, and User Messaging Platform (UMP) consent. This library provides a streamlined approach to integrating ads and handling user consent for privacy compliance, while also offering a sample project to demonstrate its usage.

## Features

- **AdMob Ads Management**: Easily integrate and manage AdMob ads in your Android applications, including banner, interstitial, and native ads.
- **Billing Management**: Seamless integration with the Google Play Billing Library to handle in-app purchases and subscriptions.
- **UMP Consent Management**: Manage user consent using Google's User Messaging Platform (UMP) to comply with privacy regulations like GDPR and CCPA.
- **Sample Project**: A fully functional sample project to demonstrate how to use the library effectively in your own apps.

## Getting Started

### Prerequisites

- Android Studio Arctic Fox (2020.3.1) or later
- Gradle 7.0 or later
- A valid AdMob account
- Google Play Console access for setting up in-app purchases

### Installation

1. **Add the library to your project**:

   Add the following to your `build.gradle` file in the `dependencies` section:

   ```groovy
   implementation 'com.i2hammad:admanagekit:1.0.0'
   ```

2. **Sync your project** with Gradle files.

### Usage

#### Initializing the Library

The `AdsConsentManager` should be initialized in the first activity of your application to ensure that the consent form is displayed to the user as required.

```java
public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize the AdsConsentManager
        AdsConsentManager.getInstance(this).requestUMP(this, new UMPResultListener() {
            @Override
            public void onCheckUMPSuccess(boolean isConsentGiven) {
                if (isConsentGiven) {
                    // Initialize Ads here
                }
            }
        });
    }
}
```

#### Managing AdMob Ads

To display an AdMob native ad, use the `AdManager` class:

```java
AdManager adManager = new AdManager(this);
adManager.loadNativeAd(adContainerView);
```

#### Handling In-App Purchases

AdManageKit simplifies the process of handling in-app purchases and subscriptions using the Google Play Billing Library. Follow these steps to set up in-app purchases:

1. **Configure your products** in the Google Play Console:
   - Create in-app products or subscriptions with unique product IDs.
   - Ensure that your app is linked to a payment account.

2. **Initialize the Billing Client**:

```java
AppPurchase.getInstance().initBilling(getApplication(), Arrays.asList(
    new PurchaseItem("your_product_id", AppPurchase.TYPE_IAP.PURCHASE)
));
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
    public void onProductPurchased(String orderId, String originalJson) {
        // Handle successful purchase
    }

    @Override
    public void displayErrorMessage(String errorMessage) {
        // Handle error in purchase
    }

    @Override
    public void onUserCancelBilling() {
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
AdsConsentManager.getInstance(this).requestUMP(this, true, "TEST_DEVICE_ID", false, new UMPResultListener() {
    @Override
    public void onCheckUMPSuccess(boolean isConsentGiven) {
        if (isConsentGiven) {
            // Proceed to load ads
        }
    }
});
```

### Sample Project

A sample project is included in the `app` directory. It demonstrates how to use AdManageKit to manage ads, purchases, and user consent. Follow these steps to run the sample project:

1. Clone the repository:

   ```bash
   git clone https://github.com/i2hammad/AdManageKit.git
   ```

2. Open the sample project in Android Studio.

3. Replace placeholders with your own AdMob IDs and configure your app in the Google Play Console for in-app purchases.

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

For any questions or issues, please open an issue in this repository or contact me at [hammad0001@gmail.com](mailto:hammad0001@gmail.com).
