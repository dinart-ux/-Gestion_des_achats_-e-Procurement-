
import { useCallback, useEffect, useRef, useState } from 'react';
import workflowApi from '../service/workflowApi';
import ActionIconButton from '../components/ActionIconButton.jsx';
import IconActionButton from '../components/IconActionButton.jsx';
import { useToastMessages } from '../components/notifications/ToastContext';
import './Categories.css';

const EMPTY_FORM = {
    nom: '',
    description: '',
};

function getErrorMessage(error) {
    return (
        error.response?.data?.message ||
        error.response?.data?.error ||
        error.message ||
        'Une erreur est survenue.'
    );
}

export default function Categories() {
    const [categories, setCategories] = useState([]);
    const [form, setForm] = useState(EMPTY_FORM);
    const [editingId, setEditingId] = useState(null);
    const [open, setOpen] = useState(false);
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);

    const { error, success, setError, setSuccess } = useToastMessages();
    const savingRef = useRef(false);

    const load = useCallback(async () => {
        setLoading(true);
        setError('');

        try {
            const response = await workflowApi.get('/categories');

            setCategories(
                Array.isArray(response.data) ? response.data : []
            );
        } catch (err) {
            setError(
                `Impossible de charger les catégories : ${getErrorMessage(err)}`
            );
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        load();
    }, [load]);

    // Fermeture avec la touche Échap
    useEffect(() => {
        if (!open) return;

        const handleEscape = (event) => {
            if (event.key === 'Escape' && !savingRef.current) {
                handleCancel();
            }
        };

        window.addEventListener('keydown', handleEscape);

        return () => {
            window.removeEventListener('keydown', handleEscape);
        };
    }, [open]);

    function handleChange(event) {
        const { name, value } = event.target;

        setForm((previous) => ({
            ...previous,
            [name]: value,
        }));
    }

    function handleNew() {
        setForm({ ...EMPTY_FORM });
        setEditingId(null);
        setError('');
        setSuccess('');
        setOpen(true);
    }

    function handleEdit(categorie) {
        setForm({
            nom: categorie.nom || '',
            description: categorie.description || '',
        });

        setEditingId(categorie.id);
        setError('');
        setSuccess('');
        setOpen(true);
    }

    function handleCancel() {
        if (savingRef.current) return;

        setForm({ ...EMPTY_FORM });
        setEditingId(null);
        setOpen(false);
        setError('');
    }

    async function handleSubmit(event) {
        event.preventDefault();

        if (savingRef.current) return;

        const nom = form.nom.trim();
        const description = form.description.trim();

        if (!nom) {
            setError('Le nom de la catégorie est obligatoire.');
            return;
        }

        if (nom.length > 100 || description.length > 255) {
            setError(
                'Le nom est limité à 100 caractères et la description à 255.'
            );
            return;
        }

        savingRef.current = true;
        setSaving(true);
        setError('');
        setSuccess('');

        try {
            const payload = { nom, description };

            if (editingId !== null) {
                await workflowApi.put(
                    `/categories/${editingId}`,
                    payload
                );

                setSuccess('Catégorie modifiée avec succès.');
            } else {
                await workflowApi.post('/categories', payload);

                setSuccess('Catégorie créée avec succès.');
            }

            setForm({ ...EMPTY_FORM });
            setEditingId(null);
            setOpen(false);

            await load();
        } catch (err) {
            setError(getErrorMessage(err));
        } finally {
            savingRef.current = false;
            setSaving(false);
        }
    }

    function isActive(categorie) {
        return (
            categorie.actif === true ||
            categorie.actif === 'true' ||
            categorie.actif === 1 ||
            categorie.actif === '1'
        );
    }

    async function handleStatus(categorie, action) {
        if (savingRef.current) return;

        const reactivation = action === 'reactiver';

        const confirmation = reactivation
            ? `Réactiver la catégorie « ${categorie.nom} » ?`
            : `Désactiver la catégorie « ${categorie.nom} » ?`;

        if (!window.confirm(confirmation)) return;

        setError('');
        setSuccess('');

        savingRef.current = true;
        setSaving(true);

        try {
            await workflowApi.put(
                `/categories/${categorie.id}/${action}`
            );

            setSuccess(
                reactivation
                    ? 'Catégorie réactivée avec succès.'
                    : 'Catégorie désactivée avec succès.'
            );

            await load();
        } catch (err) {
            setError(getErrorMessage(err));
        } finally {
            savingRef.current = false;
            setSaving(false);
        }
    }

    return (
        <div className="categories-page">

            {/* EN-TÊTE */}

            <div className="categories-header">
                <div>
                    <span className="categories-eyebrow">
                        PARAMÈTRES DES ACHATS
                    </span>

                    <h1>Catégories</h1>

                    <p>
                        Organisez les demandes d'achat en fonction
                        de leurs catégories.
                    </p>
                </div>

                <IconActionButton
                    icon="create"
                    type="button"
                    className="categories-add-button"
                    onClick={handleNew}
                    disabled={saving}
                >
                    Ajouter une catégorie
                </IconActionButton>
            </div>

            {/* NOTIFICATIONS */}

            {error && (
                <div className="categories-alert categories-alert-error" role="alert">
                    <span>!</span>
                    {error}
                </div>
            )}

            {success && (
                <div className="categories-alert categories-alert-success" role="status">
                    <span>✓</span>
                    {success}
                </div>
            )}

            {/* LISTE DES CATÉGORIES */}

            <section className="categories-card">

                <div className="categories-list-heading">
                    <div>
                        <h2>Liste des catégories</h2>
                        <p>
                            Consultez et gérez les catégories enregistrées.
                        </p>
                    </div>

                    <div className="categories-list-tools">
                        <span className="categories-count">
                            {categories.length} résultat(s)
                        </span>

                        <ActionIconButton
                            icon="refresh"
                            label="Actualiser la liste des catégories"
                            onClick={load}
                            disabled={loading}
                            loading={loading}
                        />
                    </div>
                </div>

                {loading && categories.length === 0 ? (
                    <div className="categories-empty">
                        <div className="categories-loader"></div>
                        <p>Chargement des catégories...</p>
                    </div>
                ) : categories.length === 0 ? (
                    <div className="categories-empty">
                        <div className="categories-empty-icon">▤</div>
                        <h3>Aucune catégorie enregistrée</h3>
                        <p>
                            Commencez par ajouter votre première catégorie.
                        </p>
                        <button
                            type="button"
                            className="categories-empty-button"
                            onClick={handleNew}
                        >
                            + Ajouter une catégorie
                        </button>
                    </div>
                ) : (
                    <div className="categories-table-wrapper">
                        <table className="categories-table">
                            <thead>
                                <tr>
                                    <th>ID</th>
                                    <th>CATÉGORIE</th>
                                    <th>DESCRIPTION</th>
                                    <th>STATUT</th>
                                    <th>ACTIONS</th>
                                </tr>
                            </thead>

                            <tbody>
                                {categories.map((categorie) => {
                                    const active = isActive(categorie);

                                    return (
                                        <tr key={categorie.id}>
                                            <td>
                                                <span className="categories-id">
                                                    #{categorie.id}
                                                </span>
                                            </td>

                                            <td>
                                                <div className="categories-name-cell">
                                                    <div className="categories-name-icon">
                                                        ▤
                                                    </div>
                                                    <strong>{categorie.nom}</strong>
                                                </div>
                                            </td>

                                            <td>
                                                <span className="categories-description">
                                                    {categorie.description || 'Aucune description'}
                                                </span>
                                            </td>

                                            <td>
                                                <span
                                                    className={
                                                        active
                                                            ? 'categories-status active'
                                                            : 'categories-status inactive'
                                                    }
                                                >
                                                    <span className="categories-status-dot"></span>
                                                    {active ? 'Active' : 'Inactive'}
                                                </span>
                                            </td>

                                            <td>
                                                <div className="categories-actions">
                                                    <ActionIconButton
                                                        icon="edit"
                                                        label={`Modifier la catégorie ${categorie.nom}`}
                                                        onClick={() => handleEdit(categorie)}
                                                        disabled={saving}
                                                    />

                                                    {active ? (
                                                        <ActionIconButton
                                                            icon="disable"
                                                            label={`Désactiver la catégorie ${categorie.nom}`}
                                                            className="categorie-disable-button"
                                                            onClick={() =>
                                                                handleStatus(categorie, 'desactiver')
                                                            }
                                                            disabled={saving}
                                                        />
                                                    ) : (
                                                        <ActionIconButton
                                                            icon="enable"
                                                            label={`Réactiver la catégorie ${categorie.nom}`}
                                                            className="categorie-enable-button"
                                                            onClick={() =>
                                                                handleStatus(categorie, 'reactiver')
                                                            }
                                                            disabled={saving}
                                                        />
                                                    )}
                                                </div>
                                            </td>
                                        </tr>
                                    );
                                })}
                            </tbody>
                        </table>
                    </div>
                )}

            </section>

            {/* MODALE AJOUT / MODIFICATION */}

            {open && (
                <div
                    className="categories-modal-overlay"
                    onMouseDown={(event) => {
                        if (
                            event.target === event.currentTarget &&
                            !savingRef.current
                        ) {
                            handleCancel();
                        }
                    }}
                >
                    <section
                        className="categories-modal"
                        role="dialog"
                        aria-modal="true"
                        aria-labelledby="categories-modal-title"
                    >

                        <div className="categories-modal-header">
                            <div className="categories-modal-heading">
                                <div className="categories-modal-icon">
                                    {editingId !== null ? '✎' : '+'}
                                </div>

                                <div>
                                    <span className="categories-eyebrow">
                                        GESTION DES CATÉGORIES
                                    </span>

                                    <h2 id="categories-modal-title">
                                        {editingId !== null
                                            ? 'Modifier la catégorie'
                                            : 'Ajouter une catégorie'}
                                    </h2>

                                    <p>
                                        {editingId !== null
                                            ? 'Modifiez les informations de cette catégorie.'
                                            : 'Renseignez les informations de la nouvelle catégorie.'}
                                    </p>
                                </div>
                            </div>

                            <button
                                type="button"
                                className="categories-modal-close"
                                onClick={handleCancel}
                                disabled={saving}
                                aria-label="Fermer"
                            >
                                ×
                            </button>
                        </div>

                        <form onSubmit={handleSubmit}>

                            <div className="categories-modal-body">

                                <div className="categories-form-intro">
                                    <span className="categories-required">*</span>
                                    Champs obligatoires
                                </div>

                                <div className="categories-field">
                                    <label htmlFor="categorie-nom">
                                        Nom de la catégorie
                                        <span className="categories-required">*</span>
                                    </label>

                                    <input
                                        id="categorie-nom"
                                        type="text"
                                        name="nom"
                                        value={form.nom}
                                        onChange={handleChange}
                                        placeholder="Ex. : Fournitures de bureau"
                                        maxLength={100}
                                        disabled={saving}
                                        required
                                        autoFocus
                                    />

                                    <small>
                                        Donnez un nom clair et facile à identifier.
                                    </small>

                                    <div className="categories-character-count">
                                        {form.nom.length}/100 caractères
                                    </div>
                                </div>

                                <div className="categories-field">
                                    <label htmlFor="categorie-description">
                                        Description
                                        <span className="categories-optional">
                                            Facultatif
                                        </span>
                                    </label>

                                    <textarea
                                        id="categorie-description"
                                        name="description"
                                        value={form.description}
                                        onChange={handleChange}
                                        placeholder="Décrivez brièvement cette catégorie..."
                                        rows={4}
                                        maxLength={255}
                                        disabled={saving}
                                    />

                                    <div className="categories-character-count">
                                        {form.description.length}/255 caractères
                                    </div>
                                </div>

                            </div>

                            <div className="categories-modal-footer">
                                <button
                                    type="button"
                                    className="categories-cancel-button"
                                    onClick={handleCancel}
                                    disabled={saving}
                                >
                                    Annuler
                                </button>

                                <button
                                    type="submit"
                                    className="categories-submit-button"
                                    disabled={saving}
                                >
                                    {saving
                                        ? 'Enregistrement...'
                                        : editingId !== null
                                            ? '✓ Enregistrer les modifications'
                                            : '+ Créer la catégorie'}
                                </button>
                            </div>

                        </form>
                    </section>
                </div>
            )}

        </div>
    );
}
