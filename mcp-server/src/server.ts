import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { registerDocumentationTools } from "./tools/documentation.js";
import { registerCodeGenerationTools } from "./tools/code-generation.js";

export async function startServer() {
  const server = new McpServer({
    name: "admanagekit-docs",
    version: "1.0.0",
  });

  registerDocumentationTools(server);
  registerCodeGenerationTools(server);

  const transport = new StdioServerTransport();
  await server.connect(transport);
}
