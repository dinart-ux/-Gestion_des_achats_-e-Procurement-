
import { useCallback, useEffect, useMemo, useState } from 'react';
import workflowApi from '../service/workflowApi';
import IconActionButton from '../components/IconActionButton.jsx';
import { useToastMessages } from '../components/notifications/ToastContext';
import './Offres.css';

const emptyForm = {
  fournisseurId: '',
  montantTotal: '',
  delaiLivraisonJours: '',
  conditions: '',
};

const formatAmount = (value) =>
  `${Number(value || 0).toLocaleString('fr-FR')} Ar`;

const getErrorMessage = (err, fallback) =>
  err.response?.data?.message ||
  err.response?.data?.error ||
  fallback;

export default function Offres() {
  const [demandes, setDemandes] = useState([]);
  const [fournisseurs, setFournisseurs] = useState([]);
  const [offersByDemand, setOffersByDemand] = useState({});
  const [offersLoadErrors, setOffersLoadErrors] = useState({});

  const [selected, setSelected] = useState('');
  const [offres, setOffres] = useState([]);
  const [form, setForm] = useState(emptyForm);

  const [search, setSearch] = useState('');
  const [filter, setFilter] = useState('TOUTES');

  const { error, setError, setSuccess } = useToastMessages();

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [loadingOffers, setLoadingOffers] = useState(false);
  const [workingOfferId, setWorkingOfferId] = useState(null);

  // Fermer la modale et réinitialiser son contenu.
  const closeSelected = useCallback(() => {
    if (saving || workingOfferId !== null) return;

    setSelected('');
    setOffres([]);
    setForm(emptyForm);
    setError('');
  }, [saving, workingOfferId, setError]);

  // Charger les demandes, les fournisseurs et les offres.
  const load = useCallback(async () => {
    setLoading(true);
    setError('');

    try {
      const [demandeResponse, fournisseurResponse] =
        await Promise.all([
          workflowApi.get('/demandes'),
          workflowApi.get('/fournisseurs'),
        ]);

      const validated = (
        Array.isArray(demandeResponse.data)
          ? demandeResponse.data
          : []
      ).filter((d) => d.statut === 'VALIDEE');

      setDemandes(validated);

      setFournisseurs(
        (
          Array.isArray(fournisseurResponse.data)
            ? fournisseurResponse.data
            : []
        ).filter((s) => s.actif !== false)
      );

      const results = await Promise.all(
        validated.map(async (demande) => {
          try {
            const response = await workflowApi.get(
              `/demandes/${demande.id}/offres`
            );

            return [
              String(demande.id),
              Array.isArray(response.data) ? response.data : [],
            ];
          } catch {
            // Une erreur de chargement n'est pas une liste vide.
            return [String(demande.id), null];
          }
        })
      );

      const successfulResults = results.filter(
        ([, offers]) => offers !== null
      );

      const failedResults = results.filter(
        ([, offers]) => offers === null
      );

      setOffersByDemand(
        Object.fromEntries(successfulResults)
      );

      setOffersLoadErrors(
        Object.fromEntries(
          failedResults.map(([id]) => [id, true])
        )
      );

    } catch (err) {
      setError(
        getErrorMessage(
          err,
          'Impossible de charger les données.'
        )
      );
    } finally {
      setLoading(false);
    }
  }, [setError]);

  useEffect(() => {
    load();
  }, [load]);

  // Fermer la modale avec la touche Échap.
  useEffect(() => {
    if (!selected) return;

    const handleKeyDown = (event) => {
      if (event.key === 'Escape') {
        closeSelected();
      }
    };

    document.addEventListener('keydown', handleKeyDown);

    return () => {
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [selected, closeSelected]);

  // Empêcher le défilement de la page derrière la modale.
  useEffect(() => {
    if (!selected) return;

    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';

    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, [selected]);

  // Retrouver la demande actuellement sélectionnée.
  const selectedDemand = demandes.find(
    (d) => String(d.id) === String(selected)
  );

  // Vérifier si une offre a déjà été retenue.
  const retainedOffer = offres.some(
    (o) => o.statut === 'RETENUE'
  );

  // Calcul des statistiques.
  const counts = useMemo(() => {
    const all = demandes.length;

    const withOffers = demandes.filter((demande) => {
      const id = String(demande.id);

      return (
        Object.prototype.hasOwnProperty.call(
          offersByDemand,
          id
        ) &&
        (offersByDemand[id] || []).length > 0
      );
    }).length;

    const withoutOffers = demandes.filter((demande) => {
      const id = String(demande.id);

      return (
        Object.prototype.hasOwnProperty.call(
          offersByDemand,
          id
        ) &&
        (offersByDemand[id] || []).length === 0
      );
    }).length;

    const retained = demandes.filter((demande) =>
      (offersByDemand[String(demande.id)] || []).some(
        (offer) => offer.statut === 'RETENUE'
      )
    ).length;

    return {
      all,
      withOffers,
      withoutOffers,
      retained,
    };
  }, [demandes, offersByDemand]);

  // Recherche et filtrage des demandes.
  const filteredDemandes = useMemo(() => {
    const query = search.trim().toLocaleLowerCase('fr');

    return demandes.filter((demande) => {
      const id = String(demande.id);
      const demandOffers = offersByDemand[id] || [];

      const hasKnownOffers =
        Object.prototype.hasOwnProperty.call(
          offersByDemand,
          id
        );

      const hasOffers =
        hasKnownOffers && demandOffers.length > 0;

      const hasRetained = demandOffers.some(
        (offer) => offer.statut === 'RETENUE'
      );

      const matchesSearch =
        !query ||
        `${demande.reference || ''} ${demande.objet || ''}`
          .toLocaleLowerCase('fr')
          .includes(query);

      const matchesFilter =
        filter === 'TOUTES' ||
        (filter === 'AVEC_OFFRES' && hasOffers) ||
        (
          filter === 'SANS_OFFRES' &&
          hasKnownOffers &&
          demandOffers.length === 0
        ) ||
        (filter === 'RETENUE' && hasRetained);

      return matchesSearch && matchesFilter;
    });
  }, [demandes, offersByDemand, search, filter]);

  // Charger les offres et ouvrir la modale.
  const loadOffers = async (id) => {
    if (loadingOffers || workingOfferId !== null) return;

    setSelected(String(id));
    setOffres([]);
    setForm(emptyForm);
    setError('');
    setLoadingOffers(true);

    try {
      const { data } = await workflowApi.get(
        `/demandes/${id}/offres`
      );

      const list = Array.isArray(data) ? data : [];

      setOffres(list);

      setOffersByDemand((current) => ({
        ...current,
        [String(id)]: list,
      }));

      setOffersLoadErrors((current) => ({
        ...current,
        [String(id)]: false,
      }));

    } catch (err) {
      setOffersLoadErrors((current) => ({
        ...current,
        [String(id)]: true,
      }));

      setError(
        getErrorMessage(
          err,
          'Impossible de charger les offres.'
        )
      );
    } finally {
      setLoadingOffers(false);
    }
  };

  // Actualiser les offres d'une demande dans les statistiques.
  const refreshDemandOffers = async (id) => {
    try {
      const { data } = await workflowApi.get(
        `/demandes/${id}/offres`
      );

      const list = Array.isArray(data) ? data : [];

      setOffersByDemand((current) => ({
        ...current,
        [String(id)]: list,
      }));

      setOffersLoadErrors((current) => ({
        ...current,
        [String(id)]: false,
      }));

    } catch {
      setOffersLoadErrors((current) => ({
        ...current,
        [String(id)]: true,
      }));
    }
  };

  // Enregistrer une nouvelle offre.
  const save = async (event) => {
    event.preventDefault();

    if (!selected) {
      setError('Sélectionnez une demande.');
      return;
    }

    setSaving(true);
    setError('');

    try {
      await workflowApi.post(
        `/demandes/${selected}/offres`,
        {
          fournisseurId: Number(form.fournisseurId),
          montantTotal: Number(form.montantTotal),

          delaiLivraisonJours:
            form.delaiLivraisonJours === ''
              ? null
              : Number(form.delaiLivraisonJours),

          conditions: form.conditions.trim() || null,
        }
      );

      setForm(emptyForm);
      setSuccess('Offre enregistrée avec succès.');

      await loadOffers(selected);
      await refreshDemandOffers(selected);

    } catch (err) {
      setError(
        getErrorMessage(
          err,
          'Impossible d’enregistrer l’offre.'
        )
      );
    } finally {
      setSaving(false);
    }
  };

  // Retenir ou refuser une offre.
  const applyOfferDecision = async (offerId, decision) => {
    if (workingOfferId !== null) return;

    setWorkingOfferId(offerId);
    setError('');

    try {
      await workflowApi.post(
        `/demandes/${selected}/offres/${offerId}/${decision}`
      );

      setSuccess(
        decision === 'retenir'
          ? 'Offre retenue et bon de commande créé avec succès.'
          : 'Offre refusée.'
      );

      await loadOffers(selected);
      await refreshDemandOffers(selected);

    } catch (err) {
      setError(
        getErrorMessage(
          err,
          decision === 'retenir'
            ? 'Impossible de retenir l’offre et de créer le bon de commande.'
            : 'Impossible de refuser cette offre.'
        )
      );

      await loadOffers(selected).catch(() => {});
    } finally {
      setWorkingOfferId(null);
    }
  };

  return (
    <div className="page-content offers-page">

      {/* LISTE DES DEMANDES */}

      <section className="content-card offers-card">

        <div className="offers-heading">
          <div>
            <h2>Offres fournisseurs</h2>
            <p>
              Consultez les demandes validées, ajoutez des offres
              et comparez les propositions des fournisseurs.
            </p>
          </div>

          <button
            type="button"
            className="offers-refresh"
            onClick={load}
            disabled={loading}
          >
            ↻ Actualiser
          </button>
        </div>

        {error && (
          <p className="workflow-error" role="status">
            {error}
          </p>
        )}

        {/* STATISTIQUES */}

        <div className="offers-stats">

          <button
            className={`offers-stat ${filter === 'TOUTES' ? 'active' : ''}`}
            onClick={() => setFilter('TOUTES')}
            type="button"
          >
            <span className="offers-stat-value">
              {counts.all}
            </span>
            <span className="offers-stat-label">
              Toutes les demandes
            </span>
          </button>

          <button
            className={`offers-stat green ${filter === 'AVEC_OFFRES' ? 'active' : ''}`}
            onClick={() => setFilter('AVEC_OFFRES')}
            type="button"
          >
            <span className="offers-stat-value">
              {counts.withOffers}
            </span>
            <span className="offers-stat-label">
              Avec offres
            </span>
          </button>

          <button
            className={`offers-stat amber ${filter === 'SANS_OFFRES' ? 'active' : ''}`}
            onClick={() => setFilter('SANS_OFFRES')}
            type="button"
          >
            <span className="offers-stat-value">
              {counts.withoutOffers}
            </span>
            <span className="offers-stat-label">
              Sans offres
            </span>
          </button>

          <button
            className={`offers-stat violet ${filter === 'RETENUE' ? 'active' : ''}`}
            onClick={() => setFilter('RETENUE')}
            type="button"
          >
            <span className="offers-stat-value">
              {counts.retained}
            </span>
            <span className="offers-stat-label">
              Offre retenue
            </span>
          </button>

        </div>

        {/* RECHERCHE ET FILTRES */}

        <div className="offers-toolbar">

          <label className="offers-search">
            <span>⌕</span>

            <input
              type="search"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Rechercher par référence ou intitulé..."
              aria-label="Rechercher une demande"
              style={{ border : 'none' }}
            />
          </label>

          <label className="offers-filter-select">
            <span>Afficher</span>

            <select
              value={filter}
              onChange={(e) => setFilter(e.target.value)}
            >
              <option value="TOUTES">Toutes les demandes</option>
              <option value="AVEC_OFFRES">Avec offres</option>
              <option value="SANS_OFFRES">Sans offres</option>
              <option value="RETENUE">Avec offre retenue</option>
            </select>
          </label>

        </div>

        {/* LISTE DES DEMANDES */}

        {loading ? (
          <p className="offers-empty">
            Chargement des demandes…
          </p>
        ) : demandes.length === 0 ? (
          <div className="offers-empty">
            <strong>Aucune demande validée</strong>
            <span>
              Les demandes apparaîtront ici après leur validation.
            </span>
          </div>
        ) : filteredDemandes.length === 0 ? (
          <div className="offers-empty">
            <strong>Aucun résultat</strong>
            <span>
              Modifiez votre recherche ou choisissez un autre filtre.
            </span>
          </div>
        ) : (

          <div className="offers-demand-list">

            {filteredDemandes.map((demande) => {
              const id = String(demande.id);
              const demandOffers = offersByDemand[id] || [];

              const hasRetained = demandOffers.some(
                (offer) => offer.statut === 'RETENUE'
              );

              const hasLoadError = offersLoadErrors[id] === true;

              const isSelected = id === String(selected);

              return (
                <article
                  className={`offers-demand-item ${isSelected ? 'selected' : ''}`}
                  key={demande.id}
                >

                  <div className="offers-demand-main">

                    <div className="offers-demand-icon">
                      ▤
                    </div>

                    <div className="offers-demand-info">
                      <strong>
                        {demande.reference || `Demande #${demande.id}`}
                      </strong>

                      <span>
                        {demande.objet || 'Sans intitulé'}
                      </span>

                      <small>
                        Montant estimé :{' '}
                        {formatAmount(demande.montant_estime)}
                      </small>
                    </div>

                  </div>

                  <div className="offers-demand-meta">

                    <span
                      className={`offers-status ${
                        hasLoadError
                          ? 'no-offers'
                          : hasRetained
                            ? 'retained'
                            : demandOffers.length
                              ? 'has-offers'
                              : 'no-offers'
                      }`}
                    >
                      {hasLoadError
                        ? 'Offres indisponibles'
                        : hasRetained
                          ? 'Offre retenue'
                          : demandOffers.length
                            ? 'Offres reçues'
                            : 'Aucune offre'}
                    </span>

                    <span className="offers-count">
                      {hasLoadError
                        ? '—'
                        : `${demandOffers.length} offre${demandOffers.length > 1 ? 's' : ''}`}
                    </span>

                    <button
                      type="button"
                      className="offers-view-button"
                      onClick={() => loadOffers(demande.id)}
                      disabled={
                        loadingOffers ||
                        workingOfferId !== null
                      }
                    >
                      {isSelected
                        ? 'Ouvrir à nouveau'
                        : demandOffers.length
                          ? 'Consulter les offres'
                          : 'Ajouter une offre'}
                    </button>

                  </div>

                </article>
              );
            })}

          </div>
        )}

      </section>

      {/* MODALE : DÉTAIL ET COMPARAISON */}

      {selectedDemand && (
        <div
          className="offers-modal-overlay"
          onMouseDown={(event) => {
            if (
              event.target === event.currentTarget
            ) {
              closeSelected();
            }
          }}
        >
          <section
            className="content-card offers-detail-card offers-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="offers-modal-title"
          >

            <div className="offers-detail-heading">

              <div>
                <span className="offers-section-kicker">
                  DÉTAIL DE LA DEMANDE
                </span>

                <h2 id="offers-modal-title">
                  {selectedDemand.reference || `Demande #${selectedDemand.id}`}
                  {' · '}
                  {selectedDemand.objet || 'Sans intitulé'}
                </h2>

                <p>
                  Montant estimé :{' '}
                  <strong>
                    {formatAmount(selectedDemand.montant_estime)}
                  </strong>
                </p>
              </div>

              <button
                type="button"
                className="offers-close-detail"
                onClick={closeSelected}
                disabled={saving || workingOfferId !== null}
                aria-label="Fermer le détail"
              >
                ×
              </button>

            </div>

            {loadingOffers ? (
              <p className="offers-empty">
                Chargement des offres…
              </p>
            ) : offersLoadErrors[String(selected)] ? (
              <div className="offers-empty">
                <strong>Impossible de charger les offres.</strong>
                <span>
                  Vérifiez la connexion au serveur puis réessayez.
                </span>

                <button
                  type="button"
                  className="offers-view-button"
                  onClick={() => loadOffers(selected)}
                >
                  Réessayer
                </button>
              </div>
            ) : (
              <>

                {/* FORMULAIRE D'AJOUT */}

                {!retainedOffer && (
                  <form
                    className="workflow-form offer-form offers-form"
                    onSubmit={save}
                  >
                    <h3>Ajouter une offre fournisseur</h3>

                    <div className="offers-form-grid">

                      <label>
                        Fournisseur

                        <select
                          required
                          value={form.fournisseurId}
                          onChange={(e) =>
                            setForm({
                              ...form,
                              fournisseurId: e.target.value,
                            })
                          }
                        >
                          <option value="">
                            Sélectionner un fournisseur…
                          </option>

                          {fournisseurs.map((supplier) => (
                            <option
                              key={supplier.id}
                              value={supplier.id}
                            >
                              {supplier.raison_sociale}
                            </option>
                          ))}
                        </select>
                      </label>

                      <label>
                        Montant total proposé

                        <input
                          required
                          type="number"
                          min="0.01"
                          step="0.01"
                          value={form.montantTotal}
                          onChange={(e) =>
                            setForm({
                              ...form,
                              montantTotal: e.target.value,
                            })
                          }
                        />
                      </label>

                      <label>
                        Délai proposé (jours)

                        <input
                          type="number"
                          min="0"
                          step="1"
                          value={form.delaiLivraisonJours}
                          onChange={(e) =>
                            setForm({
                              ...form,
                              delaiLivraisonJours: e.target.value,
                            })
                          }
                        />
                      </label>

                      <label>
                        Conditions propres à l’offre

                        <input
                          maxLength="255"
                          value={form.conditions}
                          onChange={(e) =>
                            setForm({
                              ...form,
                              conditions: e.target.value,
                            })
                          }
                        />
                      </label>

                    </div>

                    <div className="offers-form-actions">

                      <IconActionButton
                        icon="save"
                        className="action-button"
                        type="submit"
                        disabled={
                          saving ||
                          fournisseurs.length === 0 ||
                          workingOfferId !== null
                        }
                      >
                        {saving
                          ? 'Enregistrement…'
                          : 'Enregistrer l’offre'}
                      </IconActionButton>

                      {fournisseurs.length === 0 && (
                        <small>
                          Aucun fournisseur actif disponible.
                        </small>
                      )}

                    </div>
                  </form>
                )}

                {/* TABLEAU DES OFFRES */}

                {offres.length === 0 ? (
                  <div className="offers-empty compact">
                    <strong>
                      Aucune offre enregistrée pour cette demande.
                    </strong>
                    <span>
                      Utilisez le formulaire ci-dessus pour ajouter
                      la première proposition.
                    </span>
                  </div>
                ) : (

                  <div className="table-wrap offers-table-wrap">
                    <table>

                      <thead>
                        <tr>
                          <th>Fournisseur</th>
                          <th>Montant</th>
                          <th>Écart à l’estimation</th>
                          <th>Délai</th>
                          <th>Paiement fournisseur</th>
                          <th>Conditions de l’offre</th>
                          <th>Statut</th>
                          <th>Action</th>
                        </tr>
                      </thead>

                      <tbody>
                        {offres.map((offer) => {
                          const difference = Number(
                            offer.ecart_montant || 0
                          );

                          return (
                            <tr key={offer.id}>
                              <td>{offer.fournisseur}</td>

                              <td>
                                {formatAmount(offer.montant_total)}
                              </td>

                              <td>
                                <span
                                  className={
                                    difference > 0
                                      ? 'amount-higher'
                                      : difference < 0
                                        ? 'amount-lower'
                                        : ''
                                  }
                                >
                                  {difference > 0 ? '+' : ''}
                                  {formatAmount(difference)}
                                </span>
                              </td>

                              <td>
                                {offer.delai_livraison_jours == null
                                  ? '—'
                                  : `${offer.delai_livraison_jours} jours`}
                              </td>

                              <td>
                                {offer.conditions_paiement || '—'}
                              </td>

                              <td>
                                {offer.conditions || '—'}
                              </td>

                              <td>
                                <span
                                  className={`offers-table-status ${
                                    String(offer.statut || '').toLowerCase()
                                  }`}
                                >
                                  {offer.statut}
                                </span>
                              </td>

                              <td className="offers-actions-cell">

                                {offer.statut === 'RECUE' &&
                                  !retainedOffer && (
                                    <IconActionButton
                                      icon="validate"
                                      type="button"
                                      className="action-button success-button"
                                      disabled={workingOfferId !== null}
                                      onClick={() =>
                                        applyOfferDecision(
                                          offer.id,
                                          'retenir'
                                        )
                                      }
                                    >
                                      {workingOfferId === offer.id
                                        ? 'Traitement…'
                                        : 'Retenir'}
                                    </IconActionButton>
                                  )}

                                {offer.statut === 'RECUE' && (
                                  <IconActionButton
                                    icon="reject"
                                    type="button"
                                    className="quiet-button danger-button"
                                    disabled={workingOfferId !== null}
                                    onClick={() =>
                                      applyOfferDecision(
                                        offer.id,
                                        'refuser'
                                      )
                                    }
                                  >
                                    Refuser
                                  </IconActionButton>
                                )}

                                {offer.statut === 'RETENUE' && (
                                  <span>Offre sélectionnée</span>
                                )}

                              </td>
                            </tr>
                          );
                        })}
                      </tbody>

                    </table>
                  </div>
                )}

              </>
            )}

          </section>
        </div>
      )}

    </div>
  );
}
