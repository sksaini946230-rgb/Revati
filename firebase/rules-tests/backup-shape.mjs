import { readFileSync } from 'node:fs';
import { initializeTestEnvironment, assertSucceeds, assertFails } from '@firebase/rules-unit-testing';
import { doc, setDoc, getDocs, collection, deleteDoc } from 'firebase/firestore';

const env = await initializeTestEnvironment({
  projectId: 'demo-revati',
  firestore: { host: '127.0.0.1', port: 8089, rules: readFileSync(process.argv[2], 'utf8') },
});
await env.clearFirestore(); // each suite starts from an empty database
const me = env.authenticatedContext('u1', { email: 'a@b.c', email_verified: true }).firestore();
const other = env.authenticatedContext('u2').firestore();
const profile = { uuid: 'x-1', name: 'राम', gender: 'MALE', dateOfBirth: '25/08/1994', timeOfBirth: '14:15',
  placeOfBirth: 'New Delhi, Delhi', latitude: 28.6139, longitude: 77.209, notes: '', createdAt: 1727000000000 };
const results = [];
const t = async (name, p) => { try { await p; results.push('ok  ' + name); } catch (e) { results.push('BAD ' + name + ' ' + e.message); } };

await t('owner writes a real profile', assertSucceeds(setDoc(doc(me, 'users/u1/kundali_profiles/x-1'), profile)));
const old = { ...profile }; delete old.uuid;
await t('an old Kotlin profile without uuid', assertSucceeds(setDoc(doc(me, 'users/u1/kundali_profiles/old'), old)));
await t('a 255-character name', assertSucceeds(setDoc(doc(me, 'users/u1/kundali_profiles/long'), { ...profile, name: 'क'.repeat(255) })));
await t('owner lists and reads', assertSucceeds(getDocs(collection(me, 'users/u1/kundali_profiles'))));
await t('another user cannot read', assertFails(getDocs(collection(other, 'users/u1/kundali_profiles'))));
await t('another user cannot write', assertFails(setDoc(doc(other, 'users/u1/kundali_profiles/z'), profile)));
await t('an extra field is refused', assertFails(setDoc(doc(me, 'users/u1/kundali_profiles/y'), { ...profile, blob: 'x' })));
await t('a huge note is refused', assertFails(setDoc(doc(me, 'users/u1/kundali_profiles/y'), { ...profile, notes: 'x'.repeat(5000) })));
await t('a string latitude is refused', assertFails(setDoc(doc(me, 'users/u1/kundali_profiles/y'), { ...profile, latitude: '28' })));
await t('another subcollection is refused', assertFails(setDoc(doc(me, 'users/u1/junk/a'), { a: 1 })));
await t('the user doc itself is refused', assertFails(setDoc(doc(me, 'users/u1'), { a: 1 })));
await env.withSecurityRulesDisabled(async (c) => setDoc(doc(c.firestore(), 'bans/u1'), { banned: true }));
await t('a banned user cannot back up', assertFails(setDoc(doc(me, 'users/u1/kundali_profiles/x-2'), profile)));
await t('a banned user can still delete', assertSucceeds(deleteDoc(doc(me, 'users/u1/kundali_profiles/x-1'))));
await t('a banned user can delete the user doc', assertSucceeds(deleteDoc(doc(me, 'users/u1'))));
console.log(results.join('\n'));
await env.cleanup();
