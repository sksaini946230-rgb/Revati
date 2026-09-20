# Brand colours — the only place hex codes are written

Given by the owner, 20 Sep 2026. **Gold is replaced by copper-orange.**
Every other file reads a token; a literal hex in `src/` outside `theme/`
fails lint.

## The logo, given 20 Sep 2026 — final

`Revati-Expo/assets/brand/logo-1024.png`: a yellow disc, a black **R**, and a
green diya flame standing in its bowl. Every icon size is derived from that one
file by `scripts/build-icons.mjs`; none is drawn by hand.

| Part | Hex |
|---|---|
| Disc | `#F3C939` |
| Letter and bowl | `#000000` |
| Flame | `#1F5E2F` |

This replaces the two-fish mark the Kotlin app still carries. Three things
about turning it into an app icon are not obvious and are encoded in the
script rather than remembered:

- The disc **touches all four sides**, so its anti-aliased edge sits inside any
  square crop and shows as a faint ring once flattened. The disc is scaled up
  and centre-cropped so that edge falls outside the frame.
- iOS trims the icon to a squircle, and at full bleed the **R's lower leg
  reaches into that corner**. The mark is inset to 86%.
- **Apple rejects an app icon with an alpha channel**, and an image can look
  opaque while still carrying one. `icon.png` is flattened and stripped.

The logo's yellow is a brand colour, not a UI token: the app's accent stays
copper `#E8934A`, which is what the owner specified for the interface. If the
accent should become the logo's yellow instead, that is one edit in
`colors.ts` and its test — but it is a decision, not a tidy-up.

## The four the owner gave

| Role | Hex | Planet |
|---|---|---|
| Green | `#2E8B4E` | Budh |
| Copper-orange (**in place of gold**) | `#E8934A` | Surya |
| Cream / white | `#F0D09A` | Shukra |
| Dark background | `#0B0E1A` | Shani |

`#0B0E1A` is already the Kotlin app's dark background, and `#E8934A` is
already its `dateTimeAccent` — so two of the four are in the app today.
The other app in the owner's account uses `#0B2010` as its dark background;
that one is **not** ours and never appears here.

## Measured contrast (WCAG), so nothing has to be guessed later

| Pair | Ratio | Verdict |
|---|---|---|
| Copper `#E8934A` on `#0B0E1A` | 7.97 | AA and AAA for text |
| Cream `#F0D09A` on `#0B0E1A` | 13.0 | AAA |
| Green `#2E8B4E` on `#0B0E1A` | 4.50 | AA for normal text, exactly at the line |
| `#0B0E1A` on a copper fill | 7.97 | AA — dark label on copper |
| `#0B0E1A` on a green fill | 4.50 | AA — **dark label on green, not white** |
| White on a green fill | 4.27 | **fails AA** — do not put white text on `#2E8B4E` |
| Green `#2E8B4E` on white | 4.27 | **fails AA** — light theme needs a darker green |
| Copper `#E8934A` on white | 2.41 | **fails AA** — light theme needs a darker copper |

## The tokens

### Dark theme (the app's home)

| Token | Hex | Notes |
|---|---|---|
| `bg` | `#0B0E1A` | |
| `surface` | `#131728` | carried over from the Kotlin app |
| `surfaceElevated` | `#1C2136` | |
| `textPrimary` | `#F5EDD6` | never pure white |
| `textSecondary` | `#9AA5C0` | |
| `textTertiary` | `#6B7799` | raised from the Kotlin `#3D4A68`, which read as blank |
| `accent` | `#E8934A` | copper — the primary accent, replacing gold `#D4A84B` |
| `accentPressed` | `#C4742F` | |
| `onAccent` | `#0B0E1A` | label on a copper fill |
| `success` / `shubh` | `#2E8B4E` | |
| `onSuccess` | `#0B0E1A` | dark label on green, never white |
| `cream` | `#F0D09A` | headings, PRO badge, chart strokes |
| `danger` / `rahuKaal` | `#E85A4A` | |
| `link` | `#4A8FE8` | |
| `border` | `#FFFFFF1F` | |
| `glassTint` | `#FFFFFF14` | the tint under Liquid Glass on iOS |

### Light theme

The dark accents fail on white, so each is darkened until it clears 4.5:1.
It is the same hue, not a different palette.

| Token | Hex | Ratio on white |
|---|---|---|
| `bg` | `#FBFAF7` | warm paper, not grey |
| `surface` | `#FFFFFF` | |
| `surfaceElevated` | `#F2F0EA` | |
| `textPrimary` | `#1A1A17` | |
| `textSecondary` | `#5C5A52` | |
| `textTertiary` | `#9A968B` | |
| `accent` (copper) | `#B5601C` | 4.51 |
| `accentPressed` | `#8A4A12` | 6.84 |
| `onAccent` | `#FFFFFF` | |
| `success` (green) | `#1F6B3A` | 6.52 |
| `cream` → used as a **fill**, not text | `#F0D09A` | cream is a dark-theme text colour; on light it is only a background |
| `danger` | `#B3261E` | |
| `link` | `#1B5FB8` | |
| `border` | `#00000014` | |

## Rules

1. **No white text on green and no copper text on white.** The table above is
   why; a test computes every foreground/background pair the theme declares and
   fails below 4.5:1 (3:1 for large text and for icons).
2. **Gold `#D4A84B` does not appear in the Expo app.** Copper replaces it
   everywhere — buttons, PRO badge, nav active state.
3. **The Kotlin Play app is not repainted without the owner saying so.** It
   still uses gold for buttons and the PRO badge. Changing it is a separate,
   asked-for job (`OWNER_CHECKLIST.md` A11).
4. **One token per meaning.** If two tokens would hold the same hex for
   different reasons, keep both names — that is the point of tokens.
5. Every colour used in an app icon, splash, widget, notification and store
   screenshot comes from this file too.
