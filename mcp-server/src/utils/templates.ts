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
    imports.push("import com.i2hammad.admanagekit.core.BillingConfig");
    imports.push(
      "import com.i2hammad.admanagekit.billing.BillingPurchaseProvider"
    );
  }
  if (o.app_open_ad_unit !== undefined || true) {
    imports.push(
      "import com.i2hammad.admanagekit.admob.AppOpenManager"
    );
  }

  return `${imports.join("\n")}

class MyApp : Application() {
    private lateinit var appOpenManager: AppOpenManager

    override fun onCreate() {
        super.onCreate()

        // Configure AdManageKit
        AdManageKitConfig.apply {
${configLines.join("\n")}
        }
${
  o.billing
    ? `
        // Set up billing provider
        BillingConfig.setPurchaseProvider(BillingPurchaseProvider())
`
    : ""
}
        // Initialize app open ads
        appOpenManager = AppOpenManager(this, ${appOpenUnit})
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
import com.i2hammad.admanagekit.config.AdManageKitConfig;
import com.i2hammad.admanagekit.config.AdLoadingStrategy;
import com.i2hammad.admanagekit.admob.AppOpenManager;
${o.billing ? "import com.i2hammad.admanagekit.core.BillingConfig;\nimport com.i2hammad.admanagekit.billing.BillingPurchaseProvider;\n" : ""}
public class MyApp extends Application {
    private AppOpenManager appOpenManager;

    @Override
    public void onCreate() {
        super.onCreate();

        // Configure AdManageKit
${configLines.join("\n")}
${o.billing ? "\n        // Set up billing provider\n        BillingConfig.INSTANCE.setPurchaseProvider(new BillingPurchaseProvider());\n" : ""}
        // Initialize app open ads
        appOpenManager = new AppOpenManager(this, "${appOpenUnit}");
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
    case "consumable":
      return generateBillingConsumable();
    case "subscription_management":
      return generateBillingSubscriptionMgmt();
    case "expiry_verification":
      return generateBillingExpiryVerification();
    case "complete":
      return [
        generateBillingSetup(options),
        generateBillingPurchase(),
        generateBillingConsumable(),
        generateBillingSubscriptionMgmt(),
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

// Define products
val products = listOf(
${productLines}
)

// Initialize billing in Application.onCreate()
AppPurchase.getInstance().initBilling(application, products)

// Set up billing provider for ad suppression
BillingConfig.setPurchaseProvider(BillingPurchaseProvider())`;
}

function generateBillingPurchase(): string {
  return `// Purchase a product
AppPurchase.getInstance().purchase(activity, "remove_ads")

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
  return `// Subscribe
AppPurchase.getInstance().subscribe(activity, "premium_monthly")

// Check subscription state
val state = AppPurchase.getInstance().getSubscriptionState("premium_monthly")
when (state) {
    SubscriptionState.ACTIVE -> showPremiumUI()
    SubscriptionState.CANCELLED -> showRenewalPrompt()
    SubscriptionState.EXPIRED -> showSubscribeButton()
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
  return `// Check subscription state
val state = AppPurchase.getInstance().getSubscriptionState("premium_monthly")
when (state) {
    SubscriptionState.ACTIVE -> showPremiumUI()
    SubscriptionState.CANCELLED -> showRenewalPrompt() // Still has access until period ends
    SubscriptionState.EXPIRED -> showSubscribeButton()
}

// Upgrade subscription
AppPurchase.getInstance().upgradeSubscription(activity, "premium_yearly")

// Downgrade subscription
AppPurchase.getInstance().downgradeSubscription(activity, "premium_basic")

// Full control with proration mode
AppPurchase.getInstance().changeSubscription(
    activity,
    "premium_monthly",         // from
    "premium_yearly",          // to
    SubscriptionReplacementMode.CHARGE_PRORATED_PRICE
)`;
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
  return `// Billing setup (Java)
List<PurchaseItem> products = Arrays.asList(
    new PurchaseItem("remove_ads", TYPE_IAP.PURCHASE, PurchaseCategory.REMOVE_ADS),
    new PurchaseItem("premium_monthly", "free_trial", TYPE_IAP.SUBSCRIPTION)
);

AppPurchase.getInstance().initBilling(getApplication(), products);
BillingConfig.INSTANCE.setPurchaseProvider(new BillingPurchaseProvider());

// Purchase
AppPurchase.getInstance().purchase(activity, "remove_ads");

// Check status
if (AppPurchase.getInstance().isPurchased()) {
    // User has premium access
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
