import React, { useState } from 'react';
import { SafeAreaView, View, Text, TouchableOpacity, ScrollView, StyleSheet } from 'react-native';
import { useNavigation } from '@react-navigation/native';
import { useStore } from '../../state/useStore';
import { byId } from '../../data/mock/catalog';
import { STORES, ZONE_LABEL } from '../../data/mock/stores';
import { RoutingService } from '../../services/RoutingService';
import ItemRow from '../../components/ItemRow';
import ZoneHeader from '../../components/ZoneHeader';
import DealCard from '../../components/DealCard';
import MapPeekSheet from './MapPeekSheet';
import RescueSheet from './RescueSheet';
import DriveInterstitial from './DriveInterstitial';
import TripSummary from './TripSummary';
import { colors, radius } from '../../theme/tokens';

export default function StoreModeScreen() {
  const navigation = useNavigation<any>();
  const trip = useStore((s) => s.trip);
  const tripPhase = useStore((s) => s.tripPhase);
  const list = useStore((s) => s.list);
  const posZone = useStore((s) => s.posZone);
  const gotIt = useStore((s) => s.gotIt);
  const applyDeal = useStore((s) => s.applyDeal);
  const setPosZone = useStore((s) => s.setPosZone);
  const say = useStore((s) => s.say);

  const [rescueKey, setRescueKey] = useState<string | null>(null);
  const [mapOpen, setMapOpen] = useState(false);

  if (!trip) {
    navigation.goBack();
    return null;
  }

  const curStore = trip.stores[trip.storeIndex];
  const store = STORES[curStore];
  const groups = RoutingService.groupByZone(list, curStore);
  const flatOrdered = groups.flatMap((g) => g.items);
  const nextItem = flatOrdered[0];
  const nextCat = nextItem ? byId(nextItem.itemId) : null;
  const dealForNext = nextCat?.deal && nextCat.deal.store === curStore && nextItem?.status === 'todo' ? nextCat.deal : null;
  const doneItems = list.filter((l) => l.status === 'got' && l.assignedStore === curStore);

  return (
    <SafeAreaView style={st.app}>
      <View style={st.header}>
        <View style={{ flex: 1 }}>
          <Text style={st.hTitle}>{store.name}</Text>
          <Text style={st.hMeta}>store mode · {trip.picked} picked</Text>
        </View>
        <TouchableOpacity onPress={() => navigation.goBack()}>
          <Text style={st.closeTx}>✕</Text>
        </TouchableOpacity>
      </View>

      <View style={st.compass}>
        <Text style={st.compassTx}>you're near {(ZONE_LABEL[posZone] ?? posZone).toUpperCase().split(' ·')[0]}</Text>
        <TouchableOpacity
          onPress={() => {
            if (nextItem && nextCat) {
              setPosZone(nextCat.at[curStore]!.zone);
              say('🔳 Position set: ' + ZONE_LABEL[nextCat.at[curStore]!.zone]);
            }
          }}
        >
          <Text style={st.scanTx}>🔳 scan QR</Text>
        </TouchableOpacity>
      </View>

      <ScrollView style={st.body} contentContainerStyle={{ paddingBottom: 24 }}>
        {flatOrdered.length === 0 ? (
          <View style={st.empty}>
            <Text style={st.emptyTx}>All picked here ✓</Text>
          </View>
        ) : (
          groups.map((g) => (
            <View key={g.zone}>
              <ZoneHeader label={ZONE_LABEL[g.zone] ?? g.zone} />
              {g.items.map((it) => {
                const c = byId(it.itemId);
                const isNext = it.key === nextItem?.key;
                return (
                  <View key={it.key}>
                    {isNext && <Text style={st.nextBadge}>NEXT</Text>}
                    <ItemRow
                      emoji={c.emoji}
                      name={c.name}
                      note={isNext ? c.hint : undefined}
                      rightText={`$${c.at[curStore]!.price.toFixed(2)}`}
                      highlighted={isNext}
                      big={isNext}
                      onPress={() => gotIt(it.key)}
                      onLongPress={() => setRescueKey(it.key)}
                    />
                    {isNext && dealForNext && (
                      <DealCard
                        label={dealForNext.label}
                        fromPrice={c.at[curStore]!.price}
                        toPrice={dealForNext.dealPrice}
                        saveAmt={dealForNext.saveAmt}
                        onPress={() => applyDeal(it.key)}
                      />
                    )}
                  </View>
                );
              })}
            </View>
          ))
        )}

        {doneItems.length > 0 && <Text style={st.doneHdr}>DONE ↓</Text>}
        {doneItems.map((it) => {
          const c = byId(it.itemId);
          return <ItemRow key={it.key} emoji={c.emoji} name={c.name} note={it.note} done />;
        })}

        <TouchableOpacity style={st.mapBtn} onPress={() => setMapOpen(true)}>
          <Text style={st.mapBtnTx}>🗺️ peek the map</Text>
        </TouchableOpacity>
      </ScrollView>

      <View style={st.hintBar}>
        <Text style={st.hintTx}>tap a row = got ✓ · long-press = not here ✗</Text>
      </View>

      <MapPeekSheet visible={mapOpen} onClose={() => setMapOpen(false)} storeId={curStore} />
      <RescueSheet itemKey={rescueKey} curStore={curStore} otherStores={trip.stores} onClose={() => setRescueKey(null)} />
      <DriveInterstitial />
      <TripSummary />
    </SafeAreaView>
  );
}

const st = StyleSheet.create({
  app: { flex: 1, backgroundColor: colors.paper },
  header: { backgroundColor: colors.market, paddingHorizontal: 16, paddingVertical: 13, flexDirection: 'row', alignItems: 'center' },
  hTitle: { color: '#fff', fontSize: 17, fontWeight: '800' },
  hMeta: { color: '#CFE0CD', fontSize: 10, fontFamily: 'monospace' },
  closeTx: { color: '#fff', fontSize: 18, paddingHorizontal: 6 },
  compass: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: 13, paddingVertical: 8, backgroundColor: '#EFF3EC' },
  compassTx: { fontSize: 11, fontFamily: 'monospace', color: '#2e4a34' },
  scanTx: { fontSize: 11, fontFamily: 'monospace', color: colors.market, fontWeight: '700' },
  body: { flex: 1, padding: 13 },
  empty: { padding: 24, alignItems: 'center' },
  emptyTx: { color: colors.dim, fontSize: 13, textAlign: 'center' },
  nextBadge: { fontSize: 10, fontFamily: 'monospace', color: colors.market, fontWeight: '800', marginTop: 12, letterSpacing: 1 },
  doneHdr: { fontSize: 10, color: '#9a9a8e', fontFamily: 'monospace', marginTop: 16, marginBottom: 2 },
  mapBtn: { backgroundColor: colors.card, borderWidth: 1, borderColor: colors.line, borderRadius: radius.button, padding: 10, alignItems: 'center', marginTop: 16 },
  mapBtnTx: { fontSize: 12, color: colors.ink },
  hintBar: { padding: 8, alignItems: 'center', borderTopWidth: 1, borderColor: colors.line, backgroundColor: colors.card },
  hintTx: { fontSize: 10.5, color: colors.dim },
});
