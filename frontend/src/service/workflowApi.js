import axios from 'axios';

const workflowApi = axios.create({
  baseURL: '/api/v2',
  headers: { 'Content-Type': 'application/json' },
});

workflowApi.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

workflowApi.interceptors.response.use(
  (response) => response,
  (error) => {
    const body = error.response?.data;
    if (body && typeof body === 'object' && !body.message) {
      body.message = body.error || body.detail || body.title || `Erreur HTTP ${error.response.status}`;
    }
    return Promise.reject(error);
  },
);

export default workflowApi;
