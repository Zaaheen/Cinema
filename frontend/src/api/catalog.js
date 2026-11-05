import { apiClient } from './client'

export async function fetchMovies(params = {}) {
  const { data } = await apiClient.get('/movies', { params })
  return data.data
}

export async function fetchMovie(movieId) {
  const { data } = await apiClient.get(`/movies/${movieId}`)
  return data.data
}

export async function fetchPromotions() {
  const { data } = await apiClient.get('/promotions')
  return data.data
}

export async function fetchTicketPrices() {
  const { data } = await apiClient.get('/ticket-prices')
  return data.data
}
