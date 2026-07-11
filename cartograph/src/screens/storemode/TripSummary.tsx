import React from 'react';
import { SafeAreaView, View, Text, TouchableOpacity, ScrollView, StyleSheet, Modal } from 'react-native';
import { useNavigation } from '@react-navigation/native';
import { useStore } from '../../state/useStore';
import { colors, radius } from '../../theme/tokens';

export default function TripSummary() {
  const navigation = useNavigation<any>();
  const tripPhase = useStore((s) => s.tripPhase);
  const summary = useStore((s) => s.summary);
  const tripMode = useStore((s) => s.tripMode);
  const say = useStore((s) => s.say);
  const finishSummary = useStore((s) => s.finishSummary);

  if (tripPhase !== 'summary' || !summary) return null;

  const done = () => {
    finishSummary();
    navigation.goBack();
  };

  return (
    <Modal visible animationType="slide" onRequestClose={done}>
      <SafeAreaView style={st.app}>
        <View style={st.header}>
          <Text style={st.hTitle}>Trip done 🎉</Text>
          <Text style={st.hMeta}>{tripMode === 'best' ? 'Save-Mart + Miller\'s' : 'MegaMart'}</Text>
        </View>
        <ScrollView style={st.body}>
          <View style={st.statRow}>
            <View style={st.bigStat}>
              <Text style={st.bigV}>{summary.minutes} min</Text>
              <Text style={st.bigL}>door to door (simulated)</Text>
            </View>
            <View style={st.bigStat}>
              <Text style={st.bigV}>${summary.moneySaved.toFixed(2)}</Text>
              <Text style={st.bigL}>saved vs one store + deals</Text>
            </View>
          </View>
          <View style={st.bigStat}>
            <Text style={st.bigV}>{summary.got} found</Text>
            <Text style={st.bigL}>{summary.swapped ? `${summary.swapped} smart swap(s), Emma approved ✓` : 'clean run ✓'}</Text>
          </View>
          <TouchableOpacity style={st.receipt} onPress={() => say('📸 Receipt scanned — prices updated for next trip ✓')}>
            <Text style={st.optB}>📸 Scan your receipts?</Text>
            <Text style={st.optP}>keeps prices honest for your next trip — 10 sec</Text>
          </TouchableOpacity>
          <View style={st.thanks}>
            <Text style={st.thanksTx}>your ✓s confirmed {summary.confirms} shelf locations for other shoppers today</Text>
          </View>
          <TouchableOpacity style={[st.cta, { marginTop: 14 }]} onPress={done}>
            <Text style={st.ctaTx}>Done</Text>
          </TouchableOpacity>
        </ScrollView>
      </SafeAreaView>
    </Modal>
  );
}

const st = StyleSheet.create({
  app: { flex: 1, backgroundColor: colors.paper },
  header: { backgroundColor: colors.market, paddingHorizontal: 16, paddingVertical: 13 },
  hTitle: { color: '#fff', fontSize: 17, fontWeight: '800' },
  hMeta: { color: '#CFE0CD', fontSize: 10, fontFamily: 'monospace' },
  body: { flex: 1, padding: 13 },
  statRow: { flexDirection: 'row', gap: 8 },
  bigStat: { flex: 1, backgroundColor: colors.card, borderWidth: 1, borderColor: colors.line, borderRadius: radius.card, padding: 13, alignItems: 'center', marginTop: 8 },
  bigV: { fontSize: 22, fontWeight: '800', color: colors.market },
  bigL: { fontSize: 10, color: colors.dim, marginTop: 2, textAlign: 'center' },
  receipt: { borderWidth: 1.5, borderStyle: 'dashed', borderColor: colors.sprout, borderRadius: radius.card, padding: 11, alignItems: 'center', marginTop: 10, backgroundColor: colors.card },
  optB: { fontSize: 12.5, fontWeight: '700', color: colors.ink },
  optP: { fontSize: 10.5, color: colors.dim },
  thanks: { backgroundColor: '#EFF3EC', borderRadius: radius.button, padding: 10, marginTop: 9 },
  thanksTx: { fontSize: 10.5, fontFamily: 'monospace', color: '#2e4a34' },
  cta: { backgroundColor: colors.market, borderRadius: radius.button, paddingVertical: 12, alignItems: 'center' },
  ctaTx: { color: '#fff', fontWeight: '700', fontSize: 14 },
});
