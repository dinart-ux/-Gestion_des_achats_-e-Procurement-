export function clearAuthSession() {
  localStorage.removeItem('token');
  localStorage.removeItem('user');
}
