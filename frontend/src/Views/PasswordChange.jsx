import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import workflowApi from '../service/workflowApi';
import { useToastMessages } from '../components/notifications/ToastContext';
import './Login.css';

export default function PasswordChange() {
  const navigate = useNavigate();
  const { setError, setSuccess } = useToastMessages();
  const [form, setForm] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' });
  const [busy, setBusy] = useState(false);

  const submit = async (event) => {
    event.preventDefault();
    if (form.newPassword !== form.confirmPassword) {
      setError('Les mots de passe ne correspondent pas.');
      return;;
    }
    setBusy(true);
    try {
      await workflowApi.post('/auth/password/change', form);
      
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
    <main className="login-page">
      <div className="container">
        <section className="card recovery-card">
          <section className="form-panel">
            <div className="form-view">
              <div className="header">
                <h2>Modifier mon mot de passe</h2>
                <p>Confirmez votre mot de passe actuel puis choisissez-en un nouveau.</p>
              </div>
              <form className="login-form" onSubmit={submit}>
                <div className="field-group">
                  <label htmlFor="current-password">Mot de passe actuel</label>
                  <input id="current-password" type="password" autoComplete="current-password" required
                    value={form.currentPassword}
                    onChange={(event) => setForm({ ...form, currentPassword: event.target.value })} />
                </div>
                <div className="field-group">
                  <label htmlFor="change-new-password">Nouveau mot de passe</label>
                  <input id="change-new-password" type="password" autoComplete="new-password" minLength="12" required
                    value={form.newPassword}
                    onChange={(event) => setForm({ ...form, newPassword: event.target.value })} />
                  <small>12 caractères minimum, majuscule, minuscule, chiffre et symbole.</small>
                </div>
                <div className="field-group">
                  <label htmlFor="change-confirm-password">Confirmation</label>
                  <input id="change-confirm-password" type="password" autoComplete="new-password" required
                    value={form.confirmPassword}
                    onChange={(event) => setForm({ ...form, confirmPassword: event.target.value })} />
                </div>
                <button className="submit" type="submit" disabled={busy}>
                  {busy ? 'Modification…' : 'Modifier le mot de passe'}
                </button>
              </form>
              <div className="footer"><Link to="/menu">Retour à l’espace de travail</Link></div>
            </div>
          </section>
        </section>
      </div>
    </main>
  );
}
