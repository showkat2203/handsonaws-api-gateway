import { STORES, StoreId } from '../data/mock/stores';
import { byId } from '../data/mock/catalog';
import { ListItem } from '../state/types';

export const RoutingService = {
  zoneIndex(store: StoreId, zone: string): number {
    return STORES[store].zones.indexOf(zone);
  },

  /** Remaining (todo) items for a store, in walking order. Dairy/Cooler (cold) sorts last. */
  orderTodo(items: ListItem[], store: StoreId): ListItem[] {
    return items
      .filter((it) => it.status === 'todo' && it.assignedStore === store)
      .slice()
      .sort((a, b) => {
        const za = byId(a.itemId).at[store]!.zone;
        const zb = byId(b.itemId).at[store]!.zone;
        const cold = (z: string) => (z === 'DAIRY' || z === 'COOLER' ? 1 : 0);
        if (cold(za) !== cold(zb)) return cold(za) - cold(zb);
        return this.zoneIndex(store, za) - this.zoneIndex(store, zb);
      });
  },

  /** Group ordered items by zone, in walking order, for the zone-header UI. */
  groupByZone(items: ListItem[], store: StoreId): { zone: string; items: ListItem[] }[] {
    const ordered = this.orderTodo(items, store);
    const groups: { zone: string; items: ListItem[] }[] = [];
    ordered.forEach((it) => {
      const zone = byId(it.itemId).at[store]!.zone;
      const last = groups[groups.length - 1];
      if (last && last.zone === zone) last.items.push(it);
      else groups.push({ zone, items: [it] });
    });
    return groups;
  },

  /** Rough "N steps ahead" phrasing for voice/position-aware answers. */
  stepsAhead(store: StoreId, fromZone: string, toZone: string): number {
    const diff = Math.abs(this.zoneIndex(store, toZone) - this.zoneIndex(store, fromZone));
    return Math.max(10, diff * 20);
  },
};
