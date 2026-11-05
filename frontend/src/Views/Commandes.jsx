
import { useCallback, useEffect, useState } from 'react';
import workflowApi from '../service/workflowApi';
import IconActionButton from '../components/IconActionButton.jsx';
import { useToastMessages } from '../components/notifications/ToastContext';
import "./Commandes.css";

export default function Commandes() {
  const [commandes, setCommandes] = useState([]);
  const [selected, setSelected] = useState(null);
  const [lines, setLines] = useState([]);
  const [observation, setObservation] = useState('');
  const [loading, setLoading] = useState(false);
  const [receiving, setReceiving] = useState(false);

  const {
    error,
    success,
    setError,
    setSuccess,
  } = useToastMessages();

  const load = useCallback(async () => {
    setLoading(true);

    try {
      const response = await workflowApi.get('/commandes');
      setCommandes(Array.isArray(response.data) ? response.data : []);
      setError('');
    } catch (e) {
      setError(
        e.response?.data?.message ||
        'Impossible de charger les commandes.'
      );
    } finally {
      setLoading(false);
    }
  }, [setError]);

  useEffect(() => {
    load();
  }, [load]);

  const formatAmount = (amount) =>
    `${Number(amount || 0).toLocaleString('fr-FR')} Ar`;

  const formatDate = (date) => {
    if (!date) return '—';

    const parsedDate = new Date(date);

    return Number.isNaN(parsedDate.getTime())
      ? '—'
      : parsedDate.toLocaleDateString('fr-FR');
  };

  const getStatusClass = (status) => {
    const value = String(status || '').toUpperCase();

    if (value.includes('RECU') || value.includes('TERMIN')) {
      return 'commande-status commande-status-success';
    }

    if (value.includes('PARTIEL')) {
      return 'commande-status commande-status-warning';
    }

    if (value.includes('ANNU')) {
      return 'commande-status commande-status-danger';
    }

    return 'commande-status commande-status-info';
  };

  const open = async (order) => {
    setError('');

    try {
      const response = await workflowApi.get(
        `/commandes/${order.id}/lignes`
      );

      if (!Array.isArray(response.data)) {
        setError('Le serveur n’a pas retourné la liste des lignes.');
        return;
      }

      setSelected(order);

      setLines(
        response.data.map((line) => ({
          ...line,
          quantiteRecue: '',
        }))
      );

      setObservation('');
    } catch (e) {
      console.error(
        'Erreur lors du chargement des lignes :',
        e.response?.data || e
      );

      setError(
        e.response?.data?.error ||
        e.response?.data?.message ||
        `Impossible de charger les lignes de la commande ${order.id}.`
      );
    }
  };

  const close = () => {
    setSelected(null);
    setLines([]);
    setObservation('');
  };

  const receive = async (e) => {
    e.preventDefault();

    if (!selected) return;

    const receivedLines = lines
      .filter((line) => Number(line.quantiteRecue) > 0)
      .map((line) => ({
        ligneCommandeId: line.id,
        quantiteRecue: Number(line.quantiteRecue),
      }));

    if (!receivedLines.length) {
      setError('Indiquez au moins une quantité reçue.');
      return;
    }

    setReceiving(true);
    setError('');

    try {
      await workflowApi.post(
        `/commandes/${selected.id}/receptions`,
        {
          lignes: receivedLines,
          observation,
        }
      );

      close();
      setSuccess('Réception enregistrée avec succès.');

      await load();
    } catch (e) {
      setError(
        e.response?.data?.message ||
        'Impossible d’enregistrer la réception.'
      );
    } finally {
      setReceiving(false);
    }
  };

  const totalCommandes = commandes.length;

  const totalMontant = commandes.reduce(
    (sum, commande) => sum + Number(commande.montant_total || 0),
    0
  );

  const commandesTerminees = commandes.filter((commande) => {
    const status = String(commande.statut || '').toUpperCase();
    return status.includes('RECU') || status.includes('TERMIN');
  }).length;

  return (
    <div className="commande-page">

      <div className="commande-header">
        <div>
          <span className="commande-eyebrow">
            GESTION DES ACHATS
          </span>
          <h1>Commandes</h1>
          <p>
            Gérez les bons de commande et suivez les réceptions
            de marchandises.
          </p>
        </div>

        <button
          type="button"
          className="commande-refresh-button"
          onClick={load}
          disabled={loading}
        >
          <span className={loading ? 'commande-spin' : ''}>↻</span>
          Actualiser
        </button>
      </div>

      {error && (
        <div className="commande-alert commande-alert-error" role="alert">
          <span>!</span>
          {error}
        </div>
      )}

      {success && (
        <div className="commande-alert commande-alert-success" role="status">
          <span>✓</span>
          {success}
        </div>
      )}

      <div className="commande-stats">

        <div className="commande-stat-card">
          <div className="commande-stat-icon icon-purple">▤</div>
          <div>
            <span className="commande-stat-label">Total commandes</span>
            <strong>{totalCommandes}</strong>
            <small>Bons de commande enregistrés</small>
          </div>
        </div>

        <div className="commande-stat-card">
          <div className="commande-stat-icon icon-blue">Ar</div>
          <div>
            <span className="commande-stat-label">Montant total</span>
            <strong>{formatAmount(totalMontant)}</strong>
            <small>Valeur des commandes</small>
          </div>
        </div>

        <div className="commande-stat-card">
          <div className="commande-stat-icon icon-green">✓</div>
          <div>
            <span className="commande-stat-label">Commandes terminées</span>
            <strong>{commandesTerminees}</strong>
            <small>Réceptions terminées</small>
          </div>
        </div>

      </div>

      <section className="commande-card">

        <div className="commande-section-heading">
          <div>
            <h2>Liste des commandes</h2>
            <p>Consultez et réceptionnez vos commandes.</p>
          </div>

          <span className="commande-count">
            {totalCommandes} commande(s)
          </span>
        </div>

        {loading && commandes.length === 0 ? (
          <div className="commande-empty">
            <div className="commande-loader"></div>
            <p>Chargement des commandes...</p>
          </div>
        ) : commandes.length === 0 ? (
          <div className="commande-empty">
            <div className="commande-empty-icon">▤</div>
            <h3>Aucune commande disponible</h3>
            <p>
              Les bons de commande apparaîtront ici dès leur création.
            </p>
          </div>
        ) : (
          <div className="commande-table-container">
            <table className="commande-table">
              <thead>
                <tr>
                  <th>N° COMMANDE</th>
                  <th>FOURNISSEUR</th>
                  <th>MONTANT</th>
                  <th>STATUT</th>
                  <th>DATE D'ÉMISSION</th>
                  <th>ACTION</th>
                </tr>
              </thead>

              <tbody>
                {commandes.map((commande) => (
                  <tr key={commande.id}>
                    <td>
                      <span className="commande-number">
                        {commande.numero || `CMD-${commande.id}`}
                      </span>
                    </td>

                    <td>
                      <span className="commande-supplier">
                        {commande.fournisseur || 'Non renseigné'}
                      </span>
                    </td>

                    <td className="commande-amount">
                      {formatAmount(commande.montant_total)}
                    </td>

                    <td>
                      <span className={getStatusClass(commande.statut)}>
                        {commande.statut || 'En cours'}
                      </span>
                    </td>

                    <td>{formatDate(commande.date_emission)}</td>

                    <td>
                      <IconActionButton
                        icon="receive"
                        className="commande-receive-button"
                        onClick={() => open(commande)}
                      >
                        Réceptionner
                      </IconActionButton>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

      </section>

      {selected && (
        <div
          className="commande-modal-overlay"
          onMouseDown={(e) => {
            if (e.target === e.currentTarget && !receiving) {
              close();
            }
          }}
        >
          <section
            className="commande-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="commande-modal-title"
          >

            <div className="commande-modal-header">
              <div>
                <span className="commande-eyebrow">
                  GESTION DES RÉCEPTIONS
                </span>

                <h2 id="commande-modal-title">
                  Réception de commande
                </h2>

                <p>
                  {selected.numero} · {selected.fournisseur}
                </p>
              </div>

              <button
                type="button"
                className="commande-close-button"
                onClick={close}
                disabled={receiving}
                aria-label="Fermer"
              >
                ×
              </button>
            </div>

            <form onSubmit={receive}>

              <div className="commande-order-info">
                <div>
                  <span>Numéro de commande</span>
                  <strong>{selected.numero}</strong>
                </div>

                <div>
                  <span>Montant total</span>
                  <strong>{formatAmount(selected.montant_total)}</strong>
                </div>

                <div>
                  <span>Statut</span>
                  <strong>{selected.statut || 'En cours'}</strong>
                </div>
              </div>

              <div className="commande-modal-content">

                <div className="commande-modal-section-title">
                  <div>
                    <h3>Lignes de commande</h3>
                    <p>Indiquez les quantités reçues maintenant.</p>
                  </div>

                  <span>{lines.length} ligne(s)</span>
                </div>

                <div className="commande-lines-container">
                  <table className="commande-lines-table">
                    <thead>
                      <tr>
                        <th>Désignation</th>
                        <th>Commandée</th>
                        <th>Déjà reçue</th>
                        <th>Restante</th>
                        <th>Prix unitaire</th>
                        <th>À recevoir</th>
                      </tr>
                    </thead>

                    <tbody>
                      {lines.map((line) => {
                        const remaining = Number(
                          line.quantite_restante ?? line.quantite
                        );

                        return (
                          <tr key={line.id}>
                            <td className="commande-line-designation">
                              {line.designation}
                            </td>

                            <td>{line.quantite}</td>

                            <td>{Number(line.quantite_deja_recue ?? 0)}</td>

                            <td>
                              <strong>{remaining}</strong>
                            </td>

                            <td>
                              {formatAmount(line.prix_unitaire)}
                            </td>

                            <td>
                              <input
                                className="commande-quantity-input"
                                aria-label={`Quantité reçue ${line.designation}`}
                                type="number"
                                min="0"
                                max={remaining}
                                step="0.01"
                                value={line.quantiteRecue}
                                disabled={remaining <= 0}
                                onChange={(e) => {
                                  const value = e.target.value;

                                  if (
                                    value !== '' &&
                                    Number(value) > remaining
                                  ) {
                                    return;
                                  }

                                  setLines((previous) =>
                                    previous.map((item) =>
                                      item.id === line.id
                                        ? {
                                            ...item,
                                            quantiteRecue: value,
                                          }
                                        : item
                                    )
                                  );
                                }}
                              />
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>

                {lines.length === 0 && (
                  <div className="commande-no-lines">
                    Aucune ligne disponible pour cette commande.
                  </div>
                )}

                <label className="commande-observation">
                  <span>Observation</span>
                  <textarea
                    value={observation}
                    onChange={(e) => setObservation(e.target.value)}
                    placeholder="Ajouter une observation sur cette réception..."
                    rows={3}
                  />
                </label>

              </div>

              <div className="commande-modal-footer">
                <button
                  type="button"
                  className="commande-cancel-button"
                  onClick={close}
                  disabled={receiving}
                >
                  Annuler
                </button>

                <button
                  type="submit"
                  className="commande-submit-button"
                  disabled={receiving || lines.length === 0}
                >
                  {receiving ? 'Enregistrement...' : '✓ Enregistrer la réception'}
                </button>
              </div>

            </form>
          </section>
        </div>
      )}

    </div>
  );
}
