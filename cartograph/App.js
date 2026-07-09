// ============================================================
// CARTOGRAPH — interactive React Native prototype (Expo)
// Every flow works with mock data. No backend, no extra deps.
// Run: paste this file into snack.expo.dev (App.js) → run on
// your phone with Expo Go. Or: npx create-expo-app, replace App.js.
// ============================================================

import React, { useState, useEffect, useRef } from 'react';
import {
  SafeAreaView, View, Text, TouchableOpacity, ScrollView,
  TextInput, Modal, StyleSheet, StatusBar,
} from 'react-native';

// ---------- BRAND ----------
const C = {
  market: '#1E4D2B', sprout: '#3E8E5A', paper: '#FAFAF7',
  tag: '#FFD23F', ink: '#161613', line: '#D8D8CF',
  card: '#FFFFFF', dim: '#77776B', blue: '#2E7CF6', red: '#7A3B32',
};

// ---------- MOCK CATALOG (the "backend") ----------
// Each item: availability per store => { zone, price }
const STORES = {
  save: { name: 'Save-Mart', dist: '0.6 km', type: 'supermarket' },
  patel: { name: 'Patel Grocery', dist: '0.4 km', type: 'indie' },
  mega: { name: 'MegaMart', dist: '2.1 km', type: 'big-box' },
};

const ZONES = {
  save: ['PRODUCE', 'A3', 'A5', 'A7', 'A9', 'DAIRY'],
  patel: ['PRODUCE', 'RICE', 'DAL', 'OIL', 'SPICE', 'FRIDGE'],
  mega: ['PRODUCE', 'A2', 'A5', 'A7', 'A9', 'A11', 'DAIRY'],
};
const ZONE_LABEL = {
  PRODUCE: 'Produce', A2: 'Aisle 2', A3: 'Aisle 3', A5: 'Aisle 5',
  A7: 'Aisle 7 · Intl', A9: 'Aisle 9', A11: 'Aisle 11',
  DAIRY: 'Dairy · back wall', RICE: 'Rice & flour stacks',
  DAL: 'Dal & pulses', OIL: 'Oil & ghee', SPICE: 'Spice wall',
  FRIDGE: 'Back fridge', COUNTER: 'Counter', ENTRANCE: 'Entrance',
};

// map coordinates (x,y) inside a 260x330 floor box, per store
const COORDS = {
  save: {
    PRODUCE: { x: 52, y: 38 }, A3: { x: 84, y: 150 }, A5: { x: 126, y: 150 },
    A7: { x: 168, y: 150 }, A9: { x: 210, y: 150 }, DAIRY: { x: 135, y: 252 },
    ENTRANCE: { x: 228, y: 300 }, CHECKOUT: { x: 66, y: 296 },
  },
  patel: {
    PRODUCE: { x: 55, y: 40 }, RICE: { x: 34, y: 150 }, DAL: { x: 86, y: 150 },
    OIL: { x: 138, y: 150 }, SPICE: { x: 222, y: 90 }, FRIDGE: { x: 200, y: 190 },
    ENTRANCE: { x: 238, y: 288 }, CHECKOUT: { x: 70, y: 288 },
  },
  mega: {
    PRODUCE: { x: 52, y: 38 }, A2: { x: 70, y: 150 }, A5: { x: 110, y: 150 },
    A7: { x: 150, y: 150 }, A9: { x: 190, y: 150 }, A11: { x: 226, y: 150 },
    DAIRY: { x: 135, y: 252 }, ENTRANCE: { x: 228, y: 300 }, CHECKOUT: { x: 66, y: 296 },
  },
};

const CATALOG = [
  { id: 'tomato', name: 'Tomatoes 1kg', emoji: '🍅', aliases: ['tamatar'], at: { save: ['PRODUCE', 2.8], patel: ['PRODUCE', 2.5], mega: ['PRODUCE', 2.6] } },
  { id: 'soap', name: 'Dish soap', emoji: '🧴', aliases: [], at: { save: ['A3', 2.99], mega: ['A5', 3.1] } },
  { id: 'penne', name: 'Penne 500g', emoji: '🍝', aliases: ['pasta'], at: { save: ['A5', 1.89], mega: ['A2', 1.95] } },
  { id: 'sauce', name: 'Pasta sauce', emoji: '🥫', aliases: ['marinara'], hint: 'right next to the penne', deal: { store: 'save', label: 'Store-brand marinara −30%', saveAmt: 1.4 }, at: { save: ['A5', 3.49], mega: ['A2', 3.3] } },
  { id: 'eggs', name: 'Eggs ×12', emoji: '🥚', aliases: ['ande'], at: { save: ['DAIRY', 3.2], mega: ['DAIRY', 3.35] } },
  { id: 'milk', name: 'Whole milk 2L', emoji: '🥛', aliases: ['doodh'], hint: 'grab last — stays cold', at: { save: ['DAIRY', 2.6], patel: ['FRIDGE', 2.75], mega: ['DAIRY', 2.7] } },
  { id: 'yogurt', name: 'Yogurt 1kg', emoji: '🥣', aliases: ['dahi', 'curd'], at: { save: ['DAIRY', 1.99], mega: ['DAIRY', 2.1] } },
  { id: 'aa', name: 'AA batteries ×8', emoji: '🔋', aliases: ['battery'], hint: 'endcap display', at: { save: ['A9', 5.49], mega: ['A9', 5.2] } },
  { id: 'atta', name: 'Atta 5kg', emoji: '🌾', aliases: ['wheat flour', 'chakki atta'], at: { patel: ['RICE', 7.99], mega: ['A7', 8.99] } },
  { id: 'chutney', name: 'Green chutney', emoji: '🫙', aliases: ['hari chutney'], hint: 'left of the paneer', deal: { store: 'patel', label: 'Fresh batch −20%', saveAmt: 0.7 }, at: { patel: ['FRIDGE', 3.49] } },
  { id: 'ginger', name: 'Ginger 200g', emoji: '🫚', aliases: ['adrak'], at: { save: ['PRODUCE', 1.1], patel: ['PRODUCE', 0.9], mega: ['PRODUCE', 1.0] } },
  // search-only odd items
  { id: 'hing', name: 'Hing (asafoetida)', emoji: '🟡', aliases: ['asafoetida', 'heeng'], searchOnly: true, at: { patel: ['SPICE', 2.49], save: ['A7', 3.79] } },
  { id: 'shelfpaper', name: 'Shelf paper', emoji: '📄', aliases: ['drawer liner'], searchOnly: true, at: { mega: ['A11', 6.99] } },
  { id: 'paneer', name: 'Paneer 1kg', emoji: '🧀', aliases: [], searchOnly: true, at: { patel: ['FRIDGE', 4.5] } },
  { id: 'oatmilk', name: 'Oat milk 1L', emoji: '🥛', aliases: [], searchOnly: true, demandNote: { patel: 96 }, at: { save: ['DAIRY', 3.99], mega: ['DAIRY', 3.89] } },
];
const byId = (id) => CATALOG.find((c) => c.id === id);

// starting household list (item ids + who added)
const START_LIST = [
  ['milk', 'you'], ['atta', 'priya'], ['soap', 'you'], ['tomato', 'you'],
  ['chutney', 'priya'], ['penne', 'you'], ['sauce', 'you'], ['eggs', 'you'],
  ['yogurt', 'priya'], ['aa', 'you'],
];

// ---------- helpers ----------
const money = (n) => '$' + n.toFixed(2);
const zoneIdx = (store, z) => ZONES[store].indexOf(z);

function planBest(list) {
  // assign each todo item to cheapest of save/patel that stocks it
  const assign = {};
  let saveTotal = 0, patelTotal = 0;
  list.forEach((it) => {
    const c = byId(it.id);
    const s = c.at.save, p = c.at.patel;
    let pick = null;
    if (s && p) pick = p[1] <= s[1] ? 'patel' : 'save';
    else if (s) pick = 'save';
    else if (p) pick = 'patel';
    assign[it.id] = pick;
    if (pick === 'save') saveTotal += s[1];
    if (pick === 'patel') patelTotal += p[1];
  });
  // one-stop comparison @ MegaMart
  let megaTotal = 0, megaHas = 0, sameAtBest = 0;
  list.forEach((it) => {
    const c = byId(it.id);
    if (c.at.mega) {
      megaTotal += c.at.mega[1]; megaHas += 1;
      const pick = assign[it.id];
      if (pick) sameAtBest += c.at[pick][1];
    }
  });
  return {
    assign, saveTotal, patelTotal, total: saveTotal + patelTotal,
    megaTotal, megaHas, savings: Math.max(0, megaTotal - sameAtBest),
  };
}

function buildQueue(list, store, assign) {
  return list
    .filter((it) => it.status === 'todo' && assign[it.id] === store)
    .sort((a, b) => zoneIdx(store, byId(a.id).at[store][0]) - zoneIdx(store, byId(b.id).at[store][0]));
}

function searchCatalog(q) {
  const s = q.trim().toLowerCase();
  if (!s) return [];
  return CATALOG.filter((c) =>
    c.name.toLowerCase().includes(s) || c.aliases.some((a) => a.toLowerCase().includes(s))
  );
}

// dotted path between waypoints (for the map)
function dots(points, per = 7) {
  const out = [];
  for (let i = 0; i < points.length - 1; i++) {
    const a = points[i], b = points[i + 1];
    for (let j = 1; j <= per; j++) {
      out.push({ x: a.x + ((b.x - a.x) * j) / (per + 1), y: a.y + ((b.y - a.y) * j) / (per + 1) });
    }
  }
  return out;
}

// ============================================================
// APP
// ============================================================
export default function App() {
  const [tab, setTab] = useState('lists');            // lists | trip | stores | you
  const [list, setList] = useState(START_LIST.map(([id, who], i) => ({ key: i + '', id, addedBy: who, status: 'todo', note: null })));
  const [plan, setPlan] = useState(null);             // result of planBest
  const [mode, setMode] = useState('best');           // best | onestop
  const [trip, setTrip] = useState(null);             // { stores:[...], si, picked, swapSavings, confirms }
  const [storeOpen, setStoreOpen] = useState(false);  // store-mode overlay
  const [rescue, setRescue] = useState(null);         // item in "not here" flow
  const [mapOpen, setMapOpen] = useState(false);
  const [drive, setDrive] = useState(false);          // between-stores interstitial
  const [summary, setSummary] = useState(null);
  const [toast, setToast] = useState(null);
  const [pasteOpen, setPasteOpen] = useState(false);
  const [pasteText, setPasteText] = useState('bring milk, atta, dish soap, tomatoes, that green chutney, pasta + sauce, eggs, yogurt and AA batteries');
  const [addText, setAddText] = useState('');
  const [searchOpen, setSearchOpen] = useState(false);
  const [query, setQuery] = useState('');
  const [posZone, setPosZone] = useState('ENTRANCE');
  const [lang, setLang] = useState('EN');
  const priyaFired = useRef(false);

  const say = (msg) => { setToast(msg); setTimeout(() => setToast(null), 3200); };

  // ----- list actions -----
  const addItem = (id, who = 'you') => {
    if (list.some((l) => l.id === id && l.status === 'todo')) { say('Already on the list ✓'); return; }
    setList((L) => [...L, { key: Math.random() + '', id, addedBy: who, status: 'todo', note: null }]);
  };
  const addByName = () => {
    const q = addText.trim().toLowerCase(); if (!q) return;
    const hit = searchCatalog(q)[0];
    if (hit) { addItem(hit.id); say(`Added ${hit.name}`); }
    else say('Not in demo catalog — try milk, hing, atta…');
    setAddText('');
  };
  const parsePaste = () => {
    let n = 0;
    CATALOG.forEach((c) => {
      const t = pasteText.toLowerCase();
      if (!c.searchOnly && (t.includes(c.id) || t.includes(c.name.split(' ')[0].toLowerCase()) || c.aliases.some((a) => t.includes(a)))) {
        if (!list.some((l) => l.id === c.id && l.status === 'todo')) { addItem(c.id, 'priya'); n++; }
      }
    });
    setPasteOpen(false);
    say(`✦ Parsed ${n} new item${n === 1 ? '' : 's'} from the text`);
  };

  // ----- plan -----
  const makePlan = () => {
    const todo = list.filter((l) => l.status === 'todo');
    setPlan(planBest(todo)); setMode('best'); setTab('trip');
  };

  // ----- trip -----
  const startTrip = () => {
    const stores = mode === 'best' ? ['save', 'patel'] : ['mega'];
    const assign = mode === 'best'
      ? plan.assign
      : Object.fromEntries(list.filter((l) => l.status === 'todo').map((l) => [l.id, byId(l.id).at.mega ? 'mega' : null]));
    setTrip({ stores, si: 0, assign, picked: 0, swapSavings: 0, confirms: 0, start: Date.now() });
    priyaFired.current = false;
    setPosZone('ENTRANCE');
    setStoreOpen(true);
  };

  const curStore = trip ? trip.stores[trip.si] : null;
  const queue = trip ? buildQueue(list, curStore, trip.assign) : [];
  const next = queue[0];
  const nextCat = next ? byId(next.id) : null;
  const dealForNext = nextCat && nextCat.deal && nextCat.deal.store === curStore && next.status === 'todo' ? nextCat.deal : null;

  const advanceIfStoreDone = (L) => {
    const q = buildQueue(L, curStore, trip.assign);
    if (q.length === 0) {
      if (trip.si < trip.stores.length - 1) { setStoreOpen(false); setDrive(true); }
      else finishTrip(L);
    }
  };

  const gotIt = () => {
    const L = list.map((l) => (l.key === next.key ? { ...l, status: 'got' } : l));
    setList(L);
    setPosZone(nextCat.at[curStore][0]);
    const picked = trip.picked + 1;
    setTrip({ ...trip, picked, confirms: trip.confirms + 1 });
    // Priya adds ginger after your 2nd pick
    if (picked === 2 && !priyaFired.current && mode === 'best') {
      priyaFired.current = true;
      setTimeout(() => {
        setList((cur) => [...cur, { key: 'ging' + Math.random(), id: 'ginger', addedBy: 'priya', status: 'todo', note: 'added mid-trip' }]);
        setTrip((t) => ({ ...t, assign: { ...t.assign, ginger: 'patel' } }));
        say('📱 Priya added Ginger — slotted into Patel · Produce (+40s)');
      }, 1100);
    }
    advanceIfStoreDone(L);
  };

  const applyDeal = () => {
    const L = list.map((l) => (l.key === next.key ? { ...l, status: 'got', note: 'swapped to store brand −' + money(dealForNext.saveAmt) } : l));
    setList(L);
    setPosZone(nextCat.at[curStore][0]);
    setTrip({ ...trip, picked: trip.picked + 1, swapSavings: trip.swapSavings + dealForNext.saveAmt, confirms: trip.confirms + 1 });
    say('Swapped & saved ' + money(dealForNext.saveAmt) + ' ✓');
    advanceIfStoreDone(L);
  };

  const rescuePick = (choice) => {
    let L = list;
    if (choice === 'swap') {
      const amt = nextCat.deal ? nextCat.deal.saveAmt : 0.6;
      L = list.map((l) => (l.key === rescue.key ? { ...l, status: 'got', note: 'substitute · saved ' + money(amt) } : l));
      setTrip({ ...trip, picked: trip.picked + 1, swapSavings: trip.swapSavings + amt });
      say('Substitute grabbed ✓');
    } else if (choice === 'other') {
      const other = trip.stores.find((s) => s !== curStore && byId(rescue.id).at[s]);
      L = list.map((l) => (l.key === rescue.key ? { ...l, note: 'moved to ' + STORES[other].name } : l));
      setTrip({ ...trip, assign: { ...trip.assign, [rescue.id]: other } });
      say('Added to ' + STORES[other].name + ' — your next stop ✓');
    } else {
      L = list.map((l) => (l.key === rescue.key ? { ...l, note: 'asked staff' } : l));
      // move to end by marking got? keep todo but requeue at end: bump zone won't change; simply mark got for demo flow
      L = L.map((l) => (l.key === rescue.key ? { ...l, status: 'got', note: 'found with staff help ✓' } : l));
      setTrip({ ...trip, picked: trip.picked + 1 });
    }
    setList(L); setRescue(null);
    say2Flag(L);
  };
  const say2Flag = (L) => { say('Shelf flagged for the store — thanks 🌱'); advanceIfStoreDone(L); };

  const finishTrip = (L) => {
    setStoreOpen(false);
    const got = L.filter((l) => l.status === 'got').length;
    const swapped = L.filter((l) => l.note && l.note.includes('sav')).length;
    setSummary({
      mins: 34, // simulated door-to-door
      moneySaved: (plan ? plan.savings : 0) + trip.swapSavings,
      got, total: L.filter((l) => l.status !== 'todo').length + buildQueue(L, curStore, trip.assign).length,
      confirms: trip.confirms, swapped,
    });
  };

  const resetDemo = () => {
    setList(START_LIST.map(([id, who], i) => ({ key: i + '', id, addedBy: who, status: 'todo', note: null })));
    setPlan(null); setTrip(null); setSummary(null); setTab('lists'); setPosZone('ENTRANCE');
  };

  // ============================================================
  // RENDER
  // ============================================================
  return (
    <SafeAreaView style={st.app}>
      <StatusBar barStyle="light-content" />
      {/* header */}
      <View style={st.header}>
        <Text style={st.hTitle}>{tab === 'lists' ? 'Cartograph' : tab === 'trip' ? 'Your trip plan' : tab === 'stores' ? 'Nearby stores' : 'You'}</Text>
        <Text style={st.hMeta}>{tab === 'trip' && plan ? (mode === 'best' ? '2 stops · best value' : '1 stop') : 'demo · no backend'}</Text>
      </View>

      {/* ---------------- TAB BODIES ---------------- */}
      {tab === 'lists' && (
        <ScrollView style={st.body} contentContainerStyle={{ paddingBottom: 24 }}>
          <View style={st.hero}>
            <Text style={st.heroWho}>HOUSEHOLD · YOU + PRIYA</Text>
            <Text style={st.heroName}>Home list</Text>
            <Text style={st.heroCnt}>
              {list.filter((l) => l.status === 'todo').length} items to get · {list.filter((l) => l.addedBy === 'priya' && l.status === 'todo').length} added by Priya
            </Text>
            <TouchableOpacity style={st.cta} onPress={makePlan}>
              <Text style={st.ctaTx}>Plan trip →</Text>
            </TouchableOpacity>
          </View>

          <View style={st.addRow}>
            <TouchableOpacity style={st.addB} onPress={() => setPasteOpen(true)}><Text style={st.addIc}>📋</Text><Text style={st.addTx}>paste text</Text></TouchableOpacity>
            <TouchableOpacity style={st.addB} onPress={() => { addItem('chutney', 'you'); say('🎤 heard: "green chutney" ✓'); }}><Text style={st.addIc}>🎤</Text><Text style={st.addTx}>speak</Text></TouchableOpacity>
            <View style={[st.addB, { flex: 1.6, flexDirection: 'row', alignItems: 'center', paddingHorizontal: 8 }]}>
              <TextInput value={addText} onChangeText={setAddText} placeholder="type an item…" placeholderTextColor="#9a9a8e" style={st.addInput} onSubmitEditing={addByName} />
              <TouchableOpacity onPress={addByName}><Text style={{ fontSize: 18, color: C.market }}>＋</Text></TouchableOpacity>
            </View>
          </View>

          <TouchableOpacity style={st.searchEntry} onPress={() => setSearchOpen(true)}>
            <Text style={{ color: '#9a9a8e' }}>🔍 Find one thing… “shelf paper”, “hing”</Text>
          </TouchableOpacity>

          {list.filter((l) => l.status === 'todo').map((l) => {
            const c = byId(l.id);
            return (
              <View key={l.key} style={st.itemRow}>
                <Text style={{ fontSize: 16 }}>{c.emoji}</Text>
                <View style={{ flex: 1, marginLeft: 8 }}>
                  <Text style={st.itemName}>{c.name}</Text>
                  {l.note ? <Text style={st.itemNote}>{l.note}</Text> : null}
                </View>
                <Text style={st.itemWho}>{l.addedBy === 'priya' ? 'Priya' : 'you'}</Text>
              </View>
            );
          })}
          {list.some((l) => l.status !== 'todo') && (
            <Text style={st.doneHdr}>picked earlier ↓</Text>
          )}
          {list.filter((l) => l.status !== 'todo').map((l) => (
            <View key={l.key} style={[st.itemRow, { opacity: 0.5 }]}>
              <Text>{byId(l.id).emoji}</Text>
              <Text style={[st.itemName, { marginLeft: 8, textDecorationLine: 'line-through' }]}>{byId(l.id).name}</Text>
            </View>
          ))}
        </ScrollView>
      )}

      {tab === 'trip' && (
        <ScrollView style={st.body} contentContainerStyle={{ paddingBottom: 24 }}>
          {!plan ? (
            <View style={st.empty}><Text style={st.emptyTx}>No plan yet — go to Lists and tap “Plan trip”.</Text></View>
          ) : mode === 'best' ? (
            <>
              <StoreCard n="①" store="save" items={list.filter((l) => l.status === 'todo' && plan.assign[l.id] === 'save')} total={plan.saveTotal} />
              <Text style={st.conn}>↓ 4 min drive</Text>
              <StoreCard n="②" store="patel" items={list.filter((l) => l.status === 'todo' && plan.assign[l.id] === 'patel')} total={plan.patelTotal} why="only nearby store with atta + chutney" />
              <View style={st.totBar}><Text style={st.totTx}>total</Text><Text style={st.totTx}>{money(plan.total)} · 35 min</Text></View>
              <View style={st.savePill}><Text style={st.savePillTx}>⛃ {money(plan.savings)} cheaper than one-stop ({plan.megaHas}/{list.filter((l) => l.status === 'todo').length} @ MegaMart {money(plan.megaTotal)})</Text></View>
              <TouchableOpacity style={st.altOpt} onPress={() => setMode('onestop')}>
                <Text style={st.altTx}>Prefer one stop? MegaMart {plan.megaHas}/{list.filter((l) => l.status === 'todo').length} · {money(plan.megaTotal)} · 23 min — tap to switch</Text>
              </TouchableOpacity>
            </>
          ) : (
            <>
              <StoreCard n="①" store="mega" items={list.filter((l) => l.status === 'todo' && byId(l.id).at.mega)} total={plan.megaTotal} why="green chutney not carried — kept for next trip" />
              <TouchableOpacity style={st.altOpt} onPress={() => setMode('best')}>
                <Text style={st.altTx}>← back to 2-stop plan ({money(plan.total)}, save {money(plan.savings)})</Text>
              </TouchableOpacity>
            </>
          )}
          {plan && (
            <TouchableOpacity style={[st.cta, { marginTop: 14 }]} onPress={startTrip}>
              <Text style={st.ctaTx}>Start trip →</Text>
            </TouchableOpacity>
          )}
        </ScrollView>
      )}

      {tab === 'stores' && (
        <ScrollView style={st.body} contentContainerStyle={{ paddingBottom: 24 }}>
          {Object.keys(STORES).map((k) => (
            <View key={k} style={st.storeInfo}>
              <View style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
                <Text style={st.itemName}>{STORES[k].name}</Text>
                <Text style={st.freshTx}>{k === 'mega' ? '◌ layout template' : '● fresh map'}</Text>
              </View>
              <Text style={st.itemNote}>{STORES[k].dist} · {STORES[k].type} · zones: {ZONES[k].map((z) => ZONE_LABEL[z].split(' ·')[0]).join(', ')}</Text>
              <Text style={[st.itemNote, { color: C.sprout }]}>
                {k === 'save' ? 'aisle data via store · updated this week' : k === 'patel' ? 'community-mapped · confirmed 2 days ago' : 'unverified guess — help confirm on your next visit'}
              </Text>
            </View>
          ))}
        </ScrollView>
      )}

      {tab === 'you' && (
        <ScrollView style={st.body} contentContainerStyle={{ paddingBottom: 24 }}>
          <View style={st.storeInfo}>
            <Text style={st.itemName}>Household</Text>
            <Text style={st.itemNote}>You · Priya  <Text style={{ color: C.sprout }}>＋ invite</Text></Text>
          </View>
          <View style={st.storeInfo}>
            <Text style={st.itemName}>Search languages</Text>
            <View style={{ flexDirection: 'row', marginTop: 6 }}>
              {['EN', 'हिंदी', 'ગુજરાતી', 'தமிழ்'].map((L) => (
                <TouchableOpacity key={L} onPress={() => { setLang(L); say('Search language: ' + L); }} style={[st.langChip, lang === L && st.langOn]}>
                  <Text style={[st.langTx, lang === L && { color: '#fff' }]}>{L}</Text>
                </TouchableOpacity>
              ))}
            </View>
          </View>
          <View style={st.storeInfo}>
            <Text style={st.itemName}>Your contribution</Text>
            <Text style={st.itemNote}>21 shelf locations confirmed · 3 receipts scanned</Text>
          </View>
          <View style={[st.storeInfo, { borderColor: C.tag, backgroundColor: '#FFFDF2' }]}>
            <Text style={st.itemName}>Cartograph Pro</Text>
            <Text style={st.itemNote}>Merge Instacart + Shipt batches into one pick path. Coming for gig pickers.</Text>
          </View>
          <TouchableOpacity style={st.altOpt} onPress={resetDemo}><Text style={st.altTx}>↺ reset demo data</Text></TouchableOpacity>
        </ScrollView>
      )}

      {/* ---------------- TAB BAR ---------------- */}
      <View style={st.tabs}>
        {[['lists', '☰', 'Lists'], ['trip', '🧭', 'Trip'], ['stores', '🏪', 'Stores'], ['you', '◎', 'You']].map(([k, ic, lb]) => (
          <TouchableOpacity key={k} style={st.tb} onPress={() => setTab(k)}>
            <Text style={{ fontSize: 16 }}>{ic}</Text>
            <Text style={[st.tbTx, tab === k && { color: C.market, fontWeight: '700' }]}>{lb}</Text>
          </TouchableOpacity>
        ))}
      </View>

      {/* ---------------- STORE MODE (takeover) ---------------- */}
      <Modal visible={storeOpen} animationType="slide" onRequestClose={() => setStoreOpen(false)}>
        <SafeAreaView style={st.app}>
          <View style={st.header}>
            <Text style={st.hTitle}>{curStore ? STORES[curStore].name : ''}</Text>
            <Text style={st.hMeta}>store mode · {trip ? trip.picked : 0} picked</Text>
          </View>
          <View style={[st.body, { flex: 1 }]}>
            {next ? (
              <>
                <Text style={st.prog}>{queue.length} to go in this store · position: {ZONE_LABEL[posZone] || 'entrance'}</Text>
                <View style={st.nextCard}>
                  <Text style={st.nextLo}>NEXT · {ZONE_LABEL[nextCat.at[curStore][0]].toUpperCase()}</Text>
                  <Text style={st.nextIt}>{nextCat.emoji} {nextCat.name}</Text>
                  {nextCat.hint ? <Text style={st.nextHint}>{nextCat.hint}</Text> : null}
                  <Text style={st.nextPrice}>{money(nextCat.at[curStore][1])}</Text>
                </View>

                {queue.slice(1, 4).map((q) => (
                  <View key={q.key} style={st.qRow}>
                    <Text style={st.qName}>{byId(q.id).emoji} {byId(q.id).name}</Text>
                    <Text style={st.qZone}>{ZONE_LABEL[byId(q.id).at[curStore][0]]}</Text>
                  </View>
                ))}

                {dealForNext && (
                  <TouchableOpacity style={st.deal} onPress={applyDeal}>
                    <Text style={st.dealB}>{dealForNext.label}</Text>
                    <Text style={st.dealS}>two shelves down · save {money(dealForNext.saveAmt)} · tap to swap ✓</Text>
                  </TouchableOpacity>
                )}

                <View style={{ flex: 1 }} />
                <TouchableOpacity style={st.mapBtn} onPress={() => setMapOpen(true)}>
                  <Text style={st.mapBtnTx}>🗺️ peek the map</Text>
                </TouchableOpacity>
                <View style={st.actRow}>
                  <TouchableOpacity style={[st.actB, st.actNot]} onPress={() => setRescue(next)}>
                    <Text style={[st.actTx, { color: C.red }]}>Not here ✗</Text>
                  </TouchableOpacity>
                  <TouchableOpacity style={[st.actB, st.actGot]} onPress={gotIt}>
                    <Text style={[st.actTx, { color: '#fff' }]}>Got it ✓</Text>
                  </TouchableOpacity>
                </View>
                <TouchableOpacity style={st.scanRow} onPress={() => { setPosZone(nextCat.at[curStore][0]); say('📍 Position set: ' + ZONE_LABEL[nextCat.at[curStore][0]]); }}>
                  <Text style={st.scanTx}>🔳 lost? tap = scan nearest shelf QR → set position</Text>
                </TouchableOpacity>
              </>
            ) : (
              <View style={st.empty}><Text style={st.emptyTx}>All picked here ✓</Text></View>
            )}
          </View>
        </SafeAreaView>

        {/* rescue sheet */}
        <Modal visible={!!rescue} transparent animationType="fade" onRequestClose={() => setRescue(null)}>
          <View style={st.dim}>
            <View style={st.sheet}>
              <Text style={st.sheetH}>Shelf’s empty? No problem.</Text>
              <Text style={st.sheetS}>{rescue ? byId(rescue.id).name : ''} · reported just now</Text>
              <TouchableOpacity style={[st.opt, st.optBest]} onPress={() => rescuePick('swap')}>
                <Text style={st.optIc}>🔄</Text>
                <View style={{ flex: 1 }}><Text style={st.optB}>Swap: store-brand version — in stock</Text><Text style={st.optP}>same shelf, two rows down · saves a little too</Text></View>
              </TouchableOpacity>
              {rescue && trip && trip.stores.some((s) => s !== curStore && byId(rescue.id).at[s]) && (
                <TouchableOpacity style={st.opt} onPress={() => rescuePick('other')}>
                  <Text style={st.optIc}>🏪</Text>
                  <View style={{ flex: 1 }}><Text style={st.optB}>{STORES[trip.stores.find((s) => s !== curStore && byId(rescue.id).at[s])].name} has it</Text><Text style={st.optP}>already on your route — move it there ✓</Text></View>
                </TouchableOpacity>
              )}
              <TouchableOpacity style={st.opt} onPress={() => rescuePick('staff')}>
                <Text style={st.optIc}>🙋</Text>
                <View style={{ flex: 1 }}><Text style={st.optB}>Ask staff</Text><Text style={st.optP}>show them this screen</Text></View>
              </TouchableOpacity>
              <Text style={st.fx}>your report quietly flags this shelf for the store — thank you</Text>
            </View>
          </View>
        </Modal>

        {/* map peek */}
        <Modal visible={mapOpen} transparent animationType="slide" onRequestClose={() => setMapOpen(false)}>
          <View style={st.dim}>
            <View style={st.mapSheet}>
              <Text style={st.sheetH}>{curStore ? STORES[curStore].name : ''} · map</Text>
              <FloorMap store={curStore} queue={queue} posZone={posZone} />
              <Text style={st.confTx}>position: {ZONE_LABEL[posZone] || 'entrance'} · from your last ✓ / QR</Text>
              <TouchableOpacity style={[st.cta, { marginTop: 10 }]} onPress={() => setMapOpen(false)}>
                <Text style={st.ctaTx}>⌄ back to list</Text>
              </TouchableOpacity>
            </View>
          </View>
        </Modal>
      </Modal>

      {/* drive interstitial */}
      <Modal visible={drive} transparent animationType="fade">
        <View style={st.dim}>
          <View style={st.sheet}>
            <Text style={st.sheetH}>Store ① done ✓</Text>
            <Text style={st.sheetS}>Drive 4 min → Patel Grocery (0.4 km)</Text>
            <TouchableOpacity style={[st.cta, { marginTop: 12 }]} onPress={() => { setDrive(false); setTrip((t) => ({ ...t, si: t.si + 1 })); setPosZone('ENTRANCE'); setStoreOpen(true); }}>
              <Text style={st.ctaTx}>Arrived →</Text>
            </TouchableOpacity>
          </View>
        </View>
      </Modal>

      {/* summary */}
      <Modal visible={!!summary} animationType="slide" onRequestClose={() => setSummary(null)}>
        <SafeAreaView style={st.app}>
          <View style={st.header}><Text style={st.hTitle}>Trip done 🎉</Text><Text style={st.hMeta}>{mode === 'best' ? 'Save-Mart + Patel' : 'MegaMart'}</Text></View>
          <ScrollView style={st.body}>
            <View style={st.statRow}>
              <View style={st.bigStat}><Text style={st.bigV}>{summary ? summary.mins : 0} min</Text><Text style={st.bigL}>door to door (simulated)</Text></View>
              <View style={st.bigStat}><Text style={st.bigV}>{summary ? money(summary.moneySaved) : ''}</Text><Text style={st.bigL}>saved vs one store + deals</Text></View>
            </View>
            <View style={st.bigStat}><Text style={st.bigV}>{summary ? summary.got : 0} found</Text><Text style={st.bigL}>{summary && summary.swapped ? summary.swapped + ' smart swap(s), Priya approved ✓' : 'clean run ✓'}</Text></View>
            <TouchableOpacity style={st.receipt} onPress={() => say('📸 Receipt scanned — prices updated for next trip ✓')}>
              <Text style={st.optB}>📸 Scan your receipts?</Text>
              <Text style={st.optP}>keeps prices honest for your next trip — 10 sec</Text>
            </TouchableOpacity>
            <View style={st.thanks}><Text style={st.thanksTx}>your ✓s confirmed {summary ? summary.confirms : 0} shelf locations for other shoppers today</Text></View>
            <TouchableOpacity style={[st.cta, { marginTop: 14 }]} onPress={() => { setSummary(null); setTab('lists'); }}>
              <Text style={st.ctaTx}>Done</Text>
            </TouchableOpacity>
          </ScrollView>
        </SafeAreaView>
      </Modal>

      {/* paste modal */}
      <Modal visible={pasteOpen} transparent animationType="fade" onRequestClose={() => setPasteOpen(false)}>
        <View style={st.dim}>
          <View style={st.sheet}>
            <Text style={st.sheetH}>Paste any text — AI parses it</Text>
            <TextInput multiline value={pasteText} onChangeText={setPasteText} style={st.pasteBox} />
            <TouchableOpacity style={[st.cta, { marginTop: 10 }]} onPress={parsePaste}><Text style={st.ctaTx}>✦ Parse into items</Text></TouchableOpacity>
            <TouchableOpacity onPress={() => setPasteOpen(false)}><Text style={st.cancelTx}>cancel</Text></TouchableOpacity>
          </View>
        </View>
      </Modal>

      {/* search overlay */}
      <Modal visible={searchOpen} animationType="slide" onRequestClose={() => setSearchOpen(false)}>
        <SafeAreaView style={st.app}>
          <View style={st.header}><Text style={st.hTitle}>Find one thing</Text><Text style={st.hMeta}>near you · {lang}</Text></View>
          <View style={st.body}>
            <View style={st.sBox}>
              <TextInput autoFocus value={query} onChangeText={setQuery} placeholder='try "hing" or "shelf paper"…' placeholderTextColor="#9a9a8e" style={{ flex: 1, fontSize: 15, color: C.ink }} />
              <TouchableOpacity onPress={() => { setQuery('hing'); }}><Text style={{ fontSize: 16 }}>🎤</Text></TouchableOpacity>
            </View>
            <View style={{ flexDirection: 'row', marginTop: 8 }}>
              {['hing', 'shelf paper', 'paneer', 'oat milk'].map((q) => (
                <TouchableOpacity key={q} style={st.chip} onPress={() => setQuery(q)}><Text style={st.chipTx}>{q}</Text></TouchableOpacity>
              ))}
            </View>
            <ScrollView style={{ marginTop: 10 }}>
              {searchCatalog(query).map((c) => (
                <View key={c.id}>
                  {Object.keys(STORES).map((s) => {
                    const hit = c.at[s];
                    const demand = c.demandNote && c.demandNote[s];
                    if (!hit && !demand) return null;
                    return (
                      <View key={s} style={[st.resl, !hit && { opacity: 0.65 }]}>
                        <View style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
                          <Text style={st.itemName}>{c.emoji} {STORES[s].name} · {STORES[s].dist}</Text>
                          <Text style={st.priceTx}>{hit ? money(hit[1]) : '—'}</Text>
                        </View>
                        {hit ? (
                          <>
                            <Text style={st.whTx}>{ZONE_LABEL[hit[0]].toUpperCase()}</Text>
                            <Text style={st.frTx}>{c.aliases.length ? '= ' + c.aliases[0] + ' · ' : ''}{s === 'patel' ? 'confirmed by a shopper 2 days ago' : s === 'save' ? 'via store data · this week' : 'layout guess — confirm?'}</Text>
                            <TouchableOpacity onPress={() => { addItem(c.id); say('Added to list ✓'); }}><Text style={st.addLink}>＋ add to list</Text></TouchableOpacity>
                          </>
                        ) : (
                          <Text style={st.frTx}>not carried · {demand} people searched here this month</Text>
                        )}
                      </View>
                    );
                  })}
                </View>
              ))}
              {query.trim() !== '' && searchCatalog(query).length === 0 && (
                <View style={st.empty}><Text style={st.emptyTx}>No match in demo catalog — every miss like this trains the alias dictionary.</Text></View>
              )}
            </ScrollView>
            <TouchableOpacity style={[st.cta, { marginTop: 8 }]} onPress={() => setSearchOpen(false)}><Text style={st.ctaTx}>close</Text></TouchableOpacity>
          </View>
        </SafeAreaView>
      </Modal>

      {/* toast */}
      {toast && (
        <View style={st.toast}><Text style={st.toastTx}>{toast}</Text></View>
      )}
    </SafeAreaView>
  );
}

// ---------- sub components ----------
function StoreCard({ n, store, items, total, why }) {
  return (
    <View style={st.stCard}>
      <View style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
        <Text style={st.itemName}>{n} {STORES[store].name}</Text>
        <Text style={st.qZone}>{items.length} items</Text>
      </View>
      <Text style={st.itemNote}>
        {STORES[store].dist} · {money(total)} · {items.map((i) => byId(i.id).name.split(' ')[0]).join(', ')}
      </Text>
      {why ? <Text style={[st.itemNote, { color: C.sprout }]}>{why}</Text> : null}
    </View>
  );
}

function FloorMap({ store, queue, posZone }) {
  if (!store) return null;
  const co = COORDS[store];
  const stops = queue.slice(0, 6).map((q, i) => ({ ...co[byId(q.id).at[store][0]], n: i + 1 }));
  const path = [co[posZone] || co.ENTRANCE, ...stops, co.CHECKOUT].filter(Boolean);
  const trail = dots(path);
  const zonesToDraw = ZONES[store];
  return (
    <View style={st.floor}>
      {zonesToDraw.map((z) => (
        <View key={z} style={[st.zoneBlock, { left: co[z].x - 26, top: co[z].y - 14 }]}>
          <Text style={st.zoneTx}>{ZONE_LABEL[z].split(' ·')[0]}</Text>
        </View>
      ))}
      {trail.map((d, i) => (
        <View key={i} style={[st.trailDot, { left: d.x - 1.5, top: d.y - 1.5 }]} />
      ))}
      {stops.map((s) => (
        <View key={s.n} style={[st.stopDot, { left: s.x - 9, top: s.y - 9 }]}>
          <Text style={st.stopTx}>{s.n}</Text>
        </View>
      ))}
      {co[posZone] && (
        <View style={[st.youDot, { left: co[posZone].x - 6, top: co[posZone].y - 6 }]} />
      )}
      <Text style={st.entrTx}>entrance ↓</Text>
    </View>
  );
}

// ---------- STYLES ----------
const st = StyleSheet.create({
  app: { flex: 1, backgroundColor: C.paper },
  header: { backgroundColor: C.market, paddingHorizontal: 16, paddingVertical: 13, flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  hTitle: { color: '#fff', fontSize: 17, fontWeight: '800' },
  hMeta: { color: '#CFE0CD', fontSize: 10, fontFamily: 'monospace' },
  body: { flex: 1, padding: 13 },

  hero: { backgroundColor: C.card, borderWidth: 1.5, borderColor: C.market, borderRadius: 14, padding: 13 },
  heroWho: { fontSize: 9.5, color: C.sprout, fontFamily: 'monospace', letterSpacing: 1 },
  heroName: { fontSize: 17, fontWeight: '800', marginTop: 2, color: C.ink },
  heroCnt: { fontSize: 11.5, color: C.dim, marginTop: 3 },
  cta: { backgroundColor: C.market, borderRadius: 11, paddingVertical: 12, alignItems: 'center', marginTop: 10 },
  ctaTx: { color: '#fff', fontWeight: '700', fontSize: 14 },

  addRow: { flexDirection: 'row', marginTop: 11, gap: 7 },
  addB: { flex: 1, backgroundColor: C.card, borderWidth: 1, borderColor: C.line, borderRadius: 10, alignItems: 'center', paddingVertical: 8 },
  addIc: { fontSize: 15 }, addTx: { fontSize: 9.5, color: C.dim, marginTop: 1 },
  addInput: { flex: 1, fontSize: 12, color: C.ink, padding: 0 },
  searchEntry: { marginTop: 11, backgroundColor: C.card, borderWidth: 1, borderColor: C.line, borderRadius: 11, padding: 11 },

  itemRow: { flexDirection: 'row', alignItems: 'center', backgroundColor: C.card, borderWidth: 1, borderColor: C.line, borderRadius: 11, padding: 10, marginTop: 7 },
  itemName: { fontSize: 13.5, fontWeight: '600', color: C.ink },
  itemNote: { fontSize: 11, color: C.dim, marginTop: 2 },
  itemWho: { fontSize: 9.5, color: C.sprout, fontFamily: 'monospace' },
  doneHdr: { fontSize: 10, color: '#9a9a8e', fontFamily: 'monospace', marginTop: 14, marginBottom: 2 },

  empty: { padding: 24, alignItems: 'center' },
  emptyTx: { color: C.dim, fontSize: 13, textAlign: 'center' },

  stCard: { backgroundColor: C.card, borderWidth: 1, borderColor: C.line, borderRadius: 13, padding: 12, marginTop: 8 },
  conn: { textAlign: 'center', fontFamily: 'monospace', fontSize: 10, color: '#8a8a7e', paddingVertical: 4 },
  totBar: { backgroundColor: C.market, borderRadius: 12, padding: 12, flexDirection: 'row', justifyContent: 'space-between', marginTop: 8 },
  totTx: { color: '#fff', fontFamily: 'monospace', fontSize: 12 },
  savePill: { backgroundColor: C.tag, borderRadius: 9, padding: 9, marginTop: 8 },
  savePillTx: { fontSize: 11.5, fontWeight: '700', color: C.ink },
  altOpt: { borderWidth: 1, borderStyle: 'dashed', borderColor: '#B08900', borderRadius: 10, padding: 9, marginTop: 8, backgroundColor: C.card },
  altTx: { fontSize: 11.5, color: '#6a5a10' },

  storeInfo: { backgroundColor: C.card, borderWidth: 1, borderColor: C.line, borderRadius: 12, padding: 12, marginTop: 8 },
  freshTx: { fontSize: 10, color: C.sprout, fontFamily: 'monospace' },
  langChip: { borderWidth: 1, borderColor: C.line, borderRadius: 999, paddingHorizontal: 10, paddingVertical: 4, marginRight: 6, backgroundColor: C.card },
  langOn: { backgroundColor: C.market, borderColor: C.market },
  langTx: { fontSize: 11, color: C.ink },

  tabs: { flexDirection: 'row', borderTopWidth: 1, borderColor: C.line, backgroundColor: '#fff' },
  tb: { flex: 1, alignItems: 'center', paddingVertical: 7 },
  tbTx: { fontSize: 9, color: '#8a8a7e', fontFamily: 'monospace' },

  prog: { fontSize: 10.5, color: '#8a8a7e', fontFamily: 'monospace' },
  nextCard: { backgroundColor: C.card, borderWidth: 1.5, borderColor: C.market, borderRadius: 15, padding: 14, marginTop: 8 },
  nextLo: { fontSize: 10, color: C.sprout, fontFamily: 'monospace', letterSpacing: 0.5 },
  nextIt: { fontSize: 21, fontWeight: '800', color: C.ink, marginTop: 3 },
  nextHint: { fontSize: 11.5, color: C.dim, marginTop: 2 },
  nextPrice: { fontSize: 12, fontFamily: 'monospace', color: C.market, marginTop: 6 },
  qRow: { flexDirection: 'row', justifyContent: 'space-between', paddingVertical: 6, borderBottomWidth: 1, borderStyle: 'dashed', borderColor: C.line, marginTop: 2 },
  qName: { fontSize: 12, color: '#55554d' },
  qZone: { fontSize: 10, fontFamily: 'monospace', color: C.sprout },
  deal: { backgroundColor: C.tag, borderRadius: 11, padding: 10, marginTop: 10 },
  dealB: { fontSize: 12, fontWeight: '700', color: C.ink },
  dealS: { fontSize: 10.5, color: '#5a4b00', marginTop: 2 },
  mapBtn: { backgroundColor: C.card, borderWidth: 1, borderColor: C.line, borderRadius: 10, padding: 9, alignItems: 'center' },
  mapBtnTx: { fontSize: 12, color: C.ink },
  actRow: { flexDirection: 'row', gap: 8, marginTop: 8 },
  actB: { flex: 1, borderRadius: 12, paddingVertical: 14, alignItems: 'center' },
  actGot: { backgroundColor: C.market },
  actNot: { backgroundColor: C.card, borderWidth: 1.5, borderColor: C.line },
  actTx: { fontWeight: '800', fontSize: 14 },
  scanRow: { marginTop: 8, backgroundColor: C.card, borderWidth: 1, borderColor: C.line, borderRadius: 10, padding: 8, alignItems: 'center' },
  scanTx: { fontSize: 10.5, color: '#55554d' },

  dim: { flex: 1, backgroundColor: 'rgba(20,25,18,0.5)', justifyContent: 'flex-end' },
  sheet: { backgroundColor: '#fff', borderTopLeftRadius: 18, borderTopRightRadius: 18, padding: 15 },
  sheetH: { fontSize: 15.5, fontWeight: '800', color: C.ink },
  sheetS: { fontSize: 11, color: C.dim, marginTop: 2 },
  opt: { flexDirection: 'row', alignItems: 'center', borderWidth: 1, borderColor: C.line, borderRadius: 11, padding: 11, marginTop: 9, gap: 10 },
  optBest: { borderColor: C.market, backgroundColor: '#F3F8F1' },
  optIc: { fontSize: 16 },
  optB: { fontSize: 12.5, fontWeight: '700', color: C.ink },
  optP: { fontSize: 10.5, color: C.dim },
  fx: { fontSize: 9, fontFamily: 'monospace', color: '#8a8a7e', textAlign: 'center', marginTop: 10 },

  mapSheet: { backgroundColor: '#fff', borderTopLeftRadius: 18, borderTopRightRadius: 18, padding: 15 },
  floor: { height: 330, backgroundColor: '#FBFBF8', borderWidth: 1, borderColor: '#C9CCC1', borderRadius: 12, marginTop: 10, overflow: 'hidden' },
  zoneBlock: { position: 'absolute', width: 52, height: 28, backgroundColor: '#DDEAE0', borderRadius: 5, alignItems: 'center', justifyContent: 'center', padding: 1 },
  zoneTx: { fontSize: 6.5, fontFamily: 'monospace', color: '#33452F', textAlign: 'center' },
  trailDot: { position: 'absolute', width: 3, height: 3, borderRadius: 2, backgroundColor: C.sprout },
  stopDot: { position: 'absolute', width: 18, height: 18, borderRadius: 9, backgroundColor: C.market, alignItems: 'center', justifyContent: 'center' },
  stopTx: { color: '#fff', fontSize: 9, fontWeight: '800' },
  youDot: { position: 'absolute', width: 12, height: 12, borderRadius: 6, backgroundColor: C.blue, borderWidth: 2, borderColor: '#fff' },
  entrTx: { position: 'absolute', right: 8, bottom: 6, fontSize: 8, fontFamily: 'monospace', color: '#8a8a7e' },
  confTx: { fontSize: 9.5, fontFamily: 'monospace', color: '#55554d', marginTop: 8 },

  statRow: { flexDirection: 'row', gap: 8 },
  bigStat: { flex: 1, backgroundColor: C.card, borderWidth: 1, borderColor: C.line, borderRadius: 13, padding: 13, alignItems: 'center', marginTop: 8 },
  bigV: { fontSize: 22, fontWeight: '800', color: C.market },
  bigL: { fontSize: 10, color: C.dim, marginTop: 2, textAlign: 'center' },
  receipt: { borderWidth: 1.5, borderStyle: 'dashed', borderColor: C.sprout, borderRadius: 12, padding: 11, alignItems: 'center', marginTop: 10, backgroundColor: C.card },
  thanks: { backgroundColor: '#EFF3EC', borderRadius: 10, padding: 10, marginTop: 9 },
  thanksTx: { fontSize: 10.5, fontFamily: 'monospace', color: '#2e4a34' },

  pasteBox: { borderWidth: 1, borderColor: C.line, borderRadius: 10, padding: 10, minHeight: 90, marginTop: 10, fontSize: 12.5, color: C.ink, textAlignVertical: 'top' },
  cancelTx: { textAlign: 'center', color: C.dim, marginTop: 10, fontSize: 12 },

  sBox: { flexDirection: 'row', alignItems: 'center', backgroundColor: C.card, borderWidth: 1.5, borderColor: C.market, borderRadius: 12, paddingHorizontal: 12, paddingVertical: 9 },
  chip: { borderWidth: 1, borderColor: C.line, borderRadius: 999, paddingHorizontal: 10, paddingVertical: 4, marginRight: 6, backgroundColor: C.card },
  chipTx: { fontSize: 11, color: C.ink },
  resl: { backgroundColor: C.card, borderWidth: 1, borderColor: C.line, borderRadius: 12, padding: 11, marginTop: 8 },
  priceTx: { fontFamily: 'monospace', fontSize: 12, color: C.market },
  whTx: { fontSize: 9.5, fontFamily: 'monospace', color: C.sprout, marginTop: 3, letterSpacing: 0.5 },
  frTx: { fontSize: 10, color: '#8a8a7e', marginTop: 2 },
  addLink: { fontSize: 11, color: C.market, fontWeight: '700', marginTop: 6 },

  toast: { position: 'absolute', bottom: 74, left: 16, right: 16, backgroundColor: C.ink, borderRadius: 12, padding: 12 },
  toastTx: { color: '#fff', fontSize: 12.5, textAlign: 'center' },
});
