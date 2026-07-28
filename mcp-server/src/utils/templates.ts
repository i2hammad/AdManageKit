import { Language, TEST_AD_UNITS } from "../types.js";

function adUnitOrDefault(
  adUnitId: string | undefined,
  type: string
): string {
  return adUnitId || TEST_AD_UNITS[type] || "ca-app-pub-xxx/yyy";
}

// ─── Config Generation ────────────────────────────────────────

interface ConfigOptions {
  language: Language;
  debug_mode?: boolean;
  smart_preloading?: boolean;
  auto_retry?: boolean;
  max_retry_attempts?: number;
  performance_metrics?: boolean;
  interstitial_strategy?: string;
  app_open_strategy?: string;
  native_strategy?: string;
  interstitial_auto_reload?: boolean;
  app_open_auto_reload?: boolean;
  rewarded_auto_reload?: boolean;
  billing?: boolean;
  app_open_ad_unit?: string;
  test_mode?: boolean;
}

export function generateConfig(options: ConfigOptions): string {
  const lang = options.language || "kotlin";

  if (lang === "kotlin") {
    return generateConfigKotlin(options);
  }
  return generateConfigJava(options);
}

function generateConfigKotlin(o: ConfigOptions): string {
  const configLines: string[] = [];

  if (o.debug_mode !== undefined)
    configLines.push(`    debugMode = ${o.debug_mode}`);
  if (o.test_mode !== undefined)
    configLines.push(`    testMode = ${o.test_mode}`);
  if (o.smart_preloading)
    configLines.push(`    enableSmartPreloading = true`);
  if (o.auto_retry !== undefined)
    configLines.push(`    autoRetryFailedAds = ${o.auto_retry}`);
  if (o.max_retry_attempts !== undefined)
    configLines.push(`    maxRetryAttempts = ${o.max_retry_attempts}`);
  if (o.performance_metrics)
    configLines.push(`    enablePerformanceMetrics = true`);

  if (o.interstitial_strategy)
    configLines.push(
      `    interstitialLoadingStrategy = AdLoadingStrategy.${o.interstitial_strategy}`
    );
  if (o.app_open_strategy)
    configLines.push(
      `    appOpenLoadingStrategy = AdLoadingStrategy.${o.app_open_strategy}`
    );
  if (o.native_strategy)
    configLines.push(
      `    nativeLoadingStrategy = AdLoadingStrategy.${o.native_strategy}`
    );

  if (o.interstitial_auto_reload !== undefined)
    configLines.push(
      `    interstitialAutoReload = ${o.interstitial_auto_reload}`
    );
  if (o.app_open_auto_reload !== undefined)
    configLines.push(`    appOpenAutoReload = ${o.app_open_auto_reload}`);
  if (o.rewarded_auto_reload !== undefined)
    configLines.push(`    rewardedAutoReload = ${o.rewarded_auto_reload}`);

  const appOpenUnit = o.app_open_ad_unit
    ? `"${o.app_open_ad_unit}"`
    : `"${TEST_AD_UNITS.app_open}"`;

  const imports = [
    "import android.app.Application",
    "import android.content.pm.PackageManager",
    "import android.os.Handler",
    "import android.os.Looper",
    "import com.google.android.libraries.ads.mobile.sdk.MobileAds",
    "import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig",
    "import com.i2hammad.admanagekit.admob.AppOpenManager",
    "import com.i2hammad.admanagekit.config.AdManageKitConfig",
  ];

  if (
    o.interstitial_strategy ||
    o.app_open_strategy ||
    o.native_strategy
  ) {
    imports.push(
      "import com.i2hammad.admanagekit.config.AdLoadingStrategy"
    );
  }
  if (o.billing) {
    imports.push("import com.i2hammad.admanagekit.billing.AppPurchase");
    imports.push("import com.i2hammad.admanagekit.billing.PurchaseItem");
    imports.push(
      "import com.i2hammad.admanagekit.billing.BillingPurchaseProvider"
    );
    imports.push("import com.i2hammad.admanagekit.core.BillingConfig");
  }

  return `${imports.sort().join("\n")}

class MyApp : Application() {
    private var appOpenManager: AppOpenManager? = null

    override fun onCreate() {
        super.onCreate()

        // Configure AdManageKit
        AdManageKitConfig.apply {
${configLines.join("\n")}
        }
${
  o.billing
    ? `
        initBilling()
`
    : ""
}
        initAds()
    }

    /**
     * Since v4.2.0 AdManageKit runs on the Next-Gen Google Mobile Ads SDK, which
     * removed the legacy SDK's silent lazy-init. MobileAds.initialize() must be
     * called explicitly once before any ad request — AdManageKit does not do it
     * for you, because it does not own your consent flow.
     */
    private fun initAds() {
        val config = InitializationConfig.Builder(readApplicationIdFromManifest()).build()

        // initialize() is blocking, so keep it off the main thread or it can ANR.
        Thread {
            MobileAds.initialize(this, config)

            // Construct AppOpenManager only after initialize() returns. It registers
            // a lifecycle observer that can fire a load immediately, and the Next-Gen
            // SDK rejects requests made before initialization completes.
            Handler(Looper.getMainLooper()).post {
                appOpenManager = AppOpenManager(this, ${appOpenUnit})
            }
        }.start()
    }

    /**
     * The Next-Gen SDK does not read the manifest tag automatically like the
     * legacy SDK did, so the application id must be supplied explicitly.
     */
    private fun readApplicationIdFromManifest(): String {
        val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        return appInfo.metaData.getString("com.google.android.gms.ads.APPLICATION_ID")
            ?: error("Missing com.google.android.gms.ads.APPLICATION_ID meta-data in AndroidManifest.xml")
    }${
      o.billing
        ? `

    private fun initBilling() {
        // A library AAR is always compiled with BuildConfig.DEBUG = false, so the
        // host app must inject its own build state. Call this BEFORE initBilling().
        AppPurchase.getInstance().setDebugMode(BuildConfig.DEBUG)

        val products = listOf(
            PurchaseItem("remove_ads", AppPurchase.TYPE_IAP.PURCHASE, PurchaseItem.PurchaseCategory.REMOVE_ADS),
            PurchaseItem("premium_monthly", AppPurchase.TYPE_IAP.SUBSCRIPTION),
            PurchaseItem("premium_yearly", AppPurchase.TYPE_IAP.SUBSCRIPTION)
        )
        AppPurchase.getInstance().initBilling(this, products)

        // Lets every ad component skip loading for paying users.
        BillingConfig.setPurchaseProvider(BillingPurchaseProvider())
    }`
        : ""
    }
}`;
}

function generateConfigJava(o: ConfigOptions): string {
  const configLines: string[] = [];

  if (o.debug_mode !== undefined)
    configLines.push(
      `        AdManageKitConfig.INSTANCE.setDebugMode(${o.debug_mode});`
    );
  if (o.test_mode !== undefined)
    configLines.push(
      `        AdManageKitConfig.INSTANCE.setTestMode(${o.test_mode});`
    );
  if (o.smart_preloading)
    configLines.push(
      `        AdManageKitConfig.INSTANCE.setEnableSmartPreloading(true);`
    );
  if (o.auto_retry !== undefined)
    configLines.push(
      `        AdManageKitConfig.INSTANCE.setAutoRetryFailedAds(${o.auto_retry});`
    );
  if (o.interstitial_strategy)
    configLines.push(
      `        AdManageKitConfig.INSTANCE.setInterstitialLoadingStrategy(AdLoadingStrategy.${o.interstitial_strategy});`
    );
  if (o.app_open_strategy)
    configLines.push(
      `        AdManageKitConfig.INSTANCE.setAppOpenLoadingStrategy(AdLoadingStrategy.${o.app_open_strategy});`
    );
  if (o.native_strategy)
    configLines.push(
      `        AdManageKitConfig.INSTANCE.setNativeLoadingStrategy(AdLoadingStrategy.${o.native_strategy});`
    );

  const appOpenUnit = o.app_open_ad_unit || TEST_AD_UNITS.app_open;

  return `import android.app.Application;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import java.util.Arrays;
import java.util.List;
import com.google.android.libraries.ads.mobile.sdk.MobileAds;
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig;
import com.i2hammad.admanagekit.config.AdManageKitConfig;
import com.i2hammad.admanagekit.config.AdLoadingStrategy;
import com.i2hammad.admanagekit.admob.AppOpenManager;
${o.billing ? "import com.i2hammad.admanagekit.billing.AppPurchase;\nimport com.i2hammad.admanagekit.billing.PurchaseItem;\nimport com.i2hammad.admanagekit.billing.BillingPurchaseProvider;\nimport com.i2hammad.admanagekit.core.BillingConfig;\n" : ""}
public class MyApp extends Application {
    private AppOpenManager appOpenManager;

    @Override
    public void onCreate() {
        super.onCreate();

        // Configure AdManageKit
${configLines.join("\n")}
${o.billing ? "\n        initBilling();\n" : ""}
        initAds();
    }

    /**
     * Since v4.2.0 AdManageKit runs on the Next-Gen Google Mobile Ads SDK, which
     * removed the legacy SDK's silent lazy-init. MobileAds.initialize() must be
     * called explicitly once before any ad request.
     */
    private void initAds() {
        InitializationConfig config =
                new InitializationConfig.Builder(readApplicationIdFromManifest()).build();

        // initialize() is blocking, so keep it off the main thread or it can ANR.
        new Thread(() -> {
            MobileAds.initialize(this, config);

            // Construct AppOpenManager only after initialize() returns — it registers a
            // lifecycle observer that can request an ad immediately, and the Next-Gen SDK
            // rejects requests made before initialization completes.
            new Handler(Looper.getMainLooper()).post(() ->
                    appOpenManager = new AppOpenManager(this, "${appOpenUnit}"));
        }).start();
    }

    /**
     * The Next-Gen SDK does not read the manifest tag automatically like the
     * legacy SDK did, so the application id must be supplied explicitly.
     */
    private String readApplicationIdFromManifest() {
        try {
            android.content.pm.ApplicationInfo info = getPackageManager()
                    .getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            String id = info.metaData.getString("com.google.android.gms.ads.APPLICATION_ID");
            if (id == null) {
                throw new IllegalStateException(
                        "Missing com.google.android.gms.ads.APPLICATION_ID meta-data in AndroidManifest.xml");
            }
            return id;
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }${
      o.billing
        ? `

    private void initBilling() {
        // A library AAR is always compiled with BuildConfig.DEBUG = false, so the
        // host app must inject its own build state. Call this BEFORE initBilling().
        AppPurchase.getInstance().setDebugMode(BuildConfig.DEBUG);

        List<PurchaseItem> products = Arrays.asList(
                new PurchaseItem("remove_ads", AppPurchase.TYPE_IAP.PURCHASE,
                        PurchaseItem.PurchaseCategory.REMOVE_ADS),
                new PurchaseItem("premium_monthly", AppPurchase.TYPE_IAP.SUBSCRIPTION),
                new PurchaseItem("premium_yearly", AppPurchase.TYPE_IAP.SUBSCRIPTION));
        AppPurchase.getInstance().initBilling(this, products);

        // Lets every ad component skip loading for paying users.
        BillingConfig.INSTANCE.setPurchaseProvider(new BillingPurchaseProvider());
    }`
        : ""
    }
}`;
}

// ─── Ad Integration Generation ────────────────────────────────

interface AdIntegrationOptions {
  ad_type: string;
  language: Language;
  ad_unit_id?: string;
  display_mode?: string;
  loading_strategy?: string;
  use_caching?: boolean;
  template?: string;
  collapsible?: boolean;
  with_callbacks?: boolean;
  with_fallbacks?: boolean;
  frequency_control?: {
    every_nth_time?: number;
    max_shows?: number;
    min_interval_seconds?: number;
  };
  exclude_activities?: string[];
  auto_reload?: boolean;
}

export function generateAdIntegration(options: AdIntegrationOptions): string {
  const lang = options.language || "kotlin";
  const type = options.ad_type;

  switch (type) {
    case "interstitial":
      return generateInterstitial(options, lang);
    case "banner":
      return generateBanner(options, lang);
    case "native_small":
    case "native_medium":
    case "native_large":
      return generateNativeView(options, lang);
    case "native_template":
      return generateNativeTemplate(options, lang);
    case "app_open":
      return generateAppOpen(options, lang);
    case "rewarded":
      return generateRewarded(options, lang);
    default:
      return `// Unknown ad type: ${type}`;
  }
}

function generateInterstitial(o: AdIntegrationOptions, lang: Language): string {
  const adUnit = adUnitOrDefault(o.ad_unit_id, "interstitial");
  const mode = o.display_mode || "builder";

  if (lang === "kotlin") {
    if (mode === "builder") {
      let builder = `// Interstitial Ad using InterstitialAdBuilder
InterstitialAdBuilder.with(activity)
    .adUnit("${adUnit}")`;

      if (o.loading_strategy)
        builder += `\n    .loadingStrategy(AdLoadingStrategy.${o.loading_strategy})`;
      if (o.with_fallbacks)
        builder += `\n    .fallback("fallback-ad-unit-id")`;
      if (o.frequency_control?.every_nth_time)
        builder += `\n    .everyNthTime(${o.frequency_control.every_nth_time})`;
      if (o.frequency_control?.max_shows)
        builder += `\n    .maxShows(${o.frequency_control.max_shows})`;
      if (o.frequency_control?.min_interval_seconds)
        builder += `\n    .minIntervalSeconds(${o.frequency_control.min_interval_seconds})`;
      if (o.auto_reload !== undefined)
        builder += `\n    .autoReload(${o.auto_reload})`;

      builder += `\n    .show { navigateNext() }`;
      return builder;
    }

    if (mode === "force") {
      return `// Load interstitial ad
AdManager.getInstance().loadInterstitialAd(this, "${adUnit}")

// Force show interstitial
AdManager.getInstance().forceShowInterstitial(this, object : AdManagerCallback() {
    override fun onNextAction() {
        navigateNext()
    }
${
  o.with_callbacks !== false
    ? `    override fun onAdLoaded() {
        Log.d("Ads", "Ad loaded")
    }
    override fun onFailedToLoad(error: AdKitError?) {
        Log.e("Ads", "Failed: \${error?.message}")
    }
    override fun onAdShowed() {
        Log.d("Ads", "Ad shown")
    }`
    : ""
}
})`;
    }

    if (mode === "time_based") {
      return `// Load interstitial ad
AdManager.getInstance().loadInterstitialAd(this, "${adUnit}")

// Show based on time interval (default 15s between ads)
AdManager.getInstance().showInterstitialAdByTime(this, object : AdManagerCallback() {
    override fun onNextAction() {
        navigateNext()
    }
})`;
    }

    if (mode === "count_based") {
      return `// Load interstitial ad
AdManager.getInstance().loadInterstitialAd(this, "${adUnit}")

// Show based on count (e.g., every 3rd call)
AdManager.getInstance().showInterstitialAdByCount(this, object : AdManagerCallback() {
    override fun onNextAction() {
        navigateNext()
    }
}, maxDisplayCount = 3)`;
    }

    if (mode === "splash_wait") {
      return `// Smart splash screen ad - waits for ad to load or shows cached
AdManager.getInstance().showOrWaitForAd(
    activity = this,
    callback = object : AdManagerCallback() {
        override fun onNextAction() {
            navigateToMain()
        }
    },
    timeoutMillis = 10_000
)`;
    }
  }

  // Java
  return `// Load interstitial ad
AdManager.getInstance().loadInterstitialAd(this, "${adUnit}");

// Show interstitial
AdManager.getInstance().forceShowInterstitial(this, new AdManagerCallback() {
    @Override
    public void onNextAction() {
        navigateNext();
    }
});`;
}

function generateBanner(o: AdIntegrationOptions, lang: Language): string {
  const adUnit = adUnitOrDefault(o.ad_unit_id, "banner");

  const xml = `<!-- XML Layout -->
<com.i2hammad.admanagekit.admob.BannerAdView
    android:id="@+id/bannerAdView"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />`;

  if (lang === "kotlin") {
    if (o.collapsible) {
      return `${xml}

// Load collapsible banner
bannerAdView.loadCollapsibleBanner(this, "${adUnit}", true)`;
    }

    let code = `${xml}

// Load banner ad
bannerAdView.loadBanner(this, "${adUnit}")`;

    if (o.with_callbacks !== false) {
      code += `

// With callback
bannerAdView.loadBanner(this, "${adUnit}", object : AdLoadCallback() {
    override fun onAdLoaded() {
        Log.d("Ads", "Banner loaded")
    }
    override fun onFailedToLoad(error: AdKitError?) {
        Log.e("Ads", "Banner failed: \${error?.message}")
    }
})`;
    }
    return code;
  }

  return `${xml}

// Load banner ad (Java)
bannerAdView.loadBanner(this, "${adUnit}");`;
}

function generateNativeView(o: AdIntegrationOptions, lang: Language): string {
  const adUnit = adUnitOrDefault(o.ad_unit_id, "native");
  const size = o.ad_type.replace("native_", "");
  const viewClass =
    size === "small"
      ? "NativeBannerSmall"
      : size === "medium"
        ? "NativeBannerMedium"
        : "NativeLarge";

  const xml = `<!-- XML Layout -->
<com.i2hammad.admanagekit.admob.${viewClass}
    android:id="@+id/${viewClass.charAt(0).toLowerCase() + viewClass.slice(1)}"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />`;

  if (lang === "kotlin") {
    const useCached = o.use_caching ? ", useCachedAd = true" : "";
    return `${xml}

// Load native ${size} ad
${viewClass.charAt(0).toLowerCase() + viewClass.slice(1)}.loadNativeBannerAd(this, "${adUnit}"${useCached})`;
  }

  return `${xml}

// Load native ${size} ad (Java)
${viewClass.charAt(0).toLowerCase() + viewClass.slice(1)}.loadNativeBannerAd(this, "${adUnit}");`;
}

function generateNativeTemplate(
  o: AdIntegrationOptions,
  lang: Language
): string {
  const adUnit = adUnitOrDefault(o.ad_unit_id, "native");
  const template = o.template || "MATERIAL3";

  const xml = `<!-- XML Layout -->
<com.i2hammad.admanagekit.admob.NativeTemplateView
    android:id="@+id/nativeTemplateView"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:adTemplate="${template.toLowerCase()}" />`;

  if (lang === "kotlin") {
    let code = `${xml}

// Load native template ad
nativeTemplateView.setTemplate(NativeAdTemplate.${template})
nativeTemplateView.loadNativeAd(activity, "${adUnit}")`;

    if (o.with_callbacks !== false) {
      code += `

// With callback and strategy
nativeTemplateView.loadNativeAd(activity, "${adUnit}", object : AdLoadCallback() {
    override fun onAdLoaded() { /* success */ }
    override fun onFailedToLoad(error: AdKitError?) { /* error */ }
}${o.loading_strategy ? `, AdLoadingStrategy.${o.loading_strategy}` : ""})`;
    }
    return code;
  }

  return `${xml}

// Load native template ad (Java)
nativeTemplateView.setTemplate(NativeAdTemplate.${template});
nativeTemplateView.loadNativeAd(activity, "${adUnit}");`;
}

function generateAppOpen(o: AdIntegrationOptions, lang: Language): string {
  const adUnit = adUnitOrDefault(o.ad_unit_id, "app_open");

  if (lang === "kotlin") {
    let code = `// In your Application class
class MyApp : Application() {
    private lateinit var appOpenManager: AppOpenManager

    override fun onCreate() {
        super.onCreate()
        appOpenManager = AppOpenManager(this, "${adUnit}")`;

    if (o.exclude_activities?.length) {
      for (const activity of o.exclude_activities) {
        code += `\n        appOpenManager.disableAppOpenWithActivity(${activity}::class.java)`;
      }
    }

    code += `
    }
}`;

    code += `

// Force show app open ad
appOpenManager.forceShowAdIfAvailable(activity, object : AdManagerCallback() {
    override fun onNextAction() {
        continueToApp()
    }
})

// Skip next ad (e.g., before external intent)
appOpenManager.skipNextAd()

// Prefetch for return from external intent
appOpenManager.prefetchNextAd()`;

    return code;
  }

  return `// In your Application class (Java)
public class MyApp extends Application {
    private AppOpenManager appOpenManager;

    @Override
    public void onCreate() {
        super.onCreate();
        appOpenManager = new AppOpenManager(this, "${adUnit}");
    }
}`;
}

function generateRewarded(o: AdIntegrationOptions, lang: Language): string {
  const adUnit = adUnitOrDefault(o.ad_unit_id, "rewarded");

  if (lang === "kotlin") {
    let code = `// Initialize once (e.g., in Application.onCreate())
RewardedAdManager.initialize(context, "${adUnit}")

// Show when ready
if (RewardedAdManager.isAdLoaded()) {
    RewardedAdManager.showAd(activity, object : RewardedAdManager.RewardedAdCallback {
        override fun onRewardEarned(rewardType: String, rewardAmount: Int) {
            grantReward(rewardType, rewardAmount)
        }
        override fun onAdDismissed() {
            continueGameFlow()
        }`;

    if (o.with_callbacks !== false) {
      code += `
        override fun onAdShowed() {
            Log.d("Ads", "Rewarded ad shown")
        }
        override fun onAdFailedToShow(error: AdKitError?) {
            Log.e("Ads", "Failed to show: \${error?.message}")
        }
        override fun onAdClicked() {
            Log.d("Ads", "Ad clicked")
        }`;
    }

    code += `
    }`;

    if (o.auto_reload !== undefined) {
      code += `, autoReload = ${o.auto_reload}`;
    }

    code += `)
}

// Preload during natural pauses
RewardedAdManager.preload(context)

// Load with timeout (for splash screens)
RewardedAdManager.loadRewardedAdWithTimeout(context, 5000, object : RewardedAdManager.OnRewardedAdLoadCallback {
    override fun onAdLoaded() { /* ready */ }
    override fun onAdFailedToLoad(error: AdKitLoadError?) { /* failed */ }
})`;

    return code;
  }

  return `// Initialize once (Java)
RewardedAdManager.INSTANCE.initialize(context, "${adUnit}");

// Show when ready
if (RewardedAdManager.INSTANCE.isAdLoaded()) {
    RewardedAdManager.INSTANCE.showAd(activity, new RewardedAdManager.RewardedAdCallback() {
        @Override
        public void onRewardEarned(@NonNull String rewardType, int rewardAmount) {
            grantReward(rewardType, rewardAmount);
        }
        @Override
        public void onAdDismissed() {
            continueGameFlow();
        }
    });
}`;
}

// ─── Billing Code Generation ──────────────────────────────────

interface BillingOptions {
  language: Language;
  scenario: string;
  products?: Array<{
    product_id: string;
    type: string;
    category?: string;
    offer_token?: string;
  }>;
}

export function generateBillingCode(options: BillingOptions): string {
  const lang = options.language || "kotlin";
  const scenario = options.scenario;

  if (lang !== "kotlin") {
    return generateBillingJava(options);
  }

  switch (scenario) {
    case "setup":
      return generateBillingSetup(options);
    case "purchase":
      return generateBillingPurchase();
    case "subscribe":
      return generateBillingSubscribe();
    case "offers":
      return generateBillingOffers();
    case "consumable":
      return generateBillingConsumable();
    case "subscription_management":
      return generateBillingSubscriptionMgmt();
    case "account_hold":
      return generateBillingAccountHold();
    case "expiry_verification":
      return generateBillingExpiryVerification();
    case "complete":
      return [
        generateBillingSetup(options),
        generateBillingPurchase(),
        generateBillingOffers(),
        generateBillingConsumable(),
        generateBillingSubscriptionMgmt(),
        generateBillingAccountHold(),
      ].join("\n\n// ---\n\n");
    default:
      return `// Unknown billing scenario: ${scenario}`;
  }
}

function generateBillingSetup(o: BillingOptions): string {
  const products = o.products || [
    { product_id: "remove_ads", type: "PURCHASE", category: "REMOVE_ADS" },
    {
      product_id: "premium_monthly",
      type: "SUBSCRIPTION",
      offer_token: "free_trial",
    },
  ];

  const productLines = products
    .map((p) => {
      if (p.type === "SUBSCRIPTION" && p.offer_token) {
        return `    PurchaseItem("${p.product_id}", "${p.offer_token}", TYPE_IAP.SUBSCRIPTION)`;
      }
      if (p.category) {
        return `    PurchaseItem("${p.product_id}", TYPE_IAP.PURCHASE, PurchaseCategory.${p.category})`;
      }
      return `    PurchaseItem("${p.product_id}", TYPE_IAP.${p.type})`;
    })
    .join(",\n");

  return `import com.i2hammad.admanagekit.billing.*
import com.i2hammad.admanagekit.core.BillingConfig

// A library AAR is always compiled with BuildConfig.DEBUG = false, so the host
// app must inject its own build state. In debug builds this routes purchase()/
// subscribe() to the dev bottom sheet. Call this BEFORE initBilling().
AppPurchase.getInstance().setDebugMode(BuildConfig.DEBUG)

// Define products
val products = listOf(
${productLines}
)

// Initialize billing in Application.onCreate()
AppPurchase.getInstance().initBilling(application, products)

// Set up billing provider for ad suppression
BillingConfig.setPurchaseProvider(BillingPurchaseProvider())

// Optional but recommended: hashed identifiers Google uses for fraud detection.
// Never pass raw emails or user ids; max 64 characters.
AppPurchase.getInstance().setObfuscatedAccountId(sha256(userId))

// Diagnose an empty paywall — reports which product ids Play declined and why.
// Register BEFORE initBilling().
AppPurchase.getInstance().setProductDetailsListener(object : ProductDetailsListener {
    override fun onProductDetailsLoaded(
        productType: String,
        loaded: List<ProductDetails>,
        unfetched: List<UnfetchedProduct>,
    ) {
        unfetched.forEach { Log.e("Billing", "\${it.productId}: status \${it.statusCode}") }
    }

    override fun onProductDetailsFailed(productType: String, responseCode: Int, debugMessage: String?) {
        Log.e("Billing", "\$productType query failed: \$responseCode \$debugMessage")
    }
})`;
}

function generateBillingPurchase(): string {
  return `// Purchase a product
AppPurchase.getInstance().purchase(activity, "remove_ads")

// If the product has multiple offers (a launch discount, a rental, a
// limited-quantity drop), buy the specific one instead — see the
// "offers" scenario.
AppPurchase.getInstance().getBestOneTimeOffer("remove_ads")?.let { offer ->
    AppPurchase.getInstance().purchase(activity, offer)
}

// Check purchase status
if (AppPurchase.getInstance().isPurchased()) {
    // User has premium access
}

// Listen for purchase events
AppPurchase.getInstance().setPurchaseHistoryListener(object : PurchaseHistoryListener {
    override fun onNewPurchase(productId: String, purchase: PurchaseResult) {
        Log.d("Billing", "Purchased: $productId")
    }
    override fun onPurchaseConsumed(productId: String, purchase: PurchaseResult) {
        Log.d("Billing", "Consumed: $productId")
    }
})`;
}

function generateBillingSubscribe(): string {
  return `val billing = AppPurchase.getInstance()

// Simple case — one offer on the product. The library picks the offer for you:
// the id configured as PurchaseItem.trialId, otherwise Play's LAST offer.
billing.subscribe(activity, "premium_monthly")

// If the product has more than one offer (base plan + free trial + intro offer),
// the line above can charge for a plan the user did not pick. Pass the offer
// explicitly instead — see the "offers" scenario for a full paywall.
val offers = billing.getOffers("premium_monthly")
billing.subscribe(activity, offers[selectedIndex])

// Check subscription state
when (billing.getSubscriptionState("premium_monthly")) {
    SubscriptionState.ACTIVE -> showPremiumUI()
    SubscriptionState.CANCELLED -> showRenewalPrompt()   // Still has access
    SubscriptionState.ON_HOLD -> showFixPaymentPrompt()  // Payment declined — no access
    SubscriptionState.EXPIRED -> showSubscribeButton()
    else -> showSubscribeButton()
}`;
}

function generateBillingOffers(): string {
  return `val billing = AppPurchase.getInstance()

// A subscription product usually carries several offers — a base plan, a free
// trial, an introductory discount. Play returns only the ones THIS account is
// eligible for, so the list is already personalized.
for (offer in billing.getOffers("premium_monthly")) {
    Log.d("IAP", "\${offer.basePlanId}/\${offer.offerId} " +
            "trial=\${offer.isFreeTrial} intro=\${offer.introPrice} base=\${offer.basePrice}")
}

// Find a specific offer rather than relying on list order
billing.getIntroOffer("premium_monthly")                    // first with an intro price
billing.getTrialOffer("premium_monthly")                    // first with a free trial
billing.getOfferByBasePlanId("premium_yearly", "yearly")    // by base plan id
billing.getOfferByTag("premium_yearly", "popular")          // by Play Console offer tag
billing.getBestValueOffer("premium_yearly")                 // lowest cost per month
billing.getCheapestFirstCycleOffer("premium_yearly")        // cheapest way in

// Render an offer without hand-parsing ProductDetails or ISO-8601 periods
val offer = billing.getBaseOffer("premium_yearly") ?: return

priceLabel.text    = offer.basePrice                        // "\$59.99"
cycleLabel.text    = BillingPeriod.formatOf(offer.billingPeriod)  // "1 year"
perMonthLabel.text = AppPurchase.formatPrice(offer.pricePerMonthMicros, offer.currencyCode)
todayLabel.text    = offer.firstCyclePrice                  // "Free", "\$1.99" or "\$59.99"

// "Save 50%" badge — compares BASE offers, so trial/intro phases don't distort it
val savings = billing.getSavingsPercent("premium_monthly", "premium_yearly")
savingsBadge.isVisible = savings > 0
savingsBadge.text = "Save \$savings%"

// Only promise a trial this account can actually claim, or Play charges immediately
subscribeButton.text = if (billing.isEligibleForFreeTrial("premium_monthly")) {
    "Start free trial"
} else {
    "Subscribe"
}

// Buy exactly the offer the user tapped
subscribeButton.setOnClickListener { billing.subscribe(activity, offer) }

// One-time products can carry multiple offers too (discounts, rentals,
// pre-orders, limited quantity)
billing.getBestOneTimeOffer("remove_ads")?.let { oneTime ->
    price.text = oneTime.formattedPrice
    badge.isVisible = oneTime.isDiscounted
    badge.text = "-\${oneTime.effectiveDiscountPercent}%"
    buyButton.isEnabled = !oneTime.isSoldOut && oneTime.isValidAt()
    buyButton.setOnClickListener { billing.purchase(activity, oneTime) }
}

// NOTE: localize periods with BillingPeriod.parse(iso) and pair unit + count with
// your own plurals resources. BillingPeriod.formatOf() is English-only.`;
}

function generateBillingAccountHold(): string {
  return `val billing = AppPurchase.getInstance()

// When Play cannot charge a subscriber, the subscription enters ACCOUNT HOLD.
// The purchase record still exists, but the user has NOT paid.
// Detected client-side since v4.4.0 — it used to need server-side verification.
if (billing.hasSubscriptionOnHold()) {
    // Play shows the user an in-app flow to fix their payment method.
    // Google recommends calling this on foreground entry for subscription apps.
    billing.showInAppMessages(activity, object : InAppMessageListener {
        override fun onSubscriptionRecovered(purchaseToken: String) {
            // AppPurchase already refreshed its state — isPurchased() is correct here.
            refreshPremiumUi()
        }

        override fun onNoActionNeeded() { }
    })
}

// Per-subscription check
val subscription = billing.getSubscription("premium_monthly")
if (subscription?.isSuspended == true) {
    showFixPaymentPrompt()
}

// IMPORTANT: isSubscriptionActive() returns false during account hold, which is
// what Google requires — an on-hold user must not keep premium access. If your
// app deliberately keeps serving these users, check isSuspended() explicitly.

// A plan change the user started but has not paid for yet. The CURRENT plan
// remains the entitlement in force until it completes.
if (billing.hasPendingSubscriptionChange()) {
    showPlanChangePendingBanner()
}`;
}

function generateBillingConsumable(): string {
  return `// Handle consumable purchases
AppPurchase.getInstance().setPurchaseHistoryListener(object : PurchaseHistoryListener {
    override fun onNewPurchase(productId: String, purchase: PurchaseResult) {
        if (productId == "coins_100") {
            addCoins(100 * purchase.quantity)
            AppPurchase.getInstance().consumePurchase(productId) // Must manually consume!
        }
    }
    override fun onPurchaseConsumed(productId: String, purchase: PurchaseResult) {
        Log.d("Billing", "Consumed: $productId")
    }
})`;
}

function generateBillingSubscriptionMgmt(): string {
  return `val billing = AppPurchase.getInstance()

// Check subscription state
when (billing.getSubscriptionState("premium_monthly")) {
    SubscriptionState.ACTIVE -> showPremiumUI()
    SubscriptionState.CANCELLED -> showRenewalPrompt()   // Still has access until period ends
    SubscriptionState.ON_HOLD -> showFixPaymentPrompt()  // Payment declined — no access (v4.4.0+)
    SubscriptionState.EXPIRED -> showSubscribeButton()
    else -> showSubscribeButton()
}

// Upgrade subscription
billing.upgradeSubscription(activity, "premium_yearly")

// Downgrade subscription
billing.downgradeSubscription(activity, "premium_basic")

// Full control with proration mode
billing.changeSubscription(
    activity,
    "premium_monthly",         // from
    "premium_yearly",          // to
    SubscriptionReplacementMode.CHARGE_PRORATED_PRICE
)

// Upgrade onto a SPECIFIC offer of the new plan. The overloads above let the
// library resolve the offer, which may not be the one the user selected.
val target = billing.getOfferByBasePlanId("premium_yearly", "yearly")
val current = billing.getSubscription("premium_monthly")
if (target != null && current != null) {
    billing.updateSubscription(
        activity,
        "premium_yearly",
        target.offerToken,
        current.purchaseToken,
        AppPurchase.SubscriptionReplacementMode.CHARGE_PRORATED_PRICE
    )
}`;
}

function generateBillingExpiryVerification(): string {
  return `// Set up server-side verification callback
AppPurchase.getInstance().setSubscriptionVerificationCallback { packageName, subscriptionId, purchaseToken, listener ->
    yourApi.verifySubscription(purchaseToken) { expiryMillis ->
        val details = SubscriptionVerificationCallback.SubscriptionDetails.Builder()
            .setExpiryTimeMillis(expiryMillis)
            .build()
        listener.onVerified(details)
    }
}

// Verify and get expiry info
AppPurchase.getInstance().verifySubscription("premium_monthly",
    object : AppPurchase.SubscriptionVerificationListener {
        override fun onVerified(subscription: PurchaseResult) {
            val expiryDate = subscription.getExpiryTimeFormatted("dd MMM yyyy")
            val daysLeft = subscription.getRemainingDays()
            val isExpired = subscription.isExpired()
        }
        override fun onVerificationFailed(error: String?) {
            Log.e("Billing", "Verification failed: $error")
        }
    }
)`;
}

function generateBillingJava(o: BillingOptions): string {
  return `// ─── Setup ───────────────────────────────────────────────────
// A library AAR is always compiled with BuildConfig.DEBUG = false, so the host
// app must inject its own build state. Call this BEFORE initBilling().
AppPurchase.getInstance().setDebugMode(BuildConfig.DEBUG);

List<PurchaseItem> products = Arrays.asList(
    new PurchaseItem("remove_ads", AppPurchase.TYPE_IAP.PURCHASE,
            PurchaseItem.PurchaseCategory.REMOVE_ADS),
    new PurchaseItem("premium_monthly", AppPurchase.TYPE_IAP.SUBSCRIPTION),
    new PurchaseItem("premium_yearly", AppPurchase.TYPE_IAP.SUBSCRIPTION)
);

AppPurchase.getInstance().initBilling(getApplication(), products);
BillingConfig.INSTANCE.setPurchaseProvider(new BillingPurchaseProvider());

// ─── Purchase ────────────────────────────────────────────────
AppPurchase.getInstance().purchase(activity, "remove_ads");

if (AppPurchase.getInstance().isPurchased()) {
    // User has premium access
}

// ─── Offers ──────────────────────────────────────────────────
// A subscription usually has several offers. subscribe(activity, id) resolves
// the offer itself and may not pick the one the user tapped — pass it directly.
AppPurchase billing = AppPurchase.getInstance();
List<OfferInfo> offers = billing.getOffers("premium_monthly");
billing.subscribe(activity, offers.get(selectedIndex));

OfferInfo base = billing.getBaseOffer("premium_yearly");
if (base != null) {
    priceLabel.setText(base.getBasePrice());
    cycleLabel.setText(BillingPeriod.formatOf(base.getBillingPeriod()));
    perMonthLabel.setText(
            AppPurchase.formatPrice(base.getPricePerMonthMicros(), base.getCurrencyCode()));
}

int savings = billing.getSavingsPercent("premium_monthly", "premium_yearly");
if (savings > 0) {
    savingsBadge.setText("Save " + savings + "%");
}

// Only promise a trial this account can actually claim
subscribeButton.setText(
        billing.isEligibleForFreeTrial("premium_monthly") ? "Start free trial" : "Subscribe");

// ─── Account hold ────────────────────────────────────────────
// isSubscriptionActive() returns false during account hold — the user's payment
// was declined and they must not keep premium access.
if (billing.hasSubscriptionOnHold()) {
    billing.showInAppMessages(activity, new InAppMessageListener() {
        @Override
        public void onSubscriptionRecovered(@NonNull String purchaseToken) {
            refreshPremiumUi();
        }

        @Override
        public void onNoActionNeeded() { }
    });
}`;
}

// ─── Compose Code Generation ──────────────────────────────────

interface ComposeOptions {
  component: string;
  ad_unit_id?: string;
  template?: string;
  loading_strategy?: string;
  with_callbacks?: boolean;
  show_mode?: string;
}

export function generateComposeCode(options: ComposeOptions): string {
  const component = options.component;

  switch (component) {
    case "banner":
      return generateComposeBanner(options);
    case "native_template":
      return generateComposeNativeTemplate(options);
    case "native_small":
    case "native_medium":
    case "native_large":
      return generateComposeNativeSize(options);
    case "interstitial":
      return generateComposeInterstitial(options);
    case "interstitial_state":
      return generateComposeInterstitialState(options);
    case "conditional_ad":
      return generateComposeConditional(options);
    case "cache_warming":
      return generateComposeCacheWarming(options);
    case "complete_screen":
      return generateComposeCompleteScreen(options);
    default:
      return `// Unknown Compose component: ${component}`;
  }
}

function generateComposeBanner(o: ComposeOptions): string {
  const adUnit = adUnitOrDefault(o.ad_unit_id, "banner");
  return `@Composable
fun MyScreen() {
    BannerAdCompose(adUnitId = "${adUnit}")
}`;
}

function generateComposeNativeTemplate(o: ComposeOptions): string {
  const adUnit = adUnitOrDefault(o.ad_unit_id, "native");
  const template = o.template || "MATERIAL3";
  const strategy = o.loading_strategy
    ? `,\n        loadingStrategy = AdLoadingStrategy.${o.loading_strategy}`
    : "";

  return `@Composable
fun MyScreen() {
    NativeTemplateCompose(
        adUnitId = "${adUnit}",
        template = NativeAdTemplate.${template}${strategy}
    )
}`;
}

function generateComposeNativeSize(o: ComposeOptions): string {
  const adUnit = adUnitOrDefault(o.ad_unit_id, "native");
  const size = o.component.replace("native_", "");
  const composable =
    size === "small"
      ? "NativeBannerSmallCompose"
      : size === "medium"
        ? "NativeBannerMediumCompose"
        : "NativeLargeCompose";
  const strategy = o.loading_strategy
    ? `,\n        loadingStrategy = AdLoadingStrategy.${o.loading_strategy}`
    : "";

  return `@Composable
fun MyScreen() {
    ${composable}(
        adUnitId = "${adUnit}"${strategy}
    )
}`;
}

function generateComposeInterstitial(o: ComposeOptions): string {
  const adUnit = adUnitOrDefault(o.ad_unit_id, "interstitial");

  return `@Composable
fun MyScreen() {
    val showInterstitial = rememberInterstitialAd(
        adUnitId = "${adUnit}",
        preloadAd = true
    )

    Button(onClick = { showInterstitial() }) {
        Text("Continue")
    }
}`;
}

function generateComposeInterstitialState(o: ComposeOptions): string {
  const adUnit = adUnitOrDefault(o.ad_unit_id, "interstitial");
  const mode = o.show_mode || "FORCE";

  return `@Composable
fun MyScreen() {
    val interstitialState = rememberInterstitialAdState(
        adUnitId = "${adUnit}",
        preloadAd = true
    )

    Button(onClick = {
        interstitialState.show(
            showMode = InterstitialShowMode.${mode},
            onNextAction = { navigateNext() }
        )
    }) {
        Text("Continue")
    }

    // Check ad state
    if (interstitialState.isLoaded) {
        Text("Ad ready")
    }
}`;
}

function generateComposeConditional(o: ComposeOptions): string {
  const adUnit = adUnitOrDefault(o.ad_unit_id, "native");

  return `@Composable
fun MyScreen() {
    // Only shows ad for non-purchased users
    ConditionalAd {
        NativeTemplateCompose(
            adUnitId = "${adUnit}",
            template = NativeAdTemplate.MATERIAL3
        )
    }
}`;
}

function generateComposeCacheWarming(o: ComposeOptions): string {
  const adUnit = adUnitOrDefault(o.ad_unit_id, "native");

  return `@Composable
fun MyScreen() {
    // Warm cache on first composition
    CacheWarmingEffect(
        adUnitId = "${adUnit}",
        count = 3
    )

    // Use cached ads
    NativeTemplateCompose(
        adUnitId = "${adUnit}",
        template = NativeAdTemplate.LIST_ITEM,
        loadingStrategy = AdLoadingStrategy.ONLY_CACHE
    )
}`;
}

function generateComposeCompleteScreen(o: ComposeOptions): string {
  const bannerUnit = adUnitOrDefault(o.ad_unit_id, "banner");
  const nativeUnit = adUnitOrDefault(undefined, "native");
  const interstitialUnit = adUnitOrDefault(undefined, "interstitial");

  return `@Composable
fun MainScreen() {
    // Cache warming on screen entry
    CacheWarmingEffect(adUnitId = "${nativeUnit}", count = 2)

    val showInterstitial = rememberInterstitialAd(
        adUnitId = "${interstitialUnit}",
        preloadAd = true
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Banner at top
        BannerAdCompose(adUnitId = "${bannerUnit}")

        // Content
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(contentList) { item ->
                ContentItem(item)
            }

            // Native ad in feed
            item {
                ConditionalAd {
                    NativeTemplateCompose(
                        adUnitId = "${nativeUnit}",
                        template = NativeAdTemplate.LIST_ITEM,
                        loadingStrategy = AdLoadingStrategy.HYBRID
                    )
                }
            }
        }

        // Interstitial trigger
        Button(
            onClick = { showInterstitial() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Next Level")
        }
    }
}`;
}
