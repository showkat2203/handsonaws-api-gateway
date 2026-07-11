import * as Speech from 'expo-speech';
import { ZONE_LABEL, StoreId } from '../data/mock/stores';
import { CatalogService } from './CatalogService';
import { RoutingService } from './RoutingService';

export const PHRASE_CHIPS = ['battery for my car key', 'shelf paper', 'fresh basil'];

export interface VoiceAnswer {
  found: boolean;
  itemId?: string;
  text: string;
}

export const VoiceService = {
  /**
   * Resolves a spoken/typed phrase to a catalog answer. When a store + position are
   * given (i.e. inside Store Mode), the phrasing includes distance/direction on-route.
   */
  ask(phrase: string, opts?: { store?: StoreId; posZone?: string }): VoiceAnswer {
    const item = CatalogService.resolveVoice(phrase);
    if (!item) {
      return { found: false, text: `No match yet for "${phrase}" — try another item.` };
    }
    const store = opts?.store;
    const avail = store ? item.at[store] : Object.values(item.at)[0];
    if (!avail || !store) {
      const anyStore = Object.keys(item.at)[0] as StoreId | undefined;
      const anyAvail = anyStore ? item.at[anyStore] : undefined;
      return {
        found: true,
        itemId: item.id,
        text: anyAvail ? `${ZONE_LABEL[anyAvail.zone]}` : `${item.name} — not carried nearby`,
      };
    }
    const zoneLabel = ZONE_LABEL[avail.zone].toUpperCase();
    if (opts?.posZone) {
      const steps = RoutingService.stepsAhead(store, opts.posZone, avail.zone);
      return {
        found: true,
        itemId: item.id,
        text: `${zoneLabel} — ${steps} steps ahead, on your route`,
      };
    }
    return { found: true, itemId: item.id, text: zoneLabel };
  },

  speak(text: string) {
    Speech.speak(text, { rate: 1.0, pitch: 1.0 });
  },

  stop() {
    Speech.stop();
  },
};
