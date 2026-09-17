# Android — whether the Play app ever moves to the Expo build

**Not decided, and not part of the iOS launch.** Until the owner says so, the
Kotlin app in `~/Revati` remains the Play app and keeps getting releases.

## Why it might move later

One codebase instead of two: every fix and feature once. The cost is a
careful migration of existing users' data and the risk of a regression on a
live app with real users.

## Preconditions before even proposing it

- The Expo Android build passes every row of `FEATURE_PARITY.md` on the test
  phone, in both languages and both themes, at 320dp and at 1.6× text.
- The engine golden report is still 0 differences.
- The Android look matches today's (or the owner approved a redesign).
- An internal-testing track on Play has run the Expo build for at least a week.

## What must carry over, or users lose data

| Item | Kotlin location | Plan |
|---|---|---|
| Package name | `com.aistudio.astroveda.kpvqzm` | Same `android.package` in the Expo config — otherwise it is a different app. |
| Signing | `upload-keystore.jks` (alias `upload`), Play app signing | Same upload key on EAS (upload the keystore as EAS credentials). |
| versionCode | commit count, now above 200 | Expo build must start **above the highest code ever uploaded**; set explicitly. |
| Saved profiles, reports, recent searches | SQLCipher Room DB `astroveda_database`, passphrase wrapped by Android Keystore alias `revati_db_passphrase_wrap`, prefs `revati_db_key` | A small Expo native module (Kotlin) that unwraps the key with the same Keystore alias, opens the old DB, and copies rows into the new database on first launch — verified row counts before the old file is renamed, never deleted in the same launch. Same safety pattern as `DatabaseEncryption.encryptExistingIfNeeded`. |
| Settings | `astroveda_prefs` (keys in `FEATURE_PARITY.md` §9) | Read once by the same native module, written into the new store. |
| PRO | Play subscription `astroveda_premium_pro_subscription` | Keep the same product ID; restore on first launch. |
| Cloud backup | Firestore `users/{uid}/kundali_profiles/{uuid}` | Same schema — no migration needed; the user stays signed in only if the Firebase Auth session is migrated, otherwise they sign in again (acceptable; say so in release notes). |
| Widgets | two `AppWidgetProvider`s | Placed widgets may disappear if the receiver class names change — keep the same fully-qualified names in the Expo widget config, or tell users to re-add. |
| Notifications | WorkManager unique work names | Cancel the old WorkManager jobs on first launch so users do not get doubled notifications. |
| Backup rules | DB and key excluded from Android backup | Same exclusions in the Expo config. |
| Data safety form | current declarations | Re-check against the Expo build's SDK list (RevenueCat, if chosen, must be added). |

## Rollout if approved

Internal testing → closed testing with the owner's second phone and an update
from the live version → staged production rollout (10% → 50% → 100%) with
Android vitals watched at each step. The Kotlin repo stays buildable the whole
time as the rollback.
