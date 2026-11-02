
import { useCallback, useEffect, useRef, useState } from 'react';
import workflowApi from '../service/workflowApi';
import ActionIconButton from '../components/ActionIconButton.jsx';
import IconActionButton from '../components/IconActionButton.jsx';
import { useToastMessages } from '../components/notifications/ToastContext';
import './Menu.css';
import './Fournisseurs.css';

const emptyForm = {
  raisonSociale: '',
  nif: '',
  stat: '',
  adresse: '',
  telephone: '',
  email: '',
  contactNom: '',
  conditionsPaiement: '',
  delaiLivraisonJours: '',
  categories: [],
};

export default function Fournisseurs() {
  const [items, setItems] = useState([]);
  const [categories, setCategories] = useState([]);
  const [form, setForm] = useState(emptyForm);
  const [open, setOpen] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const {error,success,setError,setSuccess,} = useToastMessages();
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const savingRef = useRef(false);
  const [searchTerm, setSearchTerm] = useState('');
  const filteredItems = items.filter((supplier) => {
  const search = searchTerm.trim().toLowerCase();

  if (!search) return true;

  return [
    supplier.raison_sociale,
    supplier.contact_nom,
    supplier.email,
    supplier.telephone,
    supplier.nif,
    supplier.stat,
  ].some((value) =>
    String(value || '').toLowerCase().includes(search)
  );
});

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [supplierResponse, categoryResponse] = await Promise.all([
        workflowApi.get('/fournisseurs'),
        workflowApi.get('/categories'),
      ]);
      setItems(supplierResponse.data);
      setCategories(categoryResponse.data);
      setError('');
    } catch (requestError) {
      setError(
        requestError.response?.data?.message ||
        'Impossible de charger les fournisseurs.'
      );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  // Gestion de la touche Echap et du défilement de la page
  useEffect(() => {
    if (!open) return;

    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';

    const handleKeyDown = (event) => {
      if (event.key === 'Escape' && !savingRef.current) {
        setForm(emptyForm);
        setEditingId(null);
        setOpen(false);
      }
    };

    window.addEventListener('keydown', handleKeyDown);

    return () => {
      document.body.style.overflow = previousOverflow;
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [open]);

  const closeForm = () => {
    setForm(emptyForm);
    setEditingId(null);
    setOpen(false);
  };

  const edit = (supplier) => {
    setEditingId(supplier.id);
    setForm({
      raisonSociale: supplier.raison_sociale || '',
      nif: supplier.nif || '',
      stat: supplier.stat || '',
      adresse: supplier.adresse || '',
      telephone: supplier.telephone || '',
      email: supplier.email || '',
      contactNom: supplier.contact_nom || '',
      conditionsPaiement: supplier.conditions_paiement || '',
      delaiLivraisonJours:
        supplier.delai_livraison_jours == null
          ? ''
          : String(supplier.delai_livraison_jours),
      categories: (supplier.categories || []).map(String),
    });
    setOpen(true);
    setError('');
  };

  const save = async (event) => {
    event.preventDefault();

    if (savingRef.current) return;

    if (!form.raisonSociale.trim()) {
      setError('La raison sociale est obligatoire.');
      return;
    }

    if (
      form.delaiLivraisonJours !== '' &&
      (
        !Number.isInteger(Number(form.delaiLivraisonJours)) ||
        Number(form.delaiLivraisonJours) < 0
      )
    ) {
      setError('Le délai doit être un nombre entier positif ou nul.');
      return;
    }

    savingRef.current = true;
    setSaving(true);
    setError('');
    setSuccess('');

    const payload = {
      ...form,
      delaiLivraisonJours:
        form.delaiLivraisonJours === ''
          ? null
          : Number(form.delaiLivraisonJours),
      categories: form.categories.map(Number),
    };

    try {
      if (editingId) {
        await workflowApi.put(`/fournisseurs/${editingId}`, payload);
      } else {
        await workflowApi.post('/fournisseurs', payload);
      }

      setSuccess(
        editingId
          ? 'Fournisseur modifié avec succès.'
          : 'Fournisseur créé avec succès.'
      );

      closeForm();
      await load();
    } catch (requestError) {
      setError(
        requestError.response?.data?.message ||
        'Impossible d’enregistrer le fournisseur.'
      );
    } finally {
      savingRef.current = false;
      setSaving(false);
    }
  };

  const deactivate = async (supplier) => {
    if (
      !window.confirm(
        `Désactiver le fournisseur « ${supplier.raison_sociale} » ? Ses offres et commandes seront conservées.`
      )
    ) return;

    setSaving(true);
    setError('');
    setSuccess('');

    try {
      await workflowApi.delete(`/fournisseurs/${supplier.id}`);

      setSuccess(
        'Fournisseur désactivé. Ses offres et commandes ont été conservées.'
      );

      await load();
    } catch (requestError) {
      setError(
        requestError.response?.data?.message ||
        'Impossible de désactiver le fournisseur.'
      );
    } finally {
      setSaving(false);
    }
  };

  const startNew = () => {
    if (open && !editingId) {
      closeForm();
    } else {
      setForm(emptyForm);
      setEditingId(null);
      setError('');
      setOpen(true);
    }
  };

  return (
    <div className="page-content">
      <section className="content-card">

        <div className="section-heading">
          <div>
            <h2>Fournisseurs</h2>
            <p>
              Consultez et mettez à jour le répertoire fournisseur.
            </p>
          </div>

          <IconActionButton
            icon="create"
            type="button"
            className="action-button"
            disabled={saving}
            onClick={startNew}
          >
            Ajouter un fournisseur
          </IconActionButton>
        </div>
        {/*rechere fournisseur */}
        <div className="supplier-search-container">
  <div className="supplier-search-box">
    <span className="supplier-search-icon">⌕</span>

    <input
    type="text"
      placeholder="Rechercher un fournisseur, un contact, un email..."
      value={searchTerm}
      onChange={(e) => setSearchTerm(e.target.value)}
      aria-label="Rechercher un fournisseur"
    />

    {searchTerm && (
      <button
        type="button"
        className="supplier-search-clear"
        onClick={() => setSearchTerm('')}
        aria-label="Effacer la recherche"
      >
        ×
      </button>
    )}
  </div>

  <span className="supplier-search-count">
    {filteredItems.length} fournisseur(s)
  </span>
</div>

        {error && !open && (
          <p className="workflow-error" role="alert">{error}</p>
        )}

        {success && (
          <p className="form-success" role="status">{success}</p>
        )}

        {/* Fenêtre modale du formulaire fournisseur */}
        {open && (
          <div
            className="supplier-modal-backdrop"
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
              className="workflow-form two-column-form supplier-modal"
              onSubmit={save}
              role="dialog"
              aria-modal="true"
              aria-labelledby="supplier-modal-title"
            >
              <div className="supplier-modal-header">
                <div>
                  <h3 id="supplier-modal-title">
                    {editingId
                      ? 'Modifier le fournisseur'
                      : 'Nouveau fournisseur'}
                  </h3>
                  <p>
                    Renseignez les informations du fournisseur.
                  </p>
                </div>

                <button
                  type="button"
                  className="supplier-modal-close"
                  onClick={closeForm}
                  disabled={saving}
                  aria-label="Fermer"
                >
                  &times;
                </button>
              </div>

              {error && (
                <p className="workflow-error supplier-modal-error" role="alert">
                  {error}
                </p>
              )}

              <label>
                Raison sociale
                <input
                  required
                  maxLength="150"
                  value={form.raisonSociale}
                  onChange={(e) =>
                    setForm({ ...form, raisonSociale: e.target.value })
                  }
                  disabled={saving}
                />
              </label>

              <label>
                Contact
                <input
                  maxLength="100"
                  value={form.contactNom}
                  onChange={(e) =>
                    setForm({ ...form, contactNom: e.target.value })
                  }
                  disabled={saving}
                />
              </label>

              <label>
                Email
                <input
                  type="email"
                  maxLength="150"
                  value={form.email}
                  onChange={(e) =>
                    setForm({ ...form, email: e.target.value })
                  }
                  disabled={saving}
                />
              </label>

              <label>
                Téléphone
                <input
                  maxLength="30"
                  value={form.telephone}
                  onChange={(e) =>
                    setForm({ ...form, telephone: e.target.value })
                  }
                  disabled={saving}
                />
              </label>

              <label>
                NIF
                <input
                  maxLength="30"
                  value={form.nif}
                  onChange={(e) =>
                    setForm({ ...form, nif: e.target.value })
                  }
                  disabled={saving}
                />
              </label>

              <label>
                STAT
                <input
                  maxLength="30"
                  value={form.stat}
                  onChange={(e) =>
                    setForm({ ...form, stat: e.target.value })
                  }
                  disabled={saving}
                />
              </label>

              <label>
                Adresse
                <input
                  maxLength="255"
                  value={form.adresse}
                  onChange={(e) =>
                    setForm({ ...form, adresse: e.target.value })
                  }
                  disabled={saving}
                />
              </label>

              <label>
                Délai habituel (jours)
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
                  disabled={saving}
                />
              </label>

              <label>
                Conditions de paiement
                <input
                  maxLength="255"
                  value={form.conditionsPaiement}
                  onChange={(e) =>
                    setForm({
                      ...form,
                      conditionsPaiement: e.target.value,
                    })
                  }
                  disabled={saving}
                />
              </label>

              <fieldset className="category-options supplier-category-options">
                <legend>Catégories</legend>

                {categories.map((category) => {
                  const id = String(category.id);

                  return (
                    <label key={id}>
                      <input
                        type="checkbox"
                        disabled={saving}
                        checked={form.categories.includes(id)}
                        onChange={(event) =>
                          setForm({
                            ...form,
                            categories: event.target.checked
                              ? [...form.categories, id]
                              : form.categories.filter(
                                  (selectedId) => selectedId !== id
                                ),
                          })
                        }
                      />
                      {category.nom}
                    </label>
                  );
                })}
              </fieldset>

              <div className="form-actions supplier-modal-actions">
                <IconActionButton
                  icon="cancel"
                  type="button"
                  className="quiet-button"
                  onClick={closeForm}
                  disabled={saving}
                >
                  Annuler
                </IconActionButton>

                <IconActionButton
                  icon="save"
                  type="submit"
                  className="action-button"
                  disabled={saving}
                >
                  {saving ? 'Enregistrement…' : 'Enregistrer'}
                </IconActionButton>
              </div>
            </form>
          </div>
        )}

        {loading ? (
  <p className="loading-state">Chargement…</p>
) : filteredItems.length === 0 ? (
  <p className="supplier-empty-state">
    {searchTerm
      ? 'Aucun fournisseur ne correspond à votre recherche.'
      : 'Aucun fournisseur enregistré.'}
  </p>
) : (
          <div className="table-wrap">
              <table className="supplier-table">
                <colgroup>
                  <col style={{ width: '15%' }} />
                  <col style={{ width: '10%' }} />
                  <col style={{ width: '18%' }} />
                  <col style={{ width: '10%' }} />
                  <col style={{ width: '9%' }} />
                  <col style={{ width: '8%' }} />
                  <col style={{ width: '10%' }} />
                  <col style={{ width: '8%' }} />
                </colgroup>
              <thead>
                <tr>
                  <th>Raison sociale</th>
                  <th>Contact</th>
                  <th>Email</th>
                  <th>Téléphone</th>
                  <th>Paiement</th>
                  <th>Délai habituel</th>
                  <th>Catégories</th>
                  <th></th>
                </tr>
              </thead>

              <tbody>
                {filteredItems.map((supplier) => (
                  <tr key={supplier.id}>
                    <td>{supplier.raison_sociale}</td>
                    <td>{supplier.contact_nom || '—'}</td>
                    <td>{supplier.email || '—'}</td>
                    <td>{supplier.telephone || '—'}</td>
                    <td>{supplier.conditions_paiement || '—'}</td>
                    <td>
                      {supplier.delai_livraison_jours == null
                        ? '—'
                        : `${supplier.delai_livraison_jours} jours`}
                    </td>
                    <td>
                      {(supplier.categories || [])
                        .map(
                          (id) =>
                            categories.find(
                              (category) => category.id === id
                            )?.nom || id
                        )
                        .join(', ') || '—'}
                    </td>
                    <td>
                      <span>
                        {supplier.actif ? 'Actif' : 'Inactif'}
                      </span>{' '}

                      <ActionIconButton
                        icon="edit"
                        label={`Modifier le fournisseur ${supplier.raison_sociale}`}
                        onClick={() => edit(supplier)}
                        disabled={saving}
                      />

                      {supplier.actif && (
                        <ActionIconButton
                          icon="disable"
                          label={`Désactiver le fournisseur ${supplier.raison_sociale}`}
                          onClick={() => deactivate(supplier)}
                          disabled={saving}
                        />
                      )}
                    </td>
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