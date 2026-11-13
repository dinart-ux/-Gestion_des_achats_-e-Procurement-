import { useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import api from '../service/api';
import { clearAuthSession } from '../service/session';
import { useToastMessages } from '../components/notifications/ToastContext';
import spatLogo from '../assets/images (1).jpeg';
import './PasswordPages.css';

const genericMessage =
  'Si les informations correspondent à un compte actif, vous recevrez un lien de récupération par e-mail.';

export default function PasswordRecovery({ mode = 'forgot' }) {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const { setError, setSuccess } = useToastMessages();

  const token = params.get('token') || '';

  const [validating, setValidating] = useState(mode === 'reset');
  const [valid, setValid] = useState(false);
  const [busy, setBusy] = useState(false);
  const [visible, setVisible] = useState({ new: false, confirm: false });
  const [form, setForm] = useState({
    matricule: '',
    email: '',
    newPassword: '',
    confirmPassword: '',
  });

  const passwordStrength = [
    form.newPassword.length >= 12,
    /[A-Z]/.test(form.newPassword),
    /[a-z]/.test(form.newPassword),
    /\d/.test(form.newPassword),
    /[^A-Za-z0-9]/.test(form.newPassword),
  ].filter(Boolean).length;

  const strengthLabel = ['Très faible', 'Faible', 'Faible', 'Moyen', 'Bon', 'Fort'][passwordStrength];

  const updateField = (field, value) => {
    setForm((previous) => ({ ...previous, [field]: value }));
  };

  useEffect(() => {
    if (mode !== 'reset') return;

    let active = true;

    api.get('/v2/auth/password/validate', { params: { token } })
      .then(({ data }) => {
        if (active) setValid(Boolean(data.valid));
      })
      .catch(() => {
        if (active) setValid(false);
      })
      .finally(() => {
        if (active) setValidating(false);
      });

    return () => {
      active = false;
    };
  }, [mode, token]);

  const submitForgot = async (event) => {
    event.preventDefault();
    setBusy(true);

    try {
      await api.post('/v2/auth/password/forgot', {
        matricule: form.matricule.trim(),
        email: form.email.trim(),
      });
      setSuccess(genericMessage);
    } catch {
      // Même réponse en cas d'échec pour ne pas révéler si un compte existe.
      setSuccess(genericMessage);
    } finally {
      setBusy(false);
    }
  };

  const submitReset = async (event) => {
    event.preventDefault();

    if (form.newPassword !== form.confirmPassword) {
      setError('Les mots de passe ne correspondent pas.');
      return;
    }

    if (passwordStrength !== 5) {
      setError('Le mot de passe doit contenir au moins 12 caractères, une majuscule, une minuscule, un chiffre et un symbole.');
      return;
    }

    setBusy(true);
    try {
      await api.post('/v2/auth/password/reset', {
        token,
        newPassword: form.newPassword,
        confirmPassword: form.confirmPassword,
      });

      clearAuthSession();
      setSuccess('Mot de passe modifié. Vous pouvez vous connecter.');
      navigate('/login');
    } catch (error) {
      setError(error.response?.data?.message || 'Le lien est invalide ou expiré.');
    } finally {
      setBusy(false);
    }
  };

  const toggleVisibility = (field) => {
    setVisible((previous) => ({ ...previous, [field]: !previous[field] }));
  };

  return (
    <main className="login-page password-page">
      <div className="container">
        <section className="card password-card" aria-label="Récupération du mot de passe">
          <aside className="visual">
            <div className="visual-content">
              <div className="visual-brand">
                <img src={spatLogo} alt="Logo SPAT" className="visual-logo" />
                <h1>Retrouvez<br />votre accès.</h1>
              </div>
              <p>
                Quelques étapes suffisent pour récupérer l’accès à votre espace
                e-Procurement en toute sécurité.
              </p>
              <p className="visual-caption">
                Votre compte, votre identité, votre sécurité.
              </p>
            </div>
          </aside>

          <section className="form-panel">
            <div className="form-view">
              {mode === 'forgot' ? (
                <>
                  <div className="header">
                    <h2>Récupération du mot de passe</h2>
                    <p>
                      Saisissez votre matricule et l’adresse e-mail associée à votre compte.
                      Les deux informations doivent correspondre aux données enregistrées.
                    </p>
                  </div>

                  <form className="login-form password-form" onSubmit={submitForgot}>
                    <div className="field-group">
                      <label htmlFor="recovery-id">Matricule</label>
                      <input
                        id="recovery-id"
                        type="text"
                        autoComplete="username"
                        placeholder="Entrez votre matricule"
                        value={form.matricule}
                        onChange={(event) => updateField('matricule', event.target.value)}
                        required
                      />
                    </div>

                    <div className="field-group">
                      <label htmlFor="recovery-email">Adresse e-mail</label>
                      <input
                        id="recovery-email"
                        type="email"
                        autoComplete="email"
                        placeholder="exemple@entreprise.com"
                        value={form.email}
                        onChange={(event) => updateField('email', event.target.value)}
                        required
                      />
                    </div>

                    <button className="submit" type="submit" disabled={busy}>
                      {busy ? 'Envoi en cours…' : 'Envoyer le lien de récupération'}
                    </button>
                  </form>
                </>
              ) : validating ? (
                <div className="header">
                  <h2>Vérification du lien</h2>
                  <p>Veuillez patienter pendant la vérification de votre lien de récupération…</p>
                </div>
              ) : !valid ? (
                <>
                  <div className="header">
                    <h2>Lien indisponible</h2>
                    <p>Ce lien est invalide, expiré ou déjà utilisé. Vous pouvez demander un nouveau lien.</p>
                  </div>
                  <Link className="password-inline-link" to="/forgot-password">
                    Demander un nouveau lien
                  </Link>
                </>
              ) : (
                <>
                  <div className="header">
                    <h2>Nouveau mot de passe</h2>
                    <p>Choisissez un mot de passe robuste pour sécuriser votre compte.</p>
                  </div>

                  <form className="login-form password-form" onSubmit={submitReset}>
                    <div className="password-strength-block">
                      <span>Robustesse du mot de passe : <strong>{strengthLabel}</strong> ({passwordStrength}/5)</span>
                      <div className="password-strength" aria-label={`Robustesse : ${passwordStrength} sur 5`}>
                        {Array.from({ length: 5 }, (_, index) => (
                          <span key={index} className={index < passwordStrength ? 'active' : ''} />
                        ))}
                      </div>
                    </div>

                    <div className="field-group">
                      <label htmlFor="new-password">Nouveau mot de passe</label>
                      <div className="password-input-wrapper">
                        <input
                          id="new-password"
                          type={visible.new ? 'text' : 'password'}
                          autoComplete="new-password"
                          minLength={12}
                          value={form.newPassword}
                          onChange={(event) => updateField('newPassword', event.target.value)}
                          required
                        />
                        <button
                          type="button"
                          className="password-toggle"
                          onClick={() => toggleVisibility('new')}
                        >
                          {visible.new ? 'Masquer' : 'Afficher'}
                        </button>
                      </div>
                      <small>12 caractères minimum, majuscule, minuscule, chiffre et symbole.</small>
                    </div>

                    <div className="field-group">
                      <label htmlFor="confirm-password">Confirmer le nouveau mot de passe</label>
                      <div className="password-input-wrapper">
                        <input
                          id="confirm-password"
                          type={visible.confirm ? 'text' : 'password'}
                          autoComplete="new-password"
                          value={form.confirmPassword}
                          onChange={(event) => updateField('confirmPassword', event.target.value)}
                          required
                        />
                        <button
                          type="button"
                          className="password-toggle"
                          onClick={() => toggleVisibility('confirm')}
                        >
                          {visible.confirm ? 'Masquer' : 'Afficher'}
                        </button>
                      </div>
                    </div>

                    <button className="submit" type="submit" disabled={busy}>
                      {busy ? 'Enregistrement…' : 'Enregistrer le mot de passe'}
                    </button>
                  </form>
                </>
              )}

              <div className="footer">
                <Link to="/login">Retour à la connexion</Link>
              </div>
            </div>
          </section>
        </section>
      </div>
    </main>
  );
}
