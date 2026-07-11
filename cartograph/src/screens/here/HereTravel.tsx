import React, { useEffect, useState } from 'react';
import { View, Text, TouchableOpacity, StyleSheet, ScrollView } from 'react-native';
import { useStore } from '../../state/useStore';
import { ContextService } from '../../services/ContextService';
import { StoreService } from '../../services/StoreService';
import { VoiceService } from '../../services/VoiceService';
import { colors, radius } from '../../theme/tokens';

const NEEDS = ['coffee', 'charger', 'snacks', 'restroom', 'gas'];

export default function HereTravel() {
  const setContext = useStore((s) => s.setContext);
  const [needs, setNeeds] = useState<string[]>(['coffee', 'charger']);

  const { best, matchCount, alt } = ContextService.bestStops(needs);
  const bestStore = StoreService.get(best.storeId);
  const altStore = alt ? StoreService.get(alt.storeId) : null;

  useEffect(() => {
    VoiceService.speak(`Best match: ${bestStore.name}, exit in ${best.minutes} minutes, matches ${matchCount} of your needs.`);
  }, [best.storeId]);

  const toggle = (n: string) => setNeeds((cur) => (cur.includes(n) ? cur.filter((x) => x !== n) : [...cur, n]));

  return (
    <ScrollView style={st.body} contentContainerStyle={{ paddingBottom: 24 }}>
      <View style={st.hero}>
        <Text style={st.heroTx}>▸ ON I-80 WEST</Text>
        <Text style={st.heroSub}>What do you need at the next stop?</Text>
      </View>

      <View style={st.chipRow}>
        {NEEDS.map((n) => {
          const on = needs.includes(n);
          return (
            <TouchableOpacity key={n} style={[st.chip, on && st.chipOn]} onPress={() => toggle(n)}>
              <Text style={[st.chipTx, on && { color: '#fff' }]}>{n}</Text>
            </TouchableOpacity>
          );
        })}
      </View>

      <View style={st.exitCard}>
        <Text style={st.exitLo}>
          {best.exit} · {best.minutes} MIN
        </Text>
        <Text style={st.exitName}>{bestStore.name}</Text>
        <Text style={st.exitMatch}>{'✓'.repeat(Math.max(1, matchCount))} {matchCount >= needs.length && needs.length > 0 ? 'all needs' : `${matchCount}/${needs.length} needs`}</Text>
        <TouchableOpacity
          style={st.guideBtn}
          onPress={() => setContext({ mode: 'unmapped', storeId: best.storeId })}
        >
          <Text style={st.guideTx}>Guide me at this stop ▸</Text>
        </TouchableOpacity>
      </View>

      {altStore && alt && (
        <View style={[st.exitCard, st.altCard]}>
          <Text style={st.exitLo}>
            {alt.exit} · {alt.minutes} MIN
          </Text>
          <Text style={[st.exitName, { color: colors.dim }]}>{altStore.name}</Text>
          {alt.unknownNeeds?.length ? (
            <Text style={st.exitMatch}>chargers unknown at this stop</Text>
          ) : null}
        </View>
      )}
    </ScrollView>
  );
}

const st = StyleSheet.create({
  body: { flex: 1, padding: 13 },
  hero: { backgroundColor: colors.ink, borderRadius: radius.card, padding: 13 },
  heroTx: { color: '#fff', fontSize: 14, fontFamily: 'monospace', letterSpacing: 0.5 },
  heroSub: { color: '#D8D8CF', fontSize: 12.5, marginTop: 4 },
  chipRow: { flexDirection: 'row', flexWrap: 'wrap', gap: 7, marginTop: 14 },
  chip: { borderWidth: 1, borderColor: colors.line, borderRadius: 999, paddingHorizontal: 12, paddingVertical: 8, backgroundColor: colors.card },
  chipOn: { backgroundColor: colors.market, borderColor: colors.market },
  chipTx: { fontSize: 12, color: colors.ink },
  exitCard: { backgroundColor: colors.card, borderWidth: 1.5, borderColor: colors.market, borderRadius: radius.card, padding: 14, marginTop: 14 },
  altCard: { borderColor: colors.line, opacity: 0.65 },
  exitLo: { fontSize: 10.5, fontFamily: 'monospace', color: colors.sprout, letterSpacing: 0.5 },
  exitName: { fontSize: 18, fontWeight: '800', color: colors.ink, marginTop: 3 },
  exitMatch: { fontSize: 12, color: colors.dim, marginTop: 4 },
  guideBtn: { backgroundColor: colors.market, borderRadius: radius.button, paddingVertical: 12, alignItems: 'center', marginTop: 10 },
  guideTx: { color: '#fff', fontWeight: '700', fontSize: 13.5 },
});
