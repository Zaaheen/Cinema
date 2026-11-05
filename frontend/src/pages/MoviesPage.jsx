import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { fetchMovies } from '../api/catalog'
import { getErrorMessage } from '../lib/errors'

export function MoviesPage() {
  const [query, setQuery] = useState('')
  const [movies, setMovies] = useState([])
  const [status, setStatus] = useState('loading')
  const [error, setError] = useState(null)

  const loadMovies = async (search = '', showLoading = true) => {
    if (showLoading) setStatus('loading')
    setError(null)
    try {
      const page = await fetchMovies({ q: search || undefined, size: 24 })
      setMovies(page.content)
      setStatus(page.content.length ? 'ready' : 'empty')
    } catch (loadError) {
      setError(getErrorMessage(loadError))
      setStatus('error')
    }
  }

  useEffect(() => {
    let active = true
    const loadInitialMovies = async () => {
      try {
        const page = await fetchMovies({ size: 24 })
        if (!active) return
        setMovies(page.content)
        setStatus(page.content.length ? 'ready' : 'empty')
      } catch (loadError) {
        if (!active) return
        setError(getErrorMessage(loadError))
        setStatus('error')
      }
    }
    loadInitialMovies()
    return () => { active = false }
  }, [])

  const submitSearch = (event) => {
    event.preventDefault()
    loadMovies(query.trim())
  }

  return (
    <section className="catalog-page">
      <p className="eyebrow">Phim</p>
      <h1>Phim đang chiếu và sắp chiếu</h1>
      <form className="catalog-search" onSubmit={submitSearch}>
        <label htmlFor="movie-search">Tìm theo tên phim</label>
        <div>
          <input id="movie-search" value={query} onChange={(event) => setQuery(event.target.value)} />
          <button className="button" type="submit">Tìm kiếm</button>
        </div>
      </form>
      {status === 'loading' ? <p className="page-status">Đang tải phim...</p> : null}
      {status === 'error' ? <p className="form-error">Không thể tải phim: {error}</p> : null}
      {status === 'empty' ? <p className="page-status">Chưa có phim phù hợp.</p> : null}
      {status === 'ready' ? (
        <div className="movie-grid">
          {movies.map((movie) => (
            <article className="movie-card" key={movie.id}>
              {movie.posterUrl ? <img src={movie.posterUrl} alt="" /> : <div className="movie-poster-placeholder">Cinema</div>}
              <div>
                <span className="badge">{movie.status === 'NOW_SHOWING' ? 'Đang chiếu' : 'Sắp chiếu'}</span>
                <h2>{movie.title}</h2>
                <p>{movie.genre || 'Đang cập nhật'} · {movie.durationMinutes} phút · {movie.version}</p>
                <Link className="button button-secondary" to={`/movies/${movie.id}`}>Xem chi tiết</Link>
              </div>
            </article>
          ))}
        </div>
      ) : null}
    </section>
  )
}
