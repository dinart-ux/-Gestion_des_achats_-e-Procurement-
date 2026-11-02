import { useCallback, useEffect, useState } from 'react';
import workflowApi from '../service/workflowApi';
import IconActionButton from '../components/IconActionButton.jsx';
import { useToastMessages } from '../components/notifications/ToastContext';
import './Menu.css';


const emptyForm = {
  fournisseurId: '',
  montantTotal: '',
  delaiLivraisonJours: '',
  conditions: '',
};

const formatAmount = (value) =>
  `${Number(value || 0).toLocaleString('fr-FR')} Ar`;

export default function Offres() {
  const [demandes, setDemandes] = useState([]);
  const [fournisseurs, setFournisseurs] = useState([]);
  const [selected, setSelected] = useState('');
  const [offres, setOffres] = useState([]);
  const [form, setForm] = useState(emptyForm);
  const {error,success,setError,setSuccess,} = useToastMessages();
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [workingOfferId, setWorkingOfferId] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);

    try {
      const [demandeResponse, fournisseurResponse] = await Promise.all([
        workflowApi.get('/demandes'),
        workflowApi.get('/fournisseurs'),
      ]);

      setDemandes(
        demandeResponse.data.filter(
          (demande) => demande.statut === 'VALIDEE'
        )
      );

      setFournisseurs(
        fournisseurResponse.data.filter(
          (supplier) => supplier.actif !== false
        )
      );

      setError('');
    } catch (requestError) {
      setError(
        requestError.response?.data?.message ||
        requestError.response?.data?.error ||
        'Impossible de charger les données.'
      );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const selectedDemand = demandes.find(
    (demande) => String(demande.id) === String(selected)
  );

  const retainedOffer = offres.some(
    (offer) => offer.statut === 'RETENUE'
  );

  const loadOffers = async (id) => {
    setSelected(id);
    setOffres([]);
    setForm(emptyForm);
    setError('');

    if (!id) return;

    try {
      const { data } = await workflowApi.get(
        `/demandes/${id}/offres`
      );

      setOffres(Array.isArray(data) ? data : []);
    } catch (requestError) {
      setError(
        requestError.response?.data?.message ||
        requestError.response?.data?.error ||
        'Impossible de charger les offres.'
      );
    }
  };

  const save = async (event) => {
    event.preventDefault();

    if (!selected) {
      setError('Sélectionnez une demande.');
      return;
    }

    setSaving(true);
    setError('');

    try {
      await workflowApi.post(`/demandes/${selected}/offres`, {
        fournisseurId: Number(form.fournisseurId),
        montantTotal: Number(form.montantTotal),
        delaiLivraisonJours:
          form.delaiLivraisonJours === ''
            ? null
            : Number(form.delaiLivraisonJours),
        conditions: form.conditions.trim() || null,
      });

      setForm(emptyForm);
      await loadOffers(selected);
    } catch (requestError) {
      setError(
        requestError.response?.data?.message ||
        requestError.response?.data?.error ||
        'Impossible d’enregistrer l’offre.'
      );
    } finally {
      setSaving(false);
    }
  };

  const applyOfferDecision = async (offerId, decision) => {
    if (workingOfferId !== null) return;

    setWorkingOfferId(offerId);
    setError('');

    try {
      // Le backend doit créer le bon de commande dans la route
      // /retenir, grâce à retenirOffreEtCreerCommande().
      await workflowApi.post(
        `/demandes/${selected}/offres/${offerId}/${decision}`
      );

      if (decision === 'retenir') {
        setError(
          'Offre retenue et bon de commande créé avec succès.'
        );

        // Actualiser les données après l'opération.
        await load();
        await loadOffers(selected);
      } else {
        await loadOffers(selected);
        setError('Offre refusée.');
      }
    } catch (requestError) {
      setError(
        requestError.response?.data?.message ||
        requestError.response?.data?.error ||
        (decision === 'retenir'
          ? 'Impossible de retenir l’offre et de créer le bon de commande.'
          : 'Impossible de refuser cette offre.')
      );

      // Recharger les offres pour afficher leur état réel.
      try {
        await loadOffers(selected);
      } catch {
        // Le message d'erreur principal reste affiché.
      }
    } finally {
      setWorkingOfferId(null);
    }
  };

  return (
    <div className="page-content">
      <section className="content-card">
        <h2>Offres fournisseurs</h2>

        <p>
          Comparez les offres des demandes validées avant de retenir
          une proposition.
        </p>

        {error && (
          <p className="workflow-error" role="status">
            {error}
          </p>
        )}

        <label className="inline-select">
          Demande

          <select
            value={selected}
            onChange={(event) => loadOffers(event.target.value)}
            disabled={loading || workingOfferId !== null}
          >
            <option value="">Choisir une demande…</option>

            {demandes.map((demande) => (
              <option key={demande.id} value={demande.id}>
                {demande.reference} · {demande.objet}
              </option>
            ))}
          </select>
        </label>

        {loading && (
          <p className="loading-state" role="status">
            Chargement…
          </p>
        )}

        {!loading && demandes.length === 0 && (
          <p>Aucune demande validée disponible.</p>
        )}

        {selectedDemand && (
          <p>
            Montant estimé de la demande :{' '}
            <strong>
              {formatAmount(selectedDemand.montant_estime)}
            </strong>
          </p>
        )}

        {selectedDemand && !retainedOffer && (
          <form
            className="workflow-form offer-form"
            onSubmit={save}
          >
            <h3>Ajouter une offre</h3>

            <label>
              Fournisseur

              <select
                required
                value={form.fournisseurId}
                onChange={(event) =>
                  setForm({
                    ...form,
                    fournisseurId: event.target.value,
                  })
                }
              >
                <option value="">Sélectionner…</option>

                {fournisseurs.map((supplier) => (
                  <option key={supplier.id} value={supplier.id}>
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
                onChange={(event) =>
                  setForm({
                    ...form,
                    montantTotal: event.target.value,
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
                onChange={(event) =>
                  setForm({
                    ...form,
                    delaiLivraisonJours: event.target.value,
                  })
                }
              />
            </label>

            <label>
              Conditions propres à l’offre

              <input
                maxLength="255"
                value={form.conditions}
                onChange={(event) =>
                  setForm({
                    ...form,
                    conditions: event.target.value,
                  })
                }
              />
            </label>

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
              {saving ? 'Enregistrement…' : 'Enregistrer l’offre'}
            </IconActionButton>
          </form>
        )}

        {selected && !loading && offres.length === 0 && (
          <p>Aucune offre enregistrée pour cette demande.</p>
        )}

        {selected && offres.length > 0 && (
          <div className="table-wrap">
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
                        {difference > 0 ? '+' : ''}
                        {formatAmount(difference)}
                      </td>

                      <td>
                        {offer.delai_livraison_jours == null
                          ? '—'
                          : `${offer.delai_livraison_jours} jours`}
                      </td>

                      <td>
                        {offer.conditions_paiement || '—'}
                      </td>

                      <td>{offer.conditions || '—'}</td>

                      <td>{offer.statut}</td>

                      <td>
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
      </section>
    </div>
  );
}