import { useEffect, useState, useMemo } from 'react';
import './Menu.css';
import { useToastMessages } from '../components/notifications/ToastContext';
import api from '../service/api';

export default function Produits() {
  const [produits, setProduits] = useState([]);
  const [loading, setLoading] = useState(true);
  const {error,success,setError,setSuccess,} = useToastMessages();

  useEffect(() => {
    const controller = new AbortController();
    
    api.get('/produits', { signal: controller.signal })
      .then(({ data }) => {
        setProduits(data || []);
        setError(null);
      })
      .catch((err) => {
        if (!controller.signal.aborted) {
          console.error('Erreur produits:', err);
          setError(err.response?.data?.message || err.message);
        }
      })
      .finally(() => setLoading(false));

    return () => controller.abort();
  }, []);

  const rows = useMemo(() => {
    return produits.map((p) => (
      <tr key={p.id}>
        <td>{p.nom}</td>
        <td>{p.categorie}</td>
        <td>{p.prixUnitaire} Ar</td>
        <td>{p.stock}</td>
      </tr>
    ));
  }, [produits]);

  return (
    <div className="page-content">
      <div className="content-card">
        <h2>Produits</h2>
        {loading && <p className="loading-state" role="status">Chargement des produits...</p>}
        {error && <p style={{ color: '#dc2626' }}>Erreur: {error}</p>}
        {!loading && produits.length === 0 && !error && <p>Aucun produit trouvé.</p>}
        {!loading && produits.length > 0 && (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Nom</th>
                  <th>Catégorie</th>
                  <th>Prix</th>
                  <th>Stock</th>
                </tr>
              </thead>
              <tbody>
                {rows}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
