export function getRoleCode(role) {
    const value = String(role ?? '').trim().toUpperCase();

    const roles = {
        '1': 'DEMANDEUR',
        '2': 'MANAGER',
        '3': 'SERVICE_ACHATS',
        '4': 'ADMIN',
        'DEMANDEUR': 'DEMANDEUR',
        'MANAGER': 'MANAGER',
        'SERVICE_ACHATS': 'SERVICE_ACHATS',
        'ADMIN': 'ADMIN',
        'ROLE_DEMANDEUR': 'DEMANDEUR',
        'ROLE_MANAGER': 'MANAGER',
        'ROLE_SERVICE_ACHATS': 'SERVICE_ACHATS',
        'ROLE_ADMIN': 'ADMIN',
    };

    return roles[value] || '';
}

export function getStoredUser() {
    try {
        const user = JSON.parse(localStorage.getItem('user') || 'null');

        if (!user || typeof user !== 'object') {
            return null;
        }

        return user;
    } catch {
        return null;
    }
}