// Runs firestore.rules against the emulator: `cd firebase && npm test`.
// These rules are the only server Revati has, so every lock is pinned here.
import { readFileSync } from 'node:fs';
import { after, before, beforeEach, test } from 'node:test';
import { assertFails, assertSucceeds, initializeTestEnvironment } from '@firebase/rules-unit-testing';
import { doc, getDoc, getDocs, collection, serverTimestamp, setDoc, deleteDoc, writeBatch } from 'firebase/firestore';

let env;
before(async () => {
  env = await initializeTestEnvironment({
    projectId: 'demo-revati',
    firestore: { rules: readFileSync('firestore.rules', 'utf8'), host: '127.0.0.1', port: 8181 },
  });
});
after(() => env.cleanup());
beforeEach(async () => {
  await env.clearFirestore();
  await env.withSecurityRulesDisabled((ctx) =>
    setDoc(doc(ctx.firestore(), 'config/admins'), { emails: ['sksaini3019@gmail.com'] }));
});

const ADMIN = { email: 'sksaini3019@gmail.com', email_verified: true, firebase: { sign_in_second_factor: 'totp' } };
const as = (uid, token = {}) => env.authenticatedContext(uid, token).firestore();
const admin = () => as('admin', ADMIN);

function banBatch(db, target, banned, { reason = 'spam', auditId = 'a1', auditTarget = target, action } = {}) {
  const b = writeBatch(db);
  b.set(doc(db, 'admin_audit', auditId), {
    actor: 'admin', actorEmail: ADMIN.email, action: action ?? (banned ? 'ban' : 'unban'),
    targetUid: auditTarget, targetEmail: 'u@x.com', detail: reason, at: serverTimestamp(),
  });
  b.set(doc(db, 'bans', target), { banned, reason, by: 'admin', at: serverTimestamp(), auditId });
  return b.commit();
}

test('an owner reaches their own profiles and nobody else does, not even an admin', async () => {
  await assertSucceeds(setDoc(doc(as('u1'), 'users/u1/kundali_profiles/p'), { name: 'A' }));
  await assertFails(getDoc(doc(as('u2'), 'users/u1/kundali_profiles/p')));
  await assertFails(getDoc(doc(admin(), 'users/u1/kundali_profiles/p')));
});

test('the gate opens for a verified allowlisted address, without the code yet', async () => {
  await assertSucceeds(getDoc(doc(as('a', { email: 'SKSAINI3019@gmail.com', email_verified: true }), 'admin_gate/probe')));
  await assertFails(getDoc(doc(as('a', { email: 'sksaini3019@gmail.com', email_verified: false }), 'admin_gate/probe')));
  await assertFails(getDoc(doc(as('b', { email: 'someone@gmail.com', email_verified: true }), 'admin_gate/probe')));
});

test('the panel needs the authenticator code, not just the address', async () => {
  const noCode = as('admin', { email: ADMIN.email, email_verified: true });
  await assertFails(getDocs(collection(noCode, 'directory')));
  await assertFails(banBatch(noCode, 'u1', true));
  await assertSucceeds(getDocs(collection(admin(), 'directory')));
});

test('a directory row carries the sign-in own email and a server time', async () => {
  const u = as('u1', { email: 'u1@x.com' });
  const row = { email: 'u1@x.com', name: 'U', createdAt: 1, lastSeenAt: serverTimestamp() };
  await assertSucceeds(setDoc(doc(u, 'directory/u1'), row));
  await assertFails(setDoc(doc(u, 'directory/u1'), { ...row, email: 'other@x.com' }));
  await assertFails(setDoc(doc(u, 'directory/u1'), { ...row, lastSeenAt: new Date(0) }));
  await assertFails(setDoc(doc(u, 'directory/u1'), { ...row, role: 'admin' }));
  await assertFails(setDoc(doc(u, 'directory/u2'), row));
  await assertFails(getDocs(collection(u, 'directory')));
  await assertSucceeds(deleteDoc(doc(u, 'directory/u1')));
});

test('a ban goes through only with its audit entry, and never without a reason', async () => {
  await assertSucceeds(banBatch(admin(), 'u1', true));
  await assertFails(setDoc(doc(admin(), 'bans/u2'), { banned: true, reason: 'x', by: 'admin', at: serverTimestamp(), auditId: 'a1' }));
  await assertFails(banBatch(admin(), 'u3', true, { reason: '' , auditId: 'a3' }));
  await assertFails(banBatch(admin(), 'u4', true, { auditId: 'a4', auditTarget: 'someone-else' }));
  await assertFails(banBatch(admin(), 'u5', true, { auditId: 'a5', action: 'unban' }));
  await assertFails(banBatch(admin(), 'admin', true, { auditId: 'a6' }));
});

test('an audit entry cannot stand alone, be edited or be deleted', async () => {
  const db = admin();
  await assertFails(setDoc(doc(db, 'admin_audit/lone'), {
    actor: 'admin', actorEmail: ADMIN.email, action: 'ban', targetUid: 'u9', targetEmail: '', detail: 'x', at: serverTimestamp(),
  }));
  await banBatch(db, 'u1', true);
  await assertFails(setDoc(doc(db, 'admin_audit/a1'), { detail: 'rewritten' }, { merge: true }));
  await assertFails(deleteDoc(doc(db, 'admin_audit/a1')));
  await assertFails(deleteDoc(doc(db, 'bans/u1')));
});

test('undo is an unban with its own entry', async () => {
  await banBatch(admin(), 'u1', true);
  await assertSucceeds(banBatch(admin(), 'u1', false, { reason: '', auditId: 'a2' }));
});

test('a banned user can read and delete their data but not back anything up', async () => {
  await setDoc(doc(as('u1'), 'users/u1/kundali_profiles/p'), { name: 'A' });
  await banBatch(admin(), 'u1', true);
  const u = as('u1');
  await assertSucceeds(getDoc(doc(u, 'bans/u1')));
  await assertSucceeds(getDoc(doc(u, 'users/u1/kundali_profiles/p')));
  await assertFails(setDoc(doc(u, 'users/u1/kundali_profiles/q'), { name: 'B' }));
  await assertSucceeds(deleteDoc(doc(u, 'users/u1/kundali_profiles/p')));
  await assertFails(getDoc(doc(as('u2'), 'bans/u1')));
});

test('nobody reads or writes the admin list from a phone, admins included', async () => {
  await assertFails(getDoc(doc(admin(), 'config/admins')));
  await assertFails(setDoc(doc(as('x', { email: 'x@gmail.com', email_verified: true }), 'config/admins'), { emails: ['x@gmail.com'] }));
});

test('anything else is denied', async () => {
  await assertFails(setDoc(doc(admin(), 'config/x'), { a: 1 }));
  await assertFails(getDoc(doc(env.unauthenticatedContext().firestore(), 'directory/u1')));
});
