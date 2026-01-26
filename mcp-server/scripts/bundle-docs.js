#!/usr/bin/env node

/**
 * Bundles documentation files from the AdManageKit repo into the
 * content/ directory so the MCP server works as a standalone npm package.
 *
 * Copies: docs/, wiki/, README.md
 */

const fs = require("fs");
const path = require("path");

const repoRoot = path.resolve(__dirname, "..", "..");
const contentDir = path.resolve(__dirname, "..", "content");

function copyDir(src, dest) {
  if (!fs.existsSync(src)) {
    console.warn(`  Skipping ${src} (not found)`);
    return 0;
  }

  fs.mkdirSync(dest, { recursive: true });
  let count = 0;

  const entries = fs.readdirSync(src, { withFileTypes: true });
  for (const entry of entries) {
    const srcPath = path.join(src, entry.name);
    const destPath = path.join(dest, entry.name);

    if (entry.isDirectory()) {
      count += copyDir(srcPath, destPath);
    } else if (entry.name.endsWith(".md")) {
      fs.copyFileSync(srcPath, destPath);
      count++;
    }
  }
  return count;
}

// Clean previous bundle
if (fs.existsSync(contentDir)) {
  fs.rmSync(contentDir, { recursive: true });
}
fs.mkdirSync(contentDir, { recursive: true });

console.log("Bundling AdManageKit documentation...");

// Copy README.md
const readmeSrc = path.join(repoRoot, "README.md");
if (fs.existsSync(readmeSrc)) {
  fs.copyFileSync(readmeSrc, path.join(contentDir, "README.md"));
  console.log("  README.md");
}

// Copy docs/
const docsCount = copyDir(
  path.join(repoRoot, "docs"),
  path.join(contentDir, "docs")
);
console.log(`  docs/ (${docsCount} files)`);

// Copy wiki/
const wikiCount = copyDir(
  path.join(repoRoot, "wiki"),
  path.join(contentDir, "wiki")
);
console.log(`  wiki/ (${wikiCount} files)`);

console.log(
  `Done. Bundled ${1 + docsCount + wikiCount} files into content/`
);
