import { Check, CircleSlash, Download, Eye, Filter, Plus, RefreshCw, Search, Trash2, X, Pencil } from 'lucide-react';
import './ActionIconButton.css';

const icons = {
  edit: Pencil,
  delete: Trash2,
  disable: CircleSlash,
  enable: Check,
  refresh: RefreshCw,
  view: Eye,
  create: Plus,
  search: Search,
  filter: Filter,
  validate: Check,
  reject: X,
  download: Download,
  close: X,
  remove: Trash2,
};

const colors = {
  edit: 'action-icon-button--metal',
  delete: 'action-icon-button--danger',
  disable: 'action-icon-button--danger',
  enable: 'action-icon-button--success',
  refresh: 'action-icon-button--metal',
  view: 'action-icon-button--metal',
  create: 'action-icon-button--success',
  search: 'action-icon-button--metal',
  filter: 'action-icon-button--metal',
  validate: 'action-icon-button--success',
  reject: 'action-icon-button--danger',
  download: 'action-icon-button--neutral',
  close: 'action-icon-button--neutral',
  remove: 'action-icon-button--danger',
};

export default function ActionIconButton({ icon, label, onClick, disabled = false, loading = false, className = '', type = 'button' }) {
  const Icon = icons[icon];
  return (
    <button
      type={type}
      className={`action-icon-button ${colors[icon] || ''} ${loading ? 'is-loading' : ''} ${className}`.trim()}
      onClick={onClick}
      disabled={disabled}
      title={label}
      aria-label={label}
      aria-busy={loading || undefined}
    >
      {Icon && <Icon size={18} strokeWidth={1.8} aria-hidden="true" focusable="false" />}
    </button>
  );
}
