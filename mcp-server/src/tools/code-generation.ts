import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { z } from "zod";
import {
  generateConfig,
  generateAdIntegration,
  generateBillingCode,
  generateComposeCode,
} from "../utils/templates.js";

export function registerCodeGenerationTools(server: McpServer) {
  // Tool 7: generate_config
  server.tool(
    "generate_config",
    "Generate AdManageKitConfig setup code for your Application class. Produces complete configuration including ad initialization, billing setup, and loading strategies.",
    {
      language: z
        .enum(["kotlin", "java"])
        .optional()
        .default("kotlin")
        .describe("Output language (default: kotlin)"),
      features: z
        .object({
          debug_mode: z.boolean().optional().describe("Enable debug mode"),
          smart_preloading: z
            .boolean()
            .optional()
            .describe("Enable smart ad preloading"),
          auto_retry: z
            .boolean()
            .optional()
            .describe("Enable automatic retry on failed ad loads"),
          max_retry_attempts: z
            .number()
            .optional()
            .describe("Max retry attempts (default: 3)"),
          performance_metrics: z
            .boolean()
            .optional()
            .describe("Enable performance tracking"),
          interstitial_strategy: z
            .enum(["ON_DEMAND", "ONLY_CACHE", "HYBRID"])
            .optional(),
          app_open_strategy: z
            .enum(["ON_DEMAND", "ONLY_CACHE", "HYBRID"])
            .optional(),
          native_strategy: z.enum(["ON_DEMAND", "HYBRID"]).optional(),
          interstitial_auto_reload: z.boolean().optional(),
          app_open_auto_reload: z.boolean().optional(),
          rewarded_auto_reload: z.boolean().optional(),
          billing: z
            .boolean()
            .optional()
            .describe("Include billing setup"),
          app_open_ad_unit: z
            .string()
            .optional()
            .describe("App open ad unit ID"),
          test_mode: z.boolean().optional().describe("Enable test mode"),
        })
        .optional()
        .default({}),
    },
    async ({ language, features }) => {
      const code = generateConfig({
        language,
        ...features,
      });

      return {
        content: [
          {
            type: "text" as const,
            text: `\`\`\`${language}\n${code}\n\`\`\``,
          },
        ],
      };
    }
  );

  // Tool 8: generate_ad_integration
  server.tool(
    "generate_ad_integration",
    "Generate complete ad integration code for a specific ad type. Includes XML layout, loading code, display code, and callback handling.",
    {
      ad_type: z
        .enum([
          "interstitial",
          "banner",
          "native_small",
          "native_medium",
          "native_large",
          "native_template",
          "app_open",
          "rewarded",
        ])
        .describe("The type of ad to generate code for"),
      language: z
        .enum(["kotlin", "java"])
        .optional()
        .default("kotlin")
        .describe("Output language (default: kotlin)"),
      ad_unit_id: z
        .string()
        .optional()
        .describe("AdMob ad unit ID (default: test ad unit)"),
      options: z
        .object({
          display_mode: z
            .enum([
              "force",
              "time_based",
              "count_based",
              "builder",
              "splash_wait",
            ])
            .optional()
            .describe("For interstitial: how to show the ad"),
          loading_strategy: z
            .enum(["ON_DEMAND", "ONLY_CACHE", "HYBRID"])
            .optional()
            .describe("Loading strategy to use"),
          use_caching: z
            .boolean()
            .optional()
            .describe("For native ads: use cached ads"),
          template: z
            .string()
            .optional()
            .describe(
              "For native_template: template name (e.g., MATERIAL3, CARD_MODERN)"
            ),
          collapsible: z
            .boolean()
            .optional()
            .describe("For banner: use collapsible banner"),
          with_callbacks: z
            .boolean()
            .optional()
            .describe("Include full callback handling (default: true)"),
          with_fallbacks: z
            .boolean()
            .optional()
            .describe("For interstitial builder: include fallback ad units"),
          frequency_control: z
            .object({
              every_nth_time: z.number().optional(),
              max_shows: z.number().optional(),
              min_interval_seconds: z.number().optional(),
            })
            .optional(),
          exclude_activities: z
            .array(z.string())
            .optional()
            .describe("For app_open: activity classes to exclude"),
          auto_reload: z
            .boolean()
            .optional()
            .describe("Auto-reload after dismissal"),
        })
        .optional()
        .default({}),
    },
    async ({ ad_type, language, ad_unit_id, options }) => {
      const code = generateAdIntegration({
        ad_type,
        language,
        ad_unit_id,
        ...options,
      });

      return {
        content: [
          {
            type: "text" as const,
            text: `\`\`\`${language}\n${code}\n\`\`\``,
          },
        ],
      };
    }
  );

  // Tool 9: generate_billing_code
  server.tool(
    "generate_billing_code",
    "Generate billing/purchase integration code for AdManageKit. Supports in-app purchases, subscriptions, consumable products, and subscription management.",
    {
      language: z
        .enum(["kotlin", "java"])
        .optional()
        .default("kotlin")
        .describe("Output language (default: kotlin)"),
      scenario: z
        .enum([
          "setup",
          "purchase",
          "subscribe",
          "consumable",
          "subscription_management",
          "expiry_verification",
          "complete",
        ])
        .describe("Which billing scenario to generate code for"),
      products: z
        .array(
          z.object({
            product_id: z.string(),
            type: z.enum(["PURCHASE", "SUBSCRIPTION"]),
            category: z
              .enum([
                "CONSUMABLE",
                "FEATURE_UNLOCK",
                "LIFETIME_PREMIUM",
                "REMOVE_ADS",
              ])
              .optional(),
            offer_token: z.string().optional(),
          })
        )
        .optional()
        .describe("Product definitions to include"),
    },
    async ({ language, scenario, products }) => {
      const code = generateBillingCode({
        language,
        scenario,
        products,
      });

      return {
        content: [
          {
            type: "text" as const,
            text: `\`\`\`${language}\n${code}\n\`\`\``,
          },
        ],
      };
    }
  );

  // Tool 10: generate_compose_code
  server.tool(
    "generate_compose_code",
    "Generate Jetpack Compose ad integration code. Supports all Compose ad components: BannerAdCompose, NativeTemplateCompose, rememberInterstitialAd, ConditionalAd, CacheWarmingEffect.",
    {
      component: z
        .enum([
          "banner",
          "native_template",
          "native_small",
          "native_medium",
          "native_large",
          "interstitial",
          "interstitial_state",
          "conditional_ad",
          "cache_warming",
          "complete_screen",
        ])
        .describe("Which Compose component to generate"),
      ad_unit_id: z
        .string()
        .optional()
        .describe("AdMob ad unit ID (default: test unit)"),
      options: z
        .object({
          template: z
            .string()
            .optional()
            .describe(
              "For native_template: NativeAdTemplate value (e.g., MATERIAL3)"
            ),
          loading_strategy: z
            .enum(["ON_DEMAND", "ONLY_CACHE", "HYBRID"])
            .optional(),
          with_callbacks: z.boolean().optional(),
          show_mode: z
            .enum(["TIME", "COUNT", "FORCE", "FORCE_WITH_DIALOG"])
            .optional()
            .describe("For interstitial_state: how to show the ad"),
        })
        .optional()
        .default({}),
    },
    async ({ component, ad_unit_id, options }) => {
      const code = generateComposeCode({
        component,
        ad_unit_id,
        ...options,
      });

      return {
        content: [
          {
            type: "text" as const,
            text: `\`\`\`kotlin\n${code}\n\`\`\``,
          },
        ],
      };
    }
  );
}
