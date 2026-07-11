import React from 'react';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import ListsScreen from '../screens/plan/ListsScreen';
import TripPlanScreen from '../screens/plan/TripPlanScreen';
import { colors } from '../theme/tokens';

export type PlanStackParamList = {
  Lists: undefined;
  TripPlan: undefined;
};

const Stack = createNativeStackNavigator<PlanStackParamList>();

export default function PlanStack() {
  return (
    <Stack.Navigator
      screenOptions={{
        headerStyle: { backgroundColor: colors.market },
        headerTintColor: '#fff',
        headerTitleStyle: { fontWeight: '800' },
      }}
    >
      <Stack.Screen name="Lists" component={ListsScreen} options={{ title: 'Home list' }} />
      <Stack.Screen name="TripPlan" component={TripPlanScreen} options={{ title: 'Your trip plan' }} />
    </Stack.Navigator>
  );
}
