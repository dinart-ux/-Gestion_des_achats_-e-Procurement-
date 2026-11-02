import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { test } from 'node:test';

const read = (name) => readFile(new URL(`../Views/${name}.jsx`, import.meta.url), 'utf8');

test('écran fournisseurs expose la création et l’état de chargement', async () => {
  const source = await read('Fournisseurs');
  assert.match(source, /Fournisseurs/);
  assert.match(source, /Ajouter un fournisseur/);
  assert.match(source, /Chargement/);
});

test('écran des offres expose la sélection et les actions de décision', async () => {
  const source = await read('Offres');
  assert.match(source, /Offres fournisseurs/);
  assert.match(source, /Choisir une demande/);
  assert.match(source, /retenir une proposition/);
});

test('l’écran commandes affiche correctement la devise', async () => {
  const source = await read('Commandes');
  assert.match(source, / Ar/);
  assert.doesNotMatch(source, /<Ar/);
});
