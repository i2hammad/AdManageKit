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
  "subscription-offers": ["wiki/Subscription-Offers.md"],
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
  "multi-provider-waterfall": [
    "docs/MULTI_PROVIDER_WATERFALL.md",
    "wiki/Multi-Provider-Waterfall.md",
  ],
  "yandex-integration": [
    "docs/YANDEX_INTEGRATION.md",
    "wiki/Yandex-Integration.md",
  ],
};

// API class names, release versions and migration versions are deliberately NOT
// listed here. They are derived from the bundled documentation at startup by
// discoverApiClassNames(), discoverReleaseVersions() and
// discoverMigrationVersions() in utils/doc-loader.ts.
//
// They used to be hardcoded and drifted badly: the release list stopped at 3.3.8
// while 38 release-notes files existed, so every version from 3.4.0 onward was
// rejected by the tool schema, and "latest" returned February 2026. Deriving them
// means shipping a doc is sufficient to make it reachable.

export const TEST_AD_UNITS: Record<string, string> = {
  banner: "ca-app-pub-3940256099942544/6300978111",
  interstitial: "ca-app-pub-3940256099942544/1033173712",
  native: "ca-app-pub-3940256099942544/2247696110",
  app_open: "ca-app-pub-3940256099942544/9257395921",
  rewarded: "ca-app-pub-3940256099942544/5224354917",
};
