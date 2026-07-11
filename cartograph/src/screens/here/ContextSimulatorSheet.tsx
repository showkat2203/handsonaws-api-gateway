import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet, Modal } from 'react-native';
import { useStore } from '../../state/useStore';
import { ContextService } from '../../services/ContextService';
import { colors, radius } from '../../theme/tokens';

interface Props {
  visible: boolean;
  onClose: () => void;
}

export default function ContextSimulatorSheet({ visible, onClose }: Props) {
  const context = useStore((s) => s.context);
  const setContext = useStore((s) => s.setContext);

  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={onClose}>
      <View style={st.dim}>
        <View style={st.sheet}>
          <Text style={st.h}>Context Simulator</Text>
          <Text style={st.s}>No GPS in this demo — pick where you "are"</Text>
          {ContextService.options().map((opt) => {
            const active = context.mode === opt.context.mode && context.storeId === opt.context.storeId;
            return (
              <TouchableOpacity
                key={opt.key}
                style={[st.opt, active && st.optActive]}
                onPress={() => {
                  setContext(opt.context);
                  onClose();
                }}
              >
                <Text style={[st.optTx, active && { color: '#fff' }]}>{opt.label}</Text>
              </TouchableOpacity>
            );
          })}
          <TouchableOpacity onPress={onClose}><Text style={st.cancel}>close</Text></TouchableOpacity>
        </View>
      </View>
    </Modal>
  );
}

const st = StyleSheet.create({
  dim: { flex: 1, backgroundColor: 'rgba(20,25,18,0.5)', justifyContent: 'flex-end' },
  sheet: { backgroundColor: '#fff', borderTopLeftRadius: 18, borderTopRightRadius: 18, padding: 16 },
  h: { fontSize: 15.5, fontWeight: '800', color: colors.ink },
  s: { fontSize: 11, color: colors.dim, marginTop: 2, marginBottom: 8 },
  opt: { borderWidth: 1, borderColor: colors.line, borderRadius: radius.card, padding: 12, marginTop: 8 },
  optActive: { backgroundColor: colors.market, borderColor: colors.market },
  optTx: { fontSize: 13, fontWeight: '600', color: colors.ink },
  cancel: { textAlign: 'center', color: colors.dim, marginTop: 12, fontSize: 12 },
});
