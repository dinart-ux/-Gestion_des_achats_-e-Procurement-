import {
  Check,
  CheckCircle2,
  CircleX,
  Download,
  Filter,
  FilePlus2,
  ListPlus,
  MessageSquareText,
  PackageCheck,
  Save,
  Send,
  X,
} from 'lucide-react';
import './ActionIconButton.css';

const icons = {
  create: FilePlus2,
  add: ListPlus,
  save: Save,
  cancel: X,
  submit: Send,
  validate: CheckCircle2,
  reject: CircleX,
  check: Check,
  remove: CircleX,
  receive: PackageCheck,
  info: MessageSquareText,
  filter: Filter,
  download: Download,
};

export default function IconActionButton({
  icon,
  children,
  className = '',
  type = 'button',
  disabled = false,
  onClick,
  ...props
}) {
  const Icon = icons[icon];

  return (
    <button
      {...props}
      type={type}
      className={`icon-action-button ${className}`.trim()}
      disabled={disabled}
      onClick={onClick}
    >
      {Icon && <Icon size={18} strokeWidth={1.8} aria-hidden="true" focusable="false" />}
      <span>{children}</span>
    </button>
  );
}
