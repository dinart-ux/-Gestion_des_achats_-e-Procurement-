import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import workflowApi from '../service/workflowApi';
import { useToastMessages } from '../components/notifications/ToastContext';
import spatLogo from '../assets/images (1).jpeg';
import './PasswordPages.css';

export default function PasswordChange() {
  const navigate = useNavigate();
  const { setError, setSuccess } = useToastMessages();

  const [form, setForm] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  });
  const [visible, setVisible] = useState({
    current: false,
    new: false,
    confirm: false,
  });
  const [busy, setBusy] = useState(false);

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

  const toggleVisibility = (field) => {
    setVisible((previous) => ({ ...previous, [field]: !previous[field] }));
  };

  const submit = async (event) => {
    event.preventDefault();

    if (form.newPassword !== form.confirmPassword) {
      setError('Les mots de passe ne correspondent pas.');
      return;
    }

    if (passwordStrength !== 5) {
      setError('Le nouveau mot de passe doit contenir au moins 12 caractères, une majuscule, une minuscule, un chiffre et un symbole.');
      return;
    }

    setBusy(true);
    try {
      await workflowApi.post('/auth/password/change', form);

      // Nettoyage local de la session après la modification.
      localStorage.removeItem('token');
      localStorage.removeItem('user');

      setSuccess('Mot de passe modifié. Connectez-vous avec votre nouveau mot de passe.');
      navigate('/login');
    } catch (error) {
      setError(error.response?.data?.message || 'Impossible de modifier le mot de passe.');
    } finally {
      setBusy(false);
    }
  };

  return (
    <main className="login-page password-page">
      <div className="container">
        <section className="card password-card" aria-label="Modification du mot de passe">
          <aside className="visual">
            <div className="visual-content">
              <div className="visual-brand">
                <img
                  src={spatLogo}
                  alt="Logo SPAT"
                  className="visual-logo"
                />
                <h1>Sécurisez<br />votre accès.</h1>
              </div>
              <p>
                Protégez votre espace e-Procurement en choisissant un mot de passe
                personnel et difficile à deviner.
              </p>
              <p className="visual-caption">
                La sécurité de votre compte commence par vous.
              </p>
            </div>
          </aside>

          <section className="form-panel">
            <div className="form-view">
              <div className="header">
                <h2>Modifier mon mot de passe</h2>
                <p>Confirmez votre mot de passe actuel, puis choisissez-en un nouveau.</p>
              </div>

              <form className="login-form password-form" onSubmit={submit}>
                <div className="field-group">
                  <label htmlFor="current-password">Mot de passe actuel</label>
                  <div className="password-input-wrapper">
                    <input
                      id="current-password"
                      type={visible.current ? 'text' : 'password'}
                      autoComplete="current-password"
                      value={form.currentPassword}
                      onChange={(event) => updateField('currentPassword', event.target.value)}
                      required
                    />
                    <button
                      type="button"
                      className="password-toggle"
                      onClick={() => toggleVisibility('current')}
                      aria-label={visible.current ? 'Masquer le mot de passe actuel' : 'Afficher le mot de passe actuel'}
                    >
                      {visible.current ? 'Masquer' : 'Afficher'}
                    </button>
                  </div>
                </div>

                <div className="field-group">
                  <label htmlFor="change-new-password">Nouveau mot de passe</label>
                  <div className="password-input-wrapper">
                    <input
                      id="change-new-password"
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
                      aria-label={visible.new ? 'Masquer le nouveau mot de passe' : 'Afficher le nouveau mot de passe'}
                    >
                      {visible.new ? 'Masquer' : 'Afficher'}
                    </button>
                  </div>
                  <div className="password-strength" aria-label={`Robustesse : ${passwordStrength} sur 5`}>
                    {Array.from({ length: 5 }, (_, index) => (
                      <span key={index} className={index < passwordStrength ? 'active' : ''} />
                    ))}
                  </div>
                  <small className="password-strength-label">
                    Robustesse : {strengthLabel} ({passwordStrength}/5)
                  </small>
                  <small>12 caractères minimum, majuscule, minuscule, chiffre et symbole.</small>
                </div>

                <div className="field-group">
                  <label htmlFor="change-confirm-password">Confirmer le nouveau mot de passe</label>
                  <div className="password-input-wrapper">
                    <input
                      id="change-confirm-password"
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
                      aria-label={visible.confirm ? 'Masquer la confirmation' : 'Afficher la confirmation'}
                    >
                      {visible.confirm ? 'Masquer' : 'Afficher'}
                    </button>
                  </div>
                </div>

                <button className="submit" type="submit" disabled={busy}>
                  {busy ? 'Modification…' : 'Modifier le mot de passe'}
                </button>
              </form>

              <div className="footer">
                <Link to="/menu">Retour à l’espace de travail</Link>
              </div>
            </div>
          </section>
        </section>
      </div>
    </main>
  );
}
