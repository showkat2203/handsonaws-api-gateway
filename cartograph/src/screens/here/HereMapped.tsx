import React, { useState } from 'react';
import { View, Text, TouchableOpacity, StyleSheet, ScrollView } from 'react-native';
import { useNavigation } from '@react-navigation/native';
import { useStore } from '../../state/useStore';
import { StoreService } from '../../services/StoreService';
import { RoutingService } from '../../services/RoutingService';
import { VoiceService } from '../../services/VoiceService';
import { byId } from '../../data/mock/catalog';
import { StoreId } from '../../data/mock/stores';
import MicButton from '../../components/MicButton';
import { colors, radius } from '../../theme/tokens';

const FACILITIES: Record<string, Record<string, string>> = {
  save: { restroom: 'back-left corner, past Dairy', pharmacy: 'front-right, by Checkout', chargers: 'Aisle 9 endcap' },
};

const CHIPS = ['milk', 'restroom', 'pharmacy', 'chargers'];

export default function HereMapped() {
  const navigation = useNavigation<any>();
  const context = useStore((s) => s.context);
  const list = useStore((s) => s.list);
  const startSingleStoreTrip = useStore((s) => s.startSingleStoreTrip);
  const [answer, setAnswer] = useState<{ chip: string; text: string } | null>(null);

  const store = StoreService.get(context.storeId!);
  const here = list.filter((l) => l.status === 'todo' && !!byId(l.itemId).at[store.id]);
  const sortedCount = here.length;
  const totalTodo = list.filter((l) => l.status === 'todo').length;
  const mins = Math.max(6, Math.round(sortedCount * 1.4 + 4));

  const askChip = (chip: string) => {
    const fac = FACILITIES[store.id]?.[chip];
    if (fac) {
      setAnswer({ chip, text: fac });
      return;
    }
    const res = VoiceService.ask(chip, { store: store.id });
    setAnswer({ chip, text: res.text });
  };

  const start = () => {
    startSingleStoreTrip(store.id);
    navigation.navigate('StoreMode');
  };

  return (
    <ScrollView style={st.body} contentContainerStyle={{ paddingBottom: 24 }}>
      <View style={st.hero}>
        <Text style={st.heroLo}>YOU'RE AT</Text>
        <Text style={st.heroName}>{store.name}</Text>
        <Text style={st.heroSub}>{store.freshness}</Text>
      </View>

      <MicButton onPress={() => navigation.navigate('VoiceSheet')} style={{ marginTop: 18 }} />
      <Text style={st.micHint}>hold to talk</Text>

      <TouchableOpacity style={st.listCard} onPress={start}>
        <Text style={st.listTx}>
          Your list here → {sortedCount} of {totalTodo} items · sorted · ~{mins} min · Start ▸
        </Text>
      </TouchableOpacity>

      <View style={st.chipRow}>
        {CHIPS.map((c) => (
          <TouchableOpacity key={c} style={st.chip} onPress={() => askChip(c)}>
            <Text style={st.chipTx}>{c}</Text>
          </TouchableOpacity>
        ))}
      </View>

      {answer && (
        <View style={st.answerCard}>
          <Text style={st.answerLabel}>{answer.chip.toUpperCase()}</Text>
          <Text style={st.answerTx}>{answer.text}</Text>
        </View>
      )}

      <Text style={st.recent}>recent find: Pasta sauce · Aisle 5 · 12 min ago</Text>
    </ScrollView>
  );
}

const st = StyleSheet.create({
  body: { flex: 1, padding: 13 },
  hero: { backgroundColor: colors.card, borderWidth: 1.5, borderColor: colors.market, borderRadius: radius.card, padding: 13 },
  heroLo: { fontSize: 9.5, color: colors.sprout, fontFamily: 'monospace', letterSpacing: 1 },
  heroName: { fontSize: 19, fontWeight: '800', marginTop: 2, color: colors.ink },
  heroSub: { fontSize: 11.5, color: colors.dim, marginTop: 3 },
  micHint: { textAlign: 'center', fontSize: 10.5, color: colors.dim, marginTop: 6, fontFamily: 'monospace' },
  listCard: { backgroundColor: colors.market, borderRadius: radius.card, padding: 14, marginTop: 16 },
  listTx: { color: '#fff', fontWeight: '700', fontSize: 13.5, textAlign: 'center' },
  chipRow: { flexDirection: 'row', flexWrap: 'wrap', gap: 7, marginTop: 14 },
  chip: { borderWidth: 1, borderColor: colors.line, borderRadius: 999, paddingHorizontal: 12, paddingVertical: 8, backgroundColor: colors.card },
  chipTx: { fontSize: 12, color: colors.ink },
  answerCard: { backgroundColor: colors.card, borderWidth: 1, borderColor: colors.market, borderRadius: radius.card, padding: 12, marginTop: 10 },
  answerLabel: { fontSize: 9.5, fontFamily: 'monospace', color: colors.sprout, letterSpacing: 1 },
  answerTx: { fontSize: 14, fontWeight: '700', color: colors.ink, marginTop: 3 },
  recent: { fontSize: 10.5, color: colors.dim, fontFamily: 'monospace', marginTop: 18, textAlign: 'center' },
});
