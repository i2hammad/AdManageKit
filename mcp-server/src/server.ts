import * as fs from "node:fs";
import * as path from "node:path";
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { registerDocumentationTools } from "./tools/documentation.js";
import { registerCodeGenerationTools } from "./tools/code-generation.js";

// Reported to clients during the MCP handshake. Read from package.json rather
// than hardcoded: it was pinned at "1.0.0" while npm had moved on to 1.2.x, and
// the publish workflow resolves the real version into package.json at publish time.
function packageVersion(): string {
  try {
    const pkg = path.resolve(__dirname, "..", "package.json");
    return JSON.parse(fs.readFileSync(pkg, "utf-8")).version ?? "0.0.0";
  } catch {
    return "0.0.0";
  }
}

export async function startServer() {
  const server = new McpServer({
    name: "admanagekit-docs",
    version: packageVersion(),
  });

  registerDocumentationTools(server);
  registerCodeGenerationTools(server);

  const transport = new StdioServerTransport();
  await server.connect(transport);
}
