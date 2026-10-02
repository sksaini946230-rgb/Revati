import { readFileSync } from 'node:fs';
import { initializeTestEnvironment, assertSucceeds, assertFails } from '@firebase/rules-unit-testing';
import { doc, setDoc, getDoc, getDocs, collection, deleteDoc, writeBatch, serverTimestamp } from 'firebase/firestore';

const env = await initializeTestEnvironment({
  projectId: 'demo-revati',
  firestore: { host: '127.0.0.1', port: 8089, rules: readFileSync(process.argv[2], 'utf8') },
});
await env.clearFirestore(); // each suite starts from an empty database
await env.withSecurityRulesDisabled(async (c) => {
  const db = c.firestore();
  await setDoc(doc(db, 'config/admins'), { emails: ['admin@revati.test'] });
  await setDoc(doc(db, 'users/u1/kundali_profiles/p1'), { name: 'A', uuid: 'p1' });
  await setDoc(doc(db, 'bans/u2'), { banned: true, reason: 'x' });
});
const anon = env.unauthenticatedContext().firestore();
const u1 = env.authenticatedContext('u1', { email: 'u1@revati.test', email_verified: true }).firestore();
const u2 = env.authenticatedContext('u2', { email: 'u2@revati.test', email_verified: true }).firestore();
const allowNoCode = env.authenticatedContext('adm', { email: 'admin@revati.test', email_verified: true }).firestore();
const unverified = env.authenticatedContext('adm2', { email: 'admin@revati.test', email_verified: false,
  firebase: { sign_in_provider: 'password', sign_in_second_factor: 'totp' } }).firestore();
const admin = env.authenticatedContext('adm', { email: 'admin@revati.test', email_verified: true,
  firebase: { sign_in_provider: 'google.com', sign_in_second_factor: 'totp' } }).firestore();

const out = [];
const t = async (name, p) => { try { await p; out.push('PASS ' + name); } catch (e) { out.push('FAIL ' + name + ' :: ' + e.message.split('\n')[0]); } };

// Ownership
await t('anonymous cannot read a backup', assertFails(getDocs(collection(anon, 'users/u1/kundali_profiles'))));
await t('user B cannot read user A backup', assertFails(getDoc(doc(u2, 'users/u1/kundali_profiles/p1'))));
await t('user B cannot delete user A backup', assertFails(deleteDoc(doc(u2, 'users/u1/kundali_profiles/p1'))));
await t('admin cannot read a user backup', assertFails(getDocs(collection(admin, 'users/u1/kundali_profiles'))));
await t('owner reads own backup', assertSucceeds(getDocs(collection(u1, 'users/u1/kundali_profiles'))));
// config
await t('nobody can read config/admins', assertFails(getDoc(doc(admin, 'config/admins'))));
await t('nobody can write config/admins', assertFails(setDoc(doc(admin, 'config/admins'), { emails: ['u1@revati.test'] })));
// directory
const row = (email) => ({ email, name: 'A', createdAt: 1, lastSeenAt: serverTimestamp() });
await t('owner writes own directory row', assertSucceeds(setDoc(doc(u1, 'directory/u1'), row('u1@revati.test'))));
await t('directory row with another email refused', assertFails(setDoc(doc(u1, 'directory/u1'), row('admin@revati.test'))));
await t('directory row for another uid refused', assertFails(setDoc(doc(u1, 'directory/u2'), row('u1@revati.test'))));
const notVerified = env.authenticatedContext('u3', { email: 'victim@revati.test', email_verified: false }).firestore();
await t('directory row with an unverified email refused', assertFails(setDoc(doc(notVerified, 'directory/u3'), row('victim@revati.test'))));
await t('directory extra field refused', assertFails(setDoc(doc(u1, 'directory/u1'), { ...row('u1@revati.test'), role: 'admin' })));
await t('user cannot list directory', assertFails(getDocs(collection(u1, 'directory'))));
await t('allowlisted without TOTP cannot list directory', assertFails(getDocs(collection(allowNoCode, 'directory'))));
await t('admin with TOTP lists directory', assertSucceeds(getDocs(collection(admin, 'directory'))));
// admin gate
await t('ordinary user gets no admin gate', assertFails(getDoc(doc(u1, 'admin_gate/probe'))));
await t('unverified admin email gets no admin gate', assertFails(getDoc(doc(unverified, 'admin_gate/probe'))));
await t('allowlisted, verified email passes admin gate', assertSucceeds(getDoc(doc(allowNoCode, 'admin_gate/probe'))));
// bans
await t('banned user reads own ban', assertSucceeds(getDoc(doc(u2, 'bans/u2'))));
await t('user cannot read another ban', assertFails(getDoc(doc(u1, 'bans/u2'))));
await t('user cannot ban anyone', assertFails(setDoc(doc(u1, 'bans/u2'), { banned: false, reason: '', by: 'u1', at: serverTimestamp(), auditId: 'a' })));
await t('user cannot unban self', assertFails(setDoc(doc(u2, 'bans/u2'), { banned: false, reason: '', by: 'u2', at: serverTimestamp(), auditId: 'a' })));
await t('admin ban without audit refused', assertFails(setDoc(doc(admin, 'bans/u1'), { banned: true, reason: 'spam', by: 'adm', at: serverTimestamp(), auditId: 'none' })));
const audited = (db, target, auditId, banned) => {
  const b = writeBatch(db);
  b.set(doc(db, 'bans', target), { banned, reason: 'spam', by: 'adm', at: serverTimestamp(), auditId });
  b.set(doc(db, 'admin_audit', auditId), { actor: 'adm', actorEmail: 'admin@revati.test', action: banned ? 'ban' : 'unban',
    targetUid: target, targetEmail: 'u1@revati.test', detail: 'spam', at: serverTimestamp() });
  return b.commit();
};
await t('admin ban with audit in one batch', assertSucceeds(audited(admin, 'u1', 'audit-1', true)));
await t('admin cannot ban self', assertFails(audited(admin, 'adm', 'audit-2', true)));
await t('allowlisted without TOTP cannot ban', assertFails(audited(allowNoCode, 'u1', 'audit-3', true)));
await t('banned user cannot back up', assertFails(setDoc(doc(u1, 'users/u1/kundali_profiles/p2'), { name: 'B', uuid: 'p2' })));
await t('banned user can still read own backup', assertSucceeds(getDocs(collection(u1, 'users/u1/kundali_profiles'))));
// audit log
await t('user cannot read audit log', assertFails(getDocs(collection(u1, 'admin_audit'))));
await t('admin cannot edit an audit entry', assertFails(setDoc(doc(admin, 'admin_audit/audit-1'), { actor: 'adm' })));
await t('admin cannot delete an audit entry', assertFails(deleteDoc(doc(admin, 'admin_audit/audit-1'))));
await t('admin reads audit log', assertSucceeds(getDocs(collection(admin, 'admin_audit'))));
// unknown paths
await t('unknown collection refused', assertFails(setDoc(doc(u1, 'anything/x'), { a: 1 })));

console.log(out.join('\n'));
console.log(`${out.filter((l) => l.startsWith('PASS')).length}/${out.length} passed`);
await env.cleanup();
