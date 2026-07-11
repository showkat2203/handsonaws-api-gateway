import React, { useState } from 'react';
import { View, Text, TouchableOpacity, ScrollView, TextInput, Modal, StyleSheet } from 'react-native';
import { useNavigation } from '@react-navigation/native';
import { useStore } from '../../state/useStore';
import { byId } from '../../data/mock/catalog';
import ItemRow from '../../components/ItemRow';
import { colors, radius } from '../../theme/tokens';

const DEFAULT_PASTE =
  'bring milk, eggs, that sourdough, dish soap, tomatoes, penne + pasta sauce, fresh basil, coffee, yogurt, paper towels, AA batteries';

export default function ListsScreen() {
  const navigation = useNavigation<any>();
  const list = useStore((s) => s.list);
  const addItem = useStore((s) => s.addItem);
  const addByName = useStore((s) => s.addByName);
  const parsePaste = useStore((s) => s.parsePaste);
  const makePlanBest = useStore((s) => s.makePlanBest);

  const [pasteOpen, setPasteOpen] = useState(false);
  const [pasteText, setPasteText] = useState(DEFAULT_PASTE);
  const [addText, setAddText] = useState('');

  const todo = list.filter((l) => l.status === 'todo');
  const done = list.filter((l) => l.status !== 'todo');
  const emmaCount = todo.filter((l) => l.addedBy === 'emma').length;

  const onAddByName = () => {
    if (!addText.trim()) return;
    addByName(addText);
    setAddText('');
  };

  return (
    <ScrollView style={st.body} contentContainerStyle={{ paddingBottom: 24 }}>
      <View style={st.hero}>
        <Text style={st.heroWho}>HOUSEHOLD · YOU + EMMA</Text>
        <Text style={st.heroName}>Home list</Text>
        <Text style={st.heroCnt}>
          {todo.length} items to get{emmaCount ? ` · Emma added ${emmaCount} today` : ''}
        </Text>
        <TouchableOpacity
          style={st.cta}
          onPress={() => {
            makePlanBest();
            navigation.navigate('TripPlan');
          }}
        >
          <Text style={st.ctaTx}>Plan trip →</Text>
        </TouchableOpacity>
      </View>

      <View style={st.addRow}>
        <TouchableOpacity style={st.addB} onPress={() => setPasteOpen(true)}>
          <Text style={st.addIc}>📋</Text>
          <Text style={st.addTx}>paste text</Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={st.addB}
          onPress={() => {
            addItem('basil', 'you');
          }}
        >
          <Text style={st.addIc}>🎤</Text>
          <Text style={st.addTx}>speak</Text>
        </TouchableOpacity>
        <View style={[st.addB, st.typeBox]}>
          <TextInput
            value={addText}
            onChangeText={setAddText}
            placeholder="type an item…"
            placeholderTextColor="#9a9a8e"
            style={st.addInput}
            onSubmitEditing={onAddByName}
          />
          <TouchableOpacity onPress={onAddByName}>
            <Text style={{ fontSize: 18, color: colors.market }}>＋</Text>
          </TouchableOpacity>
        </View>
      </View>

      {todo.map((l) => {
        const c = byId(l.itemId);
        return (
          <ItemRow
            key={l.key}
            emoji={c.emoji}
            name={c.name}
            note={l.note}
            rightText={l.addedBy === 'emma' ? 'Emma' : 'you'}
          />
        );
      })}

      {done.length > 0 && <Text style={st.doneHdr}>picked earlier ↓</Text>}
      {done.map((l) => {
        const c = byId(l.itemId);
        return <ItemRow key={l.key} emoji={c.emoji} name={c.name} done />;
      })}

      <Modal visible={pasteOpen} transparent animationType="fade" onRequestClose={() => setPasteOpen(false)}>
        <View style={st.dim}>
          <View style={st.sheet}>
            <Text style={st.sheetH}>Paste any text — AI parses it</Text>
            <TextInput multiline value={pasteText} onChangeText={setPasteText} style={st.pasteBox} />
            <TouchableOpacity
              style={st.cta}
              onPress={() => {
                parsePaste(pasteText);
                setPasteOpen(false);
              }}
            >
              <Text style={st.ctaTx}>✦ Parse into items</Text>
            </TouchableOpacity>
            <TouchableOpacity onPress={() => setPasteOpen(false)}>
              <Text style={st.cancelTx}>cancel</Text>
            </TouchableOpacity>
          </View>
        </View>
      </Modal>
    </ScrollView>
  );
}

const st = StyleSheet.create({
  body: { flex: 1, padding: 13, backgroundColor: colors.paper },
  hero: { backgroundColor: colors.card, borderWidth: 1.5, borderColor: colors.market, borderRadius: radius.card, padding: 13 },
  heroWho: { fontSize: 9.5, color: colors.sprout, fontFamily: 'monospace', letterSpacing: 1 },
  heroName: { fontSize: 17, fontWeight: '800', marginTop: 2, color: colors.ink },
  heroCnt: { fontSize: 11.5, color: colors.dim, marginTop: 3 },
  cta: { backgroundColor: colors.market, borderRadius: radius.button, paddingVertical: 12, alignItems: 'center', marginTop: 10 },
  ctaTx: { color: '#fff', fontWeight: '700', fontSize: 14 },
  addRow: { flexDirection: 'row', marginTop: 11, gap: 7 },
  addB: { flex: 1, backgroundColor: colors.card, borderWidth: 1, borderColor: colors.line, borderRadius: radius.button, alignItems: 'center', paddingVertical: 8 },
  addIc: { fontSize: 15 },
  addTx: { fontSize: 9.5, color: colors.dim, marginTop: 1 },
  typeBox: { flex: 1.6, flexDirection: 'row', alignItems: 'center', paddingHorizontal: 8 },
  addInput: { flex: 1, fontSize: 12, color: colors.ink, padding: 0 },
  doneHdr: { fontSize: 10, color: '#9a9a8e', fontFamily: 'monospace', marginTop: 14, marginBottom: 2 },
  dim: { flex: 1, backgroundColor: 'rgba(20,25,18,0.5)', justifyContent: 'flex-end' },
  sheet: { backgroundColor: '#fff', borderTopLeftRadius: 18, borderTopRightRadius: 18, padding: 15 },
  sheetH: { fontSize: 15.5, fontWeight: '800', color: colors.ink },
  pasteBox: { borderWidth: 1, borderColor: colors.line, borderRadius: radius.button, padding: 10, minHeight: 90, marginTop: 10, fontSize: 12.5, color: colors.ink, textAlignVertical: 'top' },
  cancelTx: { textAlign: 'center', color: colors.dim, marginTop: 10, fontSize: 12 },
});
