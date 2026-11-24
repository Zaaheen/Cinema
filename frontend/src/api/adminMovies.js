import { apiClient } from './client'

export async function getAdminMovies(params = {}, { signal } = {}) {
  const { data } = await apiClient.get('/admin/movies', { params, signal })
  return data.data
}

export async function getAdminMovie(movieId, { signal } = {}) {
  const { data } = await apiClient.get(`/admin/movies/${movieId}`, { signal })
  return data.data
}

export async function getAdminGenres({ signal } = {}) {
  const { data } = await apiClient.get('/admin/genres', { signal })
  return data.data
}

export async function createAdminMovie(payload) {
  const { data } = await apiClient.post('/admin/movies', payload)
  return data.data
}

export async function updateAdminMovie(movieId, payload) {
  const { data } = await apiClient.patch(`/admin/movies/${movieId}`, payload)
  return data.data
}

export async function uploadAdminMoviePoster(movieId, file) {
  const body = new FormData()
  body.append('file', file)
  const { data } = await apiClient.post(`/admin/movies/${movieId}/poster`, body)
  return data.data
}

export async function uploadAdminMovieBanner(movieId, file) {
  const body = new FormData()
  body.append('file', file)
  const { data } = await apiClient.post(`/admin/movies/${movieId}/banner`, body)
  return data.data
}

export async function updateAdminMovieTrailer(movieId, trailerUrl) {
  const { data } = await apiClient.patch(`/admin/movies/${movieId}/trailer`, { trailerUrl })
  return data.data
}
