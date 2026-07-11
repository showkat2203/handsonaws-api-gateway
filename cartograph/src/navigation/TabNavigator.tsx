import React from 'react';
import { Text } from 'react-native';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { TabParamList } from './types';
import HereScreen from '../screens/here/HereScreen';
import PlanStack from './PlanStack';
import YouScreen from '../screens/you/YouScreen';
import { colors } from '../theme/tokens';

const Tab = createBottomTabNavigator<TabParamList>();

const ICONS: Record<keyof TabParamList, string> = {
  Here: '📍',
  Plan: '🧭',
  You: '◎',
};

export default function TabNavigator() {
  return (
    <Tab.Navigator
      screenOptions={({ route }) => ({
        headerShown: false,
        tabBarActiveTintColor: colors.market,
        tabBarInactiveTintColor: '#8a8a7e',
        tabBarIcon: () => <Text style={{ fontSize: 16 }}>{ICONS[route.name as keyof TabParamList]}</Text>,
        tabBarLabelStyle: { fontSize: 10, fontFamily: 'monospace' },
      })}
    >
      <Tab.Screen name="Here" component={HereScreen} />
      <Tab.Screen name="Plan" component={PlanStack} />
      <Tab.Screen name="You" component={YouScreen} />
    </Tab.Navigator>
  );
}
