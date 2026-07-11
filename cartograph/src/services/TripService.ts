import { byId } from '../data/mock/catalog';
import { StoreId } from '../data/mock/stores';
import { ListItem } from '../state/types';

export interface StorePlan {
  store: StoreId;
  itemIds: string[];
  total: number;
  reason?: string;
}

export interface TripPlan {
  mode: 'best';
  stores: StorePlan[];
  assign: Record<string, StoreId>;
  total: number;
  minutes: number;
  megaTotal: number;
  megaHas: number;
  savings: number;
}

export interface OneStopPlan {
  mode: 'onestop';
  store: StorePlan;
  assign: Record<string, StoreId>;
  droppedNote?: string;
  minutes: number;
}

function reasonFor(store: StoreId, itemIds: string[]): string | undefined {
  const exclusive = itemIds
    .map((id) => byId(id))
    .filter((c) => Object.keys(c.at).length === 1 && c.at[store])
    .map((c) => c.name.toLowerCase());
  if (exclusive.length === 0) return undefined;
  return `only store nearby with ${exclusive.join(' + ')}`;
}

export const TripService = {
  /** Splits the todo list across Save-Mart + Miller's by price/availability. */
  planBest(items: ListItem[]): TripPlan {
    const assign: Record<string, StoreId> = {};
    const saveIds: string[] = [];
    const millerIds: string[] = [];
    let saveTotal = 0;
    let millerTotal = 0;

    items.forEach((it) => {
      const c = byId(it.itemId);
      const s = c.at.save;
      const m = c.at.miller;
      let pick: StoreId | undefined;
      if (s && m) pick = m.price <= s.price ? 'miller' : 'save';
      else if (s) pick = 'save';
      else if (m) pick = 'miller';
      if (!pick) return;
      assign[it.itemId] = pick;
      if (pick === 'save') {
        saveIds.push(it.itemId);
        saveTotal += s!.price;
      } else {
        millerIds.push(it.itemId);
        millerTotal += m!.price;
      }
    });

    let megaTotal = 0;
    let megaHas = 0;
    let sameAtBest = 0;
    items.forEach((it) => {
      const c = byId(it.itemId);
      if (c.at.mega) {
        megaTotal += c.at.mega.price;
        megaHas += 1;
        const pick = assign[it.itemId];
        if (pick) sameAtBest += byId(it.itemId).at[pick]!.price;
      }
    });

    const allStores: StorePlan[] = [
      { store: 'save', itemIds: saveIds, total: saveTotal, reason: reasonFor('save', saveIds) },
      { store: 'miller', itemIds: millerIds, total: millerTotal, reason: reasonFor('miller', millerIds) },
    ];
    const stores = allStores.filter((s) => s.itemIds.length > 0);

    return {
      mode: 'best',
      stores,
      assign,
      total: saveTotal + millerTotal,
      minutes: 8 + stores.reduce((n, s) => n + s.itemIds.length * 1.6, 0),
      megaTotal,
      megaHas,
      savings: Math.max(0, megaTotal - sameAtBest),
    };
  },

  /** One-stop alternative at MegaMart. Items MegaMart doesn't carry are kept for next trip. */
  planOneStop(items: ListItem[]): OneStopPlan {
    const assign: Record<string, StoreId> = {};
    const itemIds: string[] = [];
    let total = 0;
    let dropped: string[] = [];
    items.forEach((it) => {
      const c = byId(it.itemId);
      if (c.at.mega) {
        assign[it.itemId] = 'mega';
        itemIds.push(it.itemId);
        total += c.at.mega.price;
      } else {
        dropped.push(c.name.toLowerCase());
      }
    });
    return {
      mode: 'onestop',
      store: { store: 'mega', itemIds, total },
      assign,
      droppedNote: dropped.length ? `${dropped.join(', ')} not carried — kept for next trip` : undefined,
      minutes: 6 + itemIds.length * 1.4,
    };
  },
};
