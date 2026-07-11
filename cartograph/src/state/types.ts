import { StoreId } from '../data/mock/stores';
import { Person } from '../data/mock/household';

export type ItemStatus = 'todo' | 'got';

export interface ListItem {
  key: string;
  itemId: string;
  addedBy: Person;
  status: ItemStatus;
  note?: string;
  /** Set once a trip plan has assigned this item to a store. */
  assignedStore?: StoreId;
}

export type ContextMode = 'mapped' | 'unmapped' | 'travel' | 'idle';

export interface AppContext {
  mode: ContextMode;
  storeId?: StoreId;
}

export type TripMode = 'best' | 'onestop';

export interface Trip {
  stores: StoreId[];
  storeIndex: number;
  picked: number;
  swapSavings: number;
  confirms: number;
  startedAt: number;
}

export interface TripSummary {
  minutes: number;
  moneySaved: number;
  got: number;
  total: number;
  confirms: number;
  swapped: number;
}
