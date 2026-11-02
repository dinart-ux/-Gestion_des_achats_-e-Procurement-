
import { useEffect, useMemo, useState } from 'react';
import workflowApi from '../service/workflowApi';
import { getRoleCode, getStoredUser } from '../service/roleUtils';
import { useToastMessages } from '../components/notifications/ToastContext';
import './Menu.css';
import './Dashboard.css';

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

const isTrue = (value) =>
    value === true || value === 1 || value === 'true' || value === '1';

function SummaryCard({ label, value, detail, icon, variant = '' }) {
    return (
        <article className={`dashboard-summary-card ${variant}`}>
            <div className="dashboard-summary-top">
                <span className="dashboard-summary-icon" aria-hidden="true">{icon}</span>
                <span className="dashboard-summary-label">{label}</span>
            </div>
            <strong className="dashboard-summary-value">{value}</strong>
            {detail && <span className="dashboard-summary-detail">{detail}</span>}
        </article>
    );
}

function BudgetDonut({ percent }) {
    const value = Math.max(0, Math.min(100, safeNumber(percent)));

    return (
        <div className="budget-donut-layout">
            <div
                className="budget-donut"
                style={{ background: `conic-gradient(var(--dashboard-violet) ${value}%, #e9edf5 ${value}% 100%)` }}
                role="img"
                aria-label={`Consommation budgétaire de ${formatNumber(value)} %`}
            >
                <div className="budget-donut-center">
                    <strong>{formatNumber(value)}%</strong>
                    <span>consommé</span>
                </div>
            </div>

            <div className="budget-donut-legend">
                <div><span className="legend-dot legend-dot-used" /><span>Budget engagé</span></div>
                <div><span className="legend-dot legend-dot-remaining" /><span>Budget restant</span></div>
            </div>
        </div>
    );
}

function BudgetBar({ budget }) {
    const allocated = safeNumber(budget.montant_alloue);
    const engaged = safeNumber(budget.montant_engage);
    const percent = allocated > 0 ? (engaged / allocated) * 100 : 0;
    const threshold = budget.seuil_alerte_pct == null ? 80 : safeNumber(budget.seuil_alerte_pct);
    const overBudget = allocated > 0 && engaged > allocated;
    const alert = allocated > 0 && (overBudget || percent >= threshold);
    const width = Math.max(0, Math.min(100, percent));

    return (
        <div className="budget-bar-row">
            <div className="budget-bar-heading">
                <div className="budget-bar-label">
                    <strong>{budget.libelle || budget.code || 'Centre de coût'}</strong>
                    {budget.code && budget.libelle && <span>{budget.code}</span>}
                </div>
                <div className="budget-bar-values">
                    <strong>{formatMoney(engaged)}</strong><span> / {formatMoney(allocated)}</span>
                </div>
            </div>

            <div
                className={`budget-bar-track ${alert ? 'is-alert' : ''}`}
                role="progressbar"
                aria-valuemin={0}
                aria-valuemax={100}
                aria-valuenow={Math.min(100, Math.round(percent))}
                aria-label={`Consommation de ${budget.libelle || budget.code || 'ce budget'}`}
            >
                <div className={`budget-bar-fill ${alert ? 'is-alert' : ''}`} style={{ width: `${width}%` }} />
            </div>

            <div className="budget-bar-footer">
                <span className={alert ? 'budget-percent warning-text' : 'budget-percent'}>
                    {formatNumber(percent)}% consommé
                </span>
                <span>Seuil : {formatNumber(threshold)}%{overBudget ? ' · Dépassement' : ''}</span>
            </div>
        </div>
    );
}

export default function Dashboard() {
    const { setError } = useToastMessages();

    const user = getStoredUser();
    const role = getRoleCode(user?.role ?? user?.roleCode);
    const canViewBudgets = ['ADMIN', 'SERVICE_ACHATS', 'MANAGER'].includes(role);

    const [budgets, setBudgets] = useState([]);
    const [budgetError, setBudgetError] = useState('');
    const [loadingBudgets, setLoadingBudgets] = useState(false);
    const [budgetView, setBudgetView] = useState('service');
    const [showBudgetDetails, setShowBudgetDetails] = useState(false);

    useEffect(() => {
        if (!canViewBudgets) return undefined;

        let active = true;

        const loadBudgets = async () => {
            setLoadingBudgets(true);
            setBudgetError('');

            try {
                const response = await workflowApi.get('budgets');
                if (active) setBudgets(getArray(response?.data ?? response));
            } catch (error) {
                if (active) {
                    const message = error?.response?.data?.message
                        || 'Impossible de charger les données budgétaires.';

                    setBudgetError(message);
                    setError(message);
                }
            } finally {
                if (active) setLoadingBudgets(false);
            }
        };

        loadBudgets();

        return () => {
            active = false;
        };
    }, [canViewBudgets]);

    const budgetDefined = (budget) => {
        if (Object.prototype.hasOwnProperty.call(budget, 'budget_id')) {
            return budget.budget_id !== null && budget.budget_id !== undefined;
        }
        return budget.montant_alloue !== null && budget.montant_alloue !== undefined;
    };

    const budgetsDefinis = useMemo(() => budgets.filter(budgetDefined), [budgets]);
    const totalBudget = budgetsDefinis.reduce((sum, budget) => sum + safeNumber(budget.montant_alloue), 0);
    const totalEngage = budgetsDefinis.reduce((sum, budget) => sum + safeNumber(budget.montant_engage), 0);
    const totalDisponible = totalBudget - totalEngage;
    const consommationGlobale = totalBudget > 0 ? (totalEngage / totalBudget) * 100 : 0;

    const budgetsParService = useMemo(() => {
        const groups = {};

        budgets.forEach((budget) => {
            const serviceName = budget.service || null;
            const serviceId = budget.service_id ?? null;
            const key = serviceName
                ? `service-${serviceId ?? serviceName}`
                : `centre-${budget.centre_cout_id ?? budget.code ?? budget.libelle}`;

            if (!groups[key]) {
                groups[key] = {
                    id: key,
                    service: serviceName,
                    libelle: serviceName || budget.libelle || budget.code || 'Centre de coût',
                    centres: [],
                };
            }

            groups[key].centres.push(budget);
        });

        return Object.values(groups).map((group) => {
            const defined = group.centres.filter(budgetDefined);
            const allocated = defined.reduce((sum, budget) => sum + safeNumber(budget.montant_alloue), 0);
            const engaged = defined.reduce((sum, budget) => sum + safeNumber(budget.montant_engage), 0);

            return {
                ...group,
                montant_alloue: allocated,
                montant_engage: engaged,
                seuil_alerte_pct: 80,
                alerte_seuil: defined.some((budget) => {
                    const amount = safeNumber(budget.montant_alloue);
                    const spent = safeNumber(budget.montant_engage);
                    const rate = amount > 0 ? spent / amount * 100 : 0;
                    const threshold = budget.seuil_alerte_pct == null ? 80 : safeNumber(budget.seuil_alerte_pct);

                    return amount > 0 && (spent > amount || rate >= threshold || isTrue(budget.alerte_seuil));
                }),
            };
        });
    }, [budgets]);

    const budgetChartRows = useMemo(() => {
        const rows = budgetView === 'service'
            ? budgetsParService
            : budgets.map((budget) => ({ ...budget, libelle: budget.libelle || budget.code || 'Centre de coût' }));

        return [...rows].sort((a, b) => {
            const aPercent = safeNumber(a.montant_alloue) > 0 ? safeNumber(a.montant_engage) / safeNumber(a.montant_alloue) : 0;
            const bPercent = safeNumber(b.montant_alloue) > 0 ? safeNumber(b.montant_engage) / safeNumber(b.montant_alloue) : 0;
            return bPercent - aPercent;
        });
    }, [budgetView, budgets, budgetsParService]);

    const budgetAlerts = budgets.filter((budget) => {
        if (!budgetDefined(budget)) return false;

        const allocated = safeNumber(budget.montant_alloue);
        const engaged = safeNumber(budget.montant_engage);

        if (allocated <= 0) return false;

        const percent = engaged / allocated * 100;
        const threshold = budget.seuil_alerte_pct == null ? 80 : safeNumber(budget.seuil_alerte_pct);

        return engaged > allocated || isTrue(budget.alerte_seuil) || percent >= threshold;
    });

    const budgetNonDefinis = budgets.filter((budget) => !budgetDefined(budget));

    return (
        <main className="dashboard-page dashboard-page-compact">
            {canViewBudgets ? (
                <section className="dashboard-section">
                    <div className="dashboard-section-heading">
                        <div>
                            <span className="dashboard-eyebrow">PILOTAGE FINANCIER</span>
                            <h2>Suivi budgétaire</h2>
                            <p>Consommation globale et budgets à surveiller.</p>
                        </div>
                        <span className="dashboard-year-badge">Exercice {new Date().getFullYear()}</span>
                    </div>

                    {loadingBudgets && <div className="dashboard-message">Chargement des budgets…</div>}
                    {budgetError && <div className="dashboard-message dashboard-message-error">{budgetError}</div>}

                    {!loadingBudgets && !budgetError && budgets.length === 0 && (
                        <div className="dashboard-message">Aucun budget n'est disponible pour cet exercice.</div>
                    )}

                    {!loadingBudgets && !budgetError && budgets.length > 0 && (
                        <>
                            <div className="dashboard-summary-grid">
                                <SummaryCard
                                    label="Budget alloué"
                                    value={formatMoney(totalBudget)}
                                    detail={`${budgetsDefinis.length} budget(s) défini(s)`}
                                    icon="◈"
                                    variant="summary-navy"
                                />
                                <SummaryCard
                                    label="Montant engagé"
                                    value={formatMoney(totalEngage)}
                                    detail={`${formatNumber(consommationGlobale)} % consommé`}
                                    icon="↗"
                                    variant="summary-violet"
                                />
                                <SummaryCard
                                    label="Solde disponible"
                                    value={formatMoney(totalDisponible)}
                                    detail={totalDisponible < 0 ? 'Budget dépassé' : 'Montant restant'}
                                    icon="◉"
                                    variant={totalDisponible < 0 ? 'summary-danger' : 'summary-light'}
                                />
                            </div>

                            <div className="dashboard-budget-overview">
                                <article className="dashboard-panel budget-global-panel">
                                    <div className="dashboard-panel-heading">
                                        <div>
                                            <h3>Consommation globale</h3>
                                            <p>Part du budget déjà engagée.</p>
                                        </div>
                                    </div>

                                    {budgetsDefinis.length > 0
                                        ? <BudgetDonut percent={consommationGlobale} />
                                        : <div className="dashboard-empty">Aucun budget n'est encore défini.</div>}

                                    <div className="budget-global-amounts">
                                        <div><span>Engagé</span><strong>{formatMoney(totalEngage)}</strong></div>
                                        <div><span>Disponible</span><strong>{formatMoney(totalDisponible)}</strong></div>
                                    </div>
                                </article>

                                <article className="dashboard-panel budget-comparison-panel">
                                    <div className="dashboard-panel-heading budget-comparison-heading">
                                        <div>
                                            <h3>Consommation par {budgetView === 'service' ? 'service' : 'centre de coût'}</h3>
                                            <p>Les budgets les plus consommés en premier.</p>
                                        </div>

                                        <div className="budget-view-toggle" role="group" aria-label="Affichage du budget">
                                            <button type="button" className={budgetView === 'service' ? 'active' : ''} onClick={() => setBudgetView('service')}>Services</button>
                                            <button type="button" className={budgetView === 'centre' ? 'active' : ''} onClick={() => setBudgetView('centre')}>Centres</button>
                                        </div>
                                    </div>

                                    {budgetChartRows.length ? (
                                        <div className="budget-bars-list">
                                            {budgetChartRows.slice(0, 6).map((budget, index) => (
                                                <BudgetBar key={`${budget.id ?? budget.centre_cout_id ?? budget.code ?? index}-${index}`} budget={budget} />
                                            ))}
                                        </div>
                                    ) : (
                                        <div className="dashboard-empty">Aucune donnée budgétaire à afficher.</div>
                                    )}

                                    {budgetChartRows.length > 6 && (
                                        <p className="dashboard-chart-note">Les 6 budgets les plus consommés sont affichés.</p>
                                    )}
                                </article>
                            </div>

                            <article className="dashboard-panel dashboard-alerts-panel">
                                <div className="dashboard-panel-heading">
                                    <div>
                                        <h3>Alertes budgétaires</h3>
                                        <p>Budgets proches du seuil ou en dépassement.</p>
                                    </div>

                                    <span className={`dashboard-alert-count ${budgetAlerts.length ? 'has-alerts' : ''}`}>
                                        {budgetAlerts.length} alerte(s)
                                    </span>
                                </div>

                                {budgetAlerts.length === 0 ? (
                                    <div className="dashboard-alert-success">
                                        <span aria-hidden="true">✓</span> Aucun budget n'a atteint son seuil d'alerte.
                                    </div>
                                ) : (
                                    <div className="dashboard-alert-list">
                                        {budgetAlerts.slice(0, 5).map((budget, index) => {
                                            const allocated = safeNumber(budget.montant_alloue);
                                            const engaged = safeNumber(budget.montant_engage);
                                            const percent = allocated > 0 ? engaged / allocated * 100 : 0;
                                            const overBudget = engaged > allocated;

                                            return (
                                                <div className={`dashboard-alert-item ${overBudget ? 'alert-critical' : ''}`} key={`${budget.budget_id ?? budget.centre_cout_id ?? index}-${index}`}>
                                                    <span className="dashboard-alert-icon" aria-hidden="true">!</span>
                                                    <div className="dashboard-alert-content">
                                                        <strong>{budget.libelle || budget.code || 'Centre de coût'}</strong>
                                                        <span>{overBudget ? `Dépassement de ${formatMoney(engaged - allocated)}` : `${formatNumber(percent)} % du budget consommé`}</span>
                                                    </div>
                                                    <div className="dashboard-alert-amount">
                                                        <strong>{formatMoney(engaged)}</strong>
                                                        <span>sur {formatMoney(allocated)}</span>
                                                    </div>
                                                </div>
                                            );
                                        })}

                                        {budgetAlerts.length > 5 && (
                                            <p className="dashboard-chart-note">Les 5 premières alertes sont affichées. Consulte les détails des budgets pour la liste complète.</p>
                                        )}
                                    </div>
                                )}

                                {budgetNonDefinis.length > 0 && (
                                    <p className="dashboard-chart-note">{budgetNonDefinis.length} centre(s) de coût sans budget défini.</p>
                                )}
                            </article>

                            <article className="dashboard-panel dashboard-budget-details">
                                <button
                                    type="button"
                                    className="dashboard-details-toggle"
                                    aria-expanded={showBudgetDetails}
                                    onClick={() => setShowBudgetDetails((previous) => !previous)}
                                >
                                    <span>
                                        <strong>Détails des budgets</strong>
                                        <small>Montants, seuils et soldes par centre de coût.</small>
                                    </span>
                                    <span className="dashboard-details-chevron" aria-hidden="true">{showBudgetDetails ? '−' : '+'}</span>
                                </button>

                                {showBudgetDetails && (
                                    <div className="dashboard-table-wrapper">
                                        <table className="dashboard-table">
                                            <thead>
                                                <tr>
                                                    <th>Centre de coût</th>
                                                    <th>Budget alloué</th>
                                                    <th>Montant engagé</th>
                                                    <th>Solde disponible</th>
                                                    <th>Consommation</th>
                                                    <th>Seuil</th>
                                                    <th>État</th>
                                                </tr>
                                            </thead>

                                            <tbody>
                                                {budgets.map((budget, index) => {
                                                    const allocated = safeNumber(budget.montant_alloue);
                                                    const engaged = safeNumber(budget.montant_engage);
                                                    const balance = allocated - engaged;
                                                    const percent = allocated > 0 ? engaged / allocated * 100 : 0;
                                                    const threshold = budget.seuil_alerte_pct == null ? 80 : safeNumber(budget.seuil_alerte_pct);
                                                    const overBudget = budgetDefined(budget) && engaged > allocated;
                                                    const alert = budgetDefined(budget) && allocated > 0 && (overBudget || isTrue(budget.alerte_seuil) || percent >= threshold);

                                                    return (
                                                        <tr key={`${budget.budget_id ?? budget.centre_cout_id ?? index}-${index}`}>
                                                            <td>
                                                                <strong>{budget.libelle || 'Centre de coût'}</strong>
                                                                <small>{budget.code || '—'}</small>
                                                            </td>
                                                            <td>{budgetDefined(budget) ? formatMoney(allocated) : 'Non défini'}</td>
                                                            <td>{budgetDefined(budget) ? formatMoney(engaged) : '—'}</td>
                                                            <td className={balance < 0 ? 'warning-text' : ''}>{budgetDefined(budget) ? formatMoney(balance) : '—'}</td>
                                                            <td>
                                                                {budgetDefined(budget) ? (
                                                                    <div className="table-progress-cell">
                                                                        <div className="table-progress-track">
                                                                            <div className={`table-progress-fill ${alert ? 'is-alert' : ''}`} style={{ width: `${Math.min(100, Math.max(0, percent))}%` }} />
                                                                        </div>
                                                                        <span>{formatNumber(percent)}%</span>
                                                                    </div>
                                                                ) : '—'}
                                                            </td>
                                                            <td>{budgetDefined(budget) ? `${formatNumber(threshold)}%` : '—'}</td>
                                                            <td>
                                                                {!budgetDefined(budget)
                                                                    ? <span className="dashboard-status status-undefined">Non défini</span>
                                                                    : overBudget
                                                                        ? <span className="dashboard-status status-danger">Dépassé</span>
                                                                        : alert
                                                                            ? <span className="dashboard-status status-warning">À surveiller</span>
                                                                            : <span className="dashboard-status status-ok">Normal</span>}
                                                            </td>
                                                        </tr>
                                                    );
                                                })}
                                            </tbody>
                                        </table>
                                    </div>
                                )}
                            </article>
                        </>
                    )}
                </section>
            ) : (
                <section className="dashboard-panel dashboard-no-access">
                    <h2>Bienvenue sur le tableau de bord</h2>
                    <p>Les informations de suivi budgétaire ne sont pas accessibles avec ton rôle actuel.</p>
                    <p>Consulte tes demandes pour suivre leur avancement.</p>
                </section>
            )}
        </main>
    );
}
