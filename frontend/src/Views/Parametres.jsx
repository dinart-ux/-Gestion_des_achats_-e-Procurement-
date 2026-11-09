
import { useCallback, useEffect, useRef, useState } from 'react';
import workflowApi from '../service/workflowApi';
import ActionIconButton from '../components/ActionIconButton.jsx';
import IconActionButton from '../components/IconActionButton.jsx';
import { useToastMessages } from '../components/notifications/ToastContext';
import './Parametres.css';

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

  return candidates
    .flat(Infinity)
    .filter((value) => value !== null && value !== undefined)
    .some((value) => {
      const normalized = String(value)
        .trim()
        .toUpperCase()
        .replace(/^ROLE_/, '');

      return normalized === 'ADMIN' || normalized === '4';
    });
}

function formatAmount(value) {
  if (value === null || value === undefined || value === '') return '—';

  return Number(value).toLocaleString('fr-FR', {
    maximumFractionDigits: 2,
  });
}

export default function Parametres() {
  const user = getUser();
  const isAdmin = isAdministrator(user);

  const [showServices, setShowServices] = useState(false);
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

  // Création d'un centre de coût.
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

  const editService = (service) => {
    setEditingServiceId(service.id);
    setServiceForm({ nom: service.nom || '' });
    setError('');
    setSuccess('');
  };

  const cancelEditService = () => {
    setEditingServiceId(null);
    setServiceForm({ nom: '' });
    setError('');
    setSuccess('');
  };

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

  const update = (id, key, value) => {
    setRows((current) =>
      current.map((row) =>
        row.centre_cout_id === id
          ? { ...row, [key]: value }
          : row
      )
    );
  };

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
    <div className="parametres-page">
      <div className="parametres-heading">
        <div>
          <span className="parametres-eyebrow">CONFIGURATION</span>
          <h1>Paramètres</h1>
          <p>
            Gérez les services, les centres de coût et les budgets de
            votre organisation.
          </p>
        </div>
        <div className="parametres-heading-icon">
          <span>⚙</span>
        </div>
      </div>

      {/* Gestion des services */}
      {isAdmin && (
        <section id="services" className="parametres-card">
          <div className="parametres-section-heading">
            <div className="parametres-section-icon purple">▦</div>
            <div>
              <h2>Gestion des services</h2>
              <p>
                Créez, consultez, modifiez ou supprimez les services de
                l'organisation.
              </p>
            </div>
          </div>

          <form className="parametres-form service-form" onSubmit={saveService}>
            <label className="parametres-field">
              <span>Nom du service</span>
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

            <div className="parametres-form-actions">
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

          {/* Bouton afficher / masquer */}
          <div className="parametres-services-toggle">
            <div>
              <h3>Liste des services</h3>
              <span className="parametres-count">
                {services.length} service(s)
              </span>
            </div>

            <button
              type="button"
              className="parametres-toggle-button"
              aria-expanded={showServices}
              aria-controls="parametres-services-list"
              onClick={() => setShowServices((current) => !current)}
            >
              <span>
                {showServices ? 'Masquer les services' : 'Afficher les services'}
              </span>
              <span className="parametres-toggle-icon" aria-hidden="true">
                {showServices ? '▲' : '▼'}
              </span>
            </button>
          </div>

          {/* Liste cachée par défaut */}
          {showServices && (
            <div id="parametres-services-list">
              {services.length === 0 ? (
                <div className="parametres-empty">
                  <span>▦</span>
                  <strong>Aucun service trouvé</strong>
                  <p>Les services créés apparaîtront ici.</p>
                </div>
              ) : (
                <div className="parametres-table-wrap">
                  <table className="parametres-table">
                    <thead>
                      <tr>
                        <th>ID</th>
                        <th>Nom du service</th>
                        <th className="actions-column">Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {services.map((service) => (
                        <tr key={service.id}>
                          <td>
                            <span className="parametres-id">
                              #{service.id}
                            </span>
                          </td>
                          <td className="parametres-primary-cell">
                            {service.nom}
                          </td>
                          <td>
                            <div className="parametres-row-actions">
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
            </div>
          )}
        </section>
      )}

      {/* Création d'un centre de coût */}
      {isAdmin && (
        <section className="parametres-card">
          <div className="parametres-section-heading">
            <div className="parametres-section-icon blue">⌖</div>
            <div>
              <h2>Créer un centre de coût</h2>
              <p>
                Enregistrez un nouveau centre de coût et associez-le à un
                service.
              </p>
            </div>
          </div>

          <form className="parametres-form centre-form" onSubmit={createCentre}>
            <label className="parametres-field">
              <span>Code du centre de coût</span>
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

            <label className="parametres-field">
              <span>Libellé</span>
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

            <label className="parametres-field">
              <span>Service associé</span>
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
                <option value="">Sélectionner un service...</option>
                {services.map((service) => (
                  <option key={service.id} value={service.id}>
                    {service.nom || service.libelle || service.code}
                  </option>
                ))}
              </select>
            </label>

            <div className="parametres-form-actions centre-submit">
              <IconActionButton
                icon="create"
                type="submit"
                disabled={creating || services.length === 0}
              >
                {creating
                  ? 'Création en cours...'
                  : 'Créer le centre de coût'}
              </IconActionButton>
            </div>
          </form>
        </section>
      )}

      {/* Liste des centres de coût */}
      <section id="centres-de-couts" className="parametres-card">
        <div className="parametres-section-heading">
          <div className="parametres-section-icon cyan">⌖</div>
          <div>
            <h2>Centres de coût existants</h2>
            <p>Consultez les centres de coût enregistrés.</p>
          </div>
          <span className="parametres-count heading-count">
            {centres.length} centre(s)
          </span>
        </div>

        {centres.length === 0 ? (
          <div className="parametres-empty">
            <span>⌖</span>
            <strong>Aucun centre de coût trouvé</strong>
            <p>Les centres de coût disponibles apparaîtront ici.</p>
          </div>
        ) : (
          <div className="parametres-table-wrap">
            <table className="parametres-table">
              <thead>
                <tr>
                  <th>Code</th>
                  <th>Libellé</th>
                  <th>Service associé</th>
                </tr>
              </thead>
              <tbody>
                {centres.map((centre) => (
                  <tr key={centre.id}>
                    <td>
                      <span className="parametres-code">
                        {centre.code}
                      </span>
                    </td>
                    <td className="parametres-primary-cell">
                      {centre.libelle}
                    </td>
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

      {/* Gestion des budgets annuels */}
      {isAdmin && (
        <section className="parametres-card">
          <div className="parametres-section-heading">
            <div className="parametres-section-icon orange">▤</div>
            <div>
              <h2>Gestion des budgets annuels</h2>
              <p>
                Définissez le montant alloué et le seuil d'alerte de chaque
                centre de coût.
              </p>
            </div>
          </div>

          <div className="parametres-budget-create">
            <div>
              <strong>Configuration des budgets</strong>
              <p>
                Vous pouvez créer un budget ou modifier un budget existant.
              </p>
            </div>
            <IconActionButton
              icon="create"
              type="button"
              className="action-button"
              onClick={openNewBudget}
            >
              Créer un budget
            </IconActionButton>
          </div>
        </section>
      )}

      {/* Budgets par centre de coût */}
      <section id="budgets" className="parametres-card">
        <div className="parametres-section-heading">
          <div className="parametres-section-icon green">▥</div>
          <div>
            <h2>Budgets par centre de coût</h2>
            <p>
              Suivez les montants alloués, les engagements et les soldes
              disponibles.
            </p>
          </div>
        </div>

        <div className="parametres-budget-note">
          <span>ⓘ</span>
          <p>
            Les engagements sont comptabilisés à la validation, à partir du
            montant estimé, sur le budget de l’année de validation.
          </p>
        </div>

        <div className="parametres-filters">
          <label className="parametres-field">
            <span>Année budgétaire</span>
            <input
              type="number"
              min="2000"
              max="2100"
              value={year}
              onChange={(e) => setYear(Number(e.target.value))}
            />
          </label>

          <label className="parametres-field">
            <span>Centre de coût</span>
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
        </div>

        {displayedBudgetRows.length === 0 ? (
          <div className="parametres-empty">
            <span>▤</span>
            <strong>Aucun budget disponible</strong>
            <p>
              Aucun budget n'est disponible pour l'année {year} avec ce
              filtre.
            </p>
          </div>
        ) : (
          <div className="parametres-table-wrap">
            <table className="parametres-table budget-table">
              <thead>
                <tr>
                  <th>Centre de coût</th>
                  <th>Montant alloué</th>
                  <th>Seuil (%)</th>
                  <th>Engagé</th>
                  <th>Solde disponible</th>
                  <th>Utilisation</th>
                  <th>Alerte</th>
                  {isAdmin && <th>Action</th>}
                </tr>
              </thead>
              <tbody>
                {displayedBudgetRows.map((row) => {
                  const utilisation = Number(row.consommation_pct || 0);
                  const alerte = Boolean(row.alerte_seuil);

                  return (
                    <tr key={row.centre_cout_id}>
                      <td className="parametres-primary-cell">
                        <span>{row.code}</span>
                        <small>{row.libelle}</small>
                      </td>

                      <td>
                        {isAdmin ? (
                          <input
                            className="parametres-table-input"
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
                          formatAmount(row.montant_alloue)
                        )}
                      </td>

                      <td>
                        {isAdmin ? (
                          <input
                            className="parametres-table-input"
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

                      <td>{formatAmount(row.montant_engage || 0)}</td>

                      <td>
                        {row.montant_alloue == null
                          ? '—'
                          : formatAmount(row.solde_disponible || 0)}
                      </td>

                      <td>
                        {row.montant_alloue == null ? (
                          '—'
                        ) : (
                          <div className="parametres-usage">
                            <div className="parametres-usage-track">
                              <span
                                className={
                                  alerte
                                    ? 'parametres-usage-fill warning'
                                    : 'parametres-usage-fill'
                                }
                                style={{
                                  width: `${Math.min(
                                    Math.max(utilisation, 0),
                                    100
                                  )}%`,
                                }}
                              />
                            </div>
                            <strong>
                              {utilisation.toLocaleString('fr-FR')} %
                            </strong>
                          </div>
                        )}
                      </td>

                      <td>
                        {row.montant_alloue == null ? (
                          <span className="parametres-status neutral">
                            Non défini
                          </span>
                        ) : alerte ? (
                          <span className="parametres-status warning">
                            Seuil atteint
                          </span>
                        ) : (
                          <span className="parametres-status success">
                            Sous le seuil
                          </span>
                        )}
                      </td>

                      {isAdmin && (
                        <td>
                          <div className="parametres-row-actions">
                            <IconActionButton
                              icon={row.budget_id ? 'save' : 'create'}
                              type="button"
                              className="quiet-button"
                              onClick={() => save(row)}
                            >
                              {row.budget_id ? 'Enregistrer' : 'Créer'}
                            </IconActionButton>
                            {row.budget_id && (
                              <ActionIconButton
                                icon="edit"
                                label={`Modifier le budget ${row.code}`}
                                onClick={() => editBudget(row)}
                              />
                            )}
                          </div>
                        </td>
                      )}
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {/* Modale de création ou modification d'un budget */}
      {budgetFormOpen && (
        <div
          className="parametres-modal-overlay"
          onMouseDown={(e) => {
            if (e.target === e.currentTarget && !savingBudget) {
              setBudgetFormOpen(false);
            }
          }}
        >
          <section
            className="parametres-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="budget-modal-title"
          >
            <div className="parametres-modal-header">
              <div>
                <span className="parametres-eyebrow">BUDGET ANNUEL</span>
                <h2 id="budget-modal-title">
                  Configurer un budget
                </h2>
                <p>
                  Renseignez les informations du budget du centre de coût.
                </p>
              </div>
              <button
                type="button"
                className="parametres-modal-close"
                aria-label="Fermer"
                onClick={() => setBudgetFormOpen(false)}
                disabled={savingBudget}
              >
                ×
              </button>
            </div>

            <form className="parametres-modal-form" onSubmit={saveBudget}>
              <label className="parametres-field">
                <span>Centre de coût</span>
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

              <label className="parametres-field">
                <span>Année budgétaire</span>
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

              <label className="parametres-field">
                <span>Montant annuel alloué</span>
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
                  placeholder="Ex. 5000000"
                />
              </label>

              <label className="parametres-field">
                <span>Seuil d'alerte (%)</span>
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
                <small>
                  Une alerte sera affichée lorsque ce pourcentage sera atteint.
                </small>
              </label>

              <div className="parametres-modal-actions">
                <IconActionButton
                  icon="cancel"
                  type="button"
                  className="quiet-button"
                  onClick={() => setBudgetFormOpen(false)}
                  disabled={savingBudget}
                >
                  Annuler
                </IconActionButton>

                <IconActionButton
                  icon="save"
                  type="submit"
                  className="action-button"
                  disabled={savingBudget}
                >
                  {savingBudget
                    ? 'Enregistrement...'
                    : 'Enregistrer le budget'}
                </IconActionButton>
              </div>
            </form>
          </section>
        </div>
      )}
    </div>
  );
}
