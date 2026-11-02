import api from './api';

export const loginUser = async (matricule, password) => {
  const { data } = await api.post('/auth/login', { matricule, password });
  return data;
};

export const registerUser = async (userData) => {
  const { data } = await api.post('/auth/register', userData);
  return data;
};
