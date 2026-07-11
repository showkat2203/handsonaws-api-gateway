# Cartograph

In-store navigation prototype — Expo (managed workflow) + TypeScript. Runs entirely on
mock data behind service interfaces; no backend, no custom native modules, works in
Expo Go on both iOS and Android.

## Run it in Expo Go

```bash
cd cartograph
npm install
npx expo start
```

Scan the QR code with the Expo Go app (iOS or Android). The app boots straight to the
**Here** tab. Use the **◎ context** pill in the header to jump between the four demo
contexts (mapped store, unmapped store, driving, idle) — there's no real GPS in this
prototype.

## Android APK via GitHub Actions

Every push that touches `cartograph/**` triggers the **Build Android APK** workflow
(`.github/workflows/build-apk.yml`). It runs `expo prebuild` + a Gradle `assembleDebug`
build and uploads the result as an artifact named `cartograph-apk`.

To install on your phone: open the workflow run on GitHub → **Artifacts** →
download `cartograph-apk` → unzip → install `app-debug.apk` (allow "install unknown
apps" when prompted).

## iOS via EAS/TestFlight later

`eas.json` includes a `preview` build profile for an internal-distribution iOS build,
but no iOS build has been run yet — that requires an Apple Developer account. Once one
is available: `eas build -p ios --profile preview`, then distribute via TestFlight.
Expo Go covers iOS testing in the meantime.

## Structure

```
src/
  theme/tokens.ts      design tokens (colors, radius, spacing)
  data/mock/            stores, catalog + aliases/intents, templates, roadStops, household
  services/              CatalogService, StoreService, RoutingService, TripService,
                         VoiceService, ContextService, MapService
  state/useStore.ts      zustand store — all app state + actions
  navigation/             Tabs (Here / Plan / You) + modals (StoreMode, VoiceSheet, StockScan)
  screens/
    here/                 HereMapped · HereUnmapped · HereTravel · HereIdle
    plan/                 Lists · TripPlan
    storemode/             StoreMode(v2) · MapPeek · RescueSheet · DriveInterstitial · TripSummary
    stockscan/             staff Stock-Scan demo
    you/
  components/             MicButton, ZoneHeader, ItemRow, DealCard, Toast, TemplateCard
```

## Swapping in a real backend

Screens only ever call functions on `src/services/*`. Every service currently reads
from `src/data/mock/*`; replacing a service's internals with real API calls (keeping
the same function signatures) is enough to swap in a live backend without touching any
screen code.

## What changed from the brief, and why

1. Placed the app under `cartograph/` inside this repo rather than a new top-level
   `cartograph` repo — repo creation for this account returned a 403 (integration not
   authorized to create repos), so the app lives alongside the unrelated AWS API
   Gateway project on the branch this session was scoped to.
2. `HereMapped`'s quick-answer chips (`restroom`, `pharmacy`, `chargers`) use a small
   hardcoded facilities map rather than the product catalog, since those aren't
   purchasable catalog items — only `milk` resolves through `CatalogService`.
3. Floor maps in Store Mode use `react-native-svg` (zone rects, dotted trail, numbered
   stops, blue position dot) instead of positioned `View`s, since `react-native-svg`
   is already an Expo Go-safe dependency and gives cleaner scaling.
4. `TripService.planBest()` computes totals/savings live from the mock catalog rather
   than hardcoding the brief's example numbers ($46.80 / $6.20) — the math is real,
   so the exact figures depend on which items are on the list at plan time.
5. Voice Sheet's "hold to talk" is a timed simulation (auto-resolves to the first
   phrase chip after ~900ms) rather than real speech-to-text, consistent with the
   brief's "no custom native modules" constraint.
