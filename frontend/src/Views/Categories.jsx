
import { useCallback, useEffect, useRef, useState } from 'react';
import workflowApi from '../service/workflowApi';
import ActionIconButton from '../components/ActionIconButton.jsx';
import IconActionButton from '../components/IconActionButton.jsx';
import { useToastMessages } from '../components/notifications/ToastContext';
import './Menu.css';

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
    const {error,success,setError,setSuccess,} = useToastMessages();
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

    function handleChange(event) {
        const { name, value } = event.target;

        setForm((previous) => ({
            ...previous,
            [name]: value,
        }));
    }

    function handleNew() {
        setForm(EMPTY_FORM);
        setEditingId(null);
        setOpen(true);
        setError('');
        setSuccess('');
    }

    function handleEdit(categorie) {
        setForm({
            nom: categorie.nom || '',
            description: categorie.description || '',
        });

        setEditingId(categorie.id);
        setOpen(true);
        setError('');
        setSuccess('');
    }

    function handleCancel() {
        setForm(EMPTY_FORM);
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
            setError('Le nom est limité à 100 caractères et la description à 255.');
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

            setForm(EMPTY_FORM);
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

        if (!window.confirm(confirmation)) {
            return;
        }

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
        <div className="page-content">
            <section className="content-card">

                <div className="section-heading">
                    <div>
                        <h2>Gestion des catégories</h2>
                        <p>
                            Crée et gère les catégories des demandes d’achat.
                        </p>
                    </div>

                    <IconActionButton
                        icon="create"
                        type="button"
                        className="action-button"
                        onClick={handleNew}
                        disabled={saving}
                    >
                        Ajouter une catégorie
                    </IconActionButton>
                </div>

                {error && (
                    <p className="workflow-error" role="alert">
                        {error}
                    </p>
                )}

                {success && (
                    <p
                        className="categories-alert success"
                        role="status"
                    >
                        {success}
                    </p>
                )}

                {open && (
                    <form
                        className="workflow-form two-column-form"
                        onSubmit={handleSubmit}
                    >
                        <label>
                            Nom de la catégorie *
                            <input
                                type="text"
                                name="nom"
                                value={form.nom}
                                onChange={handleChange}
                                placeholder="Ex. : Fournitures de bureau"
                                maxLength={100}
                                disabled={saving}
                                required
                            />
                        </label>

                        <label>
                            Description
                            <textarea
                                name="description"
                                value={form.description}
                                onChange={handleChange}
                                placeholder="Description de la catégorie"
                                rows={3}
                                maxLength={255}
                                disabled={saving}
                            />
                        </label>

                        <div className="form-actions">
                            <IconActionButton
                                icon="cancel"
                                type="button"
                                className="quiet-button"
                                onClick={handleCancel}
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
                                {saving
                                    ? 'Enregistrement...'
                                    : editingId !== null
                                        ? 'Enregistrer les modifications'
                                        : 'Créer la catégorie'}
                            </IconActionButton>
                        </div>
                    </form>
                )}

                <div className="section-heading categories-list-heading">
                    <div>
                        <h3>Liste des catégories</h3>
                        <p>
                            {categories.length} catégorie(s) enregistrée(s).
                        </p>
                    </div>

                    <ActionIconButton icon="refresh" label="Actualiser la liste des catégories" onClick={load} disabled={loading} loading={loading} />
                </div>

                {loading ? (
                    <p className="loading-state" role="status">Chargement des catégories...</p>
                ) : categories.length === 0 ? (
                    <p>Aucune catégorie enregistrée.</p>
                ) : (
                    <div className="table-wrap">
                        <table>
                            <thead>
                                <tr>
                                    <th>ID</th>
                                    <th>Nom</th>
                                    <th>Description</th>
                                    <th>Statut</th>
                                    <th>Actions</th>
                                </tr>
                            </thead>

                            <tbody>
                                {categories.map((categorie) => {
                                    const active = isActive(categorie);

                                    return (
                                        <tr key={categorie.id}>
                                            <td>{categorie.id}</td>

                                            <td>
                                                <strong>
                                                    {categorie.nom}
                                                </strong>
                                            </td>

                                            <td>
                                                {categorie.description || '—'}
                                            </td>

                                            <td>
                                                {active ? 'Active' : 'Inactive'}
                                            </td>

                                            <td>
                                                <div className="categorie-actions">
                                                    <ActionIconButton icon="edit" label={`Modifier la catégorie ${categorie.nom}`} onClick={() => handleEdit(categorie)} disabled={saving} />

                                                    {active ? (
                                                        <ActionIconButton icon="disable" label={`Désactiver la catégorie ${categorie.nom}`} className="categorie-disable-button" onClick={() => handleStatus(categorie, 'desactiver')} disabled={saving} />
                                                    ) : (
                                                        <ActionIconButton icon="enable" label={`Réactiver la catégorie ${categorie.nom}`} className="categorie-enable-button" onClick={() => handleStatus(categorie, 'reactiver')} disabled={saving} />
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
        </div>
    );
}
