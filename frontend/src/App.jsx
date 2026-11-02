import {
    BrowserRouter,
    Routes,
    Route,
    Navigate,
    useLocation,
} from 'react-router-dom';

import './App.css';

import Login from './Views/Login';
import Menu from './Views/Menu';

import { getStoredUser } from './service/roleUtils';
import { ToastProvider } from './components/notifications/ToastContext';

function AppRoutes() {
    const location = useLocation();
    const token = localStorage.getItem('token');
    const user = getStoredUser();

    const isLogged = Boolean(token && user && user.role != null);

    return (
        <Routes location={location}>
            <Route
                path="/login"
                element={
                    isLogged
                        ? <Navigate to="/menu" replace />
                        : <Login />
                }
            />

            <Route
                path="/menu/*"
                element={
                    isLogged
                        ? <Menu />
                        : <Navigate to="/login" replace />
                }
            />

            <Route
                path="*"
                element={
                    <Navigate
                        to={isLogged ? '/menu' : '/login'}
                        replace
                    />
                }
            />
        </Routes>
    );
}

export default function App() {
    return (
        <ToastProvider>
            <BrowserRouter>
                <AppRoutes />
            </BrowserRouter>
        </ToastProvider>
    );
}