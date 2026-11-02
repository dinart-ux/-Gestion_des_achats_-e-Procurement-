import { useCallback, useEffect, useMemo, useState } from 'react';
import workflowApi from '../service/workflowApi';
import { getRoleCode, getStoredUser } from '../service/roleUtils';
import './Menu.css';
import './Rapports.css';

const today = new Date().toISOString().slice(0, 10);
const yearStart = `${today.slice(0, 4)}-01-01`;

const safeNumber = (value) => {
    const number = Number(value);
    return Number.isFinite(number) ? number : 0;
};

const formatMoney = (value) => new Intl.NumberFormat('fr-FR', {
    style: 'currency',
    currency: 'MGA',
    maximumFractionDigits: 0,
}).format(safeNumber(value));

const formatNumber = (value) => new Intl.NumberFormat('fr-FR', {
    maximumFractionDigits: 2,
}).format(safeNumber(value));

const getArray = (data) => {
    if (Array.isArray(data)) return data;
    if (Array.isArray(data?.content)) return data.content;
    if (Array.isArray(data?.data)) return data.data;
    return [];
};

const getLabel = (row, keys, fallback = 'Non renseigné') => {
    for (const key of keys) {
        if (row?.[key] !== undefined && row?.[key] !== null && row[key] !== '') {
            return String(row[key]);
        }
    }
    return fallback;
};

const getExpense = (row) => safeNumber(
    row?.depenses ??
    row?.depense ??
    row?.montant ??
    row?.total ??
    row?.montant_total ??
    row?.montant_engage ??
    0
);

function ExpenseBars({ rows = [], labelKeys }) {
    const normalizedRows = useMemo(() => (
        (Array.isArray(rows) ? rows : [])
            .map((row) => ({
                label: getLabel(row, labelKeys || ['nom', 'libelle', 'label']),
                value: getExpense(row),
            }))
            .sort((a, b) => b.value - a.value)
    ), [rows, labelKeys]);

    const maxValue = Math.max(1, ...normalizedRows.map((row) => row.value));

    if (!normalizedRows.length) {
        return <div className="dashboard-empty">Aucune donnée disponible pour cette période.</div>;
    }

    return (
        <div className="expense-bars">
            {normalizedRows.slice(0, 8).map((row, index) => (
                <div className="expense-bar-item" key={`${row.label}-${index}`}>
                    <div className="expense-bar-heading">
                        <span title={row.label}>{row.label}</span>
                        <strong>{formatMoney(row.value)}</strong>
                    </div>
                    <div className="expense-bar-track">
                        <div className="expense-bar-fill" style={{ width: `${Math.max(0, row.value / maxValue * 100)}%` }} />
                    </div>
                </div>
            ))}
        </div>
    );
}

function ReportKpi({ label, value, detail }) {
    return (
        <article className="dashboard-kpi-card">
            <span>{label}</span>
            <strong>{value}</strong>
            {detail && <small>{detail}</small>}
        </article>
    );
}

export default function Rapports() {
    const user = getStoredUser();
    const role = getRoleCode(user?.role ?? user?.roleCode);
    const canReport = ['ADMIN', 'SERVICE_ACHATS'].includes(role);

    const [du, setDu] = useState(yearStart);
    const [au, setAu] = useState(today);
    const [report, setReport] = useState(null);
    const [reportError, setReportError] = useState('');
    const [loadingReport, setLoadingReport] = useState(false);

    const loadReport = useCallback(async () => {
        if (!canReport) return;
        if (!du || !au || du > au) {
            setReportError('Vérifie les dates : la date de début doit précéder la date de fin.');
            return;
        }

        setLoadingReport(true);
        setReportError('');
        try {
            const response = await workflowApi.get('rapports', { params: { du, au } });
            setReport(response?.data ?? response);
        } catch (error) {
            setReportError(
                error?.response?.data?.message ||
                'Impossible de charger les rapports pour cette période.'
            );
        } finally {
            setLoadingReport(false);
        }
    }, [canReport, du, au]);

    useEffect(() => {
        if (canReport) loadReport();
    }, [canReport, loadReport]);

    const indicateurs = report?.indicateurs || {};
    const parStatut = getArray(report?.parStatut);
    const parService = getArray(report?.parService);
    const parFournisseur = getArray(report?.parFournisseur);
    const parCategorie = getArray(report?.parCategorie);

    const exportCsv = () => {
        if (!report) return;

        const rows = [
            ['Rapport e-Procurement'],
            ['Période', du, au],
            [],
            ['Indicateur', 'Valeur'],
            ['Nombre de demandes', indicateurs.nombre_demandes ?? 0],
            ['Demandes rejetées', indicateurs.demandes_rejetees ?? 0],
            ['Taux de rejet (%)', indicateurs.taux_rejet_pct ?? 0],
            ['Délai moyen de traitement (jours)', indicateurs.delai_moyen_traitement_jours ?? 0],
            ['Dépenses', indicateurs.depenses ?? 0],
            [],
            ['Dépenses par service'],
            ['Service', 'Dépenses'],
            ...parService.map((row) => [getLabel(row, ['service', 'nom_service', 'libelle', 'nom']), getExpense(row)]),
            [],
            ['Demandes par statut'],
            ['Statut', 'Nombre'],
            ...parStatut.map((row) => [getLabel(row, ['statut', 'label', 'nom']), row.nombre ?? row.total ?? row.count ?? row.nombre_demandes ?? 0]),
            [],
            ['Dépenses par fournisseur'],
            ['Fournisseur', 'Dépenses'],
            ...parFournisseur.map((row) => [getLabel(row, ['fournisseur', 'nom_fournisseur', 'libelle', 'nom']), getExpense(row)]),
            [],
            ['Dépenses par catégorie'],
            ['Catégorie', 'Dépenses'],
            ...parCategorie.map((row) => [getLabel(row, ['categorie', 'nom_categorie', 'libelle', 'nom']), getExpense(row)]),
        ];

        const csv = rows.map((row) => row.map((cell) => `"${String(cell ?? '').replace(/"/g, '""')}"`).join(';')).join('\r\n');
        const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `rapport-e-procurement-${du}-${au}.csv`;
        document.body.appendChild(link);
        link.click();
        link.remove();
        URL.revokeObjectURL(url);
    };

    if (!canReport) {
        return (
            <main className="rapports-page">
                <div className="dashboard-message dashboard-message-error">Tu n'as pas l'autorisation d'accéder aux rapports.</div>
            </main>
        );
    }

    return (
        <main className="rapports-page">
            <section className="dashboard-section">
                <div className="dashboard-section-heading">
                    <div>
                        <span className="dashboard-eyebrow">ANALYSE DES ACHATS</span>
                        <h2>Vue d'ensemble</h2>
                        <p>Choisis une période pour actualiser les indicateurs et les graphiques.</p>
                    </div>
                </div>

                <form className="dashboard-filter-panel" onSubmit={(event) => { event.preventDefault(); loadReport(); }}>
                    <div className="dashboard-date-field">
                        <label htmlFor="reports-date-from">Du</label>
                        <input id="reports-date-from" type="date" value={du} max={au || undefined} onChange={(event) => setDu(event.target.value)} />
                    </div>
                    <div className="dashboard-date-field">
                        <label htmlFor="reports-date-to">Au</label>
                        <input id="reports-date-to" type="date" value={au} min={du || undefined} onChange={(event) => setAu(event.target.value)} />
                    </div>
                    <button className="dashboard-primary-button" type="submit" disabled={loadingReport}>
                        {loadingReport ? 'Chargement…' : 'Actualiser'}
                    </button>
                    <button className="dashboard-secondary-button" type="button" onClick={exportCsv} disabled={!report || loadingReport}>
                        Exporter CSV
                    </button>
                </form>

                {reportError && <div className="dashboard-message dashboard-message-error">{reportError}</div>}
                {loadingReport && <div className="dashboard-message">Chargement des rapports…</div>}

                {!loadingReport && !reportError && report && (
                    <>
                        <div className="dashboard-report-kpis">
                            <ReportKpi label="Nombre de demandes" value={formatNumber(indicateurs.nombre_demandes)} detail="Sur la période" />
                            <ReportKpi label="Demandes rejetées" value={formatNumber(indicateurs.demandes_rejetees)} detail={`${formatNumber(indicateurs.taux_rejet_pct)} % de rejet`} />
                            <ReportKpi label="Délai moyen" value={`${formatNumber(indicateurs.delai_moyen_traitement_jours)} j`} detail="Délai de traitement" />
                            <ReportKpi label="Dépenses totales" value={formatMoney(indicateurs.depenses)} detail="Montant rapporté par l'API" />
                        </div>

                        <div className="dashboard-analytics-grid">
                            <article className="dashboard-panel">
                                <div className="dashboard-panel-heading">
                                    <div><h3>Dépenses par service</h3><p>Comparaison des montants déclarés par service.</p></div>
                                </div>
                                <ExpenseBars rows={parService} labelKeys={['service', 'nom_service', 'libelle', 'nom']} />
                            </article>

                            <article className="dashboard-panel">
                                <div className="dashboard-panel-heading">
                                    <div><h3>Demandes par statut</h3><p>État des demandes sur la période sélectionnée.</p></div>
                                </div>
                                {parStatut.length ? (
                                    <div className="dashboard-status-list">
                                        {parStatut.map((row, index) => (
                                            <div className="dashboard-status-row" key={`${row.statut ?? row.label ?? index}-${index}`}>
                                                <span className="dashboard-status-name">{getLabel(row, ['statut', 'label', 'nom'])}</span>
                                                <strong>{formatNumber(row.nombre ?? row.total ?? row.count ?? row.nombre_demandes ?? 0)}</strong>
                                            </div>
                                        ))}
                                    </div>
                                ) : <div className="dashboard-empty">Aucune donnée de statut disponible.</div>}
                            </article>
                        </div>

                        <div className="dashboard-expenses-comparison rapports-expenses-grid">
                            <article className="dashboard-panel">
                                <div className="dashboard-panel-heading">
                                    <div><h3>Dépenses par fournisseur</h3><p>Les 8 fournisseurs aux montants les plus élevés.</p></div>
                                </div>
                                <ExpenseBars rows={parFournisseur} labelKeys={['fournisseur', 'nom_fournisseur', 'libelle', 'nom']} />
                            </article>
                            <article className="dashboard-panel">
                                <div className="dashboard-panel-heading">
                                    <div><h3>Dépenses par catégorie</h3><p>Répartition des achats par catégorie.</p></div>
                                </div>
                                <ExpenseBars rows={parCategorie} labelKeys={['categorie', 'nom_categorie', 'libelle', 'nom']} />
                            </article>
                        </div>
                    </>
                )}

                {!loadingReport && !reportError && !report && (
                    <div className="dashboard-message">Aucun rapport chargé. Choisis une période puis clique sur Actualiser.</div>
                )}
            </section>
        </main>
    );
}