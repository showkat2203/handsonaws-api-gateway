import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { useStore } from '../state/useStore';
import { colors, radius } from '../theme/tokens';

export default function Toast() {
  const toast = useStore((s) => s.toast);
  if (!toast) return null;
  return (
    <View style={st.toast} pointerEvents="none">
      <Text style={st.tx}>{toast}</Text>
    </View>
  );
}

const st = StyleSheet.create({
  toast: {
    position: 'absolute',
    bottom: 74,
    left: 16,
    right: 16,
    backgroundColor: colors.ink,
    borderRadius: radius.card,
    padding: 12,
  },
  tx: { color: '#fff', fontSize: 12.5, textAlign: 'center' },
});
