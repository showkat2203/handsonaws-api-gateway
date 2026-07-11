import React from 'react';
import { View, Text, TouchableOpacity, ScrollView, StyleSheet } from 'react-native';
import { useNavigation } from '@react-navigation/native';
import { useStore } from '../../state/useStore';
import { byId } from '../../data/mock/catalog';
import { STORES } from '../../data/mock/stores';
import { colors, radius } from '../../theme/tokens';

export default function TripPlanScreen() {
  const navigation = useNavigation<any>();
  const planBest = useStore((s) => s.planBest);
  const planOneStop = useStore((s) => s.planOneStop);
  const tripMode = useStore((s) => s.tripMode);
  const setTripMode = useStore((s) => s.setTripMode);
  const startTrip = useStore((s) => s.startTrip);

  if (!planBest) {
    return (
      <View style={st.body}>
        <View style={st.empty}>
          <Text style={st.emptyTx}>No plan yet — go to Lists and tap "Plan trip".</Text>
        </View>
      </View>
    );
  }

  const start = () => {
    startTrip(tripMode);
    navigation.navigate('StoreMode');
  };

  return (
    <ScrollView style={st.body} contentContainerStyle={{ paddingBottom: 24 }}>
      <Text style={st.subHeader}>{tripMode === 'best' ? '2 stops · best value' : '1 stop'}</Text>

      {tripMode === 'best' ? (
        <>
          {planBest.stores.map((sp, i) => (
            <React.Fragment key={sp.store}>
              {i > 0 && <Text style={st.conn}>↓ 4 min drive</Text>}
              <StoreCard n={i === 0 ? '①' : '②'} storeId={sp.store} itemIds={sp.itemIds} total={sp.total} reason={sp.reason} />
            </React.Fragment>
          ))}
          <View style={st.totBar}>
            <Text style={st.totTx}>total</Text>
            <Text style={st.totTx}>
              ${planBest.total.toFixed(2)} · {Math.round(planBest.minutes)} min
            </Text>
          </View>
          <View style={st.savePill}>
            <Text style={st.savePillTx}>
              ⛃ ${planBest.savings.toFixed(2)} cheaper than one-stop ({planBest.megaHas}/
              {planBest.stores.reduce((n, s) => n + s.itemIds.length, 0)} @ MegaMart ${planBest.megaTotal.toFixed(2)})
            </Text>
          </View>
          <TouchableOpacity style={st.altOpt} onPress={() => setTripMode('onestop')}>
            <Text style={st.altTx}>
              Prefer one stop? MegaMart {planBest.megaHas}/{planBest.stores.reduce((n, s) => n + s.itemIds.length, 0)} ·
              ${planBest.megaTotal.toFixed(2)} · {Math.round((planOneStop?.minutes ?? 23))} min — tap to switch
            </Text>
          </TouchableOpacity>
        </>
      ) : planOneStop ? (
        <>
          <StoreCard n="①" storeId="mega" itemIds={planOneStop.store.itemIds} total={planOneStop.store.total} reason={planOneStop.droppedNote} />
          <TouchableOpacity style={st.altOpt} onPress={() => setTripMode('best')}>
            <Text style={st.altTx}>
              ← back to 2-stop plan (${planBest.total.toFixed(2)}, save ${planBest.savings.toFixed(2)})
            </Text>
          </TouchableOpacity>
        </>
      ) : null}

      <TouchableOpacity style={[st.cta, { marginTop: 14 }]} onPress={start}>
        <Text style={st.ctaTx}>Start trip →</Text>
      </TouchableOpacity>
    </ScrollView>
  );
}

function StoreCard({ n, storeId, itemIds, total, reason }: { n: string; storeId: keyof typeof STORES; itemIds: string[]; total: number; reason?: string }) {
  const store = STORES[storeId];
  return (
    <View style={st.stCard}>
      <View style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
        <Text style={st.itemName}>
          {n} {store.name}
        </Text>
        <Text style={st.zoneTx}>{itemIds.length} items</Text>
      </View>
      <Text style={st.itemNote}>
        {store.dist} · ${total.toFixed(2)} · {itemIds.map((id) => byId(id).name.split(' ')[0]).join(', ')}
      </Text>
      {reason ? <Text style={[st.itemNote, { color: colors.sprout }]}>{reason}</Text> : null}
    </View>
  );
}

const st = StyleSheet.create({
  body: { flex: 1, padding: 13, backgroundColor: colors.paper },
  subHeader: { fontSize: 10.5, color: colors.dim, fontFamily: 'monospace', marginBottom: 2 },
  empty: { padding: 24, alignItems: 'center' },
  emptyTx: { color: colors.dim, fontSize: 13, textAlign: 'center' },
  stCard: { backgroundColor: colors.card, borderWidth: 1, borderColor: colors.line, borderRadius: radius.card, padding: 12, marginTop: 8 },
  conn: { textAlign: 'center', fontFamily: 'monospace', fontSize: 10, color: '#8a8a7e', paddingVertical: 4 },
  itemName: { fontSize: 13.5, fontWeight: '600', color: colors.ink },
  itemNote: { fontSize: 11, color: colors.dim, marginTop: 2 },
  zoneTx: { fontSize: 10, fontFamily: 'monospace', color: colors.sprout },
  totBar: { backgroundColor: colors.market, borderRadius: radius.card, padding: 12, flexDirection: 'row', justifyContent: 'space-between', marginTop: 8 },
  totTx: { color: '#fff', fontFamily: 'monospace', fontSize: 12 },
  savePill: { backgroundColor: colors.tag, borderRadius: 9, padding: 9, marginTop: 8 },
  savePillTx: { fontSize: 11.5, fontWeight: '700', color: colors.ink },
  altOpt: { borderWidth: 1, borderStyle: 'dashed', borderColor: '#B08900', borderRadius: radius.button, padding: 9, marginTop: 8, backgroundColor: colors.card },
  altTx: { fontSize: 11.5, color: '#6a5a10' },
  cta: { backgroundColor: colors.market, borderRadius: radius.button, paddingVertical: 12, alignItems: 'center' },
  ctaTx: { color: '#fff', fontWeight: '700', fontSize: 14 },
});
