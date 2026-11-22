import { api } from './api';

export async function fetchHome() {
  const { data } = await api.get('/home');
  return data.data;
}
