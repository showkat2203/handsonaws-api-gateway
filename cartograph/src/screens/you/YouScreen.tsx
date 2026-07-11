import React from 'react';
import { SafeAreaView, View, Text, TouchableOpacity, ScrollView, StyleSheet } from 'react-native';
import { useNavigation } from '@react-navigation/native';
import { useStore } from '../../state/useStore';
import { HOUSEHOLD } from '../../data/mock/household';
import { colors, radius } from '../../theme/tokens';

const LANGS = ['EN', 'हिंदी', 'Español', 'Français'];

export default function YouScreen() {
  const navigation = useNavigation<any>();
  const lang = useStore((s) => s.lang);
  const setLang = useStore((s) => s.setLang);
  const shelfConfirms = useStore((s) => s.shelfConfirms);
  const say = useStore((s) => s.say);
  const resetDemo = useStore((s) => s.resetDemo);

  return (
    <SafeAreaView style={st.app}>
      <View style={st.header}>
        <Text style={st.hTitle}>You</Text>
        <Text style={st.hMeta}>demo · no backend</Text>
      </View>
      <ScrollView style={st.body} contentContainerStyle={{ paddingBottom: 24 }}>
        <View style={st.card}>
          <Text style={st.name}>Household</Text>
          <Text style={st.note}>
            {HOUSEHOLD.map((h) => h.name).join(' · ')}  <Text style={{ color: colors.sprout }}>＋ invite</Text>
          </Text>
        </View>

        <View style={st.card}>
          <Text style={st.name}>Search language</Text>
          <Text style={[st.note, { marginBottom: 4 }]}>English-first UI · search language is a stub for now</Text>
          <View style={{ flexDirection: 'row', flexWrap: 'wrap' }}>
            {LANGS.map((L) => (
              <TouchableOpacity
                key={L}
                onPress={() => {
                  setLang(L);
                  say('Search language: ' + L);
                }}
                style={[st.langChip, lang === L && st.langOn]}
              >
                <Text style={[st.langTx, lang === L && { color: '#fff' }]}>{L}</Text>
              </TouchableOpacity>
            ))}
          </View>
        </View>

        <View style={st.card}>
          <Text style={st.name}>Your contribution</Text>
          <Text style={st.note}>{shelfConfirms} shelf locations confirmed · 3 receipts scanned</Text>
        </View>

        <TouchableOpacity style={st.card} onPress={() => navigation.navigate('StockScan')}>
          <Text style={st.name}>Store tools →</Text>
          <Text style={st.note}>Stock-Scan demo (staff mode lite)</Text>
        </TouchableOpacity>

        <View style={[st.card, st.pro]}>
          <Text style={st.name}>Cartograph Pro</Text>
          <Text style={st.note}>Merge Instacart + Shipt batches into one pick path. Coming for gig pickers.</Text>
        </View>

        <TouchableOpacity style={st.altOpt} onPress={resetDemo}>
          <Text style={st.altTx}>↺ reset demo data</Text>
        </TouchableOpacity>
      </ScrollView>
    </SafeAreaView>
  );
}

const st = StyleSheet.create({
  app: { flex: 1, backgroundColor: colors.paper },
  header: { backgroundColor: colors.market, paddingHorizontal: 16, paddingVertical: 13 },
  hTitle: { color: '#fff', fontSize: 17, fontWeight: '800' },
  hMeta: { color: '#CFE0CD', fontSize: 10, fontFamily: 'monospace' },
  body: { flex: 1, padding: 13 },
  card: { backgroundColor: colors.card, borderWidth: 1, borderColor: colors.line, borderRadius: radius.card, padding: 12, marginTop: 8 },
  pro: { borderColor: colors.tag, backgroundColor: '#FFFDF2' },
  name: { fontSize: 13.5, fontWeight: '700', color: colors.ink },
  note: { fontSize: 11, color: colors.dim, marginTop: 3 },
  langChip: { borderWidth: 1, borderColor: colors.line, borderRadius: 999, paddingHorizontal: 10, paddingVertical: 5, marginRight: 6, marginTop: 6, backgroundColor: colors.card },
  langOn: { backgroundColor: colors.market, borderColor: colors.market },
  langTx: { fontSize: 11, color: colors.ink },
  altOpt: { borderWidth: 1, borderStyle: 'dashed', borderColor: '#B08900', borderRadius: radius.button, padding: 10, marginTop: 12, alignItems: 'center', backgroundColor: colors.card },
  altTx: { fontSize: 12, color: '#6a5a10', fontWeight: '600' },
});
