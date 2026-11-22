import { apiClient } from './client'

export async function getAdminRooms({ signal } = {}) {
  const { data } = await apiClient.get('/admin/rooms', { signal })
  return data.data
}

export async function getAdminShowtimes(date, { signal } = {}) {
  const { data } = await apiClient.get('/admin/showtimes', { params: { date }, signal })
  return data.data
}

export async function createAdminShowtime(payload) {
  const { data } = await apiClient.post('/admin/showtimes', payload)
  return data.data
}

export async function updateAdminShowtime(showtimeId, payload) {
  const { data } = await apiClient.patch(`/admin/showtimes/${showtimeId}`, payload)
  return data.data
}

export async function cancelAdminShowtime(showtimeId) {
  const { data } = await apiClient.post(`/admin/showtimes/${showtimeId}/cancel`)
  return data.data
}
