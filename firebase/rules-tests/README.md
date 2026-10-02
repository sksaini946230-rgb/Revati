# Firestore rules tests

Run against the local Firestore emulator. Nothing here touches the live project.

```
cd <scratch dir> && npm i @firebase/rules-unit-testing firebase
java -jar ~/.cache/firebase/emulators/cloud-firestore-emulator-*.jar --host 127.0.0.1 --port 8089 &
node authorization.mjs ../firestore.rules   # 32 cases: ownership, admin + TOTP, bans, audit log
node backup-shape.mjs ../firestore.rules    # 14 cases: the kundali_profiles shape and size bound
```

Last run on 3 Oct 2026: 32/32 and 14/14 against b83bc1d.
