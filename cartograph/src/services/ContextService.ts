import { AppContext } from '../state/types';
import { ROAD_STOPS, RoadStop } from '../data/mock/roadStops';

export interface SimulatorOption {
  key: string;
  label: string;
  context: AppContext;
}

export const SIMULATOR_OPTIONS: SimulatorOption[] = [
  { key: 'save', label: 'At Save-Mart (mapped)', context: { mode: 'mapped', storeId: 'save' } },
  { key: 'riverside', label: 'At Riverside QuickMart (unmapped)', context: { mode: 'unmapped', storeId: 'riverside' } },
  { key: 'travel', label: 'Driving on I-80', context: { mode: 'travel' } },
  { key: 'idle', label: 'At home', context: { mode: 'idle' } },
];

export const ContextService = {
  options(): SimulatorOption[] {
    return SIMULATOR_OPTIONS;
  },

  /** Best matching exit for the selected travel needs, plus one dimmed alternative. */
  bestStops(needs: string[]): { best: RoadStop; matchCount: number; alt?: RoadStop } {
    const scored = ROAD_STOPS.map((stop) => ({
      stop,
      matchCount: needs.filter((n) => stop.needs.includes(n)).length,
    })).sort((a, b) => b.matchCount - a.matchCount);
    return { best: scored[0].stop, matchCount: scored[0].matchCount, alt: scored[1]?.stop };
  },
};
