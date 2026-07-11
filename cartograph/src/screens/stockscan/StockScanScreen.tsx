import React, { useState } from 'react';
import { SafeAreaView, View, Text, TouchableOpacity, StyleSheet, Modal } from 'react-native';
import { useNavigation } from '@react-navigation/native';
import { useStore } from '../../state/useStore';
import { colors, radius } from '../../theme/tokens';

const SHELF_TAG = 'SAVE-MART · A5 · RIGHT · BOTTOM';
const PRODUCTS = ['Pasta sauce', 'Penne', 'Dish soap'];

type Step = 'idle' | 'shelf' | 'product' | 'success';

export default function StockScanScreen() {
  const navigation = useNavigation<any>();
  const stockScanCount = useStore((s) => s.stockScanCount);
  const pinStockScan = useStore((s) => s.pinStockScan);
  const [step, setStep] = useState<Step>('idle');
  const [productIdx, setProductIdx] = useState(0);

  const product = PRODUCTS[productIdx % PRODUCTS.length];

  const scanProduct = () => {
    setStep('success');
    pinStockScan();
  };

  const again = () => {
    setProductIdx((i) => i + 1);
    setStep('idle');
  };

  return (
    <Modal visible animationType="slide" onRequestClose={() => navigation.goBack()}>
      <SafeAreaView style={st.app}>
        <View style={st.header}>
          <Text style={st.hTitle}>Stock-Scan</Text>
          <TouchableOpacity onPress={() => navigation.goBack()}>
            <Text style={st.closeTx}>✕</Text>
          </TouchableOpacity>
        </View>
        <View style={st.body}>
          <Text style={st.copy}>No cameras. No beacons. One extra second while stocking.</Text>

          {step === 'idle' && (
            <TouchableOpacity style={st.bigBtn} onPress={() => setStep('shelf')}>
              <Text style={st.bigIc}>🔳</Text>
              <Text style={st.bigTx}>Scan shelf tag</Text>
            </TouchableOpacity>
          )}

          {step === 'shelf' && (
            <>
              <View style={st.lockCard}>
                <Text style={st.lockLabel}>SHELF LOCKED IN</Text>
                <Text style={st.lockTx}>{SHELF_TAG}</Text>
              </View>
              <TouchableOpacity style={st.bigBtn} onPress={scanProduct}>
                <Text style={st.bigIc}>📦</Text>
                <Text style={st.bigTx}>Scan product barcode</Text>
              </TouchableOpacity>
            </>
          )}

          {step === 'success' && (
            <>
              <View style={st.successCard}>
                <Text style={st.successTx}>
                  {product} pinned to A5 · right · bottom — restocking is remapping ✓
                </Text>
              </View>
              <View style={st.counter}>
                <Text style={st.counterTx}>items pinned this session: {stockScanCount}</Text>
              </View>
              <TouchableOpacity style={st.bigBtn} onPress={again}>
                <Text style={st.bigIc}>🔳</Text>
                <Text style={st.bigTx}>Scan next shelf</Text>
              </TouchableOpacity>
            </>
          )}
        </View>
      </SafeAreaView>
    </Modal>
  );
}

const st = StyleSheet.create({
  app: { flex: 1, backgroundColor: colors.paper },
  header: { backgroundColor: colors.market, paddingHorizontal: 16, paddingVertical: 13, flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  hTitle: { color: '#fff', fontSize: 17, fontWeight: '800' },
  closeTx: { color: '#fff', fontSize: 18 },
  body: { flex: 1, padding: 16 },
  copy: { fontSize: 11.5, color: colors.dim, fontFamily: 'monospace', textAlign: 'center', marginBottom: 20 },
  bigBtn: { backgroundColor: colors.market, borderRadius: radius.card, paddingVertical: 28, alignItems: 'center', marginTop: 12 },
  bigIc: { fontSize: 30 },
  bigTx: { color: '#fff', fontWeight: '800', fontSize: 16, marginTop: 8 },
  lockCard: { backgroundColor: colors.card, borderWidth: 1.5, borderColor: colors.market, borderRadius: radius.card, padding: 14, alignItems: 'center' },
  lockLabel: { fontSize: 9.5, fontFamily: 'monospace', color: colors.sprout, letterSpacing: 1 },
  lockTx: { fontSize: 16, fontWeight: '800', color: colors.ink, marginTop: 4 },
  successCard: { backgroundColor: colors.tag, borderRadius: radius.card, padding: 16 },
  successTx: { fontSize: 14, fontWeight: '700', color: colors.ink },
  counter: { backgroundColor: '#EFF3EC', borderRadius: radius.button, padding: 10, marginTop: 10 },
  counterTx: { fontSize: 11, fontFamily: 'monospace', color: '#2e4a34', textAlign: 'center' },
});
