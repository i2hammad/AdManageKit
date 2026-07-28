import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { z } from "zod";
import {
  loadTopicFiles,
  loadApiSections,
  loadReleaseNotes,
  loadMigrationGuide,
  discoverApiClassNames,
  discoverReleaseVersions,
  discoverMigrationVersions,
} from "../utils/doc-loader.js";
import { searchDocs } from "../utils/search.js";
import { TOPIC_MAP } from "../types.js";

const topicKeys = Object.keys(TOPIC_MAP) as [string, ...string[]];

/**
 * Builds an enum schema from values discovered in the bundled docs.
 *
 * z.enum() requires a non-empty tuple, so if discovery finds nothing (docs
 * missing or unreadable) this degrades to a plain string rather than throwing
 * at registration time and taking the whole server down. Each tool still
 * validates the value at call time and reports what is actually available.
 */
function discoveredEnum(values: string[], description: string) {
  return values.length > 0
    ? z.enum(values as [string, ...string[]]).describe(description)
    : z.string().describe(description);
}

export function registerDocumentationTools(server: McpServer) {
  // Tool 1: search_docs
  server.tool(
    "search_docs",
    "Search across all AdManageKit documentation (docs/, wiki/, README) for a query. Returns matching sections with file paths and relevant snippets.",
    {
      query: z
        .string()
        .describe(
          "Search query (e.g., 'collapsible banner', 'retry logic', 'HYBRID strategy')"
        ),
      max_results: z
        .number()
        .optional()
        .default(5)
        .describe("Maximum number of matching sections to return (default: 5, max: 15)"),
    },
    async ({ query, max_results }) => {
      const results = searchDocs(query, max_results);

      if (results.length === 0) {
        return {
          content: [
            {
              type: "text" as const,
              text: `No results found for "${query}". Try different keywords or use list_documentation to see available topics.`,
            },
          ],
        };
      }

      const formatted = results
        .map(
          (r, i) =>
            `### Result ${i + 1} (score: ${r.score})\n**File:** ${r.file}\n**Section:** ${r.heading}\n\n${r.content}`
        )
        .join("\n\n---\n\n");

      return {
        content: [
          {
            type: "text" as const,
            text: `Found ${results.length} results for "${query}":\n\n${formatted}`,
          },
        ],
      };
    }
  );

  // Tool 2: get_doc_by_topic
  server.tool(
    "get_doc_by_topic",
    "Get documentation for a specific AdManageKit topic. Topics include ad types (interstitial, native, banner, app-open, rewarded), features (loading-strategies, configuration, compose), and billing (billing-integration, subscriptions, etc.).",
    {
      topic: z
        .enum(topicKeys)
        .describe("The topic to retrieve documentation for"),
    },
    async ({ topic }) => {
      const files = TOPIC_MAP[topic];
      if (!files) {
        return {
          content: [
            {
              type: "text" as const,
              text: `Unknown topic "${topic}". Use list_documentation to see available topics.`,
            },
          ],
        };
      }

      const content = loadTopicFiles(files);
      if (!content) {
        return {
          content: [
            {
              type: "text" as const,
              text: `No documentation found for topic "${topic}". The documentation files may not exist yet.`,
            },
          ],
        };
      }

      return {
        content: [{ type: "text" as const, text: content }],
      };
    }
  );

  // Tool 3: get_api_reference
  server.tool(
    "get_api_reference",
    "Get the API reference for a specific AdManageKit class or component. Returns method signatures, parameters, and usage examples.",
    {
      class_name: discoveredEnum(
        discoverApiClassNames(),
        "The class or component name to look up"
      ),
    },
    async ({ class_name }) => {
      const sections = loadApiSections();
      const content = sections.get(class_name);

      if (!content) {
        // Try partial match
        const partialMatch = Array.from(sections.entries()).find(([key]) =>
          key.toLowerCase().includes(class_name.toLowerCase())
        );

        if (partialMatch) {
          return {
            content: [
              {
                type: "text" as const,
                text: `### ${partialMatch[0]}\n\n${partialMatch[1]}`,
              },
            ],
          };
        }

        return {
          content: [
            {
              type: "text" as const,
              text: `No API reference found for "${class_name}". Available classes: ${Array.from(sections.keys()).join(", ")}`,
            },
          ],
        };
      }

      return {
        content: [{ type: "text" as const, text: `### ${class_name}\n\n${content}` }],
      };
    }
  );

  // Tool 4: get_release_notes
  server.tool(
    "get_release_notes",
    "Get release notes for a specific AdManageKit version. Returns new features, breaking changes, migration guides, and bug fixes.",
    {
      version: discoveredEnum(
        ["latest", ...discoverReleaseVersions()],
        "The version to get release notes for (e.g., '4.4.0', '3.0.0'). Use 'latest' for the most recent."
      ),
    },
    async ({ version }) => {
      const content = loadReleaseNotes(version);

      if (!content) {
        return {
          content: [
            {
              type: "text" as const,
              text: `No release notes found for version ${version}. Available versions: ${discoverReleaseVersions().join(", ")}`,
            },
          ],
        };
      }

      return {
        content: [{ type: "text" as const, text: content }],
      };
    }
  );

  // Tool 5: get_migration_guide
  server.tool(
    "get_migration_guide",
    "Get the migration guide for upgrading between AdManageKit versions. Covers breaking changes, deprecated APIs, and step-by-step migration instructions.",
    {
      target_version: discoveredEnum(
        discoverMigrationVersions(),
        "The version you are migrating TO (e.g., '4.2.0', '3.0.0')"
      ),
    },
    async ({ target_version }) => {
      const content = loadMigrationGuide(target_version);

      if (!content) {
        return {
          content: [
            {
              type: "text" as const,
              text: `No migration guide found for version ${target_version}. Migration guides are available for: ${discoverMigrationVersions().join(", ")}`,
            },
          ],
        };
      }

      return {
        content: [{ type: "text" as const, text: content }],
      };
    }
  );

  // Tool 6: list_documentation
  server.tool(
    "list_documentation",
    "List all available AdManageKit documentation topics, API references, release notes, and wiki pages.",
    {},
    async () => {
      const apiClassNames = discoverApiClassNames();
      const releaseVersions = discoverReleaseVersions();
      const migrationVersions = discoverMigrationVersions();

      const listing = `# AdManageKit Documentation

## Ad Types
- **interstitial** - Interstitial ad implementation and builder pattern
- **native** - Native ad caching and display
- **banner** - Banner ads with auto-refresh and collapsible support
- **app-open** - Lifecycle-aware app open ads
- **rewarded** - Rewarded video ads with reward tracking

## Features
- **loading-strategies** - ON_DEMAND, ONLY_CACHE, HYBRID, FRESH_WITH_CACHE_FALLBACK
- **frequency-control** - Time-based and count-based ad frequency control
- **configuration** - AdManageKitConfig settings reference
- **compose** - Jetpack Compose integration
- **interstitial-builder** - InterstitialAdBuilder fluent API
- **native-template-view** - 27+ native ad templates
- **native-preloading** - Preloading strategies for native ads
- **native-caching** - NativeAdManager caching system
- **banner-improvements** - Banner ad enhancements
- **loading-strategy-examples** - Code examples for loading strategies

## Billing
- **billing-integration** - Google Play Billing setup and usage
- **purchase-categories** - CONSUMABLE, FEATURE_UNLOCK, LIFETIME_PREMIUM, REMOVE_ADS
- **consumables** - Consumable product handling
- **subscriptions** - Subscription management, account hold, payment recovery
- **subscription-offers** - Offers, trials, intro pricing, price comparison
- **subscription-upgrades** - Upgrade/downgrade flows

## Multi-Provider
- **multi-provider-waterfall** - Waterfall ad loading from multiple networks
- **yandex-integration** - Yandex Ads SDK provider setup

## Other
- **java-usage** - Java (non-Kotlin) usage guide

## API Reference Classes
${apiClassNames.map((c) => `- ${c}`).join("\n")}

## Release Notes
${releaseVersions.map((v) => `- v${v}`).join("\n")}

## Migration Guides
${migrationVersions.map((v) => `- Migrating to ${v}`).join("\n")}

---
Use \`get_doc_by_topic\`, \`get_api_reference\`, \`get_release_notes\`, or \`get_migration_guide\` to access specific documentation.
Use \`search_docs\` to search across all documentation.`;

      // The grouped topic listing above is hand-written, so it can fall behind
      // TOPIC_MAP. Surface anything it missed rather than leaving a registered
      // topic undiscoverable — the failure mode this tool previously had.
      const undocumented = topicKeys.filter(
        (key) => !listing.includes(`**${key}**`)
      );
      const complete =
        undocumented.length > 0
          ? listing.replace(
              "\n---\nUse `get_doc_by_topic`",
              `\n## Other Topics\n${undocumented
                .map((k) => `- **${k}**`)
                .join("\n")}\n\n---\nUse \`get_doc_by_topic\``
            )
          : listing;

      return {
        content: [{ type: "text" as const, text: complete }],
      };
    }
  );
}
