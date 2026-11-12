import { useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import api from '../service/api';
import { clearAuthSession } from '../service/session';
import { useToastMessages } from '../components/notifications/ToastContext';
import './PasswordPages.css';

const generic = 'Si les informations correspondent à un compte actif, vous recevrez un lien de récupération par e-mail.';

export default function PasswordRecovery({ mode }) {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const { setError, setSuccess } = useToastMessages();
  const token = params.get('token') || '';
  const [validating, setValidating] = useState(mode === 'reset');
  const [valid, setValid] = useState(false);
  const [busy, setBusy] = useState(false);
  const [visible, setVisible] = useState(false);
  const [form, setForm] = useState({ matricule: '', email: '', newPassword: '', confirmPassword: '' });
  const passwordStrength = [
    form.newPassword.length >= 12,
    /[A-Z]/.test(form.newPassword),
    /[a-z]/.test(form.newPassword),
    /\d/.test(form.newPassword),
    /[^A-Za-z0-9]/.test(form.newPassword),
  ].filter(Boolean).length;
  const strengthLabel = ['Très faible', 'Faible', 'Faible', 'Moyen', 'Bon', 'Fort'][passwordStrength];

  useEffect(() => {
    if (mode !== 'reset') return;
    api.get('/v2/auth/password/validate', { params: { token } })
      .then(({ data }) => setValid(Boolean(data.valid)))
      .catch(() => setValid(false))
      .finally(() => setValidating(false));
  }, [mode, token]);

  const submitForgot = async (event) => {
    event.preventDefault(); setBusy(true);
    try {
      await api.post('/v2/auth/password/forgot', { matricule: form.matricule, email: form.email });
      setSuccess(generic);
    } catch { setSuccess(generic); }
    finally { setBusy(false); }
  };

  const submitReset = async (event) => {
    event.preventDefault();
    if (form.newPassword !== form.confirmPassword) { setError('Les mots de passe ne correspondent pas.'); return; }
    setBusy(true);
    try {
      await api.post('/v2/auth/password/reset', { token, newPassword: form.newPassword, confirmPassword: form.confirmPassword });
      clearAuthSession();
      setSuccess('Mot de passe modifié. Vous pouvez vous connecter.');
      navigate('/login');
    } catch (error) { setError(error.response?.data?.message || 'Le lien est invalide ou expiré.'); }
    finally { setBusy(false); }
  };

  return <main className="login-page"><div className="container"><section className="card recovery-card"><section className="form-panel"><div className="form-view">
    {mode === 'forgot' ? <>
      <div className="header"><h2>Récupération du mot de passe</h2><p>Indiquez votre matricule et l’adresse e-mail associée à votre compte.</p></div>
      <form className="login-form" onSubmit={submitForgot}>
        <div className="field-group"><label htmlFor="recovery-id">Matricule</label><input id="recovery-id" required value={form.matricule} onChange={e => setForm({...form, matricule:e.target.value})}/></div>
        <div className="field-group"><label htmlFor="recovery-email">Adresse e-mail</label><input id="recovery-email" type="email" autoComplete="email" required value={form.email} onChange={e => setForm({...form, email:e.target.value})}/></div>
        <button className="submit" disabled={busy}>{busy ? 'Envoi…' : 'Envoyer le lien'}</button>
      </form>
    </> : validating ? <p>Vérification du lien…</p> : !valid ? <><div className="header"><h2>Lien indisponible</h2><p>Ce lien est invalide, expiré ou déjà utilisé.</p></div><Link to="/forgot-password">Demander un nouveau lien</Link></> : <>
      <div className="header"><h2>Nouveau mot de passe</h2><p>Choisissez un mot de passe robuste.</p></div>
      <form className="login-form" onSubmit={submitReset}>
        <small>Robustesse du mot de passe : {strengthLabel} ({passwordStrength}/5).</small>
        <div className="field-group"><label htmlFor="new-password">Nouveau mot de passe</label><input id="new-password" type={visible?'text':'password'} minLength="12" required value={form.newPassword} onChange={e=>setForm({...form,newPassword:e.target.value})}/><button type="button" className="link-button" onClick={()=>setVisible(!visible)}>{visible?'Masquer':'Afficher'}</button><small>12 caractères minimum, majuscule, minuscule, chiffre et symbole.</small></div>
        <div className="field-group"><label htmlFor="confirm-password">Confirmation</label><input id="confirm-password" type={visible?'text':'password'} required value={form.confirmPassword} onChange={e=>setForm({...form,confirmPassword:e.target.value})}/></div>
        <button className="submit" disabled={busy}>{busy?'Enregistrement…':'Enregistrer le mot de passe'}</button>
      </form>
    </>}
    <div className="footer"><Link to="/login">Retour à la connexion</Link></div>
  </div></section></section></div></main>;
}
