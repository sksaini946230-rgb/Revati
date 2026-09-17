# Two looks, one app — iOS and Android kept apart

The owner's instruction: the iPhone app gets a fully transparent, glass tab bar
and Apple's look throughout; Android keeps its own look; **the two must never
mix.** This document is how that is enforced rather than hoped for.

## What stays the same on both

- Brand: the Revati logo, the gold accent, the celestial background, the two
  typefaces (Outfit for Latin, Noto Sans Devanagari).
- Content, order of sections, wording, both languages, every calculation.
- Light and dark, following the system.

## What differs

| Element | iOS | Android |
|---|---|---|
| Tab bar | System tab bar via expo-router native tabs — Liquid Glass on iOS 26, shrinks on scroll, SF Symbols icons | Today's floating capsule bar, labels under every icon, selected tab in a tinted pill (see the nav-bar notes in `CLAUDE.md`) |
| Cards and grouped sections | Glass surfaces (`expo-glass-effect`) on iOS 26; solid fallback when Reduce Transparency is on or on iOS < 26 | Today's glassmorphic cards, unchanged |
| Navigation headers | Large titles that collapse, native back swipe | Today's top header bar |
| Sub-tabs | Native segmented control look | Today's pill row |
| Sheets and dialogs | Bottom sheets with detents, native alerts for confirmations | Today's dialogs |
| Pickers | Native iOS date/time wheels and menus | Today's pickers |
| Spacing and radii | A little roomier and rounder (one scale constant each) | Today's values |
| Haptics | Light haptic on tab and segment change | none (unchanged) |
| Icons | SF Symbols where one exists | Material icons (unchanged) |
| Back behaviour | Swipe back; no hardware back | Hardware back returns to Panchang |

Inputs, the PRO dialog's purchase button and destructive confirmations stay
solid on iOS too — Apple's own apps keep controls that take typing or money
opaque.

## How the separation is enforced

1. **One place decides.** `src/ui/platform/` exports the tab bar, surfaces,
   segmented control, sheet and picker. Screens import those, never a
   platform module directly, and never write `Platform.OS === 'ios'` for
   styling. Platform-specific files use the `.ios.tsx` / `.android.tsx`
   suffixes so the bundler picks one and the other is not even loaded.
2. **Tokens carry the difference.** `spacing()` and `radius()` return the iOS
   scale only on iOS; Android's numbers are the ones today's app uses.
3. **A test guards Android.** `platformLook.test.tsx` renders every shared
   component once as `android` and compares against a stored snapshot. An iOS
   change that alters the Android output fails it. A second test asserts that
   no file outside `src/ui/platform/` imports a glass or native-tab module.
4. **Lint rule:** `Platform.OS` is forbidden outside `src/ui/platform/` and
   `src/services/` (where store and sign-in genuinely differ).

## iPhone sizes to design and check

| Class | Points (w × h) | Devices |
|---|---|---|
| Small | 375 × 667 | iPhone SE (2nd/3rd gen) — home button, no notch |
| Compact | 375 × 812 | 12/13 mini |
| Standard | 390–402 × 844–874 | 13/14/15/16/17 |
| Large | 428–440 × 926–956 | Plus / Pro Max / Air |

Rules carried from the Android work:

- **The English labels are the long ones.** Check every tab and segment in
  English at the smallest width, with the widest label selected.
- **Hindi line boxes are taller.** Devanagari must never lose its matras; test
  with real Hindi, not lorem ipsum.
- **Text size:** honour Dynamic Type up to a cap (the Android app caps at
  1.3×); containers that hold text use min-height, never fixed height.
- **Safe areas:** Dynamic Island and the home indicator; nothing interactive
  under either.
- **Orientation:** portrait only on iPhone for v1. Landscape was never designed
  on Android either (`CLAUDE.md`, "Landscape is cramped").
- **iPad:** `supportsTablet: false` for v1. Apple still runs iPhone apps on iPad
  in compatibility mode and reviewers may test there, so the app must work at
  that size — it just is not laid out for it.

## Phase-1 deliverable

Mockups for every screen in `FEATURE_PARITY.md`, iOS and Android side by side,
at the small and large sizes, in Hindi and English, light and dark. The owner
approves them before any screen is built. The Android mockups are today's app
unless the owner asks for an Android redesign — that is a separate yes.
