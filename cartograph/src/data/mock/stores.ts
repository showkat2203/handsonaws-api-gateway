export type StoreId = 'save' | 'miller' | 'mega' | 'riverside' | 'shell' | 'quickmart45';

export type StoreKind = 'mapped' | 'unmapped' | 'template';
export type StoreType = 'supermarket' | 'indie' | 'big-box' | 'convenience' | 'gas';

export interface Point {
  x: number;
  y: number;
}

export interface Store {
  id: StoreId;
  name: string;
  dist: string;
  type: StoreType;
  kind: StoreKind;
  zones: string[];
  coords: Record<string, Point>;
  freshness: string;
}

export const ZONE_LABEL: Record<string, string> = {
  PRODUCE: 'Produce',
  A3: 'Aisle 3',
  A5: 'Aisle 5',
  A7: 'Aisle 7',
  A9: 'Aisle 9',
  A11: 'Aisle 11',
  DAIRY: 'Dairy · stays cold, grab last',
  BAKERY: 'Bakery',
  PANTRY: 'Pantry',
  COOLER: 'Cooler · stays cold, grab last',
  COUNTER: 'Counter',
  ENTRANCE: 'Entrance',
  CHECKOUT: 'Checkout',
};

export const STORES: Record<StoreId, Store> = {
  save: {
    id: 'save',
    name: 'Save-Mart',
    dist: '0.6 mi',
    type: 'supermarket',
    kind: 'mapped',
    zones: ['PRODUCE', 'A3', 'A5', 'A7', 'A9', 'DAIRY'],
    freshness: '● fresh map · deals active',
    coords: {
      ENTRANCE: { x: 228, y: 300 },
      PRODUCE: { x: 52, y: 38 },
      A3: { x: 84, y: 150 },
      A5: { x: 126, y: 150 },
      A7: { x: 168, y: 150 },
      A9: { x: 210, y: 150 },
      DAIRY: { x: 135, y: 252 },
      CHECKOUT: { x: 66, y: 296 },
    },
  },
  miller: {
    id: 'miller',
    name: "Miller's Corner Market",
    dist: '0.4 mi',
    type: 'indie',
    kind: 'mapped',
    zones: ['PRODUCE', 'BAKERY', 'PANTRY', 'COOLER', 'COUNTER'],
    freshness: '● community mapped · confirmed 2 days ago',
    coords: {
      ENTRANCE: { x: 238, y: 288 },
      PRODUCE: { x: 55, y: 40 },
      BAKERY: { x: 34, y: 150 },
      PANTRY: { x: 138, y: 150 },
      COOLER: { x: 200, y: 190 },
      COUNTER: { x: 70, y: 288 },
    },
  },
  mega: {
    id: 'mega',
    name: 'MegaMart',
    dist: '2.1 mi',
    type: 'big-box',
    kind: 'template',
    zones: ['PRODUCE', 'A3', 'A5', 'A7', 'A9', 'A11', 'DAIRY'],
    freshness: '◌ layout template · unverified',
    coords: {
      ENTRANCE: { x: 228, y: 300 },
      PRODUCE: { x: 52, y: 38 },
      A3: { x: 70, y: 150 },
      A5: { x: 110, y: 150 },
      A7: { x: 150, y: 150 },
      A9: { x: 190, y: 150 },
      A11: { x: 226, y: 150 },
      DAIRY: { x: 135, y: 252 },
      CHECKOUT: { x: 66, y: 296 },
    },
  },
  riverside: {
    id: 'riverside',
    name: 'Riverside QuickMart',
    dist: '0.2 mi',
    type: 'convenience',
    kind: 'unmapped',
    zones: ['COUNTER', 'COOLER', 'PANTRY'],
    freshness: '◌ not mapped — using typical layout',
    coords: {
      ENTRANCE: { x: 130, y: 300 },
      COUNTER: { x: 130, y: 260 },
      COOLER: { x: 210, y: 150 },
      PANTRY: { x: 60, y: 150 },
    },
  },
  shell: {
    id: 'shell',
    name: 'Shell Travel Plaza',
    dist: 'Exit 42',
    type: 'gas',
    kind: 'template',
    zones: ['COUNTER', 'COOLER', 'PANTRY'],
    freshness: '◌ partial template',
    coords: {
      ENTRANCE: { x: 130, y: 300 },
      COUNTER: { x: 130, y: 260 },
      COOLER: { x: 210, y: 150 },
      PANTRY: { x: 60, y: 150 },
    },
  },
  quickmart45: {
    id: 'quickmart45',
    name: 'QuickMart + Dunkin',
    dist: 'Exit 45',
    type: 'convenience',
    kind: 'unmapped',
    zones: ['COUNTER', 'COOLER', 'PANTRY'],
    freshness: '◌ not mapped',
    coords: {
      ENTRANCE: { x: 130, y: 300 },
      COUNTER: { x: 130, y: 260 },
      COOLER: { x: 210, y: 150 },
      PANTRY: { x: 60, y: 150 },
    },
  },
};
