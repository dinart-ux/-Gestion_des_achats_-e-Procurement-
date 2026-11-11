import assert from 'node:assert/strict';
import test from 'node:test';
import { clearAuthSession } from '../service/session.js';

test('la fermeture de session retire le JWT et les informations utilisateur', () => {
  const values = new Map([
    ['token', 'old-jwt'],
    ['user', JSON.stringify({ matricule: 'EMP-12' })],
    ['preferences', 'kept'],
  ]);
  globalThis.localStorage = {
    removeItem(key) { values.delete(key); },
  };

  clearAuthSession();

  assert.equal(values.has('token'), false);
  assert.equal(values.has('user'), false);
  assert.equal(values.get('preferences'), 'kept');
  delete globalThis.localStorage;
});
