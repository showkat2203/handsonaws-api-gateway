import React, { useState } from 'react';
import { SafeAreaView, View, Text, TouchableOpacity, StyleSheet } from 'react-native';
import { useStore } from '../../state/useStore';
import { colors } from '../../theme/tokens';
import ContextSimulatorSheet from './ContextSimulatorSheet';
import HereMapped from './HereMapped';
import HereUnmapped from './HereUnmapped';
import HereTravel from './HereTravel';
import HereIdle from './HereIdle';

export default function HereScreen() {
  const context = useStore((s) => s.context);
  const [simOpen, setSimOpen] = useState(false);

  return (
    <SafeAreaView style={st.app}>
      <View style={st.header}>
        <View style={{ flex: 1 }}>
          <Text style={st.title}>Cartograph</Text>
          <Text style={st.meta}>demo · no backend</Text>
        </View>
        <TouchableOpacity style={st.simPill} onPress={() => setSimOpen(true)}>
          <Text style={st.simTx}>◎ context</Text>
        </TouchableOpacity>
      </View>

      {context.mode === 'mapped' && <HereMapped />}
      {context.mode === 'unmapped' && <HereUnmapped />}
      {context.mode === 'travel' && <HereTravel />}
      {context.mode === 'idle' && <HereIdle />}

      <ContextSimulatorSheet visible={simOpen} onClose={() => setSimOpen(false)} />
    </SafeAreaView>
  );
}

const st = StyleSheet.create({
  app: { flex: 1, backgroundColor: colors.paper },
  header: {
    backgroundColor: colors.market,
    paddingHorizontal: 16,
    paddingVertical: 13,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  title: { color: '#fff', fontSize: 17, fontWeight: '800' },
  meta: { color: '#CFE0CD', fontSize: 10, fontFamily: 'monospace' },
  simPill: { borderWidth: 1, borderColor: '#CFE0CD', borderRadius: 999, paddingHorizontal: 10, paddingVertical: 6 },
  simTx: { color: '#fff', fontSize: 11, fontFamily: 'monospace' },
});
