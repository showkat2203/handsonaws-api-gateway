import { STORES, StoreId, Point } from '../data/mock/stores';
import { byId } from '../data/mock/catalog';
import { ListItem } from '../state/types';
import { RoutingService } from './RoutingService';

export interface MapStop extends Point {
  n: number;
  itemId: string;
}

export interface FloorMapData {
  zones: { zone: string; label: string; point: Point }[];
  stops: MapStop[];
  trail: Point[];
  you?: Point;
}

/** Evenly spaced dots along a polyline, for the dotted trail on the map peek. */
function dots(points: Point[], per = 7): Point[] {
  const out: Point[] = [];
  for (let i = 0; i < points.length - 1; i++) {
    const a = points[i];
    const b = points[i + 1];
    for (let j = 1; j <= per; j++) {
      out.push({ x: a.x + ((b.x - a.x) * j) / (per + 1), y: a.y + ((b.y - a.y) * j) / (per + 1) });
    }
  }
  return out;
}

export const MapService = {
  build(store: StoreId, items: ListItem[], posZone: string): FloorMapData {
    const s = STORES[store];
    const queue = RoutingService.orderTodo(items, store).slice(0, 6);
    const stops: MapStop[] = queue.map((it, i) => ({
      ...s.coords[byId(it.itemId).at[store]!.zone],
      n: i + 1,
      itemId: it.itemId,
    }));
    const posPoint = s.coords[posZone] ?? s.coords.ENTRANCE;
    const checkout = s.coords.CHECKOUT ?? s.coords.COUNTER;
    const path = [posPoint, ...stops, checkout].filter(Boolean) as Point[];
    return {
      zones: s.zones.map((z) => ({ zone: z, label: z, point: s.coords[z] })),
      stops,
      trail: dots(path),
      you: posPoint,
    };
  },
};
