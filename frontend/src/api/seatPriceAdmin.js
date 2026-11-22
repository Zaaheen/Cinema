import { apiClient } from './client'

export async function getSeatPrices(filters = {}) {
  const { data } = await apiClient.get('/admin/seat-prices', { params: filters })
  return data.data
}

export async function createSeatPriceDraft(payload) {
  const { data } = await apiClient.post('/admin/seat-prices', payload)
  return data.data
}

export async function updateSeatPrice(id, payload) {
  const { data } = await apiClient.patch(`/admin/seat-prices/${id}`, payload)
  return data.data
}
