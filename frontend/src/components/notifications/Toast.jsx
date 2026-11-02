import { useEffect } from 'react';

import {
    CheckCircle2,
    AlertCircle,
    X,
} from 'lucide-react';

export default function Toast({
    id,
    message,
    type,
    onClose,
}) {
    const isSuccess = type === 'success';

    useEffect(() => {
        const timer = setTimeout(() => {
            onClose(id);
        }, 4000);

        return () => clearTimeout(timer);
    }, [id, onClose]);

    return (
        <div
            className={`toast toast-${type}`}
            role={isSuccess ? 'status' : 'alert'}
        >
            <div className="toast-icon">
                {isSuccess ? (
                    <CheckCircle2 size={23} />
                ) : (
                    <AlertCircle size={23} />
                )}
            </div>

            <div className="toast-content">
                <strong>
                    {isSuccess ? 'Succès' : 'Erreur'}
                </strong>

                <p>{message}</p>
            </div>

            <button
                type="button"
                className="toast-close"
                onClick={() => onClose(id)}
                aria-label="Fermer la notification"
            >
                <X size={18} />
            </button>
        </div>
    );
}