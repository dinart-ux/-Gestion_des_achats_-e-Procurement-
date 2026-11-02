import {
    createContext,
    useCallback,
    useContext,
    useState,
} from 'react';
import './Toast.css';

import Toast from './Toast';

const ToastContext = createContext(null);

export function ToastProvider({ children }) {
    const [toasts, setToasts] = useState([]);

    const showToast = useCallback((message, type = 'success') => {
        if (!message) return;

        const id = `${Date.now()}-${Math.random()}`;

        setToasts((previous) => [
            ...previous,
            {
                id,
                message: String(message),
                type,
            },
        ]);
    }, []);

    const removeToast = useCallback((id) => {
        setToasts((previous) =>
            previous.filter((toast) => toast.id !== id)
        );
    }, []);

    const value = {
        showToast,
        removeToast,
    };

    return (
        <ToastContext.Provider value={value}>
            {children}

            <div className="toast-container">
                {toasts.map((toast) => (
                    <Toast
                        key={toast.id}
                        {...toast}
                        onClose={removeToast}
                    />
                ))}
            </div>
        </ToastContext.Provider>
    );
}

export function useToast() {
    const context = useContext(ToastContext);

    if (!context) {
        throw new Error(
            'useToast doit être utilisé dans ToastProvider.'
        );
    }

    return context;
}

// Compatibilité avec les anciens setError et setSuccess.
export function useToastMessages() {
    const { showToast } = useToast();

    const setError = useCallback((message) => {
        if (message) showToast(message, 'error');
    }, [showToast]);

    const setSuccess = useCallback((message) => {
        if (message) showToast(message, 'success');
    }, [showToast]);

    return {
        error: '',
        success: '',
        setError,
        setSuccess,
    };
}