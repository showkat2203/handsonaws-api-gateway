export interface TemplateGuess {
  need: string;
  zone: string;
  copy: string;
  confidence: string;
}

export interface StoreTemplate {
  type: 'convenience' | 'gas';
  guesses: TemplateGuess[];
}

export const TEMPLATES: Record<'convenience' | 'gas', StoreTemplate> = {
  convenience: {
    type: 'convenience',
    guesses: [
      { need: 'chargers', zone: 'COUNTER', copy: 'usually on the counter rack', confidence: 'right ~8 in 10' },
      { need: 'cold drinks', zone: 'COOLER', copy: 'usually back-wall coolers', confidence: 'right ~8 in 10' },
      { need: 'restroom', zone: 'PANTRY', copy: 'usually the back corridor', confidence: 'right ~7 in 10' },
      { need: 'snacks', zone: 'PANTRY', copy: 'usually center racks', confidence: 'right ~9 in 10' },
    ],
  },
  gas: {
    type: 'gas',
    guesses: [
      { need: 'chargers', zone: 'COUNTER', copy: 'usually on the counter rack', confidence: 'right ~8 in 10' },
      { need: 'cold drinks', zone: 'COOLER', copy: 'usually back-wall coolers', confidence: 'right ~8 in 10' },
      { need: 'restroom', zone: 'PANTRY', copy: 'usually the back corridor', confidence: 'right ~7 in 10' },
      { need: 'snacks', zone: 'PANTRY', copy: 'usually center racks', confidence: 'right ~9 in 10' },
    ],
  },
};
