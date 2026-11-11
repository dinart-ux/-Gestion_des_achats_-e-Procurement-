import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { test } from 'node:test';

const read = (name) => readFile(new URL(`../Views/${name}.jsx`, import.meta.url), 'utf8');

test('écran de connexion expose les champs attendus', async () => {
  const source = await read('Login');
  assert.match(source, /Bienvenue/);
  assert.match(source, /Entrez votre matricule/);
  assert.match(source, /Entrez votre mot de passe/);
  assert.match(source, /Se connecter/);
});

test('écran des demandes propose la création et un état de chargement', async () => {
  const source = await read('Demandes');
  assert.match(source, /Demandes d/);
  assert.match(source, /Nouvelle demande/);
  assert.match(source, /Chargement/);
});

test('le formulaire réédite les catégories enregistrées et protège l’historique de validation', async () => {
  const source = await read('Demandes');
  assert.match(source, /categorieId: line\.categorie_id/);
  assert.match(source, /workflowApi\.put\(`\/demandes\/\$\{id\}`/);
  assert.match(source, /setTimeout\(\(\) => \{/);
  assert.match(source, /Brouillon enregistré/);
  assert.match(source, /\/soumettre/);
  assert.match(source, /createDraftPromiseRef/);
  assert.match(source, /creationFailedRef\.current/);
  assert.match(source, /Réessayer la sauvegarde/);
  assert.match(source, /workflowApi\.delete\(`\/demandes\/\$\{id\}`/);
  assert.match(source, /selected\.peut_supprimer/);
});

test('écran des validations présente filtres et état vide', async () => {
  const source = await read('Validations');
  assert.match(source, /Validations des demandes/);
  assert.match(source, /Tous mes services/);
  assert.match(source, /Aucune demande en attente/);
});

test('navigation configure les modules selon les rôles', async () => {
  const source = await read('Menu');
  for (const role of ['DEMANDEUR', 'MANAGER', 'SERVICE_ACHATS', 'ADMIN']) {
    assert.ok(source.includes(role), `rôle absent du menu: ${role}`);
  }
  for (const label of ['Demandes', 'Validations', 'Commandes', 'Fournisseurs', 'Offres', 'Utilisateurs']) {
    assert.ok(source.includes(label), `module absent du menu: ${label}`);
  }
});

test('la page paramètres affiche les calculs et enregistre les budgets par année', async () => {
  const source = await read('Parametres');
  assert.match(source, /workflowApi\.get\('\/budgets'/);
  assert.match(source, /workflowApi\.put\('\/budgets'/);
  assert.match(source, /solde_disponible/);
  assert.match(source, /consommation_pct/);
  assert.match(source, /alerte_seuil/);
  assert.match(source, /Créer le budget/);
});

test('le formulaire filtre les Managers par service et efface une sélection périmée', async () => {
  const source = await read('Utilisateurs');
  assert.match(source, /\/utilisateurs\/managers/);
  assert.match(source, /roleCode !== 'DEMANDEUR' \|\| !form\.service/);
  assert.match(source, /name === 'service' \|\| name === 'roleCode'.*managerId: ''/s);
  assert.match(source, /managerSelectionValide/);
  assert.match(source, /Aucun Manager actif n’est disponible pour ce service/);
});

test('la récupération vérifie matricule et e-mail et expose le parcours de réinitialisation', async () => {
  const recovery = await read('PasswordRecovery');
  const app = await readFile(new URL('../App.jsx', import.meta.url), 'utf8');
  assert.match(recovery, /\/v2\/auth\/password\/forgot/);
  assert.match(recovery, /\{ matricule: form\.matricule, email: form\.email \}/);
  assert.match(recovery, /htmlFor="recovery-email"/);
  assert.match(recovery, /type="email"/);
  assert.match(recovery, /\/v2\/auth\/password\/reset/);
  assert.match(app, /path="\/forgot-password"/);
  assert.match(app, /path="\/reset-password"/);
});
