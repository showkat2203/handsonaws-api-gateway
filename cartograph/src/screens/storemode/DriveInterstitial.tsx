import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet, Modal } from 'react-native';
import { STORES } from '../../data/mock/stores';
import { useStore } from '../../state/useStore';
import { colors, radius } from '../../theme/tokens';

export default function DriveInterstitial() {
  const trip = useStore((s) => s.trip);
  const tripPhase = useStore((s) => s.tripPhase);
  const arrivedNextStore = useStore((s) => s.arrivedNextStore);

  if (!trip || tripPhase !== 'drive') return null;
  const doneStore = STORES[trip.stores[trip.storeIndex]];
  const nextStore = STORES[trip.stores[trip.storeIndex + 1]];

  return (
    <Modal visible transparent animationType="fade">
      <View style={st.dim}>
        <View style={st.sheet}>
          <Text style={st.h}>{doneStore.name} done ✓</Text>
          <Text style={st.s}>
            Drive 4 min → {nextStore.name} ({nextStore.dist})
          </Text>
          <TouchableOpacity style={st.cta} onPress={arrivedNextStore}>
            <Text style={st.ctaTx}>Arrived →</Text>
          </TouchableOpacity>
        </View>
      </View>
    </Modal>
  );
}

const st = StyleSheet.create({
  dim: { flex: 1, backgroundColor: 'rgba(20,25,18,0.5)', justifyContent: 'flex-end' },
  sheet: { backgroundColor: '#fff', borderTopLeftRadius: 18, borderTopRightRadius: 18, padding: 15 },
  h: { fontSize: 15.5, fontWeight: '800', color: colors.ink },
  s: { fontSize: 11, color: colors.dim, marginTop: 2 },
  cta: { backgroundColor: colors.market, borderRadius: radius.button, paddingVertical: 12, alignItems: 'center', marginTop: 12 },
  ctaTx: { color: '#fff', fontWeight: '700', fontSize: 14 },
});
