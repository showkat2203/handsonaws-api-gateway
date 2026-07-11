import React, { useState } from 'react';
import { SafeAreaView, View, Text, TouchableOpacity, TextInput, StyleSheet, Modal } from 'react-native';
import { useNavigation } from '@react-navigation/native';
import { useStore } from '../../state/useStore';
import { VoiceService, PHRASE_CHIPS } from '../../services/VoiceService';
import { colors, radius } from '../../theme/tokens';

export default function VoiceSheet() {
  const navigation = useNavigation<any>();
  const context = useStore((s) => s.context);
  const posZone = useStore((s) => s.posZone);
  const trip = useStore((s) => s.trip);
  const addItem = useStore((s) => s.addItem);
  const say = useStore((s) => s.say);

  const [query, setQuery] = useState('');
  const [answer, setAnswer] = useState<{ itemId?: string; text: string; found: boolean } | null>(null);
  const [listening, setListening] = useState(false);

  const inStoreMode = context.mode === 'travel' ? false : !!trip;
  const store = trip ? trip.stores[trip.storeIndex] : context.storeId;

  const ask = (phrase: string) => {
    setQuery(phrase);
    const res = VoiceService.ask(phrase, { store, posZone: inStoreMode ? posZone : undefined });
    setAnswer(res);
    if (context.mode === 'travel') VoiceService.speak(res.text);
  };

  const holdToTalk = () => {
    setListening(true);
    setTimeout(() => {
      setListening(false);
      ask(PHRASE_CHIPS[0]);
    }, 900);
  };

  return (
    <Modal visible animationType="slide" onRequestClose={() => navigation.goBack()}>
      <SafeAreaView style={st.app}>
        <View style={st.header}>
          <Text style={st.hTitle}>Ask Cartograph</Text>
          <TouchableOpacity onPress={() => navigation.goBack()}>
            <Text style={st.closeTx}>✕</Text>
          </TouchableOpacity>
        </View>
        <View style={st.body}>
          <TouchableOpacity style={[st.wave, listening && st.waveOn]} onPress={holdToTalk}>
            <Text style={st.waveTx}>{listening ? '〜〜〜 listening 〜〜〜' : '🎤 hold to talk'}</Text>
          </TouchableOpacity>

          <View style={st.chipRow}>
            {PHRASE_CHIPS.map((p) => (
              <TouchableOpacity key={p} style={st.chip} onPress={() => ask(p)}>
                <Text style={st.chipTx}>{p}</Text>
              </TouchableOpacity>
            ))}
          </View>

          <TextInput
            value={query}
            onChangeText={setQuery}
            placeholder='or type: "shelf paper"…'
            placeholderTextColor="#9a9a8e"
            style={st.input}
            onSubmitEditing={() => ask(query)}
          />

          {answer && (
            <View style={st.answerCard}>
              <Text style={st.answerTx}>{answer.text}</Text>
              {answer.found && answer.itemId && (
                <TouchableOpacity
                  onPress={() => {
                    addItem(answer.itemId!);
                    say('Added to list ✓');
                  }}
                >
                  <Text style={st.addLink}>＋ add to list</Text>
                </TouchableOpacity>
              )}
            </View>
          )}
        </View>
      </SafeAreaView>
    </Modal>
  );
}

const st = StyleSheet.create({
  app: { flex: 1, backgroundColor: colors.paper },
  header: { backgroundColor: colors.market, paddingHorizontal: 16, paddingVertical: 13, flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  hTitle: { color: '#fff', fontSize: 17, fontWeight: '800' },
  closeTx: { color: '#fff', fontSize: 18 },
  body: { flex: 1, padding: 16 },
  wave: { backgroundColor: colors.card, borderWidth: 1.5, borderColor: colors.market, borderRadius: radius.card, paddingVertical: 26, alignItems: 'center' },
  waveOn: { backgroundColor: colors.market },
  waveTx: { fontSize: 14, fontWeight: '700', color: colors.ink },
  chipRow: { flexDirection: 'row', flexWrap: 'wrap', gap: 7, marginTop: 14 },
  chip: { borderWidth: 1, borderColor: colors.line, borderRadius: 999, paddingHorizontal: 12, paddingVertical: 8, backgroundColor: colors.card },
  chipTx: { fontSize: 12, color: colors.ink },
  input: { borderWidth: 1.5, borderColor: colors.market, borderRadius: radius.card, padding: 12, marginTop: 14, fontSize: 14, color: colors.ink, backgroundColor: colors.card },
  answerCard: { backgroundColor: colors.card, borderWidth: 1, borderColor: colors.market, borderRadius: radius.card, padding: 14, marginTop: 14 },
  answerTx: { fontSize: 15, fontWeight: '700', color: colors.ink },
  addLink: { fontSize: 12, color: colors.market, fontWeight: '700', marginTop: 8 },
});
