import React from 'react';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { RootStackParamList } from './types';
import TabNavigator from './TabNavigator';
import StoreModeScreen from '../screens/storemode/StoreModeScreen';
import VoiceSheet from '../screens/voice/VoiceSheet';
import StockScanScreen from '../screens/stockscan/StockScanScreen';

const Stack = createNativeStackNavigator<RootStackParamList>();

export default function RootNavigator() {
  return (
    <Stack.Navigator screenOptions={{ headerShown: false }}>
      <Stack.Screen name="Tabs" component={TabNavigator} />
      <Stack.Group screenOptions={{ presentation: 'fullScreenModal' }}>
        <Stack.Screen name="StoreMode" component={StoreModeScreen} />
      </Stack.Group>
      <Stack.Group screenOptions={{ presentation: 'modal' }}>
        <Stack.Screen name="VoiceSheet" component={VoiceSheet} />
        <Stack.Screen name="StockScan" component={StockScanScreen} />
      </Stack.Group>
    </Stack.Navigator>
  );
}
