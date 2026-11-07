import { apiClient, csrfHeaders } from './client'

export async function loginRequest(username, password) {
  const { data } = await apiClient.post('/auth/login', { username, password })
  return data.data
}

export async function registerRequest(payload) {
  const { data } = await apiClient.post('/auth/register', payload)
  return data.data
}

export async function refreshRequest() {
  const { data } = await apiClient.post('/auth/refresh', null, {
    headers: await csrfHeaders(),
  })
  return data.data
}

export async function logoutRequest() {
  await apiClient.post('/auth/logout', null, { headers: await csrfHeaders() })
}

export async function fetchMe() {
  const { data } = await apiClient.get('/auth/me')
  return data.data
}

export async function updateProfileRequest(payload) {
  const { data } = await apiClient.patch('/members/me', payload)
  return data.data
}

export async function changePasswordRequest(payload) {
  await apiClient.post('/members/me/change-password', payload)
}
