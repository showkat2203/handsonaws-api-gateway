import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { colors } from '../theme/tokens';

export default function ZoneHeader({ label }: { label: string }) {
  return (
    <View style={st.wrap}>
      <Text style={st.tx}>{label.toUpperCase()}</Text>
    </View>
  );
}

const st = StyleSheet.create({
  wrap: { marginTop: 14, marginBottom: 2, paddingHorizontal: 2 },
  tx: { fontSize: 10.5, fontFamily: 'monospace', color: colors.sprout, letterSpacing: 1 },
});
