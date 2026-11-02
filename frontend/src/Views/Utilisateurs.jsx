import React, { useEffect, useMemo, useState } from 'react';

import api from '../service/api';
import workflowApi from '../service/workflowApi';
import { getRoleCode } from '../service/roleUtils';

import ActionIconButton from '../components/ActionIconButton.jsx';
import IconActionButton from '../components/IconActionButton.jsx';

import { ChevronLeft, ChevronRight, Search } from 'lucide-react';

import './Utilisateurs.css';

// ======================================================
// CONSTANTES
// ======================================================

const formulaireVide = {
  matricule: '',
  nom: '',
  prenom: '',
  email: '',
  telephone: '',
  password: '',
  passwordConfirmation: '',
  roleCode: '',
  service: '',
  managerId: '',
};

const TAILLE_PAGE = 5;

// ======================================================
// FONCTIONS UTILITAIRES
// ======================================================

function initials(user) {
  return (
    `${user.prenom?.trim()?.[0] || ''}${user.nom?.trim()?.[0] || ''}`
      .toUpperCase() || '?'
  );
}

function messageErreur(error) {
  return (
    error.response?.data?.message ||
    error.response?.data?.error ||
    error.message ||
    'Une erreur est survenue.'
  );
}

// ======================================================
// COMPOSANT PRINCIPAL
// ======================================================

export default function Utilisateurs() {
  // ----------------------------------------------------
  // ÉTATS
  // ----------------------------------------------------

  const [utilisateurs, setUtilisateurs] = useState([]);
  const [roles, setRoles] = useState([]);
  const [services, setServices] = useState([]);
  const [managers, setManagers] = useState([]);

  const [form, setForm] = useState(formulaireVide);
  const [editingId, setEditingId] = useState(null);

  const [message, setMessage] = useState('');
  const [erreur, setErreur] = useState(false);

  const [chargement, setChargement] = useState(true);
  const [chargementServices, setChargementServices] = useState(true);
  const [chargementManagers, setChargementManagers] = useState(false);
  const [erreurManagers, setErreurManagers] = useState('');

  const [envoi, setEnvoi] = useState(false);

  // Recherche et filtres
  const [recherche, setRecherche] = useState('');
  const [filtreRole, setFiltreRole] = useState('');
  const [filtreStatut, setFiltreStatut] = useState('tous');

  // Pagination
  const [page, setPage] = useState(1);

  // ----------------------------------------------------
  // CHARGEMENT DES UTILISATEURS ET DES RÔLES
  // ----------------------------------------------------

  async function chargerDonnees() {
    setChargement(true);

    try {
      const [usersResponse, rolesResponse] = await Promise.all([
        api.get('/utilisateurs'),
        api.get('/roles'),
      ]);

      setUtilisateurs(
        Array.isArray(usersResponse.data) ? usersResponse.data : []
      );

      setRoles(
        Array.isArray(rolesResponse.data) ? rolesResponse.data : []
      );

      setErreur(false);
    } catch (error) {
      setErreur(true);
      setMessage(messageErreur(error));
    } finally {
      setChargement(false);
    }
  }

  useEffect(() => {
    chargerDonnees();
  }, []);

  // ----------------------------------------------------
  // CHARGEMENT DES SERVICES
  // ----------------------------------------------------

  useEffect(() => {
    let active = true;

    workflowApi
      .get('/services')
      .then(({ data }) => {
        if (active) {
          setServices(Array.isArray(data) ? data : []);
        }
      })
      .catch((error) => {
        if (active) {
          setServices([]);
          setErreur(true);

          setMessage(
            `Impossible de charger les services : ${messageErreur(error)}`
          );
        }
      })
      .finally(() => {
        if (active) {
          setChargementServices(false);
        }
      });

    return () => {
      active = false;
    };
  }, []);

  // ----------------------------------------------------
  // CHARGEMENT DES MANAGERS SELON LE SERVICE
  // ----------------------------------------------------

  useEffect(() => {
    if (form.roleCode !== 'DEMANDEUR' || !form.service) {
      setManagers([]);
      setErreurManagers('');
      setChargementManagers(false);

      return undefined;
    }

    let active = true;

    setChargementManagers(true);
    setErreurManagers('');

    api
      .get('/utilisateurs/managers', {
        params: {
          serviceId: form.service,
        },
      })
      .then(({ data }) => {
        if (active) {
          setManagers(Array.isArray(data) ? data : []);
        }
      })
      .catch((error) => {
        if (active) {
          setManagers([]);
          setErreurManagers(messageErreur(error));
        }
      })
      .finally(() => {
        if (active) {
          setChargementManagers(false);
        }
      });

    return () => {
      active = false;
    };
  }, [form.roleCode, form.service]);

  // ----------------------------------------------------
  // GESTION DU FORMULAIRE
  // ----------------------------------------------------

  const modifierChamp = (event) => {
    const { name, value } = event.target;

    setForm((previous) => ({
      ...previous,
      [name]: value,

      ...(name === 'service' || name === 'roleCode'
        ? { managerId: '' }
        : {}),
    }));
  };

  // ----------------------------------------------------
  // MODIFICATION D'UN UTILISATEUR
  // ----------------------------------------------------

  const commencerModification = (user) => {
    setEditingId(user.matricule || user.id);

    setForm({
      matricule: user.matricule || user.id || '',
      nom: user.nom || '',
      prenom: user.prenom || '',
      email: user.email || '',
      telephone: user.telephone || '',

      password: '',
      passwordConfirmation: '',

      roleCode: getRoleCode(user.roleCode || user.role),

      service: user.service == null ? '' : String(user.service),

      managerId:
        user.managerId == null ? '' : String(user.managerId),
    });

    setErreur(false);
    setMessage('');
    setErreurManagers('');
  };

  // ----------------------------------------------------
  // ANNULATION DE LA MODIFICATION
  // ----------------------------------------------------

  const annulerModification = () => {
    setEditingId(null);
    setForm(formulaireVide);
    setManagers([]);
    setErreurManagers('');
    setMessage('');
  };

  // ----------------------------------------------------
  // VALIDATION DU MANAGER
  // ----------------------------------------------------

  const managerSelectionValide = managers.some(
    (manager) => String(manager.id) === String(form.managerId)
  );

  // ----------------------------------------------------
  // ENREGISTREMENT D'UN UTILISATEUR
  // ----------------------------------------------------

  async function handleSubmit(event) {
    event.preventDefault();

    setEnvoi(true);
    setErreur(false);
    setMessage('');

    // Vérification des mots de passe
    if (
      !editingId &&
      form.password !== form.passwordConfirmation
    ) {
      setErreur(true);
      setMessage('Les deux mots de passe ne correspondent pas.');
      setEnvoi(false);

      return;
    }

    // Préparation des données
    const payload = editingId
      ? {
          nom: form.nom.trim(),
          prenom: form.prenom.trim(),
          email: form.email.trim(),
          roleCode: form.roleCode,
          service: form.service,
          managerId:
            form.roleCode === 'DEMANDEUR'
              ? Number(form.managerId)
              : null,
          telephone: form.telephone.trim(),
        }
      : {
          matricule: form.matricule.trim(),
          nom: form.nom.trim(),
          prenom: form.prenom.trim(),
          email: form.email.trim(),
          password: form.password,
          roleCode: form.roleCode,
          service: form.service,
          managerId:
            form.roleCode === 'DEMANDEUR'
              ? Number(form.managerId)
              : null,
          telephone: form.telephone.trim(),
        };

    // Vérification du manager pour un demandeur
    if (
      form.roleCode === 'DEMANDEUR' &&
      !managerSelectionValide
    ) {
      setErreur(true);

      setMessage(
        'Sélectionnez un Manager actif du même service pour ce Demandeur.'
      );

      setEnvoi(false);

      return;
    }

    try {
      if (editingId) {
        await api.put(
          `/utilisateurs/${encodeURIComponent(editingId)}`,
          payload
        );
      } else {
        await api.post('/utilisateurs', payload);
      }

      setMessage(
        editingId
          ? 'Utilisateur modifié avec succès.'
          : 'Utilisateur créé avec succès.'
      );

      setEditingId(null);
      setForm(formulaireVide);
      setManagers([]);

      await chargerDonnees();
    } catch (error) {
      setErreur(true);
      setMessage(messageErreur(error));
    } finally {
      setEnvoi(false);
    }
  }

  // ----------------------------------------------------
  // DÉSACTIVATION D'UN UTILISATEUR
  // ----------------------------------------------------

  async function desactiver(user) {
    const matricule = user.matricule || user.id;

    const confirmation = window.confirm(
      `Désactiver le compte de ${user.prenom} ${user.nom} (${matricule}) ? Les demandes et validations associées seront conservées.`
    );

    if (!confirmation) return;

    setEnvoi(true);
    setErreur(false);
    setMessage('');

    try {
      await api.delete(
        `/utilisateurs/${encodeURIComponent(matricule)}`
      );

      setMessage(
        'Utilisateur désactivé. Ses données métier ont été conservées.'
      );

      await chargerDonnees();
    } catch (error) {
      setErreur(true);
      setMessage(messageErreur(error));
    } finally {
      setEnvoi(false);
    }
  }

  // ----------------------------------------------------
  // RECHERCHE ET FILTRAGE
  // ----------------------------------------------------

  const utilisateursFiltres = useMemo(() => {
    const query = recherche.trim().toLocaleLowerCase('fr');

    return utilisateurs.filter((user) => {
      const role =
        getRoleCode(user.roleCode || user.role) ||
        user.roleLibelle ||
        '';

      const matchQuery =
        !query ||
        [
          user.nom,
          user.prenom,
          user.matricule,
          user.id,
          user.email,
        ].some((value) =>
          String(value || '').toLocaleLowerCase('fr').includes(query)
        );

      const matchRole =
        !filtreRole || role === filtreRole;

      const matchStatut =
        filtreStatut === 'tous' ||
        (filtreStatut === 'actif'
          ? user.actif === true
          : user.actif === false);

      return matchQuery && matchRole && matchStatut;
    });
  }, [utilisateurs, recherche, filtreRole, filtreStatut]);

  // ----------------------------------------------------
  // PAGINATION
  // ----------------------------------------------------

  const pagesTotal = Math.max(
    1,
    Math.ceil(utilisateursFiltres.length / TAILLE_PAGE)
  );

  const pageCourante = Math.min(page, pagesTotal);

  const utilisateursPage = utilisateursFiltres.slice(
    (pageCourante - 1) * TAILLE_PAGE,
    pageCourante * TAILLE_PAGE
  );

  const reinitialiserFiltres = (setter) => (event) => {
    setter(event.target.value);
    setPage(1);
  };

  // ====================================================
  // AFFICHAGE
  // ====================================================

  return (
    <div className="utilisateurs-container">

      {/* ================================================
          EN-TÊTE DE LA PAGE
      ================================================ */}

      <div className="users-page-heading">
        <div>
          <h2>Gestion des utilisateurs</h2>

          <p className="users-page-description">
            Gérez les accès, les rôles et les rattachements de votre organisation.
          </p>
        </div>

        <ActionIconButton
          icon="refresh"
          label="Actualiser"
          onClick={chargerDonnees}
          disabled={chargement || envoi}
          loading={chargement}
          className="users-refresh-button"
        />
      </div>

      {/* ================================================
          MESSAGES DE NOTIFICATION
      ================================================ */}

      {message && (
        <div
          className={`users-message ${erreur ? 'is-error' : 'is-success'}`}
          role={erreur ? 'alert' : 'status'}
        >
          {message}
        </div>
      )}

      <div className="utilisateurs-layout">

        {/* ==============================================
            FORMULAIRE DE CRÉATION / MODIFICATION
        ============================================== */}

        <section
          className="utilisateur-form-section"
          aria-labelledby="users-form-title"
        >
          <form
            className="utilisateur-form"
            onSubmit={handleSubmit}
          >
            <div className="users-card-heading">
              <div>
                <h3 id="users-form-title">
                  {editingId
                    ? 'Modifier un utilisateur'
                    : 'Créer un utilisateur'}
                </h3>

                <p>
                  {editingId
                    ? 'Mettez à jour les informations du compte.'
                    : 'Renseignez les informations du nouveau compte.'}
                </p>
              </div>
            </div>

            <div className="users-form-grid">

              {/* Matricule */}
              {!editingId && (
                <div className="users-field">
                  <label htmlFor="matricule">
                    Matricule <span>*</span>
                  </label>

                  <input
                    id="matricule"
                    name="matricule"
                    value={form.matricule}
                    onChange={modifierChamp}
                    maxLength={20}
                    required
                    disabled={envoi}
                  />
                </div>
              )}

              {/* Prénom */}
              <div className="users-field">
                <label htmlFor="prenom">
                  Prénom <span>*</span>
                </label>

                <input
                  id="prenom"
                  name="prenom"
                  value={form.prenom}
                  onChange={modifierChamp}
                  maxLength={100}
                  required
                  disabled={envoi}
                  autoComplete="given-name"
                />
              </div>

              {/* Nom */}
              <div className="users-field">
                <label htmlFor="nom">
                  Nom <span>*</span>
                </label>

                <input
                  id="nom"
                  name="nom"
                  value={form.nom}
                  onChange={modifierChamp}
                  maxLength={100}
                  required
                  disabled={envoi}
                  autoComplete="family-name"
                />
              </div>

              {/* E-mail */}
              <div className="users-field users-field-full">
                <label htmlFor="email">
                  Adresse e-mail <span>*</span>
                </label>

                <input
                  id="email"
                  name="email"
                  type="email"
                  value={form.email}
                  onChange={modifierChamp}
                  maxLength={150}
                  required
                  disabled={envoi}
                  autoComplete="email"
                />
              </div>

              {/* Téléphone */}
              <div className="users-field users-field-full">
                <label htmlFor="telephone">
                  Téléphone
                </label>

                <input
                  id="telephone"
                  name="telephone"
                  type="tel"
                  value={form.telephone}
                  onChange={modifierChamp}
                  maxLength={30}
                  disabled={envoi}
                  autoComplete="tel"
                />
              </div>

              {/* Service */}
              <div className="users-field">
                <label htmlFor="service">
                  Service <span>*</span>
                </label>

                <select
                  id="service"
                  name="service"
                  value={form.service}
                  onChange={modifierChamp}
                  required
                  disabled={envoi || chargementServices}
                >
                  <option value="">
                    {chargementServices
                      ? 'Chargement…'
                      : 'Sélectionner un service'}
                  </option>

                  {services.map((service) => (
                    <option key={service.id} value={service.id}>
                      {service.nom ||
                        service.libelle ||
                        `Service ${service.id}`}
                    </option>
                  ))}
                </select>

                {!chargementServices && services.length === 0 && (
                  <small role="alert">
                    Aucun service disponible.
                  </small>
                )}
              </div>

              {/* Rôle */}
              <div className="users-field">
                <label htmlFor="roleCode">
                  Rôle <span>*</span>
                </label>

                <select
                  id="roleCode"
                  name="roleCode"
                  value={form.roleCode}
                  onChange={modifierChamp}
                  required
                  disabled={envoi}
                >
                  <option value="">
                    Sélectionner un rôle
                  </option>

                  {roles.map((role) => (
                    <option key={role.id} value={role.code}>
                      {role.libelle || role.code}
                    </option>
                  ))}
                </select>
              </div>

              {/* Manager responsable */}
              {form.roleCode === 'DEMANDEUR' && (
                <div className="users-field users-field-full">
                  <label htmlFor="managerId">
                    Manager responsable <span>*</span>
                  </label>

                  <select
                    id="managerId"
                    name="managerId"
                    value={form.managerId}
                    onChange={modifierChamp}
                    required
                    disabled={
                      envoi ||
                      chargementManagers ||
                      !form.service
                    }
                  >
                    <option value="">
                      {chargementManagers
                        ? 'Chargement des managers…'
                        : 'Sélectionner un manager'}
                    </option>

                    {managers.map((manager) => (
                      <option key={manager.id} value={manager.id}>
                        {manager.prenom} {manager.nom} ({manager.matricule})
                      </option>
                    ))}
                  </select>

                  {!chargementManagers &&
                    !erreurManagers &&
                    form.service &&
                    managers.length === 0 && (
                      <small role="alert">
                        Aucun Manager actif n’est disponible pour ce service.
                      </small>
                    )}

                  {erreurManagers && (
                    <small role="alert">
                      Impossible de charger les managers : {erreurManagers}
                    </small>
                  )}
                </div>
              )}

              {/* Mots de passe */}
              {!editingId && (
                <>
                  <div className="users-field">
                    <label htmlFor="password">
                      Mot de passe <span>*</span>
                    </label>

                    <input
                      id="password"
                      name="password"
                      type="password"
                      value={form.password}
                      onChange={modifierChamp}
                      minLength={8}
                      maxLength={72}
                      autoComplete="new-password"
                      required
                      disabled={envoi}
                    />
                  </div>

                  <div className="users-field">
                    <label htmlFor="passwordConfirmation">
                      Confirmer le mot de passe <span>*</span>
                    </label>

                    <input
                      id="passwordConfirmation"
                      name="passwordConfirmation"
                      type="password"
                      value={form.passwordConfirmation}
                      onChange={modifierChamp}
                      minLength={8}
                      maxLength={72}
                      autoComplete="new-password"
                      required
                      disabled={envoi}
                    />
                  </div>
                </>
              )}

            </div>

            {/* Actions du formulaire */}
            <div className="users-form-actions">
              {editingId && (
                <IconActionButton
                  icon="cancel"
                  className="users-secondary-button"
                  type="button"
                  onClick={annulerModification}
                  disabled={envoi}
                >
                  Annuler
                </IconActionButton>
              )}

              <IconActionButton
                icon={editingId ? 'save' : 'create'}
                className="users-primary-button"
                type="submit"
                disabled={
                  envoi ||
                  chargement ||
                  chargementServices ||
                  chargementManagers ||
                  (form.roleCode === 'DEMANDEUR' &&
                    !managerSelectionValide)
                }
              >
                {envoi
                  ? 'Enregistrement…'
                  : editingId
                    ? 'Enregistrer les modifications'
                    : 'Créer l’utilisateur'}
              </IconActionButton>
            </div>
          </form>
        </section>

        {/* ==============================================
            LISTE DES UTILISATEURS
        ============================================== */}

        <section
          className="utilisateur-list-section"
          aria-labelledby="users-list-title"
        >
          <div className="users-list-heading">
            <div>
              <h3 id="users-list-title">
                Liste des utilisateurs
              </h3>

              <p>
                {utilisateurs.length} compte
                {utilisateurs.length > 1 ? 's' : ''} au total
              </p>
            </div>
          </div>

          {/* Recherche et filtres */}
          <div className="users-list-controls">

            <label className="users-search">
              <span className="sr-only">
                Rechercher un utilisateur
              </span>
                {/*<Search
                  aria-hidden="true"
                  focusable="false"
                />*/}
            <div className="supplier-search-box">
              <span className="supplier-search-icon">⌕</span>

              <input
                type="text"
                placeholder="Nom, matricule ou e-mail…"
                value={recherche}
                onChange={reinitialiserFiltres(setRecherche)}
              />
            </div>
            </label>

            <label
              className="sr-only"
              htmlFor="users-role-filter"
            >
              Filtrer par rôle
            </label>

            <select
              id="users-role-filter"
              value={filtreRole}
              onChange={reinitialiserFiltres(setFiltreRole)}
            >
              <option value="">Tous les rôles</option>

              {roles.map((role) => (
                <option key={role.id} value={role.code}>
                  {role.libelle || role.code}
                </option>
              ))}
            </select>

            <label
              className="sr-only"
              htmlFor="users-status-filter"
            >
              Filtrer par statut
            </label>

            <select
              id="users-status-filter"
              value={filtreStatut}
              onChange={reinitialiserFiltres(setFiltreStatut)}
            >
              <option value="tous">Tous les statuts</option>
              <option value="actif">Actifs</option>
              <option value="inactif">Inactifs</option>
            </select>

          </div>

          {/* États de chargement et résultats */}
          {chargement ? (
            <p className="users-empty-state" role="status">
              Chargement des utilisateurs…
            </p>
          ) : erreur && utilisateurs.length === 0 ? (
            <p className="users-empty-state" role="alert">
              Impossible de charger les utilisateurs. {message}
            </p>
          ) : utilisateurs.length === 0 ? (
            <p className="users-empty-state">
              Aucun utilisateur n’est encore enregistré.
            </p>
          ) : utilisateursFiltres.length === 0 ? (
            <p className="users-empty-state">
              Aucun résultat ne correspond à votre recherche ou à vos filtres.
            </p>
          ) : (
            <>
              {/* Tableau */}
              <div className="table-responsive">
                <table>
                  <thead>
                    <tr>
                      <th>Utilisateur</th>
                      <th>Matricule</th>
                      <th>Service</th>
                      <th>Rôle</th>
                      <th>Statut</th>
                      <th>Actions</th>
                    </tr>
                  </thead>

                  <tbody>
                    {utilisateursPage.map((user) => {
                      const role =
                        getRoleCode(user.roleCode || user.role) ||
                        user.roleLibelle ||
                        'Non défini';

                      const service = services.find(
                        (item) =>
                          String(item.id) === String(user.service)
                      );

                      return (
                        <tr key={user.matricule || user.id}>
                          <td>
                            <div className="users-identity">
                              <span
                                className="users-avatar"
                                aria-hidden="true"
                              >
                                {initials(user)}
                              </span>

                              <span className="users-identity-copy">
                                <strong>
                                  {user.prenom} {user.nom}
                                </strong>

                                <span>{user.email}</span>
                              </span>
                            </div>
                          </td>

                          <td>
                            {user.matricule || user.id}
                          </td>

                          <td>
                            {user.serviceNom ||
                              service?.nom ||
                              user.service ||
                              'Aucun'}
                          </td>

                          <td>
                            <span
                              className={`users-role-badge users-role-${String(role).toLowerCase()}`}
                            >
                              {role}
                            </span>
                          </td>

                          <td>
                            <span
                              className={`users-status-badge ${
                                user.actif
                                  ? 'users-status-active'
                                  : 'users-status-inactive'
                              }`}
                            >
                              <span aria-hidden="true" />

                              {user.actif ? 'Actif' : 'Inactif'}
                            </span>
                          </td>

                          <td className="users-row-actions">
                            <ActionIconButton
                              icon="edit"
                              label={`Modifier l'utilisateur ${user.prenom} ${user.nom}`}
                              className="users-edit-button"
                              onClick={() => commencerModification(user)}
                              disabled={envoi}
                            />

                            {user.actif && (
                              <ActionIconButton
                                icon="disable"
                                label={`Désactiver l'utilisateur ${user.prenom} ${user.nom}`}
                                className="users-disable-button"
                                onClick={() => desactiver(user)}
                                disabled={envoi}
                              />
                            )}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>

              {/* Pagination */}
              <div className="users-pagination">
                <span>
                  {(pageCourante - 1) * TAILLE_PAGE + 1}
                  –
                  {Math.min(
                    pageCourante * TAILLE_PAGE,
                    utilisateursFiltres.length
                  )}{' '}
                  sur {utilisateursFiltres.length} résultat
                  {utilisateursFiltres.length > 1 ? 's' : ''}
                </span>

                <div>
                  <button
                    type="button"
                    onClick={() =>
                      setPage((current) => Math.max(1, current - 1))
                    }
                    disabled={pageCourante === 1}
                    aria-label="Page précédente"
                    title="Page précédente"
                  >
                    <ChevronLeft size={18} aria-hidden="true" />
                  </button>

                  <span>
                    Page {pageCourante} sur {pagesTotal}
                  </span>

                  <button
                    type="button"
                    onClick={() =>
                      setPage((current) =>
                        Math.min(pagesTotal, current + 1)
                      )
                    }
                    disabled={pageCourante === pagesTotal}
                    aria-label="Page suivante"
                    title="Page suivante"
                  >
                    <ChevronRight size={18} aria-hidden="true" />
                  </button>
                </div>
              </div>
            </>
          )}
        </section>
      </div>
    </div>
  );
}
