export type Person = 'you' | 'emma';

export const HOUSEHOLD: { id: Person; name: string }[] = [
  { id: 'you', name: 'You' },
  { id: 'emma', name: 'Emma' },
];

export const START_LIST: [string, Person][] = [
  ['milk', 'you'],
  ['eggs', 'you'],
  ['sourdough', 'emma'],
  ['soap', 'you'],
  ['tomatoes', 'you'],
  ['penne', 'you'],
  ['sauce', 'you'],
  ['basil', 'emma'],
  ['coffee', 'you'],
  ['yogurt', 'emma'],
  ['papertowels', 'you'],
  ['aa', 'you'],
];
