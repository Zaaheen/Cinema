import axios from 'axios';
// This API client is prepared for a Spring Boot backend, but the Stitch UI is mock-only.
// Set VITE_API_BASE_URL=http://localhost:8080/api/v1 when your backend is ready.
export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1',
  timeout: 15000,
});
// Suggested endpoints (not implemented yet): /movies, /showtimes, /rooms, /bookings, /auth.
