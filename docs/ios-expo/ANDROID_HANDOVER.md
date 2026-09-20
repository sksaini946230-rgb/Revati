# Android — moving the Play app to the Expo build

**Decided on 20 Sep 2026: Android moves to Expo.** One codebase, every fix and
feature once.

**It happens last, and that is part of the decision, not a delay.** The Play
app has real users with saved kundalis, settings and PRO. Until the Expo build
does everything the Kotlin one does, shipping it as an update would take a
working app away from them and give back an unfinished one. So the Kotlin app
in `~/Revati` stays the Play app and keeps getting releases until every
precondition below is met.

## Three things that are not reversible

1. **The upload key.** Play accepts a differently signed upload never, not
   once. The Play upload key must be imported into EAS before the first
   production Android build; the keystore EAS generated on 20 Sep 2026 for
   `app.revati.jyotish` is not it.
2. **The package name.** The day `android.package` becomes
   `com.aistudio.astroveda.kpvqzm`, this build is an *update* to the installed
   app. A development build with that package installed on the test phone
   **wipes the real app and its data.** `appConfig.test.ts` in the Expo repo
   fails if the package changes without the migration module beside it.
3. **The users' data.** It lives in a SQLCipher database whose key is wrapped
   by an Android Keystore alias. Once the new app has written over it, there
   is no undo.

   **The owner stated on 20 Sep 2026 that Revati has no real users yet — every
   install is one of their own test accounts.** That is taken at face value and
   it changes the size of this step from "write a native migration module" to
   "confirm the number and say so in the release notes". It is worth one look
   before the handover build: Play Console → Statistics → **active
   installs / total installs**. If that number is anything but the owner's own
   handful, the migration comes back, in full, before the package changes.

   Either way the first Expo release **never deletes the old database**. It is
   renamed, not removed, so a wrong assumption is recoverable from a bug report
   rather than being final.

## Preconditions before it ships

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
| Saved profiles, reports, recent searches | SQLCipher Room DB `astroveda_database`, passphrase wrapped by Android Keystore alias `revati_db_passphrase_wrap`, prefs `revati_db_key` | **Only if the install count says so** (see above). If it does: a small Expo native module (Kotlin) that unwraps the key with the same Keystore alias, opens the old DB, and copies rows into the new database on first launch — row counts verified before the old file is renamed, never deleted in the same launch. Same safety pattern as `DatabaseEncryption.encryptExistingIfNeeded`. |
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
