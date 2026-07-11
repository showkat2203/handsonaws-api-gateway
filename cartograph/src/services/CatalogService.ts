import { CATALOG, CatalogItem, byId } from '../data/mock/catalog';

function norm(s: string): string {
  return s.trim().toLowerCase();
}

export const CatalogService = {
  all(): CatalogItem[] {
    return CATALOG;
  },

  byId,

  /** Text search across name + aliases (used by Lists paste/typed add and search UI). */
  search(query: string): CatalogItem[] {
    const q = norm(query);
    if (!q) return [];
    return CATALOG.filter(
      (c) => c.name.toLowerCase().includes(q) || c.aliases.some((a) => a.toLowerCase().includes(q))
    );
  },

  /** Voice resolution: matches spoken phrases against names, aliases, and intents. */
  resolveVoice(phrase: string): CatalogItem | null {
    const q = norm(phrase);
    if (!q) return null;
    const intentHit = CATALOG.find((c) => c.intents?.some((i) => norm(i) === q || q.includes(norm(i))));
    if (intentHit) return intentHit;
    const exact = CATALOG.find((c) => norm(c.name) === q || c.aliases.some((a) => norm(a) === q));
    if (exact) return exact;
    const fuzzy = CATALOG.find(
      (c) => c.name.toLowerCase().includes(q) || c.aliases.some((a) => a.toLowerCase().includes(q))
    );
    return fuzzy ?? null;
  },
};
