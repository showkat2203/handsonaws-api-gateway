import { StoreId } from './stores';

export interface Availability {
  zone: string;
  price: number;
}

export interface Deal {
  store: StoreId;
  label: string;
  saveAmt: number;
  dealPrice: number;
}

export interface CatalogItem {
  id: string;
  name: string;
  emoji: string;
  aliases: string[];
  intents?: string[];
  hint?: string;
  searchOnly?: boolean;
  deal?: Deal;
  demandNote?: Partial<Record<StoreId, string>>;
  at: Partial<Record<StoreId, Availability>>;
}

export const CATALOG: CatalogItem[] = [
  {
    id: 'milk',
    name: 'Milk 2L',
    emoji: '🥛',
    aliases: ['whole milk'],
    at: {
      save: { zone: 'DAIRY', price: 2.6 },
      miller: { zone: 'COOLER', price: 2.95 },
      mega: { zone: 'DAIRY', price: 2.7 },
    },
  },
  {
    id: 'eggs',
    name: 'Eggs ×12',
    emoji: '🥚',
    aliases: [],
    at: {
      save: { zone: 'DAIRY', price: 3.2 },
      mega: { zone: 'DAIRY', price: 3.35 },
    },
  },
  {
    id: 'sourdough',
    name: 'Sourdough loaf',
    emoji: '🍞',
    aliases: ['bread'],
    at: {
      miller: { zone: 'BAKERY', price: 4.5 },
    },
  },
  {
    id: 'soap',
    name: 'Dish soap',
    emoji: '🧴',
    aliases: [],
    at: {
      save: { zone: 'A3', price: 2.99 },
      mega: { zone: 'A3', price: 3.1 },
    },
  },
  {
    id: 'tomatoes',
    name: 'Tomatoes 1lb',
    emoji: '🍅',
    aliases: [],
    at: {
      save: { zone: 'PRODUCE', price: 2.8 },
      miller: { zone: 'PRODUCE', price: 2.5 },
      mega: { zone: 'PRODUCE', price: 2.6 },
    },
  },
  {
    id: 'penne',
    name: 'Penne',
    emoji: '🍝',
    aliases: ['pasta'],
    at: {
      save: { zone: 'A5', price: 1.89 },
      mega: { zone: 'A5', price: 1.95 },
    },
  },
  {
    id: 'sauce',
    name: 'Pasta sauce',
    emoji: '🥫',
    aliases: ['marinara'],
    hint: 'right next to the penne',
    deal: { store: 'save', label: 'Store-brand −30%', saveAmt: 1.4, dealPrice: 2.09 },
    at: {
      save: { zone: 'A5', price: 3.49 },
      mega: { zone: 'A5', price: 3.3 },
    },
  },
  {
    id: 'basil',
    name: 'Fresh basil',
    emoji: '🌿',
    aliases: [],
    at: {
      miller: { zone: 'PRODUCE', price: 2.49 },
    },
  },
  {
    id: 'coffee',
    name: 'Ground coffee',
    emoji: '☕',
    aliases: [],
    at: {
      save: { zone: 'A7', price: 8.99 },
      mega: { zone: 'A7', price: 8.49 },
    },
  },
  {
    id: 'yogurt',
    name: 'Yogurt',
    emoji: '🥣',
    aliases: ['curd'],
    at: {
      save: { zone: 'DAIRY', price: 1.99 },
      mega: { zone: 'DAIRY', price: 2.1 },
    },
  },
  {
    id: 'papertowels',
    name: 'Paper towels',
    emoji: '🧻',
    aliases: [],
    at: {
      save: { zone: 'A9', price: 5.99 },
      mega: { zone: 'A9', price: 5.49 },
    },
  },
  {
    id: 'aa',
    name: 'AA batteries ×8',
    emoji: '🔋',
    aliases: ['battery'],
    hint: 'endcap display',
    at: {
      save: { zone: 'A9', price: 5.49 },
      mega: { zone: 'A9', price: 5.2 },
    },
  },
  {
    id: 'ginger',
    name: 'Ginger',
    emoji: '🫚',
    aliases: [],
    at: {
      save: { zone: 'PRODUCE', price: 1.1 },
      miller: { zone: 'PRODUCE', price: 0.9 },
    },
  },
  {
    id: 'keyfob',
    name: 'CR2032 battery',
    emoji: '🔘',
    aliases: ['cr2032', 'coin battery'],
    intents: ['battery for my car key', 'key fob battery'],
    searchOnly: true,
    at: {
      save: { zone: 'A9', price: 4.99 },
    },
  },
  {
    id: 'shelfpaper',
    name: 'Shelf paper',
    emoji: '📄',
    aliases: ['drawer liner'],
    searchOnly: true,
    at: {
      mega: { zone: 'A11', price: 6.99 },
    },
  },
  {
    id: 'oatmilk',
    name: 'Oat milk',
    emoji: '🥛',
    aliases: [],
    searchOnly: true,
    demandNote: { miller: '96 people searched here this month' },
    at: {
      save: { zone: 'DAIRY', price: 3.99 },
      mega: { zone: 'DAIRY', price: 3.89 },
    },
  },
];

export const byId = (id: string): CatalogItem => {
  const item = CATALOG.find((c) => c.id === id);
  if (!item) throw new Error(`Unknown catalog item: ${id}`);
  return item;
};
