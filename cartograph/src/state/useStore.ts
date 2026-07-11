import { create } from 'zustand';
import { CatalogService } from '../services/CatalogService';
import { TripService, TripPlan, OneStopPlan } from '../services/TripService';
import { RoutingService } from '../services/RoutingService';
import { byId } from '../data/mock/catalog';
import { START_LIST, Person } from '../data/mock/household';
import { STORES, StoreId } from '../data/mock/stores';
import { AppContext, ListItem, Trip, TripSummary, TripMode } from './types';

interface AppState {
  list: ListItem[];
  context: AppContext;
  planBest: TripPlan | null;
  planOneStop: OneStopPlan | null;
  tripMode: TripMode;
  trip: Trip | null;
  tripPhase: 'idle' | 'shopping' | 'drive' | 'summary';
  summary: TripSummary | null;
  posZone: string;
  toast: string | null;
  emmaGingerFired: boolean;
  confirmedNeeds: Record<string, string[]>;
  shelfConfirms: number;
  stockScanCount: number;
  lang: string;

  say: (msg: string) => void;
  clearToast: () => void;

  addItem: (itemId: string, addedBy?: Person) => void;
  addByName: (text: string) => boolean;
  parsePaste: (text: string) => number;

  setContext: (ctx: AppContext) => void;
  confirmNeed: (storeId: string, need: string) => void;

  makePlanBest: () => void;
  makePlanOneStop: () => void;
  setTripMode: (m: TripMode) => void;

  startTrip: (mode: TripMode) => void;
  startSingleStoreTrip: (storeId: StoreId) => void;
  gotIt: (key: string) => void;
  applyDeal: (key: string) => void;
  rescueSwap: (key: string) => void;
  rescueOther: (key: string) => void;
  rescueStaff: (key: string) => void;
  arrivedNextStore: () => void;
  setPosZone: (zone: string) => void;
  finishSummary: () => void;

  pinStockScan: () => void;
  setLang: (l: string) => void;
  resetDemo: () => void;
}

function freshList(): ListItem[] {
  return START_LIST.map(([id, who], i) => ({ key: `${i}`, itemId: id, addedBy: who, status: 'todo' as const }));
}

function advance(get: () => AppState, set: (partial: Partial<AppState>) => void) {
  const { list, trip } = get();
  if (!trip) return;
  const curStore = trip.stores[trip.storeIndex];
  const queue = RoutingService.orderTodo(list, curStore);
  if (queue.length > 0) return;
  if (trip.storeIndex < trip.stores.length - 1) {
    set({ tripPhase: 'drive' });
  } else {
    const gotCount = list.filter((l) => l.status === 'got').length;
    const swapped = list.filter((l) => l.note && l.note.toLowerCase().includes('sav')).length;
    const planSavings = get().tripMode === 'best' ? get().planBest?.savings ?? 0 : 0;
    set({
      tripPhase: 'summary',
      summary: {
        minutes: Math.round(trip.stores.length * 15 + 4),
        moneySaved: planSavings + trip.swapSavings,
        got: gotCount,
        total: list.length,
        confirms: trip.confirms,
        swapped,
      },
    });
  }
}

export const useStore = create<AppState>((set, get) => ({
  list: freshList(),
  context: { mode: 'mapped', storeId: 'save' },
  planBest: null,
  planOneStop: null,
  tripMode: 'best',
  trip: null,
  tripPhase: 'idle',
  summary: null,
  posZone: 'ENTRANCE',
  toast: null,
  emmaGingerFired: false,
  confirmedNeeds: {},
  shelfConfirms: 21,
  stockScanCount: 0,
  lang: 'EN',

  say: (msg) => {
    set({ toast: msg });
    setTimeout(() => {
      if (get().toast === msg) set({ toast: null });
    }, 3200);
  },
  clearToast: () => set({ toast: null }),

  addItem: (itemId, addedBy = 'you') => {
    const { list } = get();
    if (list.some((l) => l.itemId === itemId && l.status === 'todo')) {
      get().say('Already on the list ✓');
      return;
    }
    set({ list: [...list, { key: `${itemId}-${Date.now()}-${Math.random()}`, itemId, addedBy, status: 'todo' }] });
  },

  addByName: (text) => {
    const hit = CatalogService.search(text)[0];
    if (hit) {
      get().addItem(hit.id);
      get().say(`Added ${hit.name}`);
      return true;
    }
    get().say('Not in demo catalog — try milk, basil, shelf paper…');
    return false;
  },

  parsePaste: (text) => {
    const t = text.toLowerCase();
    let n = 0;
    CatalogService.all().forEach((c) => {
      if (c.searchOnly) return;
      const nameHit = t.includes(c.name.split(' ')[0].toLowerCase());
      const aliasHit = c.aliases.some((a) => t.includes(a.toLowerCase()));
      if ((nameHit || aliasHit) && !get().list.some((l) => l.itemId === c.id && l.status === 'todo')) {
        get().addItem(c.id, 'emma');
        n += 1;
      }
    });
    get().say(`✦ Parsed ${n} new item${n === 1 ? '' : 's'} from the text`);
    return n;
  },

  setContext: (ctx) => set({ context: ctx }),

  confirmNeed: (storeId, need) => {
    const cur = get().confirmedNeeds[storeId] ?? [];
    if (cur.includes(need)) return;
    set({ confirmedNeeds: { ...get().confirmedNeeds, [storeId]: [...cur, need] } });
    set({ shelfConfirms: get().shelfConfirms + 1 });
  },

  makePlanBest: () => {
    const todo = get().list.filter((l) => l.status === 'todo');
    set({ planBest: TripService.planBest(todo), tripMode: 'best' });
  },
  makePlanOneStop: () => {
    const todo = get().list.filter((l) => l.status === 'todo');
    set({ planOneStop: TripService.planOneStop(todo) });
  },
  setTripMode: (m) => {
    set({ tripMode: m });
    if (m === 'onestop' && !get().planOneStop) get().makePlanOneStop();
  },

  startTrip: (mode) => {
    const assign = mode === 'best' ? get().planBest?.assign : get().planOneStop?.assign;
    if (!assign) return;
    const stores: StoreId[] = mode === 'best' ? ['save', 'miller'] : ['mega'];
    const list = get().list.map((l) => (assign[l.itemId] ? { ...l, assignedStore: assign[l.itemId] } : l));
    set({
      list,
      tripMode: mode,
      trip: { stores, storeIndex: 0, picked: 0, swapSavings: 0, confirms: 0, startedAt: Date.now() },
      tripPhase: 'shopping',
      posZone: 'ENTRANCE',
      emmaGingerFired: false,
    });
  },

  startSingleStoreTrip: (storeId) => {
    const list = get().list.map((l) =>
      l.status === 'todo' && byId(l.itemId).at[storeId] ? { ...l, assignedStore: storeId } : l
    );
    set({
      list,
      trip: { stores: [storeId], storeIndex: 0, picked: 0, swapSavings: 0, confirms: 0, startedAt: Date.now() },
      tripPhase: 'shopping',
      posZone: 'ENTRANCE',
      emmaGingerFired: false,
    });
  },

  gotIt: (key) => {
    const { list, trip } = get();
    if (!trip) return;
    const curStore = trip.stores[trip.storeIndex];
    const target = list.find((l) => l.key === key);
    if (!target) return;
    const zone = byId(target.itemId).at[curStore]!.zone;
    const nextList = list.map((l) => (l.key === key ? { ...l, status: 'got' as const } : l));
    const picked = trip.picked + 1;
    set({
      list: nextList,
      posZone: zone,
      trip: { ...trip, picked, confirms: trip.confirms + 1 },
      shelfConfirms: get().shelfConfirms + 1,
    });

    if (picked === 2 && !get().emmaGingerFired && get().tripMode === 'best' && trip.stores.includes('miller')) {
      set({ emmaGingerFired: true });
      setTimeout(() => {
        const withGinger: ListItem = {
          key: `ginger-${Date.now()}`,
          itemId: 'ginger',
          addedBy: 'emma',
          status: 'todo',
          assignedStore: 'miller',
          note: 'added mid-trip',
        };
        set({ list: [...get().list, withGinger] });
        get().say("📱 Emma added Ginger — slotted into Miller's · Produce");
      }, 1100);
    }

    advance(get, set);
  },

  applyDeal: (key) => {
    const { list, trip } = get();
    if (!trip) return;
    const curStore = trip.stores[trip.storeIndex];
    const target = list.find((l) => l.key === key);
    if (!target) return;
    const deal = byId(target.itemId).deal!;
    const zone = byId(target.itemId).at[curStore]!.zone;
    const nextList = list.map((l) =>
      l.key === key ? { ...l, status: 'got' as const, note: `swapped to store brand −$${deal.saveAmt.toFixed(2)}` } : l
    );
    set({
      list: nextList,
      posZone: zone,
      trip: { ...trip, picked: trip.picked + 1, swapSavings: trip.swapSavings + deal.saveAmt, confirms: trip.confirms + 1 },
      shelfConfirms: get().shelfConfirms + 1,
    });
    get().say(`Swapped & saved $${deal.saveAmt.toFixed(2)} ✓`);
    advance(get, set);
  },

  rescueSwap: (key) => {
    const { list, trip } = get();
    if (!trip) return;
    const target = list.find((l) => l.key === key);
    if (!target) return;
    const c = byId(target.itemId);
    const amt = c.deal?.saveAmt ?? 0.6;
    set({
      list: list.map((l) => (l.key === key ? { ...l, status: 'got' as const, note: `substitute · saved $${amt.toFixed(2)}` } : l)),
      trip: { ...trip, picked: trip.picked + 1, swapSavings: trip.swapSavings + amt },
    });
    get().say('Substitute grabbed ✓');
    advance(get, set);
  },

  rescueOther: (key) => {
    const { list, trip } = get();
    if (!trip) return;
    const target = list.find((l) => l.key === key);
    if (!target) return;
    const curStore = trip.stores[trip.storeIndex];
    const other = trip.stores.find((s) => s !== curStore && byId(target.itemId).at[s]);
    if (!other) return;
    set({
      list: list.map((l) => (l.key === key ? { ...l, assignedStore: other, note: `moved to next stop` } : l)),
    });
    get().say(`Added to ${STORES[other].name} — your next stop ✓`);
    advance(get, set);
  },

  rescueStaff: (key) => {
    const { list, trip } = get();
    if (!trip) return;
    set({
      list: list.map((l) => (l.key === key ? { ...l, status: 'got' as const, note: 'found with staff help ✓' } : l)),
      trip: { ...trip, picked: trip.picked + 1 },
      shelfConfirms: get().shelfConfirms + 1,
    });
    get().say('Shelf flagged for the store — thanks 🌱');
    advance(get, set);
  },

  arrivedNextStore: () => {
    const { trip } = get();
    if (!trip) return;
    set({ trip: { ...trip, storeIndex: trip.storeIndex + 1 }, posZone: 'ENTRANCE', tripPhase: 'shopping' });
  },

  setPosZone: (zone) => set({ posZone: zone }),

  finishSummary: () => set({ trip: null, tripPhase: 'idle', summary: null }),

  pinStockScan: () => set({ stockScanCount: get().stockScanCount + 1 }),
  setLang: (l) => set({ lang: l }),

  resetDemo: () =>
    set({
      list: freshList(),
      planBest: null,
      planOneStop: null,
      tripMode: 'best',
      trip: null,
      tripPhase: 'idle',
      summary: null,
      posZone: 'ENTRANCE',
      emmaGingerFired: false,
      confirmedNeeds: {},
      context: { mode: 'mapped', storeId: 'save' },
    }),
}));
