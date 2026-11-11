import { useNavigate } from 'react-router-dom';
import { useState } from 'react';
import './Login.css';
import { loginUser } from '../service/AuthService';
import spatLogo from '../assets/images (1).jpeg';

export default function Login() {
    const navigate = useNavigate();
    const [matricule, setMatricule] = useState('');
    const [password, setPassword] = useState('');
    const [email, setEmail] = useState('');
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const [flipped, setFlipped] = useState(false);


    const Connection = async (e) => {
        e.preventDefault();
        setError('');
        setLoading(true);

        try {
            const data = await loginUser(matricule, password);

            localStorage.setItem('token', data.token || '');
            localStorage.setItem('user', JSON.stringify(data));

            navigate('/menu');
        } catch (error) {
            console.error('Erreur :', error.message);
            setError(
                error.response?.data?.message ||
                error.message ||
                'Connexion impossible.'
            );
        } finally {
            setLoading(false);
        }
    };

    return (
        <main className="login-page">
            <div className="container">
                <section className="card" aria-label="Authentification e-Procurement">
                    {/* Partie illustrée : emplacement prévu pour le logo SPAT */}
                    <aside className="visual">
                        <div className="visual-content">
                            <div className="visual-brand">
                                <img
                                    src={spatLogo}
                                    alt="Logo SPAT — Société du Port à gestion Autonome de Toamasina"
                                    className="visual-logo"
                                />
                                <h1>Bienvenue<br />à vous.</h1>
                            </div>

                            <p>
                                Connectez-vous à votre espace e-Procurement pour accéder
                                aux fonctionnalités de votre compte.
                            </p>
                            <p className="visual-caption">
                                Une gestion des achats simple et efficace.
                            </p>
                        </div>
                    </aside>

                    {/* Partie droite : un seul formulaire est affiché à la fois */}
                    <section className="form-panel">
                        {!flipped ? (
                            <div className="form-view">
                                <div className="header">
                                    <h2>Connexion</h2>
                                    <p>Entrez votre matricule et votre mot de passe pour accéder à votre espace.</p>
                                </div>
                                <div className="login-error-slot" aria-live="polite">

                                {error && (
                                    <div className="login-error" role="alert">
                                        {error}
                                    </div>
                                )}
                                </div>

                                <form className="login-form" onSubmit={Connection}>
                                    <div className="field-group">
                                        <label htmlFor="login-matricule">Matricule</label>
                                        <input
                                            id="login-matricule"
                                            type="text"
                                            placeholder="Entrez votre matricule"
                                            autoComplete="username"
                                            value={matricule}
                                            onChange={(e) => setMatricule(e.target.value)}
                                            required
                                        />
                                    </div>

                                    <div className="field-group">
                                        <label htmlFor="login-password">Mot de passe</label>
                                        <input
                                            id="login-password"
                                            type="password"
                                            placeholder="Entrez votre mot de passe"
                                            autoComplete="current-password"
                                            value={password}
                                            onChange={(e) => setPassword(e.target.value)}
                                            required
                                        />
                                    </div>

                                    <button type="submit" className="submit" disabled={loading}>
                                        {loading ? 'Connexion en cours...' : 'Se connecter'}
                                    </button>
                                </form>

                                <div className="footer">
                                    <span>Mot de passe oublié ?</span>{' '}
                                    <button
                                        type="button"
                                        className="link-button"
                                        onClick={() => {
                                            navigate('/forgot-password');
                                        }}
                                    >
                                        Réinitialiser par e-mail
                                    </button>
                                </div>
                            </div>
                        ) : (
                            <div className="form-view recovery-view">
                                <div className="header">
                                    <h2>Besoin d’aide ?</h2>
                                    <p>
                                        Préparez les informations de votre compte pour faciliter
                                        la récupération de votre accès.
                                    </p>
                                </div>

                                <form
                                    className="login-form"
                                    onSubmit={(e) => {
                                        e.preventDefault();
                                        // Le projet actuel ne possède pas d'API de récupération.
                                        // On évite donc de prétendre qu'une réinitialisation a été envoyée.
                                    }}
                                >
                                    <div className="field-group">
                                        <label htmlFor="recovery-matricule">Matricule</label>
                                        <input
                                            id="recovery-matricule"
                                            type="text"
                                            placeholder="Entrez votre matricule"
                                            autoComplete="username"
                                            value={matricule}
                                            onChange={(e) => setMatricule(e.target.value)}
                                            required
                                        />
                                    </div>

                                    <div className="field-group">
                                        <label htmlFor="recovery-email">Adresse e-mail</label>
                                        <input
                                            id="recovery-email"
                                            type="email"
                                            placeholder="Entrez votre adresse e-mail"
                                            autoComplete="email"
                                            value={email}
                                            onChange={(e) => setEmail(e.target.value)}
                                            required
                                        />
                                    </div>

                                    <p className="recovery-note">
                                        La réinitialisation doit être demandée à l’administrateur
                                        de votre application.
                                    </p>

                                    <button
                                        type="button"
                                        className="submit"
                                        onClick={() => setFlipped(false)}
                                    >
                                        Retour à la connexion
                                    </button>
                                </form>

                                <div className="footer">
                                    <button
                                        type="button"
                                        className="link-button"
                                        onClick={() => setFlipped(false)}
                                    >
                                        Revenir à la connexion
                                    </button>
                                </div>
                            </div>
                        )}
                    </section>
                </section>
            </div>
        </main>
    );
}
