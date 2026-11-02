
import { useCallback, useEffect, useRef, useState } from 'react';
import workflowApi from '../service/workflowApi';
import ActionIconButton from '../components/ActionIconButton.jsx';
import IconActionButton from '../components/IconActionButton.jsx';
import { useToastMessages } from '../components/notifications/ToastContext';
import './Menu.css';

function getUser() {
  try {
    return JSON.parse(localStorage.getItem('user') || '{}');
  } catch {
    return {};
  }
}

function isAdministrator(user) {
  const role = user.role;

  const candidates = [
    user.roleCode,
    user.role_code,
    user.role_id,
    user.roleId,
    typeof role === 'object' && role !== null ? role.code : role,
    typeof role === 'object' && role !== null ? role.id : null,
    user.authorities,
    user.roles,
  ];

  const values = candidates.flat(Infinity).filter(
    (value) => value !== null && value !== undefined
  );

  return values.some((value) => {
    const normalized = String(value)
      .trim()
      .toUpperCase()
      .replace(/^ROLE_/, '');

    return normalized === 'ADMIN' || normalized === '4';
  });
}

export default function Parametres() {
  const user = getUser();
  const isAdmin = isAdministrator(user);

  const [rows, setRows] = useState([]);
  const [centres, setCentres] = useState([]);
  const [budgetCentres, setBudgetCentres] = useState([]);
  const [services, setServices] = useState([]);

  const [serviceForm, setServiceForm] = useState({ nom: '' });
  const [editingServiceId, setEditingServiceId] = useState(null);
  const [savingService, setSavingService] = useState(false);

  const savingServiceRef = useRef(false);
  const creatingCentreRef = useRef(false);

  const [year, setYear] = useState(new Date().getFullYear());
  const [centreFilter, setCentreFilter] = useState('');
  const [budgetFormOpen, setBudgetFormOpen] = useState(false);
  const [savingBudget, setSavingBudget] = useState(false);

  const [budgetForm, setBudgetForm] = useState({
    centreCoutId: '',
    annee: new Date().getFullYear(),
    montantAlloue: '',
    seuilAlertePct: 80,
  });

  const { setError, setSuccess } = useToastMessages();

  const [creating, setCreating] = useState(false);

  const [form, setForm] = useState({
    code: '',
    libelle: '',
    serviceId: '',
  });

  // Charger les budgets de l'année sélectionnée.
  const load = useCallback(async () => {
    try {
      const { data } = await workflowApi.get('/budgets', {
        params: { annee: Number(year) },
      });

      setRows(Array.isArray(data) ? data : []);
    } catch (e) {
      setError(
        e.response?.data?.message ||
          e.response?.data?.error ||
          'Impossible de charger les budgets.'
      );
    }
  }, [year, setError]);

  // Charger les centres de coût actifs pour la gestion des budgets.
  const loadBudgetCentres = useCallback(async () => {
    if (!isAdmin) return;

    try {
      const { data } = await workflowApi.get('/budgets/centres-cout');
      setBudgetCentres(Array.isArray(data) ? data : []);
    } catch (e) {
      setError(
        e.response?.data?.message ||
          e.response?.data?.error ||
          'Impossible de charger les centres de coût actifs.'
      );
    }
  }, [isAdmin, setError]);

  // Charger les centres de coût.
  const loadCentres = useCallback(async () => {
    try {
      const { data } = await workflowApi.get('/centres-cout');
      setCentres(Array.isArray(data) ? data : []);
    } catch (e) {
      setError(
        e.response?.data?.message ||
          e.response?.data?.error ||
          'Impossible de charger les centres de coût.'
      );
    }
  }, [setError]);

  // Charger les services.
  const loadServices = useCallback(async () => {
    try {
      const { data } = await workflowApi.get('/services');
      const liste = Array.isArray(data) ? data : [];

      setServices(liste);

      setForm((current) => {
        if (
          liste.length > 0 &&
          !liste.some(
            (service) => String(service.id) === String(current.serviceId)
          )
        ) {
          return {
            ...current,
            serviceId: String(liste[0].id),
          };
        }

        return current;
      });
    } catch (e) {
      setServices([]);
      setError(
        e.response?.data?.message ||
          'Impossible de charger les services.'
      );
    }
  }, [setError]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    loadCentres();
  }, [loadCentres]);

  useEffect(() => {
    loadBudgetCentres();
  }, [loadBudgetCentres]);

  useEffect(() => {
    if (isAdmin) {
      loadServices();
    }
  }, [isAdmin, loadServices]);

  // Créer un centre de coût.
  const createCentre = async (e) => {
    e.preventDefault();

    if (creatingCentreRef.current) return;

    setError('');
    setSuccess('');

    const code = form.code.trim();
    const libelle = form.libelle.trim();
    const serviceId = Number(form.serviceId);

    if (!code || !libelle) {
      setError('Le code et le libellé sont obligatoires.');
      return;
    }

    if (code.length > 20 || libelle.length > 100) {
      setError('Le code est limité à 20 caractères et le libellé à 100.');
      return;
    }

    if (!Number.isInteger(serviceId) || serviceId <= 0) {
      setError('Sélectionne un service valide.');
      return;
    }

    creatingCentreRef.current = true;
    setCreating(true);

    try {
      await workflowApi.post('/centres-cout', {
        code,
        libelle,
        serviceId,
      });

      setSuccess('Centre de coût créé avec succès.');

      setForm((current) => ({
        ...current,
        code: '',
        libelle: '',
      }));

      await loadCentres();
      await load();
    } catch (e) {
      setError(
        e.response?.data?.message ||
          e.response?.data?.error ||
          'Impossible de créer le centre de coût. Vérifie la route backend.'
      );
    } finally {
      creatingCentreRef.current = false;
      setCreating(false);
    }
  };

  // Créer ou modifier un service.
  const saveService = async (e) => {
    e.preventDefault();

    if (savingServiceRef.current) return;

    setError('');
    setSuccess('');

    const nom = serviceForm.nom.trim();

    if (!nom) {
      setError('Le nom du service est obligatoire.');
      return;
    }

    if (nom.length > 100) {
      setError('Le nom du service ne peut pas dépasser 100 caractères.');
      return;
    }

    savingServiceRef.current = true;
    setSavingService(true);

    try {
      if (editingServiceId !== null) {
        await workflowApi.put(`/services/${editingServiceId}`, { nom });
        setSuccess('Service modifié avec succès.');
      } else {
        await workflowApi.post('/services', { nom });
        setSuccess('Service créé avec succès.');
      }

      setServiceForm({ nom: '' });
      setEditingServiceId(null);

      await loadServices();
      await loadCentres();
    } catch (e) {
      setError(
        e.response?.data?.message ||
          e.response?.data?.error ||
          'Impossible d’enregistrer le service.'
      );
    } finally {
      savingServiceRef.current = false;
      setSavingService(false);
    }
  };

  // Préparer la modification d'un service.
  const editService = (service) => {
    setEditingServiceId(service.id);
    setServiceForm({ nom: service.nom || '' });
    setError('');
    setSuccess('');
  };

  // Annuler la modification.
  const cancelEditService = () => {
    setEditingServiceId(null);
    setServiceForm({ nom: '' });
    setError('');
    setSuccess('');
  };

  // Supprimer un service.
  const deleteService = async (service) => {
    if (savingServiceRef.current) return;

    const confirmer = window.confirm(
      `Veux-tu vraiment supprimer le service "${service.nom}" ?`
    );

    if (!confirmer) return;

    setError('');
    setSuccess('');

    savingServiceRef.current = true;
    setSavingService(true);

    try {
      const { data } = await workflowApi.delete(`/services/${service.id}`);

      setSuccess(data.message || 'Service supprimé avec succès.');

      if (editingServiceId === service.id) {
        cancelEditService();
      }

      await loadServices();
      await loadCentres();
    } catch (e) {
      setError(
        e.response?.data?.message ||
          e.response?.data?.error ||
          'Impossible de supprimer le service.'
      );
    } finally {
      savingServiceRef.current = false;
      setSavingService(false);
    }
  };

  // Modifier un champ du budget.
  const update = (id, key, value) => {
    setRows((current) =>
      current.map((row) =>
        row.centre_cout_id === id
          ? { ...row, [key]: value }
          : row
      )
    );
  };

  // Enregistrer un budget existant.
  const save = async (row) => {
    setError('');
    setSuccess('');

    const montant = Number(row.montant_alloue);
    const seuil = Number(row.seuil_alerte_pct ?? 80);
    const annee = Number(year);

    if (
      row.montant_alloue === '' ||
      !Number.isFinite(montant) ||
      montant <= 0
    ) {
      setError('Le montant alloué doit être strictement positif.');
      return;
    }

    if (!Number.isInteger(annee) || annee < 2000 || annee > 2100) {
      setError('L’année budgétaire doit être comprise entre 2000 et 2100.');
      return;
    }

    if (!Number.isFinite(seuil) || seuil < 0 || seuil > 100) {
      setError('Le seuil d’alerte doit être compris entre 0 et 100.');
      return;
    }

    try {
      await workflowApi.put('/budgets', {
        centreCoutId: row.centre_cout_id,
        annee,
        montantAlloue: montant,
        seuilAlertePct: seuil,
      });

      await load();
      setSuccess('Budget enregistré avec succès.');
    } catch (e) {
      setError(
        e.response?.data?.message ||
          e.response?.data?.error ||
          'Impossible d’enregistrer le budget.'
      );
    }
  };

  const displayedBudgetRows = rows.filter(
    (row) =>
      !centreFilter ||
      String(row.centre_cout_id) === String(centreFilter)
  );

  const openNewBudget = () => {
    setBudgetForm({
      centreCoutId: '',
      annee: Number(year),
      montantAlloue: '',
      seuilAlertePct: 80,
    });

    setBudgetFormOpen(true);
    setError('');
    setSuccess('');
  };

  // Vérifier si un budget existe déjà pour le centre et l'année choisis.
  const updateBudgetSelection = async (key, value) => {
    const next = { ...budgetForm, [key]: value };

    setBudgetForm(next);

    if (
      !next.centreCoutId ||
      !Number.isInteger(Number(next.annee)) ||
      Number(next.annee) < 2000 ||
      Number(next.annee) > 2100
    ) {
      setBudgetForm((current) => ({
        ...current,
        montantAlloue: '',
        seuilAlertePct: 80,
      }));
      return;
    }

    try {
      const { data } = await workflowApi.get('/budgets', {
        params: {
          annee: Number(next.annee),
          centreCoutId: Number(next.centreCoutId),
        },
      });

      const existing = Array.isArray(data)
        ? data.find((row) => row.budget_id)
        : null;

      setBudgetForm((current) => ({
        ...current,
        montantAlloue: existing?.montant_alloue ?? '',
        seuilAlertePct: existing?.seuil_alerte_pct ?? 80,
      }));

      if (existing) {
        setSuccess(
          'Un budget existe déjà pour ce centre et cette année ; vous pourrez le modifier.'
        );
      } else {
        setSuccess('');
      }
    } catch (e) {
      setError(
        e.response?.data?.message ||
          e.response?.data?.error ||
          'Impossible de vérifier le budget existant.'
      );
    }
  };

  // Modifier un budget existant dans le formulaire.
  const editBudget = (row) => {
    setBudgetForm({
      centreCoutId: String(row.centre_cout_id),
      annee: Number(row.annee || year),
      montantAlloue: row.montant_alloue ?? '',
      seuilAlertePct: row.seuil_alerte_pct ?? 80,
    });

    setYear(Number(row.annee || year));
    setBudgetFormOpen(true);
    setError('');
    setSuccess('');
  };

  // Créer ou modifier un budget.
  const saveBudget = async (event) => {
    event.preventDefault();

    setError('');
    setSuccess('');

    const centreCoutId = Number(budgetForm.centreCoutId);
    const montant = Number(budgetForm.montantAlloue);
    const seuil = Number(budgetForm.seuilAlertePct);
    const annee = Number(budgetForm.annee);

    if (!Number.isInteger(centreCoutId) || centreCoutId <= 0) {
      setError('Sélectionnez un centre de coût actif.');
      return;
    }

    if (
      budgetForm.montantAlloue === '' ||
      !Number.isFinite(montant) ||
      montant <= 0
    ) {
      setError('Le montant alloué doit être strictement positif.');
      return;
    }

    if (!Number.isInteger(annee) || annee < 2000 || annee > 2100) {
      setError('L’année budgétaire doit être comprise entre 2000 et 2100.');
      return;
    }

    if (!Number.isFinite(seuil) || seuil < 0 || seuil > 100) {
      setError('Le seuil d’alerte doit être compris entre 0 et 100.');
      return;
    }

    setSavingBudget(true);

    try {
      await workflowApi.put('/budgets', {
        centreCoutId,
        annee,
        montantAlloue: montant,
        seuilAlertePct: seuil,
      });

      setYear(annee);
      setCentreFilter(String(centreCoutId));
      setBudgetFormOpen(false);

      setBudgetForm({
        centreCoutId: '',
        annee,
        montantAlloue: '',
        seuilAlertePct: 80,
      });

      setSuccess('Budget enregistré avec succès.');

      const { data } = await workflowApi.get('/budgets', {
        params: { annee },
      });

      setRows(Array.isArray(data) ? data : []);
    } catch (e) {
      setError(
        e.response?.data?.message ||
          e.response?.data?.error ||
          'Impossible d’enregistrer le budget.'
      );
    } finally {
      setSavingBudget(false);
    }
  };

  return (
    <div className="page-content">
      {/* Gestion des services : administrateur uniquement */}
      {isAdmin && (
        <section id="services" className="content-card">
          <h2>Gestion des services</h2>

          <p>
            Crée, consulte, modifie ou supprime les services de
            l'organisation.
          </p>

          <form onSubmit={saveService}>
            <label>
              Nom du service

              <input
                type="text"
                value={serviceForm.nom}
                onChange={(e) =>
                  setServiceForm({ nom: e.target.value })
                }
                placeholder="Ex. Informatique"
                maxLength={100}
                disabled={savingService}
                required
              />
            </label>

            <div className="service-form-actions">
              <IconActionButton
                icon={editingServiceId !== null ? 'save' : 'create'}
                type="submit"
                disabled={savingService}
              >
                {savingService
                  ? 'Enregistrement...'
                  : editingServiceId !== null
                    ? 'Enregistrer les modifications'
                    : 'Créer le service'}
              </IconActionButton>

              {editingServiceId !== null && (
                <IconActionButton
                  icon="cancel"
                  type="button"
                  className="quiet-button"
                  onClick={cancelEditService}
                >
                  Annuler
                </IconActionButton>
              )}
            </div>
          </form>

          <h3>Liste des services</h3>

          {services.length === 0 ? (
            <p>Aucun service trouvé.</p>
          ) : (
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Nom du service</th>
                    <th>Actions</th>
                  </tr>
                </thead>

                <tbody>
                  {services.map((service) => (
                    <tr key={service.id}>
                      <td>{service.id}</td>
                      <td>{service.nom}</td>
                      <td>
                        <div className="service-row-actions">
                          <ActionIconButton
                            icon="edit"
                            label={`Modifier le service ${service.nom}`}
                            onClick={() => editService(service)}
                            disabled={savingService}
                          />

                          <ActionIconButton
                            icon="delete"
                            label={`Supprimer le service ${service.nom}`}
                            className="service-delete-button"
                            onClick={() => deleteService(service)}
                            disabled={savingService}
                          />
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      )}

      {/* Création réservée à l'administrateur */}
      {isAdmin && (
        <section className="content-card">
          <h2>Créer un centre de coût</h2>

          <p>
            Enregistre un nouveau centre de coût et associe-le à un service.
          </p>

          <form onSubmit={createCentre}>
            <label>
              Code du centre de coût

              <input
                type="text"
                value={form.code}
                onChange={(e) =>
                  setForm((current) => ({
                    ...current,
                    code: e.target.value,
                  }))
                }
                placeholder="Ex. CC-001"
                maxLength={20}
                required
                disabled={creating}
              />
            </label>

            <label>
              Libellé

              <input
                type="text"
                value={form.libelle}
                onChange={(e) =>
                  setForm((current) => ({
                    ...current,
                    libelle: e.target.value,
                  }))
                }
                placeholder="Ex. Informatique"
                maxLength={100}
                required
                disabled={creating}
              />
            </label>

            <label>
              Service associé

              <select
                value={form.serviceId}
                onChange={(e) =>
                  setForm((current) => ({
                    ...current,
                    serviceId: e.target.value,
                  }))
                }
                required
                disabled={creating || services.length === 0}
              >
                <option value="">Sélectionner un service…</option>

                {services.map((service) => (
                  <option key={service.id} value={service.id}>
                    {service.nom || service.libelle || service.code}
                  </option>
                ))}
              </select>
            </label>

            <IconActionButton
              icon="create"
              type="submit"
              disabled={creating || services.length === 0}
            >
              {creating
                ? 'Création en cours...'
                : 'Créer le centre de coût'}
            </IconActionButton>
          </form>
        </section>
      )}

      {/* Liste des centres de coût */}
      <section id="centres-de-couts" className="content-card">
        <h2>Centres de coût existants</h2>

        {centres.length === 0 ? (
          <p>Aucun centre de coût trouvé.</p>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Code</th>
                  <th>Libellé</th>
                  <th>Service</th>
                </tr>
              </thead>

              <tbody>
                {centres.map((centre) => (
                  <tr key={centre.id}>
                    <td>{centre.code}</td>
                    <td>{centre.libelle}</td>
                    <td>
                      {centre.service || centre.service_id || '—'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {/* Gestion des budgets */}
      {isAdmin && (
        <section className="content-card">
          <h2>Gestion des budgets annuels</h2>

          <IconActionButton
            icon="create"
            type="button"
            className="action-button"
            onClick={openNewBudget}
          >
            Créer un budget
          </IconActionButton>

          {budgetFormOpen && (
            <form className="filter-form" onSubmit={saveBudget}>
              <label>
                Centre de coût

                <select
                  required
                  value={budgetForm.centreCoutId}
                  onChange={(e) =>
                    updateBudgetSelection('centreCoutId', e.target.value)
                  }
                >
                  <option value="">Sélectionner un centre actif</option>

                  {budgetCentres.map((centre) => (
                    <option
                      key={centre.centre_cout_id}
                      value={centre.centre_cout_id}
                    >
                      {centre.code} — {centre.libelle}
                    </option>
                  ))}
                </select>
              </label>

              <label>
                Année budgétaire

                <input
                  required
                  type="number"
                  min="2000"
                  max="2100"
                  value={budgetForm.annee}
                  onChange={(e) =>
                    updateBudgetSelection('annee', Number(e.target.value))
                  }
                />
              </label>

              <label>
                Montant annuel alloué

                <input
                  required
                  type="number"
                  min="0.01"
                  step="0.01"
                  value={budgetForm.montantAlloue}
                  onChange={(e) =>
                    setBudgetForm((current) => ({
                      ...current,
                      montantAlloue: e.target.value,
                    }))
                  }
                />
              </label>

              <label>
                Seuil d’alerte (%)

                <input
                  required
                  type="number"
                  min="0"
                  max="100"
                  step="0.01"
                  value={budgetForm.seuilAlertePct}
                  onChange={(e) =>
                    setBudgetForm((current) => ({
                      ...current,
                      seuilAlertePct: e.target.value,
                    }))
                  }
                />
              </label>

              <div className="form-actions">
                <IconActionButton
                  icon="save"
                  type="submit"
                  className="action-button"
                  disabled={savingBudget}
                >
                  {savingBudget
                    ? 'Enregistrement…'
                    : 'Enregistrer le budget'}
                </IconActionButton>

                <IconActionButton
                  icon="cancel"
                  type="button"
                  className="quiet-button"
                  onClick={() => setBudgetFormOpen(false)}
                  disabled={savingBudget}
                >
                  Annuler
                </IconActionButton>
              </div>
            </form>
          )}
        </section>
      )}

      {/* Budgets par centre de coût */}
      <section id="budgets" className="content-card">
        <h2>Budgets par centre de coût</h2>

        <p>
          Les engagements sont comptabilisés à la validation, à partir du
          montant estimé, sur le budget de l’année de validation.
        </p>

        <label>
          Année

          <input
            type="number"
            min="2000"
            max="2100"
            value={year}
            onChange={(e) => setYear(Number(e.target.value))}
          />
        </label>

        <label>
          Centre de coût

          <select
            value={centreFilter}
            onChange={(e) => setCentreFilter(e.target.value)}
          >
            <option value="">Tous les centres</option>

            {rows.map((row) => (
              <option
                key={row.centre_cout_id}
                value={row.centre_cout_id}
              >
                {row.code} — {row.libelle}
              </option>
            ))}
          </select>
        </label>

        {displayedBudgetRows.length === 0 ? (
          <p>Aucun budget disponible pour cette année.</p>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Centre de coût</th>
                  <th>Montant alloué</th>
                  <th>Seuil d’alerte (%)</th>
                  <th>Engagé</th>
                  <th>Solde disponible</th>
                  <th>Utilisation</th>
                  <th>Alerte</th>
                  {isAdmin && <th>Action</th>}
                </tr>
              </thead>

              <tbody>
                {displayedBudgetRows.map((row) => (
                  <tr key={row.centre_cout_id}>
                    <td>
                      {row.code} — {row.libelle}
                    </td>

                    <td>
                      {isAdmin ? (
                        <input
                          aria-label={`Budget alloué ${row.code}`}
                          type="number"
                          min="0"
                          step="0.01"
                          value={row.montant_alloue ?? ''}
                          onChange={(e) =>
                            update(
                              row.centre_cout_id,
                              'montant_alloue',
                              e.target.value
                            )
                          }
                        />
                      ) : (
                        row.montant_alloue ?? 'Non défini'
                      )}
                    </td>

                    <td>
                      {isAdmin ? (
                        <input
                          aria-label={`Seuil d’alerte ${row.code}`}
                          type="number"
                          min="0"
                          max="100"
                          step="0.01"
                          value={row.seuil_alerte_pct ?? 80}
                          onChange={(e) =>
                            update(
                              row.centre_cout_id,
                              'seuil_alerte_pct',
                              e.target.value
                            )
                          }
                        />
                      ) : (
                        row.seuil_alerte_pct ?? '—'
                      )}
                    </td>

                    <td>
                      {Number(row.montant_engage || 0).toLocaleString('fr-FR')}
                    </td>

                    <td>
                      {row.montant_alloue == null
                        ? '—'
                        : Number(row.solde_disponible || 0).toLocaleString('fr-FR')}
                    </td>

                    <td>
                      {row.montant_alloue == null
                        ? '—'
                        : `${Number(row.consommation_pct || 0).toLocaleString('fr-FR')} %`}
                    </td>

                    <td>
                      {row.montant_alloue == null
                        ? 'Budget non défini'
                        : row.alerte_seuil
                          ? 'Seuil atteint'
                          : 'Sous le seuil'}
                    </td>

                    {isAdmin && (
                      <td>
                        <IconActionButton
                          icon={row.budget_id ? 'save' : 'create'}
                          type="button"
                          className="quiet-button"
                          onClick={() => save(row)}
                        >
                          {row.budget_id ? 'Enregistrer' : 'Créer le budget'}
                        </IconActionButton>
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}
