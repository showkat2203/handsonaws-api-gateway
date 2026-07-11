import { StoreId } from './stores';

export interface RoadStop {
  storeId: StoreId;
  exit: string;
  minutes: number;
  needs: string[];
  unknownNeeds?: string[];
}

export const ROAD_STOPS: RoadStop[] = [
  {
    storeId: 'shell',
    exit: 'EXIT 42',
    minutes: 4,
    needs: ['coffee', 'charger', 'restroom', 'gas', 'snacks'],
  },
  {
    storeId: 'quickmart45',
    exit: 'EXIT 45',
    minutes: 9,
    needs: ['coffee', 'snacks'],
    unknownNeeds: ['charger'],
  },
];
