import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { z } from "zod";
import {
  loadTopicFiles,
  loadApiSections,
  loadReleaseNotes,
  loadMigrationGuide,
} from "../utils/doc-loader.js";
import { searchDocs } from "../utils/search.js";
import {
  TOPIC_MAP,
  API_CLASS_NAMES,
  RELEASE_VERSIONS,
  MIGRATION_VERSIONS,
} from "../types.js";

const topicKeys = Object.keys(TOPIC_MAP) as [string, ...string[]];

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
      class_name: z
        .enum(API_CLASS_NAMES)
        .describe("The class or component name to look up"),
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
      version: z
        .enum(["latest", ...RELEASE_VERSIONS])
        .describe(
          "The version to get release notes for (e.g., '3.3.5', '3.0.0'). Use 'latest' for the most recent."
        ),
    },
    async ({ version }) => {
      const content = loadReleaseNotes(version);

      if (!content) {
        return {
          content: [
            {
              type: "text" as const,
              text: `No release notes found for version ${version}. Available versions: ${RELEASE_VERSIONS.join(", ")}`,
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
      target_version: z
        .enum(MIGRATION_VERSIONS)
        .describe("The version you are migrating TO (e.g., '3.0.0', '2.9.0')"),
    },
    async ({ target_version }) => {
      const content = loadMigrationGuide(target_version);

      if (!content) {
        return {
          content: [
            {
              type: "text" as const,
              text: `No migration guide found for version ${target_version}. Migration guides are available for: ${MIGRATION_VERSIONS.join(", ")}`,
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
- **subscriptions** - Subscription management
- **subscription-upgrades** - Upgrade/downgrade flows

## Other
- **java-usage** - Java (non-Kotlin) usage guide

## API Reference Classes
${API_CLASS_NAMES.map((c) => `- ${c}`).join("\n")}

## Release Notes
${RELEASE_VERSIONS.map((v) => `- v${v}`).join("\n")}

## Migration Guides
${MIGRATION_VERSIONS.map((v) => `- Migrating to ${v}`).join("\n")}

---
Use \`get_doc_by_topic\`, \`get_api_reference\`, \`get_release_notes\`, or \`get_migration_guide\` to access specific documentation.
Use \`search_docs\` to search across all documentation.`;

      return {
        content: [{ type: "text" as const, text: listing }],
      };
    }
  );
}
