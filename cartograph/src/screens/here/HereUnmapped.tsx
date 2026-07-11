import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet, ScrollView } from 'react-native';
import { useStore } from '../../state/useStore';
import { StoreService } from '../../services/StoreService';
import { TEMPLATES } from '../../data/mock/templates';
import TemplateCard from '../../components/TemplateCard';
import { colors, radius } from '../../theme/tokens';

export default function HereUnmapped() {
  const context = useStore((s) => s.context);
  const confirmedNeeds = useStore((s) => s.confirmedNeeds);
  const confirmNeed = useStore((s) => s.confirmNeed);
  const say = useStore((s) => s.say);

  const store = StoreService.get(context.storeId!);
  const template = TEMPLATES[store.type === 'gas' ? 'gas' : 'convenience'];
  const confirmed = confirmedNeeds[store.id] ?? [];
  const partial = confirmed.length >= 3;

  const confirmThree = () => {
    template.guesses.slice(0, 3).forEach((g) => confirmNeed(store.id, g.need));
    say('✨ Confirmed 3 spots — thanks!');
  };

  return (
    <ScrollView style={st.body} contentContainerStyle={{ paddingBottom: 24 }}>
      <View style={st.hero}>
        <Text style={st.heroName}>{store.name}</Text>
        <Text style={st.heroSub}>{partial ? '◐ community mapped (partial)' : store.freshness}</Text>
      </View>

      <View style={st.banner}>
        <Text style={st.bannerTx}>
          Convenience-store template · right ~8 in 10 · answers say "usually"
        </Text>
      </View>

      {template.guesses.map((g) => (
        <TemplateCard
          key={g.need}
          need={g.need}
          copy={g.copy}
          confidence={g.confidence}
          confirmed={confirmed.includes(g.need)}
          onConfirm={() => {
            confirmNeed(store.id, g.need);
            say(`Confirmed: ${g.need} ✓`);
          }}
          onReject={() => say(`Thanks — we'll re-check ${g.need}`)}
        />
      ))}

      {!partial && (
        <TouchableOpacity style={st.confirmCard} onPress={confirmThree}>
          <Text style={st.confirmTx}>✨ 10 seconds: confirm 3 spots</Text>
          <Text style={st.confirmSub}>help the next shopper skip the guesswork</Text>
        </TouchableOpacity>
      )}
    </ScrollView>
  );
}

const st = StyleSheet.create({
  body: { flex: 1, padding: 13 },
  hero: { backgroundColor: colors.card, borderWidth: 1.5, borderColor: colors.line, borderRadius: radius.card, padding: 13 },
  heroName: { fontSize: 17, fontWeight: '800', color: colors.ink },
  heroSub: { fontSize: 11.5, color: colors.dim, marginTop: 3 },
  banner: { backgroundColor: colors.tag, borderRadius: radius.card, padding: 10, marginTop: 10 },
  bannerTx: { fontSize: 11.5, color: colors.ink, fontWeight: '600' },
  confirmCard: { borderWidth: 1.5, borderStyle: 'dashed', borderColor: colors.sprout, borderRadius: radius.card, padding: 12, alignItems: 'center', marginTop: 12, backgroundColor: colors.card },
  confirmTx: { fontSize: 13, fontWeight: '700', color: colors.market },
  confirmSub: { fontSize: 10.5, color: colors.dim, marginTop: 2 },
});
