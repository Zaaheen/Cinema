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

export async function fetchGenres() {
  const { data } = await apiClient.get('/genres')
  return data.data
}

export async function createMovie(movie) {
  const { data } = await apiClient.post('/admin/movies', movie)
  return data.data
}

export async function updateMovie(movieId, movie) {
  const { data } = await apiClient.patch(`/admin/movies/${movieId}`, movie)
  return data.data
}

export async function uploadMoviePoster(file) {
  const formData = new FormData()
  formData.append('file', file)
  const { data } = await apiClient.post('/admin/movies/poster', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return data.data
}

export async function fetchAdminRooms() {
  const { data } = await apiClient.get('/admin/rooms')
  return data.data
}

export async function fetchAdminShowtimes(date) {
  const { data } = await apiClient.get('/admin/showtimes', { params: { date } })
  return data.data
}

export async function createShowtimes(movieId, showtimes) {
  const { data } = await apiClient.post('/admin/showtimes', { movieId, showtimes })
  return data.data
}

export async function updateShowtime(showtimeId, showtime) {
  const { data } = await apiClient.patch(`/admin/showtimes/${showtimeId}`, showtime)
  return data.data
}

export async function cancelShowtime(showtimeId) {
  await apiClient.delete(`/admin/showtimes/${showtimeId}`)
}

export async function fetchShowtimes(movieId, date) {
  const { data } = await apiClient.get(
      `/movies/${movieId}/showtimes`,
      {
        params: { date },
      },
  )

  return data.data
}
