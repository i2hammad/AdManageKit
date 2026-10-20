# AdManageKit MCP Server

[![npm](https://img.shields.io/npm/v/admanagekit-mcp-server)](https://www.npmjs.com/package/admanagekit-mcp-server)

A [Model Context Protocol](https://modelcontextprotocol.io/) server for [AdManageKit](https://github.com/i2hammad/AdManageKit), the Android library for AdMob ads, Google Play Billing and UMP consent. It lets Claude Code, Claude Desktop, Cursor and other MCP clients look up AdManageKit documentation and generate integration code.

The package bundles the library's `README.md`, `docs/` and `wiki/` at publish time, so it works offline and matches the latest release.

## Setup

**Claude Desktop / Cursor** (`claude_desktop_config.json` or the client's MCP settings):

```json
{
  "mcpServers": {
    "admanagekit": {
      "command": "npx",
      "args": ["-y", "admanagekit-mcp-server"]
    }
  }
}
```

**Claude Code:**

```bash
claude mcp add admanagekit -- npx -y admanagekit-mcp-server
```

Inside the AdManageKit repository it's configured automatically by `.mcp.json`, which runs the local build.

## Tools

| Tool | What it does |
|---|---|
| `search_docs` | Full-text search across the README, docs and wiki |
| `get_doc_by_topic` | Fetch the guides for a topic: `getting-started`, `interstitial`, `native`, `banner`, `app-open`, `rewarded`, `compose`, `billing-integration`, `subscriptions`, `multi-provider-waterfall`, ... |
| `get_api_reference` | API reference for a class (`AdManager`, `AppOpenManager`, `AppPurchase`, ...) |
| `get_release_notes` | Release notes for a version, or `latest` |
| `get_migration_guide` | Upgrade steps for a target version |
| `list_documentation` | Every topic, class, release and migration guide available |
| `generate_config` | `Application` setup: `AdManageKitConfig`, SDK initialization, billing |
| `generate_ad_integration` | Code for a given ad type, display mode and loading strategy |
| `generate_billing_code` | Purchases, subscriptions, consumables, offers |
| `generate_compose_code` | Jetpack Compose ad components |

Code generators output **Kotlin** or **Java**.

Topics, API classes, release versions and migration guides are discovered from the bundled docs, so a new doc is reachable as soon as it ships.

## Development

```bash
npm install
npm run build   # compiles to dist/ and bundles docs into content/
npm start       # runs the server on stdio
```

Set `ADMANAGEKIT_REPO_ROOT` to serve docs from a different checkout.

Publishing is automated: `.github/workflows/publish-mcp.yml` republishes to npm whenever `docs/`, `wiki/`, `README.md` or `mcp-server/` changes on `main`. The version is resolved at publish time; bump `package.json` only for a deliberate minor or major release.

## License

MIT
