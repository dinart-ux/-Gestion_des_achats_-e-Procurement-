import { useCallback, useEffect, useState } from 'react';
import workflowApi from '../service/workflowApi';
import IconActionButton from '../components/IconActionButton.jsx';
import { useToastMessages } from '../components/notifications/ToastContext';
import './Menu.css';

export default function Commandes() {
  const [commandes, setCommandes] = useState([]);
  const [selected, setSelected] = useState(null);
  const [lines, setLines] = useState([]);
  const [observation, setObservation] = useState('');
  const {error,success,setError,setSuccess,} = useToastMessages();
  const load = useCallback(async () => {
    try { setCommandes((await workflowApi.get('/commandes')).data); setError(''); }
    catch (e) { setError(e.response?.data?.message || 'Impossible de charger les commandes.'); }
  }, []);
  useEffect(() => { load(); }, [load]);
  
const open = async (order) => {
  setError('');

  try {
    const response = await workflowApi.get(
      `/commandes/${order.id}/lignes`
    );

    const rows = response.data;

    if (!Array.isArray(rows)) {
      setError('Le serveur n’a pas retourné la liste des lignes.');
      return;
    }

    setSelected(order);

    setLines(
      rows.map((line) => ({
        ...line,
        quantiteRecue: '',
      }))
    );

    setObservation('');
  } catch (e) {
    console.error(
      'Erreur lors du chargement des lignes de commande :',
      e.response?.data || e
    );

    setError(
      e.response?.data?.error ||
      e.response?.data?.message ||
      `Impossible de charger les lignes de la commande ${order.id}.`
    );
  }
};

  const receive = async (e) => {
    e.preventDefault();
    const receivedLines = lines.filter((line) => Number(line.quantiteRecue) > 0).map((line) => ({ ligneCommandeId: line.id, quantiteRecue: Number(line.quantiteRecue) }));
    if (!receivedLines.length) { setError('Indiquez au moins une quantité reçue.'); return; }
    try {
      await workflowApi.post(`/commandes/${selected.id}/receptions`, { lignes: receivedLines, observation });
      setSelected(null);
      setSuccess('Réception enregistrée.');
      await load();
    } catch (e) { setError(e.response?.data?.message || 'Impossible d’enregistrer la réception.'); }
  };
  return <div className="page-content"><section className="content-card">
    <h2>Commandes</h2><p>Bons de commande et état des réceptions.</p>
    {error && <p className="workflow-error" role="status">{error}</p>}
    {commandes.length === 0 ? <p>Aucune commande enregistrée.</p> : <div className="table-wrap"><table><thead><tr><th>N° commande</th><th>Fournisseur</th><th>Montant</th><th>Statut</th><th>Date d’émission</th><th></th></tr></thead><tbody>
      {commandes.map((c) => <tr key={c.id}><td>{c.numero}</td><td>{c.fournisseur}</td><td>{Number(c.montant_total).toLocaleString('fr-FR')} Ar</td><td><span className="status-pill">{c.statut}</span></td><td>{new Date(c.date_emission).toLocaleDateString('fr-FR')}</td><td><IconActionButton icon="receive" className="quiet-button" onClick={() => open(c)}>Réceptionner</IconActionButton></td></tr>)}
    </tbody></table></div>}
  </section>
  {selected && <section className="content-card detail-card"><div className="section-heading"><div><h2>Réception — {selected.numero}</h2><p>{selected.fournisseur} · {selected.statut}</p></div><IconActionButton icon="cancel" className="quiet-button" onClick={() => setSelected(null)}>Fermer</IconActionButton></div>
    <form onSubmit={receive}><div className="table-wrap"><table><thead><tr><th>Ligne</th><th>Quantité commandée</th><th>Déjà reçue</th><th>Reste à recevoir</th><th>Prix unitaire</th><th>Quantité reçue maintenant</th></tr></thead><tbody>
      
      {lines.map((line) => (
        <tr key={line.id}>
          <td>{line.designation}</td>
          <td>{line.quantite}</td>
          <td>{Number(line.quantite_deja_recue ?? 0)}</td>
          <td>{Number(line.quantite_restante ?? line.quantite)}</td>
          <td>
            {Number(line.prix_unitaire).toLocaleString('fr-FR')} Ar
          </td>
          <td>
            <input
              aria-label={`Quantité reçue ${line.designation}`}
              type="number"
              min="0"
              max={Number(line.quantite_restante ?? line.quantite)}
              step="0.01"
              value={line.quantiteRecue}
              disabled={Number(line.quantite_restante ?? line.quantite) <= 0}
              onChange={(e) =>
                setLines((previous) =>
                  previous.map((item) =>
                    item.id === line.id
                      ? { ...item, quantiteRecue: e.target.value }
                      : item
                  )
                )
              }
            />
          </td>
        </tr>
      ))}

          </tbody></table></div><label className="comment-label">Observation<textarea value={observation} onChange={(e) => setObservation(e.target.value)} /></label><IconActionButton icon="save" type="submit" className="action-button">Enregistrer la réception</IconActionButton></form>
  </section>}
  </div>;
}
