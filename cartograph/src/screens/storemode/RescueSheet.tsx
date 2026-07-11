import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet, Modal } from 'react-native';
import { byId } from '../../data/mock/catalog';
import { STORES, StoreId } from '../../data/mock/stores';
import { useStore } from '../../state/useStore';
import { colors, radius } from '../../theme/tokens';

interface Props {
  itemKey: string | null;
  curStore: StoreId;
  otherStores: StoreId[];
  onClose: () => void;
}

export default function RescueSheet({ itemKey, curStore, otherStores, onClose }: Props) {
  const rescueSwap = useStore((s) => s.rescueSwap);
  const rescueOther = useStore((s) => s.rescueOther);
  const rescueStaff = useStore((s) => s.rescueStaff);
  const list = useStore((s) => s.list);

  const item = itemKey ? list.find((l) => l.key === itemKey) : null;
  if (!item) return null;
  const c = byId(item.itemId);
  const otherWithStock = otherStores.find((s) => s !== curStore && c.at[s]);

  return (
    <Modal visible={!!itemKey} transparent animationType="fade" onRequestClose={onClose}>
      <View style={st.dim}>
        <View style={st.sheet}>
          <Text style={st.h}>Shelf's empty? No problem.</Text>
          <Text style={st.s}>{c.name} · reported just now</Text>

          <TouchableOpacity
            style={[st.opt, st.optBest]}
            onPress={() => {
              rescueSwap(item.key);
              onClose();
            }}
          >
            <Text style={st.ic}>🔄</Text>
            <View style={{ flex: 1 }}>
              <Text style={st.optB}>Swap: store-brand version — in stock</Text>
              <Text style={st.optP}>same shelf, two rows down · saves a little too</Text>
            </View>
          </TouchableOpacity>

          {otherWithStock && (
            <TouchableOpacity
              style={st.opt}
              onPress={() => {
                rescueOther(item.key);
                onClose();
              }}
            >
              <Text style={st.ic}>🏪</Text>
              <View style={{ flex: 1 }}>
                <Text style={st.optB}>{STORES[otherWithStock].name} has it</Text>
                <Text style={st.optP}>already on your route — move it there ✓</Text>
              </View>
            </TouchableOpacity>
          )}

          <TouchableOpacity
            style={st.opt}
            onPress={() => {
              rescueStaff(item.key);
              onClose();
            }}
          >
            <Text style={st.ic}>🙋</Text>
            <View style={{ flex: 1 }}>
              <Text style={st.optB}>Ask staff</Text>
              <Text style={st.optP}>show them this screen</Text>
            </View>
          </TouchableOpacity>

          <Text style={st.fx}>your report quietly flags this shelf for the store — thank you</Text>
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
  opt: { flexDirection: 'row', alignItems: 'center', borderWidth: 1, borderColor: colors.line, borderRadius: radius.card, padding: 11, marginTop: 9, gap: 10 },
  optBest: { borderColor: colors.market, backgroundColor: '#F3F8F1' },
  ic: { fontSize: 16 },
  optB: { fontSize: 12.5, fontWeight: '700', color: colors.ink },
  optP: { fontSize: 10.5, color: colors.dim },
  fx: { fontSize: 9, fontFamily: 'monospace', color: '#8a8a7e', textAlign: 'center', marginTop: 10 },
});
