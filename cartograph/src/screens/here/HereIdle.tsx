import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet, ScrollView } from 'react-native';
import { useNavigation } from '@react-navigation/native';
import { StoreService } from '../../services/StoreService';
import MicButton from '../../components/MicButton';
import { colors, radius } from '../../theme/tokens';

export default function HereIdle() {
  const navigation = useNavigation<any>();
  const stores = StoreService.nearby();

  return (
    <ScrollView style={st.body} contentContainerStyle={{ paddingBottom: 24 }}>
      <Text style={st.h}>Nearby stores</Text>
      {stores.map((s) => (
        <View key={s.id} style={st.storeRow}>
          <Text style={st.storeName}>{s.name}</Text>
          <Text style={st.storeSub}>
            {s.dist} · {s.type} · {s.freshness}
          </Text>
        </View>
      ))}

      <MicButton onPress={() => navigation.navigate('VoiceSheet')} style={{ marginTop: 20 }} />
      <Text style={st.micHint}>hold to talk</Text>

      <TouchableOpacity style={st.cta} onPress={() => navigation.navigate('Plan', { screen: 'Lists' })}>
        <Text style={st.ctaTx}>plan this week's trip →</Text>
      </TouchableOpacity>
    </ScrollView>
  );
}

const st = StyleSheet.create({
  body: { flex: 1, padding: 13 },
  h: { fontSize: 12, fontFamily: 'monospace', color: colors.dim, letterSpacing: 1, marginBottom: 4 },
  storeRow: { backgroundColor: colors.card, borderWidth: 1, borderColor: colors.line, borderRadius: radius.card, padding: 12, marginTop: 8 },
  storeName: { fontSize: 14, fontWeight: '700', color: colors.ink },
  storeSub: { fontSize: 11, color: colors.dim, marginTop: 3 },
  micHint: { textAlign: 'center', fontSize: 10.5, color: colors.dim, marginTop: 6, fontFamily: 'monospace' },
  cta: { backgroundColor: colors.market, borderRadius: radius.card, paddingVertical: 14, alignItems: 'center', marginTop: 20 },
  ctaTx: { color: '#fff', fontWeight: '700', fontSize: 14 },
});
