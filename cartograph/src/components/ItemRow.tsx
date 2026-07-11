import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';
import { colors, radius } from '../theme/tokens';

interface Props {
  emoji: string;
  name: string;
  note?: string;
  rightText?: string;
  onPress?: () => void;
  onLongPress?: () => void;
  done?: boolean;
  highlighted?: boolean;
  big?: boolean;
}

export default function ItemRow({ emoji, name, note, rightText, onPress, onLongPress, done, highlighted, big }: Props) {
  return (
    <TouchableOpacity
      style={[st.row, highlighted && st.highlighted, done && st.done]}
      onPress={onPress}
      onLongPress={onLongPress}
      disabled={!onPress && !onLongPress}
      delayLongPress={350}
    >
      <Text style={{ fontSize: big ? 22 : 16 }}>{emoji}</Text>
      <View style={{ flex: 1, marginLeft: 10 }}>
        <Text style={[st.name, big && st.nameBig, done && st.strike]}>{name}</Text>
        {note ? <Text style={st.note}>{note}</Text> : null}
      </View>
      {rightText ? <Text style={st.right}>{rightText}</Text> : null}
    </TouchableOpacity>
  );
}

const st = StyleSheet.create({
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.card,
    borderWidth: 1,
    borderColor: colors.line,
    borderRadius: radius.card,
    padding: 12,
    marginTop: 7,
    minHeight: 48,
  },
  highlighted: { borderColor: colors.market, borderWidth: 1.5, backgroundColor: '#F3F8F1' },
  done: { opacity: 0.5 },
  name: { fontSize: 13.5, fontWeight: '600', color: colors.ink },
  nameBig: { fontSize: 20, fontWeight: '800' },
  strike: { textDecorationLine: 'line-through' },
  note: { fontSize: 11, color: colors.dim, marginTop: 2 },
  right: { fontSize: 11, fontFamily: 'monospace', color: colors.sprout },
});
