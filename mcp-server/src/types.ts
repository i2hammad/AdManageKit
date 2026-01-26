export type Language = "kotlin" | "java";

export interface DocSection {
  file: string;
  heading: string;
  content: string;
  level: number;
}

export interface SearchResult {
  file: string;
  heading: string;
  content: string;
  score: number;
}

export const TOPIC_MAP: Record<string, string[]> = {
  interstitial: [
    "docs/interstitial-ads.md",
    "docs/INTERSTITIAL_BUILDER_GUIDE.md",
    "wiki/Interstitial-Ads.md",
  ],
  native: ["docs/native-ads-caching.md", "wiki/NativeAdManager.md"],
  banner: ["docs/BANNER_AD_IMPROVEMENTS.md", "wiki/Banner-Ads.md"],
  "app-open": ["docs/app-open-ads.md", "wiki/App-Open-Ads.md"],
  rewarded: ["docs/rewarded-ads.md", "wiki/Rewarded-Ads.md"],
  "loading-strategies": [
    "docs/AD_LOADING_STRATEGIES.md",
    "wiki/Ad-Loading-Strategies.md",
  ],
  "frequency-control": ["docs/AD_FREQUENCY_CONTROL.md"],
  configuration: ["docs/CONFIGURATION_USAGE.md", "wiki/Configuration.md"],
  compose: ["docs/COMPOSE_INTEGRATION.md", "wiki/Jetpack-Compose.md"],
  "interstitial-builder": ["docs/INTERSTITIAL_BUILDER_GUIDE.md"],
  "billing-integration": [
    "docs/APP_PURCHASE_GUIDE.md",
    "wiki/Billing-Integration.md",
  ],
  "purchase-categories": ["wiki/Purchase-Categories.md"],
  consumables: ["wiki/Consumable-Products.md"],
  subscriptions: ["wiki/Subscriptions.md"],
  "subscription-upgrades": ["wiki/Subscription-Upgrades.md"],
  "java-usage": ["docs/JAVA_USAGE_GUIDE.md"],
  "native-template-view": ["docs/NATIVE_TEMPLATE_VIEW.md"],
  "native-preloading": ["docs/NATIVE_AD_PRELOADING.md"],
  "native-caching": [
    "docs/native-ads-caching.md",
    "docs/NATIVE_AD_MANAGER_ENHANCEMENTS.md",
  ],
  "banner-improvements": ["docs/BANNER_AD_IMPROVEMENTS.md"],
  "loading-strategy-examples": ["docs/LOADING_STRATEGY_EXAMPLES.md"],
};

export const API_CLASS_NAMES = [
  "AdManageKitConfig",
  "BillingConfig",
  "AdManager",
  "AppOpenManager",
  "BannerAdView",
  "NativeAdViews",
  "NativeAdManager",
  "RewardedAdManager",
  "AppPurchase",
  "PurchaseItem",
  "AdRetryManager",
  "AdDebugUtils",
  "AdLoadCallback",
  "AdManagerCallback",
  "InterstitialAdBuilder",
  "WeakReferenceHolder",
] as const;

export const RELEASE_VERSIONS = [
  "3.3.5",
  "3.3.4",
  "3.3.3",
  "3.3.2",
  "3.3.0",
  "3.1.0",
  "3.0.0",
  "2.9.0",
  "2.8.0",
  "2.7.0",
  "2.6.0",
  "2.5.0",
  "2.3.0",
  "2.2.0",
  "2.1.0",
] as const;

export const MIGRATION_VERSIONS = ["3.0.0", "2.9.0", "2.8.0", "2.7.0"] as const;

export const TEST_AD_UNITS: Record<string, string> = {
  banner: "ca-app-pub-3940256099942544/6300978111",
  interstitial: "ca-app-pub-3940256099942544/1033173712",
  native: "ca-app-pub-3940256099942544/2247696110",
  app_open: "ca-app-pub-3940256099942544/9257395921",
  rewarded: "ca-app-pub-3940256099942544/5224354917",
};
