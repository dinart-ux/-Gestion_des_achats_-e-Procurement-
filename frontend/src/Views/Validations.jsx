import { useCallback, useEffect, useState } from 'react';
import workflowApi from '../service/workflowApi';
import ActionIconButton from '../components/ActionIconButton.jsx';
import IconActionButton from '../components/IconActionButton.jsx';
import './Menu.css';

const EMPTY_FILTERS = { serviceId: '', montantMin: '', montantMax: '', demandeur: '' };
const DECISION_LABELS = {
  VALIDEE: 'Validée',
  REJETEE: 'Rejetée',
  PRECISION_DEMANDEE: 'Précisions demandées',
};

function formatAmount(value) {
  return `${Number(value || 0).toLocaleString('fr-FR')} Ar`;
}

function errorMessage(error, fallback) {
  return error.response?.data?.error || error.response?.data?.message || fallback;
}

export default function Validations() {
  const [rows, setRows] = useState([]);
  const [services, setServices] = useState([]);
  const [view, setView] = useState('pending');
  const [filters, setFilters] = useState(EMPTY_FILTERS);
  const [appliedFilters, setAppliedFilters] = useState(EMPTY_FILTERS);
  const [selected, setSelected] = useState(null);
  const [comment, setComment] = useState('');
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [busy, setBusy] = useState(false);

  const load = useCallback(async (currentView, currentFilters) => {
    setBusy(true);
    try {
      const params = Object.fromEntries(
        Object.entries(currentFilters).filter(([, value]) => value !== '')
      );
      const endpoint = currentView === 'pending'
        ? '/validations/en-attente'
        : '/validations/traitees';
      const { data } = await workflowApi.get(endpoint, { params });
      setRows(Array.isArray(data) ? data : []);
      setError('');
    } catch (e) {
      setError(errorMessage(e, 'Impossible de charger les validations.'));
    } finally {
      setBusy(false);
    }
  }, []);

  useEffect(() => {
    load('pending', EMPTY_FILTERS);
    workflowApi.get('/services')
      .then(({ data }) => setServices(Array.isArray(data) ? data : []))
      .catch((e) => setError(errorMessage(e, 'Impossible de charger les services.')));
  }, [load]);

  const applyFilters = (event) => {
    event.preventDefault();
    if (filters.montantMin !== '' && filters.montantMax !== ''
      && Number(filters.montantMin) > Number(filters.montantMax)) {
      setError('Le montant minimum doit être inférieur ou égal au montant maximum.');
      return;
    }
    setAppliedFilters({ ...filters });
    load(view, filters);
  };

  const clearFilters = () => {
    setFilters(EMPTY_FILTERS);
    setAppliedFilters(EMPTY_FILTERS);
    load(view, EMPTY_FILTERS);
  };

  const changeView = (nextView) => {
    setView(nextView);
    setSelected(null);
    setComment('');
    setNotice('');
    load(nextView, appliedFilters);
  };

  const examiner = async (row) => {
    const demandeId = row.demande_id ?? row.id;
    if (demandeId == null) {
      setError("Impossible d'identifier cette demande.");
      return;
    }
    setBusy(true);
    setError('');
    try {
      const { data } = await workflowApi.get(`/demandes/${demandeId}`);
      setSelected({ ...data, demande_id: data.demande_id ?? data.id ?? demandeId });
      setComment('');
    } catch (e) {
      setError(errorMessage(e, 'Impossible de charger le détail de la demande.'));
    } finally {
      setBusy(false);
    }
  };

  const decide = async (decision) => {
    const demandeId = selected?.demande_id ?? selected?.id;
    if (!demandeId) return;
    if (decision !== 'VALIDEE' && !comment.trim()) {
      setError('Un commentaire est obligatoire pour un rejet ou une demande de précisions.');
      return;
    }
    setBusy(true);
    setError('');
    setNotice('');
    try {
      const { data } = await workflowApi.post(`/demandes/${demandeId}/decision`, {
        decision,
        commentaire: comment.trim() || null,
      });
      setSelected(null);
      setComment('');
      await load(view, appliedFilters);
      setNotice(data.statut === 'VALIDEE'
        ? 'Demande validée : le service achats peut maintenant comparer les offres.'
        : decision === 'PRECISION_DEMANDEE'
          ? 'Précisions demandées. La demande est revenue au statut brouillon.'
          : 'Demande rejetée.');
    } catch (e) {
      setError(errorMessage(e, "La décision n'a pas pu être enregistrée."));
    } finally {
      setBusy(false);
    }
  };

  const closeDetail = () => {
    setSelected(null);
    setComment('');
    setError('');
  };

  return (
    <div className="page-content">
      <section className="content-card">
        <h2>Validations des demandes</h2>
        <p>Les demandes affichées respectent votre périmètre de manager et le niveau courant du circuit.</p>

        <div className="form-actions" role="tablist" aria-label="Vues des validations">
          <button type="button" role="tab" aria-selected={view === 'pending'} className={view === 'pending' ? 'action-button' : 'quiet-button'} onClick={() => changeView('pending')}>En attente</button>
          <button type="button" role="tab" aria-selected={view === 'treated'} className={view === 'treated' ? 'action-button' : 'quiet-button'} onClick={() => changeView('treated')}>Traitées</button>
        </div>

        <form className="workflow-form filter-form" onSubmit={applyFilters}>
          <label>Service
            <select value={filters.serviceId} onChange={(e) => setFilters({ ...filters, serviceId: e.target.value })}>
              <option value="">Tous mes services</option>
              {services.map((service) => <option key={service.id} value={service.id}>{service.nom}</option>)}
            </select>
          </label>
          <label>Montant minimum
            <input type="number" min="0" step="0.01" value={filters.montantMin} onChange={(e) => setFilters({ ...filters, montantMin: e.target.value })} />
          </label>
          <label>Montant maximum
            <input type="number" min="0" step="0.01" value={filters.montantMax} onChange={(e) => setFilters({ ...filters, montantMax: e.target.value })} />
          </label>
          <label>Demandeur
            <input type="search" value={filters.demandeur} onChange={(e) => setFilters({ ...filters, demandeur: e.target.value })} placeholder="Nom ou prénom" />
          </label>
          <div className="form-actions">
            <IconActionButton icon="filter" type="submit" className="action-button" disabled={busy}>Filtrer</IconActionButton>
            <IconActionButton icon="cancel" type="button" className="quiet-button" disabled={busy} onClick={clearFilters}>Effacer</IconActionButton>
          </div>
        </form>

        {error && <p className="workflow-error" role="alert">{error}</p>}
        {notice && <p role="status">{notice}</p>}
        {busy && !selected && <p role="status">Chargement…</p>}
        {!busy && rows.length === 0 ? (
          <p>{view === 'pending' ? 'Aucune demande en attente à votre niveau.' : 'Aucune demande traitée pour ces critères.'}</p>
        ) : rows.length > 0 && (
          <div className="table-wrap"><table>
            <thead><tr>
              <th>Référence</th>{/*<th>Tentative</th>*/}<th>Demandeur</th><th>Objet</th><th>Montant</th><th>Service</th>
              {/*<th>{view === 'pending' ? 'Niveau' : 'Décision'}</th>*/}
              {view === 'treated' && <th>Décision</th>}
              {view === 'treated' && <><th>Commentaire</th><th>Date de décision</th></>}
              <th>Action</th>
            </tr></thead>
            <tbody>{rows.map((row, index) => (
              <tr key={row.validation_id ?? row.demande_id ?? index}>
                <td>{row.reference || '—'}</td>
                {/*<td>{row.tentative ?? '—'}{row.tentative_active === false ? ' (historique)' : ''}</td>*/}
                <td>{row.demandeur_prenom || ''} {row.demandeur_nom || ''}</td>
                <td>{row.objet || '—'}</td>
                <td>{formatAmount(row.montant_estime)}</td>
                <td>{row.service_nom || '—'}</td>
                {/*<td>{view === 'pending'
                  ? (row.manager_obligatoire ? 'Manager (étape obligatoire)' : (row.niveau ?? '—'))
                  : row.tentative_active === false && row.decision === 'EN_ATTENTE'
                    ? 'En attente (ancienne tentative)'
                    : (DECISION_LABELS[row.decision] || row.decision || '—')}</td>*/}
                {view === 'treated' && (
                  <td>
                    {row.tentative_active === false && row.decision === 'EN_ATTENTE'
                      ? 'En attente (ancienne tentative)'
                      : (DECISION_LABELS[row.decision] || row.decision || '—')}
                  </td>
                )}

                {view === 'treated' && <>
                  <td>{row.commentaire || '—'}</td>
                  <td>{row.date_decision ? new Date(row.date_decision).toLocaleString('fr-FR') : '—'}</td>
                </>}
                <td><ActionIconButton icon="view" label={`${view === 'pending' ? 'Examiner' : 'Consulter'} la demande ${row.reference || ''}`} disabled={busy} onClick={() => examiner(row)} /></td>
              </tr>
            ))}</tbody>
          </table></div>
        )}
      </section>

      {selected && <section className="content-card detail-card">
        <div className="section-heading"><div>
          <h2>{selected.reference || 'Demande'} — {selected.objet || 'Détail'}</h2>
          <p>{selected.centre_cout || 'Centre de coût non renseigné'} · {formatAmount(selected.montant_estime)}</p>
          <p>Statut : {selected.statut || '—'}</p>
        </div><IconActionButton icon="cancel" type="button" className="quiet-button" onClick={closeDetail} disabled={busy}>Fermer</IconActionButton></div>
        <p>{selected.justification || 'Aucune justification.'}</p>
        <h3>Lignes de la demande</h3>
        <div className="table-wrap"><table>
          <thead><tr><th>Désignation</th><th>Quantité</th><th>Prix estimé unitaire</th><th>Total estimé</th></tr></thead>
          <tbody>{Array.isArray(selected.lignes) && selected.lignes.length > 0
            ? selected.lignes.map((line, index) => <tr key={line.id ?? index}>
              <td>{line.designation || '—'}</td><td>{line.quantite ?? 0}</td>
              <td>{formatAmount(line.prix_unitaire_estime)}</td>
              <td>{formatAmount(Number(line.quantite || 0) * Number(line.prix_unitaire_estime || 0))}</td>
            </tr>)
            : <tr><td colSpan="4">Aucune ligne disponible.</td></tr>}
          </tbody>
        </table></div>
        {view === 'pending' && <>
          <label className="comment-label" htmlFor="commentaire-validation">Commentaire
            <textarea id="commentaire-validation" value={comment} onChange={(e) => setComment(e.target.value)} placeholder="Obligatoire pour un rejet ou une demande de précisions" rows={3} disabled={busy} />
          </label>
          <div className="form-actions">
            <IconActionButton icon="reject" type="button" disabled={busy} className="quiet-button danger-button" onClick={() => decide('REJETEE')}>Rejeter</IconActionButton>
            <IconActionButton icon="info" type="button" disabled={busy} className="quiet-button" onClick={() => decide('PRECISION_DEMANDEE')}>Demander des précisions</IconActionButton>
            <IconActionButton icon="validate" type="button" disabled={busy} className="action-button success-button" onClick={() => decide('VALIDEE')}>Valider</IconActionButton>
          </div>
        </>}
      </section>}
    </div>
  );
}
