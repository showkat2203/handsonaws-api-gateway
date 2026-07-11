import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';
import { colors, radius } from '../theme/tokens';

interface Props {
  need: string;
  copy: string;
  confidence: string;
  confirmed: boolean;
  onConfirm: () => void;
  onReject: () => void;
}

export default function TemplateCard({ need, copy, confidence, confirmed, onConfirm, onReject }: Props) {
  return (
    <View style={st.wrap}>
      <View style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
        <Text style={st.need}>{need}</Text>
        <Text style={confirmed ? st.confirmedTag : st.guessTag}>
          {confirmed ? 'CONFIRMED BY YOU' : 'TEMPLATE GUESS'}
        </Text>
      </View>
      <Text style={st.copy}>
        {copy} · {confidence}
      </Text>
      {!confirmed && (
        <View style={st.row}>
          <TouchableOpacity style={st.btnYes} onPress={onConfirm}>
            <Text style={st.btnTx}>✓ still there</Text>
          </TouchableOpacity>
          <TouchableOpacity style={st.btnNo} onPress={onReject}>
            <Text style={[st.btnTx, { color: colors.alert }]}>✗ not there</Text>
          </TouchableOpacity>
        </View>
      )}
    </View>
  );
}

const st = StyleSheet.create({
  wrap: { backgroundColor: colors.card, borderWidth: 1, borderColor: colors.line, borderRadius: radius.card, padding: 12, marginTop: 8 },
  need: { fontSize: 13.5, fontWeight: '700', color: colors.ink, textTransform: 'capitalize' },
  guessTag: { fontSize: 9, fontFamily: 'monospace', color: '#B08900' },
  confirmedTag: { fontSize: 9, fontFamily: 'monospace', color: colors.sprout },
  copy: { fontSize: 11.5, color: colors.dim, marginTop: 3 },
  row: { flexDirection: 'row', gap: 8, marginTop: 8 },
  btnYes: { flex: 1, borderWidth: 1, borderColor: colors.market, borderRadius: radius.button, paddingVertical: 8, alignItems: 'center' },
  btnNo: { flex: 1, borderWidth: 1, borderColor: colors.line, borderRadius: radius.button, paddingVertical: 8, alignItems: 'center' },
  btnTx: { fontSize: 12, fontWeight: '700', color: colors.market },
});
