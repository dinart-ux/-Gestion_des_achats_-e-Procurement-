import { useCallback, useEffect, useRef, useState } from 'react';
import workflowApi from '../service/workflowApi.js';
import ActionIconButton from '../components/ActionIconButton.jsx';
import IconActionButton from '../components/IconActionButton.jsx';
import { useToastMessages } from '../components/notifications/ToastContext.jsx';
import './Menu.css';
import './Demandes.css';

const initialForm = () => ({
  centreCoutId: '',
  objet: '',
  justification: '',
  lignes: [blankLine()],
});

function blankLine() {
  return {
    type: 'ARTICLE',
    designation: '',
    quantite: '1',
    prixUnitaireEstime: '0',
    categorieId: '',
  };
}

export default function Demandes() {
  const [demandes, setDemandes] = useState([]);
  const [centres, setCentres] = useState([]);
  const [categories, setCategories] = useState([]);

  const [selected, setSelected] = useState(null);
  const [detailTab, setDetailTab] = useState('informations');
  const [formOpen, setFormOpen] = useState(false);
  const [editingId, setEditingId] = useState(null);

  const [form, setForm] = useState(initialForm);
  const [searchTerm, setSearchTerm] = useState('');

  const [loading, setLoading] = useState(true);
  const {error,success,setError,setSuccess,} = useToastMessages();
  const [saving, setSaving] = useState(false);
  const [autosaveStatus, setAutosaveStatus] = useState('');
  const [autosaveFailed, setAutosaveFailed] = useState(false);

  const savingRef = useRef(false);
  const saveQueueRef = useRef(Promise.resolve());
  const draftIdRef = useRef(null);
  const draftKeyRef = useRef(null);
  const createDraftPromiseRef = useRef(null);
  const creationFailedRef = useRef(false);
  const revisionRef = useRef(0);
  const autosaveTimerRef = useRef(null);
  const pendingSavesRef = useRef(0);
  const latestFormRef = useRef(form);
  const latestVersionRef = useRef(0);

  // Recherche des demandes
  const filteredDemandes = demandes.filter((demande) => {
    const search = searchTerm.trim().toLowerCase();

    if (!search) return true;

    const searchableValues = [
      demande.reference,
      demande.objet,
      demande.demandeur_prenom,
      demande.demandeur_nom,
      `${demande.demandeur_prenom || ''} ${demande.demandeur_nom || ''}`,
      demande.statut,
    ];

    return searchableValues.some((value) =>
      String(value || '').toLowerCase().includes(search)
    );
  });

  // Chargement des demandes, centres de coût et catégories
  const load = useCallback(async () => {
    setLoading(true);

    try {
      const [d, c, cats] = await Promise.all([
        workflowApi.get('/demandes'),
        workflowApi.get('/centres-cout'),
        workflowApi.get('/categories'),
      ]);

      setDemandes(d.data);
      setCentres(c.data);
      setCategories(cats.data);
      setError('');
    } catch (e) {
      setError(
        e.response?.data?.message ||
        'Impossible de charger les demandes.'
      );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  // Actualisation automatique lors d'un événement WebSocket.
useEffect(() => {
  const handleWorkflowUpdate = (event) => {
    const evenement = event.detail;

    if (
      evenement?.type === 'DEMANDE_SOUMISE' ||
      evenement?.type === 'VALIDATION_TRAITEE'
    ) {
      console.log(
        'Nouvelle demande soumise :',
        evenement.reference
      );

      load();
    }
  };

  window.addEventListener(
    'workflow-update',
    handleWorkflowUpdate
  );

  return () => {
    window.removeEventListener(
      'workflow-update',
      handleWorkflowUpdate
    );
  };
}, [load]);

  // Fermeture de la modale de détails avec Échap
  useEffect(() => {
    if (!selected) return undefined;

    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';

    const handleEscape = (event) => {
      if (event.key === 'Escape') {
        setSelected(null);
      }
    };

    window.addEventListener('keydown', handleEscape);

    return () => {
      document.body.style.overflow = previousOverflow;
      window.removeEventListener('keydown', handleEscape);
    };
  }, [selected]);

  // Fermeture de la modale du formulaire avec Échap
  useEffect(() => {
    if (!formOpen) return undefined;

    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';

    const handleEscape = (event) => {
      if (event.key === 'Escape' && !savingRef.current) {
        closeForm();
      }
    };

    window.addEventListener('keydown', handleEscape);

    return () => {
      document.body.style.overflow = previousOverflow;
      window.removeEventListener('keydown', handleEscape);
    };
  }, [formOpen]);

  // Fermer et réinitialiser le formulaire
  const closeForm = () => {
    if (savingRef.current) return;
    if (autosaveTimerRef.current !== null) {
      window.clearTimeout(autosaveTimerRef.current);
      autosaveTimerRef.current = null;
      persistDraft(form).then(() => closeForm()).catch(() => {});
      return;
    }
    if (pendingSavesRef.current > 0) {
      savingRef.current = true;
      setSaving(true);
      saveQueueRef.current.then(() => {
        savingRef.current = false;
        setSaving(false);
        closeForm();
      }, () => {
        savingRef.current = false;
        setSaving(false);
        setError('La dernière sauvegarde a échoué. Le formulaire reste ouvert pour réessayer.');
      });
      return;
    }

    setForm(initialForm());
    setEditingId(null);
    draftIdRef.current = null;
    draftKeyRef.current = null;
    createDraftPromiseRef.current = null;
    creationFailedRef.current = false;
    revisionRef.current = 0;
    setFormOpen(false);
    setError('');
    load();
  };

  // Ouvrir la modale pour une nouvelle demande
  const startNew = () => {
    if (savingRef.current) return;

    setForm(initialForm());
    setEditingId(null);
    draftIdRef.current = null;
    draftKeyRef.current = crypto.randomUUID();
    createDraftPromiseRef.current = null;
    creationFailedRef.current = false;
    setAutosaveFailed(false);
    revisionRef.current = 0;
    setSelected(null);
    setError('');
    setSuccess('');
    setFormOpen(true);
  };

  // Afficher les détails d'une demande
  const openDetail = async (id) => {
    try {
      const { data } = await workflowApi.get(`/demandes/${id}`);
      setDetailTab('informations');
      setSelected(data);
      setError('');
    } catch (e) {
      setError(
        e.response?.data?.message ||
        'Impossible de charger cette demande.'
      );
    }
  };

  // Modifier une ligne du formulaire
  const updateLine = (index, key, value) => {
    setForm((current) => ({
      ...current,
      lignes: current.lignes.map((line, i) =>
        i === index ? { ...line, [key]: value } : line
      ),
    }));
  };

  // Ajouter une ligne
  const addLine = () => {
    setForm((current) => ({
      ...current,
      lignes: [...current.lignes, blankLine()],
    }));
  };

  // Retirer une ligne
  const removeLine = (index) => {
    setForm((current) => ({
      ...current,
      lignes: current.lignes.filter((_, i) => i !== index),
    }));
  };

  const persistDraft = (snapshot, version = latestVersionRef.current) => {
    pendingSavesRef.current += 1;
    saveQueueRef.current = saveQueueRef.current.catch(() => {}).then(async () => {
      if (version < latestVersionRef.current &&
          JSON.stringify(snapshot) !== JSON.stringify(latestFormRef.current)) return null;
      const payload = {
        ...snapshot,
        centreCoutId: snapshot.centreCoutId ? Number(snapshot.centreCoutId) : null,
        revision: revisionRef.current,
        cleBrouillon: draftKeyRef.current,
        lignes: snapshot.lignes.map((line) => ({
          ...line,
          categorieId: line.categorieId ? Number(line.categorieId) : null,
          quantite: line.quantite === '' ? null : Number(line.quantite),
          prixUnitaireEstime: line.prixUnitaireEstime === '' ? null : Number(line.prixUnitaireEstime),
          designation: line.designation || null,
        })),
      };
      const id = draftIdRef.current;
      let response;
      if (id) {
        response = await workflowApi.put(`/demandes/${id}`, payload);
        revisionRef.current = Number(response.data.revision ?? revisionRef.current + 1);
      } else {
        if (creationFailedRef.current) {
          throw new Error('La création du brouillon a échoué. Corrigez le problème puis réessayez la sauvegarde.');
        }
        if (!createDraftPromiseRef.current) {
          createDraftPromiseRef.current = workflowApi.post('/demandes', payload)
            .then((created) => {
              draftIdRef.current = created.data.id;
              draftKeyRef.current = created.data.cle_brouillon || draftKeyRef.current;
              revisionRef.current = Number(created.data.revision ?? 0);
              return created;
            })
            .catch((e) => {
              creationFailedRef.current = true;
              setAutosaveFailed(true);
              throw e;
            })
            .finally(() => { createDraftPromiseRef.current = null; });
        }
        response = await createDraftPromiseRef.current;
      }
      setEditingId(response.data.id);
      setAutosaveFailed(false);
      setAutosaveStatus('Brouillon enregistré');
      return response.data;
    }).catch((e) => {
      setAutosaveStatus(e.response?.data?.message || 'Échec de la sauvegarde. Vérifiez votre connexion ou réessayez manuellement.');
      if (formOpen && e.config && !e.response) {
        window.setTimeout(() => persistDraft(latestFormRef.current, latestVersionRef.current).catch(() => {}), 2000);
      }
      throw e;
    });
    return saveQueueRef.current.finally(() => {
      pendingSavesRef.current = Math.max(0, pendingSavesRef.current - 1);
    });
  };

  const retryAutosave = () => {
    creationFailedRef.current = false;
    setAutosaveFailed(false);
    persistDraft(latestFormRef.current, latestVersionRef.current).catch(() => {});
  };

  // Sauvegarde périodique vers l'API : fermeture ou déconnexion ne modifie jamais le statut.
  useEffect(() => {
    if (!formOpen) return undefined;
    latestFormRef.current = form;
    latestVersionRef.current += 1;
    const version = latestVersionRef.current;
    setAutosaveStatus('Modifications en attente…');
    const timer = window.setTimeout(() => {
      autosaveTimerRef.current = null;
      if (!savingRef.current) persistDraft(form, version).catch(() => {});
    }, editingId ? 150 : 0);
    autosaveTimerRef.current = timer;
    return () => window.clearTimeout(timer);
  }, [form, formOpen]);

  // La soumission explicite attend la dernière sauvegarde avant de lancer le workflow.
  const saveDraft = async (event) => {
    event.preventDefault();
    if (savingRef.current) return;
    window.clearTimeout(autosaveTimerRef.current);
    savingRef.current = true;
    setSaving(true);
    setError('');
    creationFailedRef.current = false;
    setAutosaveFailed(false);
    try {
      await persistDraft(form);
      const { data } = await workflowApi.post(`/demandes/${draftIdRef.current}/soumettre`);
      setSuccess('Demande soumise à validation.');
      setForm(initialForm());
      setEditingId(null);
      draftIdRef.current = null;
      draftKeyRef.current = null;
      revisionRef.current = 0;
      setFormOpen(false);
      await load();
      setSelected(data);
    } catch (e) {
      setError(e.response?.data?.message || 'Impossible de sauvegarder ou soumettre la demande.');
    } finally {
      savingRef.current = false;
      setSaving(false);
    }
  };

  // Soumettre une demande à validation
  const submit = async (id) => {
    if (savingRef.current) return;

    try {
      const { data } = await workflowApi.post(
        `/demandes/${id}/soumettre`
      );

      await load();
      setSelected(data);
    } catch (e) {
      setError(
        e.response?.data?.message ||
        'Impossible de soumettre la demande.'
      );
    }
  };

  // Supprimer un brouillon
  const removeDraft = async (id) => {
    if (savingRef.current) return;

    if (
      !window.confirm(
        'Supprimer définitivement ce brouillon de demande ?'
      )
    ) return;

    savingRef.current = true;
    setSaving(true);
    setError('');

    try {
      await workflowApi.delete(`/demandes/${id}`);

      setSelected(null);
      setSuccess('Brouillon supprimé avec succès.');

      await load();
    } catch (e) {
      setError(
        e.response?.data?.message ||
        'Impossible de supprimer ce brouillon.'
      );
    } finally {
      savingRef.current = false;
      setSaving(false);
    }
  };

  // Modifier un brouillon depuis ses détails
  const editDraft = (draft) => {
    setEditingId(draft.id);
    draftIdRef.current = draft.id;
    draftKeyRef.current = draft.cle_brouillon || null;
    createDraftPromiseRef.current = null;
    creationFailedRef.current = false;
    setAutosaveFailed(false);
    revisionRef.current = Number(draft.revision || 0);

    setForm({
      centreCoutId: String(draft.centre_cout_id),
      objet: draft.objet,
      justification: draft.justification || '',
      lignes: (draft.lignes || []).map((line) => ({
        type: line.type,
        designation: line.designation,
        quantite: String(line.quantite),
        prixUnitaireEstime: String(line.prix_unitaire_estime),
        categorieId: line.categorie_id
          ? String(line.categorie_id)
          : '',
      })),
    });

    setSelected(null);
    setError('');
    setFormOpen(true);
  };

  const ariary = (value) =>
    new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'MGA',
    }).format(value || 0);

  return (
    <div className="page-content">
      <section className="content-card">

        {/* En-tête */}
        <div className="section-heading">
          <div>
            <h2>Demandes d’achat</h2>
            <p>
              Créez une demande et suivez ses validations.
            </p>
          </div>

          <IconActionButton
            icon="create"
            type="button"
            className="action-button"
            disabled={saving}
            onClick={startNew}
          >
            Nouvelle demande
          </IconActionButton>
        </div>

        {/* Barre de recherche */}
        <div className="request-search-container">
          <div className="request-search-box">
            <span className="request-search-icon" aria-hidden="true">
              ⌕
            </span>

            <input
              style={{border: "none",outline: "none"}}
              type="text"
              placeholder="Rechercher par référence, objet, demandeur ou statut..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              aria-label="Rechercher une demande"
            />

            {searchTerm && (
              <button
                type="button"
                className="request-search-clear"
                onClick={() => setSearchTerm('')}
                aria-label="Effacer la recherche"
              >
                ×
              </button>
            )}
          </div>

          <span className="request-search-count">
            {filteredDemandes.length} demande(s)
          </span>
        </div>

        {/* Messages */}
        {error && !formOpen && !selected && (
          <p className="workflow-error" role="alert">
            {error}
          </p>
        )}

        {success && (
          <p className="form-success" role="status">
            {success}
          </p>
        )}

        {/* Modale de création et modification */}
        {formOpen && (
          <div
            className="request-form-modal-backdrop"
            onMouseDown={(event) => {
              if (
                event.target === event.currentTarget &&
                !savingRef.current
              ) {
                closeForm();
              }
            }}
          >
            <form
              className="workflow-form request-form-modal"
              onSubmit={saveDraft}
              role="dialog"
              aria-modal="true"
              aria-labelledby="request-form-modal-title"
            >
              <div className="request-form-modal-header">
                <div>
                  <h3 id="request-form-modal-title">
                    {editingId
                      ? 'Modifier la demande'
                      : 'Nouvelle demande d’achat'}
                  </h3>

                  <p>
                    Renseignez les informations et les articles demandés.
                  </p>
                  <p role="status" aria-live="polite">{autosaveStatus}</p>
                  {autosaveFailed && (
                    <button type="button" onClick={retryAutosave} disabled={saving}>
                      Réessayer la sauvegarde
                    </button>
                  )}
                </div>

                <button
                  type="button"
                  className="request-form-modal-close"
                  onClick={closeForm}
                  disabled={saving}
                  aria-label="Fermer"
                >
                  ×
                </button>
              </div>

              {error && (
                <p className="workflow-error request-form-modal-error" role="alert">
                  {error}
                </p>
              )}

              {/* Informations générales */}
              <div className="request-form-general">
                <label>
                  Objet de la demande
                  <input
                    required
                    maxLength="200"
                    value={form.objet}
                    onChange={(e) =>
                      setForm({
                        ...form,
                        objet: e.target.value,
                      })
                    }
                    disabled={saving}
                    placeholder="Ex. : Achat de matériel informatique"
                  />
                </label>

                <label>
                  Centre de coût
                  <select
                    required
                    value={form.centreCoutId}
                    onChange={(e) =>
                      setForm({
                        ...form,
                        centreCoutId: e.target.value,
                      })
                    }
                    disabled={saving}
                  >
                    <option value="">Sélectionner un centre de coût…</option>

                    {centres.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.code} — {c.libelle}
                      </option>
                    ))}
                  </select>
                </label>

                <label className="request-justification">
                  Justification
                  <textarea
                    value={form.justification}
                    onChange={(e) =>
                      setForm({
                        ...form,
                        justification: e.target.value,
                      })
                    }
                    disabled={saving}
                    placeholder="Expliquez la nécessité de cet achat..."
                    rows="3"
                  />
                </label>
              </div>

              {/* Lignes de la demande */}
              <div className="request-form-lines">
                <div className="request-form-lines-heading">
                  <div>
                    <h3>Articles et services demandés</h3>
                    <p>
                      Ajoutez les articles ou services nécessaires.
                    </p>
                  </div>

                  <span className="request-line-count">
                    {form.lignes.length} ligne(s)
                  </span>
                </div>

                {form.lignes.map((line, index) => (
                  <div className="request-line" key={index}>
                    <div className="request-line-number">
                      {index + 1}
                    </div>

                    <select
                      aria-label={`Type de la ligne ${index + 1}`}
                      value={line.type}
                      onChange={(e) =>
                        updateLine(index, 'type', e.target.value)
                      }
                      disabled={saving}
                    >
                      <option value="ARTICLE">ARTICLE</option>
                      <option value="SERVICE">SERVICE</option>
                    </select>

                    <input
                      aria-label={`Désignation de la ligne ${index + 1}`}
                      required
                      maxLength="200"
                      placeholder="Désignation"
                      value={line.designation}
                      onChange={(e) =>
                        updateLine(index, 'designation', e.target.value)
                      }
                      disabled={saving}
                    />

                    <select
                      aria-label={`Catégorie de la ligne ${index + 1}`}
                      value={line.categorieId}
                      onChange={(e) =>
                        updateLine(index, 'categorieId', e.target.value)
                      }
                      disabled={saving}
                    >
                      <option value="">Sans catégorie</option>

                      {categories.map((c) => (
                        <option key={c.id} value={c.id}>
                          {c.nom}
                        </option>
                      ))}
                    </select>

                    <input
                      aria-label={`Quantité de la ligne ${index + 1}`}
                      required
                      type="number"
                      min="0.01"
                      max="9999999999.99"
                      step="0.01"
                      placeholder="Quantité"
                      value={line.quantite}
                      onChange={(e) =>
                        updateLine(index, 'quantite', e.target.value)
                      }
                      disabled={saving}
                    />

                    <input
                      aria-label={`Prix unitaire de la ligne ${index + 1}`}
                      required
                      type="number"
                      min="0"
                      max="9999999999999.99"
                      step="0.01"
                      placeholder="Prix unitaire"
                      value={line.prixUnitaireEstime}
                      onChange={(e) =>
                        updateLine(
                          index,
                          'prixUnitaireEstime',
                          e.target.value
                        )
                      }
                      disabled={saving}
                    />

                    <button
                      type="button"
                      className="request-line-remove"
                      onClick={() => removeLine(index)}
                      disabled={saving || form.lignes.length === 1}
                      title="Retirer cette ligne"
                    >
                      ×
                    </button>
                  </div>
                ))}

                <button
                  type="button"
                  className="request-add-line"
                  onClick={addLine}
                  disabled={saving}
                >
                  + Ajouter une ligne
                </button>
              </div>

              {/* Actions */}
              <div className="form-actions request-form-modal-actions">
                <IconActionButton
                  icon="cancel"
                  type="button"
                  className="quiet-button"
                  disabled={saving}
                  onClick={closeForm}
                >
                  Annuler
                </IconActionButton>

                <IconActionButton
                  icon="save"
                  type="submit"
                  className="action-button"
                  disabled={saving || !form.centreCoutId}
                >
                  {saving ? 'Enregistrement…' : 'Soumettre la demande'}
                </IconActionButton>
              </div>
            </form>
          </div>
        )}

        {/* Tableau des demandes */}
        {loading ? (
          <p className="loading-state" role="status">
            Chargement…
          </p>
        ) : filteredDemandes.length === 0 ? (
          <p className="request-empty-state">
            {searchTerm
              ? 'Aucune demande ne correspond à votre recherche.'
              : 'Aucune demande pour le moment.'}
          </p>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Référence</th>
                  <th>Objet</th>
                  <th>Demandeur</th>
                  <th>Montant</th>
                  <th>Statut</th>
                  <th></th>
                </tr>
              </thead>

              <tbody>
                {filteredDemandes.map((d) => (
                  <tr key={d.id}>
                    <td>{d.reference}</td>
                    <td>{d.objet}</td>
                    <td>
                      {d.demandeur_prenom} {d.demandeur_nom}
                    </td>
                    <td>{ariary(d.montant_estime)}</td>
                    <td>
                      <span className="status-pill">
                        {d.statut}
                      </span>
                    </td>
                    <td>
                      <ActionIconButton
                        icon="view"
                        label={`Consulter la demande ${d.reference || d.objet}`}
                        onClick={() => openDetail(d.id)}
                      />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {/* Modale de détails */}
      {selected && (
        <div className="request-modal-backdrop" onMouseDown={(event) => {
          if (event.target === event.currentTarget) setSelected(null);
        }}>
          <section className="content-card detail-card request-modal request-detail-modal"
            role="dialog" aria-modal="true" aria-labelledby="request-modal-title">
            <div className="request-detail-header">
              <div className="request-detail-heading">
                <div className="request-detail-icon" aria-hidden="true">▤</div>
                <div className="request-detail-title">
                  <span className="request-detail-eyebrow">DÉTAILS DE LA DEMANDE</span>
                  <h2 id="request-modal-title">{selected.reference || 'Demande d’achat'}</h2>
                  <p>{selected.objet || 'Sans objet renseigné'}</p>
                </div>
              </div>
              <div className="request-detail-header-actions">
                <span className={`request-detail-status status-${String(selected.statut || '').toLowerCase()}`}>
                  {selected.statut || 'Statut inconnu'}
                </span>
                <button type="button" className="request-modal-close"
                  onClick={() => setSelected(null)} aria-label="Fermer les détails de la demande">×</button>
              </div>
            </div>

            <div className="request-detail-summary">
              <div className="request-detail-summary-item">
                <span>Centre de coût</span><strong>{selected.centre_cout || '—'}</strong>
              </div>
              <div className="request-detail-summary-item">
                <span>Demandeur</span>
                <strong>{[selected.demandeur_prenom, selected.demandeur_nom].filter(Boolean).join(' ') || '—'}</strong>
              </div>
              <div className="request-detail-summary-item">
                <span>Montant estimé</span><strong>{ariary(selected.montant_estime)}</strong>
              </div>
            </div>

            <div className="request-detail-tabs" role="tablist" aria-label="Contenu de la demande">
              <button type="button" role="tab" id="request-tab-informations"
                aria-selected={detailTab === 'informations'} aria-controls="request-panel-informations"
                className={`request-detail-tab ${detailTab === 'informations' ? 'active' : ''}`}
                onClick={() => setDetailTab('informations')}>
                <span aria-hidden="true">▦</span> Informations
                <span className="request-detail-tab-count">{selected.lignes?.length || 0}</span>
              </button>
              <button type="button" role="tab" id="request-tab-historique"
                aria-selected={detailTab === 'historique'} aria-controls="request-panel-historique"
                className={`request-detail-tab ${detailTab === 'historique' ? 'active' : ''}`}
                onClick={() => setDetailTab('historique')}>
                <span aria-hidden="true">◷</span> Historique de validation
                <span className="request-detail-tab-count">{selected.historique?.length || 0}</span>
              </button>
            </div>

            {detailTab === 'informations' ? (
              <div id="request-panel-informations" className="request-detail-panel"
                role="tabpanel" aria-labelledby="request-tab-informations">
                <section className="request-detail-section">
                  <div className="request-detail-section-heading">
                    <div><h3>Informations générales</h3>
                      <p>Les informations principales de cette demande d’achat.</p></div>
                  </div>
                  <div className="request-detail-info-grid">
                    <div className="request-detail-info-card"><span>Référence</span><strong>{selected.reference || '—'}</strong></div>
                    <div className="request-detail-info-card"><span>Demandeur</span>
                      <strong>{[selected.demandeur_prenom, selected.demandeur_nom].filter(Boolean).join(' ') || '—'}</strong></div>
                    <div className="request-detail-info-card"><span>Centre de coût</span><strong>{selected.centre_cout || '—'}</strong></div>
                    <div className="request-detail-info-card"><span>Statut actuel</span><strong>{selected.statut || '—'}</strong></div>
                    <div className="request-detail-info-card request-detail-info-card-wide"><span>Objet de la demande</span><strong>{selected.objet || '—'}</strong></div>
                    <div className="request-detail-info-card request-detail-info-card-wide"><span>Justification</span>
                      <p>{selected.justification || 'Aucune justification renseignée.'}</p></div>
                  </div>
                </section>

                <section className="request-detail-section request-detail-lines-section">
                  <div className="request-detail-section-heading">
                    <div><h3>Articles et services demandés</h3>
                      <p>Le détail des lignes et des montants estimés.</p></div>
                    <span className="request-detail-count">{selected.lignes?.length || 0} ligne(s)</span>
                  </div>
                  {selected.lignes?.length ? (
                    <div className="table-wrap request-detail-table-wrap">
                      <table className="request-detail-table">
                        <thead><tr><th>Désignation</th><th>Catégorie</th><th>Quantité</th><th>Prix unitaire</th><th>Total</th></tr></thead>
                        <tbody>{selected.lignes.map((l) => (
                          <tr key={l.id}>
                            <td>{l.designation}</td><td>{l.categorie || '—'}</td><td>{l.quantite}</td>
                            <td>{ariary(l.prix_unitaire_estime)}</td>
                            <td className="request-detail-total">{ariary(Number(l.quantite || 0) * Number(l.prix_unitaire_estime || 0))}</td>
                          </tr>
                        ))}</tbody>
                      </table>
                    </div>
                  ) : <p className="request-detail-empty">Aucun article ou service enregistré.</p>}
                </section>
              </div>
            ) : (
              <div id="request-panel-historique" className="request-detail-panel"
                role="tabpanel" aria-labelledby="request-tab-historique">
                <section className="request-detail-section">
                  <div className="request-detail-section-heading">
                    <div><h3>Historique de validation</h3>
                      <p>Suivez les différentes étapes de traitement de votre demande.</p></div>
                    <span className="request-detail-count">{selected.historique?.length || 0} événement(s)</span>
                  </div>
                  {selected.historique?.length ? (
                    <ol className="request-detail-timeline">
                      {selected.historique.map((h, i) => {
                        const historyStatus = String(h.nouveau_statut || '').toLowerCase();
                        const isPending = historyStatus.includes('attente') || historyStatus.includes('cours');
                        const isRejected = historyStatus.includes('refus') || historyStatus.includes('rejet');
                        return (
                          <li key={`${h.date_changement}-${i}`}
                            className={`request-detail-timeline-item ${isPending ? 'is-pending' : ''} ${isRejected ? 'is-rejected' : ''}`}>
                            <span className="request-detail-timeline-marker" aria-hidden="true">
                              {isPending ? '…' : isRejected ? '×' : '✓'}
                            </span>
                            <div className="request-detail-history-card">
                              <div className="request-detail-history-top">
                                <strong>{h.nouveau_statut || 'Événement'}</strong>
                                <time dateTime={h.date_changement || undefined}>
                                  {h.date_changement ? new Date(h.date_changement).toLocaleString('fr-FR') : 'Date inconnue'}
                                </time>
                              </div>
                              <p className="request-detail-history-author">
                                {`${h.utilisateur_prenom || ''} ${h.utilisateur_nom || ''}`.trim() || 'Utilisateur non renseigné'}
                              </p>
                              <p className="request-detail-history-comment">{h.commentaire || 'Aucun commentaire.'}</p>
                            </div>
                          </li>
                        );
                      })}
                    </ol>
                  ) : (
                    <div className="request-detail-history-empty">
                      <span aria-hidden="true">◷</span><strong>Aucun historique disponible</strong>
                      <p>Les événements de validation apparaîtront ici lorsque la demande sera traitée.</p>
                    </div>
                  )}
                </section>
              </div>
            )}

            {selected.statut === 'BROUILLON' && (
              <div className="form-actions request-detail-actions">
                <ActionIconButton icon="edit" label={`Modifier la demande ${selected.reference}`}
                  onClick={() => editDraft(selected)} />
                {selected.peut_supprimer && (
                  <ActionIconButton icon="delete" label={`Supprimer le brouillon ${selected.reference}`}
                    onClick={() => removeDraft(selected.id)} disabled={saving} />
                )}
                <IconActionButton icon="submit" className="action-button" disabled={saving}
                  onClick={() => submit(selected.id)}>Soumettre à validation</IconActionButton>
              </div>
            )}
            <div className="request-modal-footer">
              <button type="button" className="request-modal-close-button" onClick={() => setSelected(null)}>Fermer</button>
            </div>
          </section>
        </div>
      )}
    </div>
  );
}
