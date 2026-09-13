# Play Store screenshots

`goldie.config.ts` is the whole of it: the eight scenes, their headlines and
subheads, the background, the typeface and the store copy. Change it and
re-render; nothing about the finished tiles lives anywhere else.

```bash
export PATH="$HOME/.npm-global/bin:$PATH"
GOLDIE_CONFIG=$PWD/goldie/goldie.config.ts goldie frame
GOLDIE_CONFIG=$PWD/goldie/goldie.config.ts goldie verify
```

Output: `goldie/out/screenshots/pixel-10-pro/en-US/` — eight 1080x1920 PNGs,
which is 9:16 and inside Play's phone-screenshot spec. `goldie/out/` is
gitignored.

## Why there are no `flow` fields, and no `goldie capture`

goldie normally drives an emulator with argent flows. There is no Android AVD
on this machine, so the raw screens were captured from the real device instead
and `goldie/out/raw/pixel-10-pro/manifest.json` was written by hand — it is
just `{sceneId, file}` pairs with **absolute** paths, plus `device`, `udid`,
`capturedAt` and `preview: null`. Everything from `frame` onward is the normal
pipeline.

To re-capture, four things matter and each of them shows up in the tiles:

1. **Render bigger than the panel.** The device is 720x1600, well under the
   1280x2856 goldie draws into. `adb shell wm size 1080x2400` with
   `adb shell wm density 420` makes Android lay out and `screencap` at
   1080x2400 — 411dp wide, an ordinary modern phone — and the tiles stop
   looking soft. Reset both afterwards with `wm size reset` / `wm density reset`.
2. **Cut the network.** `adb shell svc wifi disable` and `svc data disable`.
   `AdBanner` is `height(0.dp)` until an ad loads, so with no network there is
   no banner in the captures and no interstitial interrupting them. Re-enable
   both when done.
3. **Clean the status bar.** SystemUI demo mode pins the clock, battery and
   signal and hides notification icons — without it a LinkedIn badge turns up
   in the middle of a tile. `settings put global sysui_demo_allowed 1`, then
   broadcast `com.android.systemui.demo` with `command enter`, `clock`,
   `battery`, `network` and `notifications visible false`. Exit it afterwards.
4. **No real user data.** The chart is "Aarav Sharma", the match is
   "Rahul & Priya" (1995-06-15 and 1998-09-22, which scores 28.5/36 — a green
   "Good Match"), and the city is Jaipur. The device's own saved profiles carry
   the owner's real name and birth time; those must never reach a tile, and
   neither must the Kundali form, whose "Recent Searches" chips show them.

## Notes

- `frame: { variant: ... }` is required even for an Android-only run. Its
  iPhone art is unused; the Pixel bezel is bundled and picked by device.
- The template is a sequence of one-tile layouts on purpose. `"editorial"`
  produced **ten** images for eight scenes, because its panorama layout splits
  one screen across two tiles — right for a strip someone scrolls, wrong for a
  Play listing that has to be eight self-contained screenshots.
- `hi-IN` has its own config in `goldie/hi/`, because goldie renders every
  locale from one set of captures and the Hindi tiles need Hindi screens:

  ```bash
  GOLDIE_CONFIG=$PWD/goldie/hi/goldie.config.ts goldie frame
  GOLDIE_CONFIG=$PWD/goldie/hi/goldie.config.ts goldie verify
  ```

  Output: `goldie/hi/out/screenshots/pixel-10-pro/hi-IN/`. Its font stack ends in
  "Kohinoor Devanagari"; without it every headline is tofu.
