import axios from 'axios'
import { env } from '../lib/env'

let accessToken = null
let refreshPromise = null

export const tokenStore = {
  get: () => accessToken,
  set: (token) => {
    accessToken = token
  },
}

export const apiClient = axios.create({
  baseURL: env.apiBaseUrl,
  withCredentials: true,
})

export async function csrfHeaders() {
  const { data } = await apiClient.get('/auth/csrf', { withCredentials: true })
  const token = data.data?.token
  if (!token) {
    throw new Error('CSRF bootstrap token is unavailable')
  }
  return { 'X-XSRF-TOKEN': token }
}

apiClient.interceptors.request.use((config) => {
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`
  }
  return config
})

async function refreshAccessToken() {
  const headers = await csrfHeaders()
  const response = await axios.post(
    `${env.apiBaseUrl}/auth/refresh`,
    null,
    { withCredentials: true, headers },
  )
  const token = response.data.data.accessToken
  tokenStore.set(token)
  return token
}

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const config = error.config
    const status = error.response?.status
    const url = config?.url ?? ''
    const isAuthEndpoint = url.includes('/auth/login') || url.includes('/auth/refresh')

    if (!config || status !== 401 || config._retry || isAuthEndpoint) {
      return Promise.reject(error)
    }

    config._retry = true
    try {
      if (!refreshPromise) {
        refreshPromise = refreshAccessToken().finally(() => {
          refreshPromise = null
        })
      }
      const token = await refreshPromise
      config.headers.Authorization = `Bearer ${token}`
      return apiClient(config)
    } catch (refreshError) {
      tokenStore.set(null)
      window.dispatchEvent(new Event('auth:logout'))
      return Promise.reject(refreshError)
    }
  },
)
