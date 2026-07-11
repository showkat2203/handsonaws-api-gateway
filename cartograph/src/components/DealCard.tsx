import React from 'react';
import { TouchableOpacity, Text, StyleSheet } from 'react-native';
import { colors, radius } from '../theme/tokens';

interface Props {
  label: string;
  fromPrice: number;
  toPrice: number;
  saveAmt: number;
  onPress: () => void;
}

export default function DealCard({ label, fromPrice, toPrice, saveAmt, onPress }: Props) {
  return (
    <TouchableOpacity style={st.wrap} onPress={onPress}>
      <Text style={st.label}>{label}</Text>
      <Text style={st.sub}>
        ${fromPrice.toFixed(2)} → ${toPrice.toFixed(2)} · save ${saveAmt.toFixed(2)} · tap to swap ✓
      </Text>
    </TouchableOpacity>
  );
}

const st = StyleSheet.create({
  wrap: { backgroundColor: colors.tag, borderRadius: radius.card, padding: 11, marginTop: 10 },
  label: { fontSize: 13, fontWeight: '700', color: colors.ink },
  sub: { fontSize: 11, color: '#5a4b00', marginTop: 3 },
});
