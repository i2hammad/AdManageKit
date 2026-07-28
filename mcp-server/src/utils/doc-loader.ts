import * as fs from "node:fs";
import * as path from "node:path";
import { DocSection } from "../types.js";

let resolvedRoot: string | null = null;
let docSections: DocSection[] | null = null;
let apiSections: Map<string, string> | null = null;
let releaseVersions: string[] | null = null;
let migrationVersions: string[] | null = null;

/**
 * Resolves the root directory containing docs/, wiki/, and README.md.
 *
 * Search order:
 * 1. ADMANAGEKIT_REPO_ROOT env var (explicit override)
 * 2. Bundled content/ directory (npm package mode)
 * 3. Repo root relative to dist/ (local dev in monorepo)
 * 4. Current working directory
 */
export function getRepoRoot(): string {
  if (resolvedRoot) return resolvedRoot;

  const candidates: string[] = [];

  // 1. Environment variable override
  if (process.env.ADMANAGEKIT_REPO_ROOT) {
    candidates.push(process.env.ADMANAGEKIT_REPO_ROOT);
  }

  // 2. Bundled content/ dir (when installed as npm package)
  //    Located at: <package>/dist/utils/ -> <package>/content/
  candidates.push(path.resolve(__dirname, "..", "..", "content"));

  // 3. Repo root relative to mcp-server/dist/utils/
  candidates.push(path.resolve(__dirname, "..", "..", ".."));

  // 4. Current working directory (when run from repo root)
  candidates.push(process.cwd());

  // 5. Parent of cwd (when run from mcp-server/)
  candidates.push(path.resolve(process.cwd(), ".."));

  for (const candidate of candidates) {
    if (
      fs.existsSync(path.join(candidate, "docs", "API_REFERENCE.md")) ||
      fs.existsSync(path.join(candidate, "README.md"))
    ) {
      resolvedRoot = candidate;
      return resolvedRoot;
    }
  }

  throw new Error(
    "Could not find AdManageKit documentation. " +
      "Set ADMANAGEKIT_REPO_ROOT environment variable or ensure the package was built with `npm run build`."
  );
}

export function loadFile(relativePath: string): string {
  try {
    const fullPath = path.join(getRepoRoot(), relativePath);
    return fs.readFileSync(fullPath, "utf-8");
  } catch {
    return "";
  }
}

function parseMarkdownSections(
  content: string,
  filePath: string
): DocSection[] {
  const sections: DocSection[] = [];
  const lines = content.split("\n");
  let currentHeading = "";
  let currentLevel = 0;
  let currentContent: string[] = [];

  for (const line of lines) {
    const headingMatch = line.match(/^(#{1,4})\s+(.+)$/);
    if (headingMatch) {
      if (currentHeading || currentContent.length > 0) {
        sections.push({
          file: filePath,
          heading: currentHeading || "(intro)",
          content: currentContent.join("\n").trim(),
          level: currentLevel,
        });
      }
      currentLevel = headingMatch[1].length;
      currentHeading = headingMatch[2];
      currentContent = [];
    } else {
      currentContent.push(line);
    }
  }

  if (currentHeading || currentContent.length > 0) {
    sections.push({
      file: filePath,
      heading: currentHeading || "(intro)",
      content: currentContent.join("\n").trim(),
      level: currentLevel,
    });
  }

  return sections;
}

function discoverFiles(dir: string, basePath: string): string[] {
  const results: string[] = [];
  try {
    const entries = fs.readdirSync(path.join(getRepoRoot(), dir), {
      withFileTypes: true,
    });
    for (const entry of entries) {
      const relPath = path.join(basePath, entry.name);
      if (entry.isDirectory()) {
        results.push(...discoverFiles(path.join(dir, entry.name), relPath));
      } else if (entry.name.endsWith(".md")) {
        results.push(relPath);
      }
    }
  } catch {
    // Directory doesn't exist
  }
  return results;
}

export function loadAllDocSections(): DocSection[] {
  if (docSections) return docSections;

  const filePaths: string[] = [];

  filePaths.push("README.md");
  filePaths.push(...discoverFiles("docs", "docs"));
  filePaths.push(...discoverFiles("wiki", "wiki"));

  docSections = [];
  for (const filePath of filePaths) {
    const content = loadFile(filePath);
    if (content) {
      docSections.push(...parseMarkdownSections(content, filePath));
    }
  }

  return docSections;
}

export function loadApiSections(): Map<string, string> {
  if (apiSections) return apiSections;

  apiSections = new Map();
  const content = loadFile("docs/API_REFERENCE.md");
  if (!content) return apiSections;

  const lines = content.split("\n");
  let currentClass = "";
  let currentContent: string[] = [];

  for (const line of lines) {
    const h3Match = line.match(/^###\s+(.+)$/);
    if (h3Match) {
      if (currentClass) {
        apiSections.set(
          normalizeClassName(currentClass),
          currentContent.join("\n").trim()
        );
      }
      currentClass = h3Match[1];
      currentContent = [line];
    } else if (currentClass) {
      if (line.match(/^##\s+/)) {
        apiSections.set(
          normalizeClassName(currentClass),
          currentContent.join("\n").trim()
        );
        currentClass = "";
        currentContent = [];
      } else {
        currentContent.push(line);
      }
    }
  }

  if (currentClass) {
    apiSections.set(
      normalizeClassName(currentClass),
      currentContent.join("\n").trim()
    );
  }

  return apiSections;
}

function normalizeClassName(name: string): string {
  return name.replace(/[`()]/g, "").trim().split(/\s+/)[0];
}

export function loadTopicFiles(files: string[]): string {
  const parts: string[] = [];
  for (const file of files) {
    const content = loadFile(file);
    if (content) {
      parts.push(`--- ${file} ---\n\n${content}`);
    }
  }
  return parts.join("\n\n");
}

/**
 * Compares two dot-separated versions numerically, newest first.
 * Falls back to string comparison for any non-numeric component.
 */
function compareVersionsDesc(a: string, b: string): number {
  const pa = a.split(".");
  const pb = b.split(".");
  for (let i = 0; i < Math.max(pa.length, pb.length); i++) {
    const na = Number(pa[i] ?? 0);
    const nb = Number(pb[i] ?? 0);
    if (Number.isNaN(na) || Number.isNaN(nb)) {
      const cmp = (pb[i] ?? "").localeCompare(pa[i] ?? "");
      if (cmp !== 0) return cmp;
      continue;
    }
    if (na !== nb) return nb - na;
  }
  return 0;
}

/**
 * Versions that have a release-notes file, newest first.
 *
 * Derived by scanning docs/release-notes/ rather than hardcoded, so a new
 * release becomes queryable as soon as its notes are bundled. The previous
 * hardcoded list silently stopped at 3.3.8 and made every later release
 * unreachable through get_release_notes.
 */
export function discoverReleaseVersions(): string[] {
  if (releaseVersions) return releaseVersions;

  const versions: string[] = [];
  for (const file of discoverFiles("docs/release-notes", "")) {
    const match = path.basename(file).match(/^RELEASE_NOTES_v(.+)\.md$/);
    if (match) versions.push(match[1]);
  }
  releaseVersions = versions.sort(compareVersionsDesc);
  return releaseVersions;
}

/**
 * Versions with a "### Migrating to X" section in the README, newest first.
 * Derived from the README so a new migration guide needs no code change.
 */
export function discoverMigrationVersions(): string[] {
  if (migrationVersions) return migrationVersions;

  const readme = loadFile("README.md");
  const found = new Set<string>();
  for (const match of readme.matchAll(/^###\s+Migrating to\s+(\S+)\s*$/gm)) {
    found.add(match[1]);
  }
  migrationVersions = Array.from(found).sort(compareVersionsDesc);
  return migrationVersions;
}

/**
 * Class/component names documented in docs/API_REFERENCE.md, in document order.
 * Derived from the file's own headings, so newly documented types are
 * immediately reachable through get_api_reference.
 */
export function discoverApiClassNames(): string[] {
  return Array.from(loadApiSections().keys());
}

/**
 * Loads release notes for a version. `"latest"` resolves to the newest version
 * actually present, rather than a hardcoded one that goes stale every release.
 */
export function loadReleaseNotes(version: string): string {
  if (version === "latest") {
    const newest = discoverReleaseVersions()[0];
    if (!newest) return "";
    version = newest;
  }
  return loadFile(`docs/release-notes/RELEASE_NOTES_v${version}.md`);
}

export function loadMigrationGuide(version: string): string {
  const readme = loadFile("README.md");
  if (!readme) return "";

  const marker = `### Migrating to ${version}`;
  const startIdx = readme.indexOf(marker);
  if (startIdx === -1) return "";

  const rest = readme.slice(startIdx);
  const lines = rest.split("\n");
  const result: string[] = [lines[0]];

  for (let i = 1; i < lines.length; i++) {
    if (
      lines[i].match(/^###\s+Migrating to/) ||
      lines[i].match(/^---$/) ||
      lines[i].match(/^##\s+/)
    ) {
      break;
    }
    result.push(lines[i]);
  }

  const releaseNotes = loadReleaseNotes(version);
  if (releaseNotes) {
    return (
      result.join("\n").trim() +
      `\n\n--- Release Notes v${version} ---\n\n${releaseNotes}`
    );
  }

  return result.join("\n").trim();
}
