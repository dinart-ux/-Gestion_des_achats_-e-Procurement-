import {
    useEffect,
    useState,
    useRef,
    useCallback,
    useMemo,
} from 'react';

import {
    NavLink,
    Route,
    Routes,
    Navigate,
    useNavigate,
    useLocation,
} from 'react-router-dom';

import {
    Building2,
    ChartNoAxesCombined,
    ClipboardCheck,
    FileText,
    House,
    Landmark,
    LogOut,
    Settings,
    ShoppingCart,
    Tags,
    Truck,
    Users,
    Wallet,
} from 'lucide-react';

import spatLogo from '../assets/images (1).jpeg';

import './Menu.css';

import Categories from './Categories.jsx';
import Dashboard from './Dashboard.jsx';
import Commandes from './Commandes.jsx';
import Parametres from './Parametres.jsx';
import PasswordChange from './PasswordChange.jsx';
import Demandes from './Demandes.jsx';
import Validations from './Validations.jsx';
import Fournisseurs from './Fournisseurs.jsx';
import Utilisateurs from './Utilisateurs.jsx';
import Offres from './Offres.jsx';
import Rapports from './Rapports.jsx';

import { getRoleCode, getStoredUser } from '../service/roleUtils';

/* Normalisation des rôles */
function normalizeRole(value) {
    if (value == null) return '';

    if (typeof value === 'object') {
        value =
            value.code ??
            value.roleCode ??
            value.libelle ??
            value.name ??
            value.id;
    }

    const normalized = String(value)
        .trim()
        .toUpperCase()
        .replace(/^ROLE_/, '');

    const roleMap = {
        '1': 'DEMANDEUR',
        '2': 'MANAGER',
        '3': 'SERVICE_ACHATS',
        '4': 'ADMIN',
        'SERVICE ACHATS': 'SERVICE_ACHATS',
        'SERVICE_ACHAT': 'SERVICE_ACHATS',
        'ADMINISTRATEUR': 'ADMIN',
    };

    return roleMap[normalized] ?? normalized;
}

function resolveRole(user) {
    if (!user) return '';

    const rawRole = user.role ?? user.roleCode;

    const candidates = [
        rawRole?.code,
        rawRole?.roleCode,
        rawRole?.libelle,
        rawRole?.name,
        getRoleCode(rawRole),
        rawRole?.id,
        rawRole,
    ];

    for (const candidate of candidates) {
        const role = normalizeRole(candidate);

        if (
            ['DEMANDEUR', 'MANAGER', 'SERVICE_ACHATS', 'ADMIN']
                .includes(role)
        ) {
            return role;
        }
    }

    return normalizeRole(rawRole);
}

/* Navigation principale */
const navItems = [
    {
        label: 'Tableau de bord',
        path: '/menu',
        icon: House,
        roles: ['DEMANDEUR', 'MANAGER', 'SERVICE_ACHATS', 'ADMIN'],
    },
    {
        label: 'Rapports et statistiques',
        path: '/menu/rapports',
        icon: ChartNoAxesCombined,
        roles: ['SERVICE_ACHATS', 'ADMIN'],
    },
    {
        label: 'Utilisateurs',
        path: '/menu/utilisateurs',
        icon: Users,
        roles: ['ADMIN'],
    },
    {
        label: 'Demandes d’achat',
        path: '/menu/demandes',
        icon: FileText,
        roles: ['DEMANDEUR', 'MANAGER', 'SERVICE_ACHATS', 'ADMIN'],
    },
    {
        label: 'Validations',
        path: '/menu/validations',
        icon: ClipboardCheck,
        roles: ['MANAGER', 'SERVICE_ACHATS', 'ADMIN'],
    },
    {
        label: 'Fournisseurs',
        path: '/menu/fournisseurs',
        icon: Truck,
        roles: ['SERVICE_ACHATS', 'ADMIN'],
    },
    {
        label: 'Offres',
        path: '/menu/offres',
        icon: Tags,
        roles: ['SERVICE_ACHATS', 'ADMIN'],
    },
    {
        label: 'Commandes',
        path: '/menu/commandes',
        icon: ShoppingCart,
        roles: ['SERVICE_ACHATS', 'ADMIN'],
    },
    {
        label: 'Catégories',
        path: '/menu/categories',
        icon: Tags,
        roles: ['ADMIN'],
    },
];

/* Sous-menu Paramètres */
const settingsItems = [
    {
        label: 'Services',
        path: '/menu/parametres#services',
        icon: Building2,
        roles: ['ADMIN'],
    },
    {
        label: 'Centres de coûts',
        path: '/menu/parametres#centres-de-couts',
        icon: Landmark,
        roles: ['MANAGER', 'SERVICE_ACHATS', 'ADMIN'],
    },
    {
        label: 'Budgets',
        path: '/menu/parametres#budgets',
        icon: Wallet,
        roles: ['MANAGER', 'SERVICE_ACHATS', 'ADMIN'],
    },
];

/* Permissions des pages */
const pagePermissions = {
    demandes: ['DEMANDEUR', 'MANAGER', 'SERVICE_ACHATS', 'ADMIN'],
    commandes: ['SERVICE_ACHATS', 'ADMIN'],
    validations: ['MANAGER', 'SERVICE_ACHATS', 'ADMIN'],
    fournisseurs: ['SERVICE_ACHATS', 'ADMIN'],
    offres: ['SERVICE_ACHATS', 'ADMIN'],
    categories: ['ADMIN'],
    parametres: ['MANAGER', 'SERVICE_ACHATS', 'ADMIN'],
    utilisateurs: ['ADMIN'],
    rapports: ['SERVICE_ACHATS', 'ADMIN'],
};

/* Protection d'une page selon le rôle */
function ProtectedPage({ role, allowedRoles, children }) {
    if (!allowedRoles.includes(role)) {
        return <Navigate to="/menu" replace />;
    }

    return children;
}

/* Icône et libellé d'un élément de navigation */
function NavigationLabel({ item }) {
    const Icon = item.icon;

    return (
        <>
            <Icon className="nav-icon" aria-hidden="true" />
            <span className="nav-label">{item.label}</span>
        </>
    );
}

export default function Menu({ onLogout }) {
    const navigate = useNavigate();
    const location = useLocation();

    const fileInputRef = useRef(null);
    const profileActionsRef = useRef(null);

    const [user, setUser] = useState(() => getStoredUser());

    const [profilePhoto, setProfilePhoto] = useState(
        () => localStorage.getItem('profilePhoto')
    );

    const [showMenu, setShowMenu] = useState(false);
    const [showLogoutModal, setShowLogoutModal] = useState(false);
    const [sidebarOpen, setSidebarOpen] = useState(false);
    const [sidebarHovered, setSidebarHovered] = useState(false);

    const [settingsOpen, setSettingsOpen] = useState(
        () => location.pathname === '/menu/parametres'
    );

    const [supplierMenuOpen, setSupplierMenuOpen] = useState(
        () =>
            location.pathname === '/menu/fournisseurs' ||
            location.pathname === '/menu/offres'
    );

    const role = useMemo(() => resolveRole(user), [user]);

    const settingsActive = location.pathname === '/menu/parametres';

    /* Fermer le menu du profil en cliquant à l'extérieur */
    useEffect(() => {
        if (!showMenu) return undefined;

        const closeOnOutsideClick = (event) => {
            if (!profileActionsRef.current?.contains(event.target)) {
                setShowMenu(false);
            }
        };

        const closeOnEscape = (event) => {
            if (event.key === 'Escape') {
                setShowMenu(false);
            }
        };

        document.addEventListener('pointerdown', closeOnOutsideClick);
        document.addEventListener('keydown', closeOnEscape);

        return () => {
            document.removeEventListener('pointerdown', closeOnOutsideClick);
            document.removeEventListener('keydown', closeOnEscape);
        };
    }, [showMenu]);

    /* Faire défiler jusqu'à la section ciblée dans Paramètres */
    useEffect(() => {
        if (!location.hash) return undefined;

        const frame = window.requestAnimationFrame(() => {
            document
                .getElementById(location.hash.slice(1))
                ?.scrollIntoView({
                    behavior: 'smooth',
                    block: 'start',
                });
        });

        return () => window.cancelAnimationFrame(frame);
    }, [location.hash]);

    /* Vérifier la session utilisateur */
    useEffect(() => {
        const token = localStorage.getItem('token');
        const storedUser = getStoredUser();

        if (!token || !storedUser || !resolveRole(storedUser)) {
            localStorage.removeItem('token');
            localStorage.removeItem('user');

            setUser(null);
            navigate('/login', { replace: true });
            return;
        }

        setUser(storedUser);
    }, [navigate]);

            /* Fermer la navigation après un changement de page */
        useEffect(() => {
            setSidebarOpen(false);
        }, [location.pathname, location.hash]);

        /* Fermer la navigation avec la touche Échap */
        useEffect(() => {
            if (!sidebarOpen) return undefined;

            const handleEscape = (event) => {
                if (event.key === 'Escape') {
                    setSidebarOpen(false);
                }
            };

            document.addEventListener('keydown', handleEscape);

            return () => {
                document.removeEventListener('keydown', handleEscape);
            };
        }, [sidebarOpen]);

    /* Éléments de navigation autorisés */
    const visibleItems = useMemo(
        () => navItems.filter((item) => item.roles.includes(role)),
        [role]
    );

    /* Titre de la page active */
    const currentPage = useMemo(() => {
        const items = [
            ...navItems,
            ...settingsItems.map((setting) => ({
                ...setting,
                anchor: true,
            })),
        ];

        const item = items
            .sort((a, b) => b.path.length - a.path.length)
            .find((navItem) => {
                if (navItem.anchor) {
                    return (
                        location.pathname === '/menu/parametres' &&
                        location.hash ===
                            `#${navItem.path.split('#')[1]}`
                    );
                }

                if (navItem.path === '/menu') {
                    return (
                        location.pathname === '/menu' ||
                        location.pathname === '/menu/'
                    );
                }

                return location.pathname === navItem.path;
            });

        return (
            item?.label ??
            (settingsActive ? 'Paramètres' : 'Espace de travail')
        );
    }, [location.pathname, location.hash, settingsActive]);

    /* Importer une photo de profil */
    const handlePhotoUpload = useCallback((event) => {
        const file = event.target.files?.[0];

        if (!file) return;

        if (!file.type.startsWith('image/')) {
            alert('Veuillez sélectionner une image valide.');
            event.target.value = '';
            return;
        }

        if (file.size > 2 * 1024 * 1024) {
            alert('La photo ne doit pas dépasser 2 Mo.');
            event.target.value = '';
            return;
        }

        const reader = new FileReader();

        reader.onloadend = () => {
            if (typeof reader.result === 'string') {
                setProfilePhoto(reader.result);
                localStorage.setItem('profilePhoto', reader.result);
            }
        };

        reader.readAsDataURL(file);
        event.target.value = '';
    }, []);

    /* Supprimer la photo de profil */
    const handleDeletePhoto = useCallback(() => {
        setProfilePhoto(null);
        localStorage.removeItem('profilePhoto');
    }, []);

    /* Déconnexion */
    const handleLogout = useCallback(() => {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        localStorage.removeItem('profilePhoto');

        setUser(null);
        setProfilePhoto(null);
        setShowMenu(false);

        if (onLogout) onLogout();

        navigate('/login', { replace: true });
    }, [onLogout, navigate]);

    if (!user || !role) {
        return (
            <div className="menu-loading">
                Chargement de votre espace...
            </div>
        );
    }

    const fullName = [user.prenom, user.nom]
        .filter(Boolean)
        .join(' ');

    return (
        <div
            className={`menu-container${
                sidebarOpen ? ' navigation-open' : ''
            }`}
        >
                <div className={`app-layout${sidebarHovered ? ' sidebar-hovered' : ''}`}>

                {/* FOND SOMBRE DU MENU MOBILE */}
                {sidebarOpen && (
                    <button
                        type="button"
                        className="sidebar-backdrop"
                        onClick={() => setSidebarOpen(false)}
                        aria-label="Fermer le menu de navigation"
                        tabIndex={0}
                    />
                )}

                {/* BARRE LATÉRALE */}
                <aside
                    className={`sidebar${
                        sidebarOpen ? ' sidebar-open' : ''
                    }${sidebarHovered ? ' sidebar-expanded' : ''}`}
                    onMouseEnter={() => setSidebarHovered(true)}
                    onMouseLeave={() => setSidebarHovered(false)}
                >
                    <div className="sidebar-brand">
                        <div className="sidebar-logo">
                            <img
                                src={spatLogo}
                                alt="Logo SPAT"
                                className="sidebar-logo-image"
                            />
                        </div>

                        <div className="sidebar-brand-text">
                            <strong>SPAT</strong>
                            <span>
                                Approvisionnement électronique
                            </span>
                        </div>
                    </div>

                    <p className="sidebar-section-title">MENU</p>

                    <nav
                        id="main-navigation"
                        className="menu-navigation"
                        aria-label="Menu principal"
                    >
                        {/* Tableau de bord */}
                        {visibleItems
                            .filter((item) => item.path === '/menu')
                            .map((item) => (
                                <NavLink
                                    key={item.path}
                                    to={item.path}
                                    end
                                    title={item.label}
                                    onClick={() => {
                                        setShowMenu(false);
                                        setSidebarOpen(false);
                                    }}
                                    className={({ isActive }) =>
                                        `nav-button${
                                            isActive ? ' active' : ''
                                        }`
                                    }
                                >
                                    <NavigationLabel item={item} />
                                </NavLink>
                            ))}

                        {/* Rapports et statistiques */}
                        {visibleItems
                            .filter(
                                (item) =>
                                    item.path === '/menu/rapports'
                            )
                            .map((item) => (
                                <NavLink
                                    key={item.path}
                                    to={item.path}
                                    title={item.label}
                                    onClick={() => {
                                        setShowMenu(false);
                                        setSidebarOpen(false);
                                    }}
                                    className={({ isActive }) =>
                                        `nav-button${
                                            isActive ? ' active' : ''
                                        }`
                                    }
                                >
                                    <NavigationLabel item={item} />
                                </NavLink>
                            ))}

                        {/* Utilisateurs */}
                        {visibleItems
                            .filter(
                                (item) =>
                                    item.path === '/menu/utilisateurs'
                            )
                            .map((item) => (
                                <NavLink
                                    key={item.path}
                                    to={item.path}
                                    title={item.label}
                                    onClick={() => setSidebarOpen(false)}
                                    className={({ isActive }) =>
                                        `nav-button${
                                            isActive ? ' active' : ''
                                        }`
                                    }
                                >
                                    <NavigationLabel item={item} />
                                </NavLink>
                            ))}

                        {/* Demandes et validations */}
                        {visibleItems
                            .filter(
                                (item) =>
                                    item.path === '/menu/demandes' ||
                                    item.path === '/menu/validations'
                            )
                            .map((item) => (
                                <NavLink
                                    key={item.path}
                                    to={item.path}
                                    title={item.label}
                                    onClick={() => setSidebarOpen(false)}
                                    className={({ isActive }) =>
                                        `nav-button${
                                            isActive ? ' active' : ''
                                        }`
                                    }
                                >
                                    <NavigationLabel item={item} />
                                </NavLink>
                            ))}

                        {/* Fournisseurs et offres */}
                        {visibleItems.some(
                            (item) =>
                                item.path === '/menu/fournisseurs'
                        ) && (
                            <div className="settings-menu">
                                <button
                                    type="button"
                                    title="Fournisseurs et offres"
                                    className={`nav-button settings-toggle${
                                        location.pathname ===
                                            '/menu/fournisseurs' ||
                                        location.pathname === '/menu/offres'
                                            ? ' settings-parent-active'
                                             : ''
                                    }`}
                                    onClick={() =>
                                        setSupplierMenuOpen(
                                            (open) => !open
                                        )
                                    }
                                    aria-expanded={supplierMenuOpen}
                                    aria-controls="supplier-submenu"
                                >
                                    <Truck
                                        className="nav-icon"
                                        aria-hidden="true"
                                    />
                                    <span className="nav-label">
                                        Fournisseurs et offres
                                    </span>

                                    <svg
                                        className={`settings-chevron${
                                            supplierMenuOpen
                                                ? ' is-open'
                                                : ''
                                        }`}
                                        viewBox="0 0 24 24"
                                        aria-hidden="true"
                                    >
                                        <path d="m6 9 6 6 6-6" />
                                    </svg>
                                </button>

                                {supplierMenuOpen && (
                                    <div
                                        className="settings-submenu"
                                        id="supplier-submenu"
                                    >
                                        {visibleItems
                                            .filter(
                                                (item) =>
                                                    item.path ===
                                                        '/menu/fournisseurs' ||
                                                    item.path ===
                                                        '/menu/offres'
                                            )
                                            .map((item) => (
                                                <NavLink
                                                    key={item.path}
                                                    to={item.path}
                                                    title={item.label}
                                                    onClick={() =>
                                                        setSidebarOpen(false)
                                                    }
                                                    className={({
                                                        isActive,
                                                    }) =>
                                                        `nav-button${
                                                            isActive
                                                                ? ' active'
                                                                : ''
                                                        }`
                                                    }
                                                >
                                                    <NavigationLabel
                                                        item={item}
                                                    />
                                                </NavLink>
                                            ))}
                                    </div>
                                )}
                            </div>
                        )}

                        {/* Commandes */}
                        {visibleItems
                            .filter(
                                (item) =>
                                    item.path === '/menu/commandes'
                            )
                            .map((item) => (
                                <NavLink
                                    key={item.path}
                                    to={item.path}
                                    title={item.label}
                                    onClick={() => setSidebarOpen(false)}
                                    className={({ isActive }) =>
                                        `nav-button${
                                            isActive ? ' active' : ''
                                        }`
                                    }
                                >
                                    <NavigationLabel item={item} />
                                </NavLink>
                            ))}

                        {/* Catégories */}
                        {visibleItems
                            .filter(
                                (item) =>
                                    item.path === '/menu/categories'
                            )
                            .map((item) => (
                                <NavLink
                                    key={item.path}
                                    to={item.path}
                                    title={item.label}
                                    onClick={() => setSidebarOpen(false)}
                                    className={({ isActive }) =>
                                        `nav-button${
                                            isActive ? ' active' : ''
                                        }`
                                    }
                                >
                                    <NavigationLabel item={item} />
                                </NavLink>
                            ))}

                        {/* Paramètres */}
                        {[
                            'MANAGER',
                            'SERVICE_ACHATS',
                            'ADMIN',
                        ].includes(role) && (
                            <div className="settings-menu">
                                <button
                                    type="button"
                                    title="Paramètres"
                                    className={`nav-button settings-toggle${
                                        settingsActive
                                            ? ' settings-parent-active'
                                            : ''
                                    }`}
                                    onClick={() =>
                                        setSettingsOpen(
                                            (open) => !open
                                        )
                                    }
                                    aria-expanded={settingsOpen}
                                    aria-controls="settings-submenu"
                                >
                                    <Settings
                                        className="nav-icon"
                                        aria-hidden="true"
                                    />
                                    <span className="nav-label">Paramètres</span>

                                    <svg
                                        className={`settings-chevron${
                                            settingsOpen
                                                ? ' is-open'
                                                : ''
                                        }`}
                                        viewBox="0 0 24 24"
                                        aria-hidden="true"
                                    >
                                        <path d="m6 9 6 6 6-6" />
                                    </svg>
                                </button>

                                {settingsOpen && (
                                    <div
                                        className="settings-submenu"
                                        id="settings-submenu"
                                    >
                                        {settingsItems
                                            .filter((item) =>
                                                item.roles.includes(role)
                                            )
                                            .map((item) => (
                                                <NavLink
                                                    key={item.path}
                                                    to={item.path}
                                                    title={item.label}
                                                    onClick={() => {
                                                        setSettingsOpen(true);
                                                        setSidebarOpen(false);
                                                    }}
                                                    className={() =>
                                                        `nav-button${
                                                            settingsActive &&
                                                            location.hash ===
                                                                `#${item.path.split('#')[1]}`
                                                                ? ' active'
                                                                : ''
                                                        }`
                                                    }
                                                >
                                                    <NavigationLabel
                                                        item={item}
                                                    />
                                                </NavLink>
                                            ))}
                                    </div>
                                )}
                            </div>
                        )}
                    </nav>

                    <div className="sidebar-footer">
                        <span className="sidebar-status-dot" aria-hidden="true" />
                        <span className="sidebar-footer-text">Plateforme active</span>
                    </div>
                </aside>

                {/* ZONE PRINCIPALE */}
                <main className="main-area">
                    {/* En-tête supérieur : bouton du menu mobile uniquement */}
                    <header className="menu-header">
                        <button
                            type="button"
                            className={`sidebar-toggle${sidebarOpen ? ' is-open' : ''}`}
                            onClick={() => setSidebarOpen((open) => !open)}
                            aria-expanded={sidebarOpen}
                            aria-controls="main-navigation"
                            aria-label={
                                sidebarOpen
                                    ? 'Fermer le menu'
                                    : 'Ouvrir le menu'
                            }
                        >
                            {sidebarOpen ? (
                                <svg viewBox="0 0 24 24" aria-hidden="true">
                                    <path d="m6 6 12 12M18 6 6 18" />
                                </svg>
                            ) : (
                                <svg viewBox="0 0 24 24" aria-hidden="true">
                                    <path d="M4 6h16M4 12h16M4 18h16" />
                                </svg>
                            )}
                        </button>
                    </header>

                    {/* EN-TÊTE COMMUN : Titre à gauche, Profil et Date à droite */}
                    <header className="dashboard-hero app-page-hero">
                        <div className="app-page-hero-content">
                            <span className="dashboard-eyebrow">
                                E-PROCUREMENT
                            </span>

                            <h1>{currentPage}</h1>

                            <p>
                                {{
                                    'Tableau de bord':
                                        'Les informations essentielles pour suivre les budgets et les alertes d’achat.',
                                    'Rapports et statistiques':
                                        'Analyse des demandes et des dépenses par période, service, fournisseur et catégorie.',
                                    'Utilisateurs':
                                        'Gestion des utilisateurs, des rôles et des droits d’accès.',
                                    'Demandes d’achat':
                                        'Création, consultation et suivi des demandes d’achat.',
                                    'Validations':
                                        'Examen des demandes et suivi du circuit de validation.',
                                    'Fournisseurs':
                                        'Gestion du référentiel des fournisseurs.',
                                    'Offres':
                                        'Consultation et comparaison des offres fournisseurs.',
                                    'Commandes':
                                        'Suivi des bons de commande et des achats.',
                                    'Catégories':
                                        'Gestion des catégories de produits et de services.',
                                    'Services':
                                        'Configuration des services de l’organisation.',
                                    'Centres de coûts':
                                        'Gestion des centres de coûts et de leur rattachement.',
                                    'Budgets':
                                        'Suivi des budgets alloués, engagés et disponibles.',
                                    'Paramètres':
                                        'Configuration des paramètres de la plateforme.',
                                }[currentPage] ||
                                    'Bienvenue dans votre espace de travail e-Procurement.'}
                            </p>
                        </div>

                        {/* Zone droite : Profil utilisateur positionné au-dessus de la date */}
                        <div className="app-page-hero-side">
                            <div className="profile-info">
                                <div className="profile-avatar">
                                    {profilePhoto ? (
                                        <img
                                            src={profilePhoto}
                                            alt="Photo de profil"
                                            className="profile-photo"
                                        />
                                    ) : (
                                        <span className="profile-initial">
                                            {(
                                                user.prenom ||
                                                user.nom ||
                                                user.matricule ||
                                                'U'
                                            )
                                                .charAt(0)
                                                .toUpperCase()}
                                        </span>
                                    )}

                                    {profilePhoto && (
                                        <button
                                            type="button"
                                            className="delete-photo-btn"
                                            onClick={handleDeletePhoto}
                                            title="Supprimer la photo"
                                            aria-label="Supprimer la photo"
                                        >
                                            ×
                                        </button>
                                    )}
                                </div>

                                <input
                                    ref={fileInputRef}
                                    type="file"
                                    accept="image/*"
                                    onChange={handlePhotoUpload}
                                    className="hidden-file-input"
                                />


                                <div className="profile-details">
                                    <strong className="user-name">
                                        {fullName || user.matricule || 'Utilisateur'}
                                    </strong>
                                    <span className="user-role">{role}</span>
                                </div>

                                <div
                                    className="profile-actions"
                                    ref={profileActionsRef}
                                >
                                    <button
                                        type="button"
                                        className="profile-menu-toggle"
                                        onClick={() => setShowMenu((previous) => !previous)}
                                        aria-expanded={showMenu}
                                        aria-haspopup="menu"
                                        aria-controls="profile-dropdown"
                                        aria-label="Ouvrir le menu du compte"
                                    />

                                {showMenu && (
                                    <div
                                        className="profile-dropdown"
                                        id="profile-dropdown"
                                        role="menu"
                                    >
                                        <button
                                            type="button"
                                            role="menuitem"
                                            onClick={() => {
                                                fileInputRef.current?.click();
                                                setShowMenu(false);
                                            }}
                                        >
                                            <span aria-hidden="true">📷</span>
                                            {profilePhoto
                                                ? 'Modifier la photo de profil'
                                                : 'Ajouter une photo de profil'}
                                        </button>

                                        <button
                                            type="button"
                                            role="menuitem"
                                            onClick={() => {
                                                setShowMenu(false);
                                                navigate('/menu/password');
                                            }}
                                        >
                                            Modifier le mot de passe
                                        </button>

                                        <button
                                            type="button"
                                            role="menuitem"
                                            onClick={() => {
                                                setShowMenu(false);
                                                setShowLogoutModal(true);
                                            }}
                                        >
                                            <LogOut aria-hidden="true" />
                                            Déconnexion
                                        </button>
                                    </div>
                                )}

                                </div>
                            </div>

                            {/* Date actuelle positionnée sous le profil avec une police plus petite */}
                            <div className="dashboard-hero-badge app-page-date">
                                <span className="dashboard-status-dot" />
                                <span>
                                    {new Date().toLocaleDateString('fr-FR', {
                                        day: '2-digit',
                                        month: 'long',
                                        year: 'numeric',
                                    })}
                                </span>
                            </div>
                        </div>
                    </header>

                    {/* CONTENU DES PAGES */}
                    <section className="page-content">
                        <Routes>
                            <Route index element={<Dashboard />} />
                            <Route path="password" element={<PasswordChange />} />

                            <Route
                                path="demandes"
                                element={
                                    <ProtectedPage
                                        role={role}
                                        allowedRoles={pagePermissions.demandes}
                                    >
                                        <Demandes />
                                    </ProtectedPage>
                                }
                            />

                            <Route
                                path="rapports"
                                element={
                                    <ProtectedPage
                                        role={role}
                                        allowedRoles={pagePermissions.rapports}
                                    >
                                        <Rapports />
                                    </ProtectedPage>
                                }
                            />

                            <Route
                                path="commandes"
                                element={
                                    <ProtectedPage
                                        role={role}
                                        allowedRoles={pagePermissions.commandes}
                                    >
                                        <Commandes />
                                    </ProtectedPage>
                                }
                            />

                            <Route
                                path="validations"
                                element={
                                    <ProtectedPage
                                        role={role}
                                        allowedRoles={pagePermissions.validations}
                                    >
                                        <Validations />
                                    </ProtectedPage>
                                }
                            />

                            <Route
                                path="fournisseurs"
                                element={
                                    <ProtectedPage
                                        role={role}
                                        allowedRoles={pagePermissions.fournisseurs}
                                    >
                                        <Fournisseurs />
                                    </ProtectedPage>
                                }
                            />

                            <Route
                                path="offres"
                                element={
                                    <ProtectedPage
                                        role={role}
                                        allowedRoles={pagePermissions.offres}
                                    >
                                        <Offres />
                                    </ProtectedPage>
                                }
                            />

                            <Route
                                path="parametres"
                                element={
                                    <ProtectedPage
                                        role={role}
                                        allowedRoles={pagePermissions.parametres}
                                    >
                                        <Parametres />
                                    </ProtectedPage>
                                }
                            />

                            <Route
                                path="utilisateurs"
                                element={
                                    <ProtectedPage
                                        role={role}
                                        allowedRoles={pagePermissions.utilisateurs}
                                    >
                                        <Utilisateurs />
                                    </ProtectedPage>
                                }
                            />

                            <Route
                                path="categories"
                                element={
                                    <ProtectedPage
                                        role={role}
                                        allowedRoles={pagePermissions.categories}
                                    >
                                        <Categories />
                                    </ProtectedPage>
                                }
                            />

                            <Route
                                path="*"
                                element={<Navigate to="/menu" replace />}
                            />
                        </Routes>
                    </section>
                    {/* MODALE DE CONFIRMATION DE DÉCONNEXION */}
                        {showLogoutModal && (
                            <div
                                className="logout-modal-overlay"
                                onClick={() => setShowLogoutModal(false)}
                            >
                                <div
                                    className="logout-modal"
                                    role="dialog"
                                    aria-modal="true"
                                    aria-labelledby="logout-modal-title"
                                    onClick={(event) => event.stopPropagation()}
                                >
                                    <div className="logout-modal-icon">
                                        <LogOut size={26} />
                                    </div>

                                    <h2 id="logout-modal-title">
                                        Confirmer la déconnexion
                                    </h2>

                                    <p>
                                        Êtes-vous sûr de vouloir vous déconnecter
                                        de votre compte ?
                                    </p>

                                    <div className="logout-modal-actions">
                                        <button
                                            type="button"
                                            className="logout-cancel-btn"
                                            onClick={() => setShowLogoutModal(false)}
                                        >
                                            Annuler
                                        </button>

                                        <button
                                            type="button"
                                            className="logout-confirm-btn"
                                            onClick={handleLogout}
                                        >
                                            <LogOut size={16} />
                                            Oui, me déconnecter
                                        </button>
                                    </div>
                                </div>
                            </div>
                        )}
                </main>
            </div>
        </div>
    );
}
