import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet, Modal } from 'react-native';
import Svg, { Rect, Circle, Text as SvgText } from 'react-native-svg';
import { StoreId, ZONE_LABEL } from '../../data/mock/stores';
import { useStore } from '../../state/useStore';
import { MapService } from '../../services/MapService';
import { colors, radius } from '../../theme/tokens';

interface Props {
  visible: boolean;
  onClose: () => void;
  storeId: StoreId;
}

export default function MapPeekSheet({ visible, onClose, storeId }: Props) {
  const list = useStore((s) => s.list);
  const posZone = useStore((s) => s.posZone);
  const data = MapService.build(storeId, list, posZone);

  return (
    <Modal visible={visible} transparent animationType="slide" onRequestClose={onClose}>
      <View style={st.dim}>
        <View style={st.sheet}>
          <Text style={st.h}>map</Text>
          <View style={st.floor}>
            <Svg width="100%" height="100%" viewBox="0 0 260 330">
              {data.zones.map((z) => (
                <React.Fragment key={z.zone}>
                  <Rect x={z.point.x - 26} y={z.point.y - 14} width={52} height={28} rx={5} fill="#DDEAE0" />
                  <SvgText x={z.point.x} y={z.point.y + 4} fontSize={7} fill="#33452F" textAnchor="middle">
                    {(ZONE_LABEL[z.zone] ?? z.zone).split(' ·')[0]}
                  </SvgText>
                </React.Fragment>
              ))}
              {data.trail.map((d, i) => (
                <Circle key={i} cx={d.x} cy={d.y} r={1.6} fill={colors.sprout} />
              ))}
              {data.stops.map((s) => (
                <React.Fragment key={s.n}>
                  <Circle cx={s.x} cy={s.y} r={10} fill={colors.market} />
                  <SvgText x={s.x} y={s.y + 4} fontSize={10} fontWeight="800" fill="#fff" textAnchor="middle">
                    {s.n}
                  </SvgText>
                </React.Fragment>
              ))}
              {data.you && <Circle cx={data.you.x} cy={data.you.y} r={6} fill={colors.blue} stroke="#fff" strokeWidth={2} />}
            </Svg>
          </View>
          <Text style={st.caption}>position: {ZONE_LABEL[posZone] ?? posZone} · from your last ✓ / QR</Text>
          <TouchableOpacity style={st.cta} onPress={onClose}>
            <Text style={st.ctaTx}>⌄ back to list</Text>
          </TouchableOpacity>
        </View>
      </View>
    </Modal>
  );
}

const st = StyleSheet.create({
  dim: { flex: 1, backgroundColor: 'rgba(20,25,18,0.5)', justifyContent: 'flex-end' },
  sheet: { backgroundColor: '#fff', borderTopLeftRadius: 18, borderTopRightRadius: 18, padding: 15 },
  h: { fontSize: 15.5, fontWeight: '800', color: colors.ink },
  floor: { height: 330, backgroundColor: '#FBFBF8', borderWidth: 1, borderColor: '#C9CCC1', borderRadius: 12, marginTop: 10, overflow: 'hidden' },
  caption: { fontSize: 9.5, fontFamily: 'monospace', color: '#55554d', marginTop: 8 },
  cta: { backgroundColor: colors.market, borderRadius: radius.button, paddingVertical: 12, alignItems: 'center', marginTop: 10 },
  ctaTx: { color: '#fff', fontWeight: '700', fontSize: 14 },
});
