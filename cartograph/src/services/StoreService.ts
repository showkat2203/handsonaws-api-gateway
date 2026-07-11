import { STORES, Store, StoreId } from '../data/mock/stores';

export const StoreService = {
  get(id: StoreId): Store {
    return STORES[id];
  },

  all(): Store[] {
    return Object.values(STORES);
  },

  /** Nearby stores for HERE / idle, closest first. */
  nearby(): Store[] {
    return [STORES.riverside, STORES.miller, STORES.save, STORES.mega].sort(
      (a, b) => parseFloat(a.dist) - parseFloat(b.dist)
    );
  },
};
