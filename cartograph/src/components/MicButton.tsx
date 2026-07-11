import React from 'react';
import { TouchableOpacity, Text, StyleSheet, ViewStyle } from 'react-native';
import { colors } from '../theme/tokens';

interface Props {
  onPress: () => void;
  size?: number;
  style?: ViewStyle;
}

export default function MicButton({ onPress, size = 96, style }: Props) {
  return (
    <TouchableOpacity
      onPress={onPress}
      style={[st.btn, { width: size, height: size, borderRadius: size / 2 }, style]}
      accessibilityLabel="Hold to talk"
    >
      <Text style={{ fontSize: size * 0.4 }}>🎤</Text>
    </TouchableOpacity>
  );
}

const st = StyleSheet.create({
  btn: {
    backgroundColor: colors.market,
    alignItems: 'center',
    justifyContent: 'center',
    alignSelf: 'center',
    shadowColor: colors.ink,
    shadowOpacity: 0.2,
    shadowRadius: 10,
    shadowOffset: { width: 0, height: 4 },
    elevation: 4,
  },
});
