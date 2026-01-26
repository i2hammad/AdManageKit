import { DocSection, SearchResult } from "../types.js";
import { loadAllDocSections } from "./doc-loader.js";

function tokenize(text: string): string[] {
  return text
    .toLowerCase()
    .split(/[\s\-_/.,;:!?()[\]{}"'`]+/)
    .filter((t) => t.length > 1);
}

function scoreSection(section: DocSection, queryTokens: string[]): number {
  const headingLower = section.heading.toLowerCase();
  const contentLower = section.content.toLowerCase();
  let score = 0;

  for (const token of queryTokens) {
    // Heading matches weighted 3x
    if (headingLower.includes(token)) {
      score += 3;
      // Exact heading word match bonus
      if (headingLower.split(/\s+/).some((w) => w === token)) {
        score += 2;
      }
    }

    // Content matches
    const contentMatches = contentLower.split(token).length - 1;
    score += Math.min(contentMatches, 5); // Cap at 5 to avoid keyword-stuffed sections dominating
  }

  // Bonus for matching all query tokens
  const allMatch = queryTokens.every(
    (t) => headingLower.includes(t) || contentLower.includes(t)
  );
  if (allMatch) {
    score += queryTokens.length;
  }

  return score;
}

export function searchDocs(
  query: string,
  maxResults: number = 5
): SearchResult[] {
  const sections = loadAllDocSections();
  const queryTokens = tokenize(query);

  if (queryTokens.length === 0) return [];

  const scored: SearchResult[] = [];

  for (const section of sections) {
    const score = scoreSection(section, queryTokens);
    if (score > 0) {
      scored.push({
        file: section.file,
        heading: section.heading,
        content: section.content,
        score,
      });
    }
  }

  // Sort by score descending
  scored.sort((a, b) => b.score - a.score);

  // Return top N, truncating content if very long
  return scored.slice(0, Math.min(maxResults, 15)).map((result) => ({
    ...result,
    content:
      result.content.length > 3000
        ? result.content.slice(0, 3000) + "\n\n... (truncated)"
        : result.content,
  }));
}
