import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { fetchMovie } from '../api/catalog'
import { getErrorMessage } from '../lib/errors'

export function MovieDetailPage() {
  const { movieId } = useParams()
  const [movie, setMovie] = useState(null)
  const [status, setStatus] = useState('loading')
  const [error, setError] = useState(null)

  useEffect(() => {
    const loadMovie = async () => {
      try {
        setMovie(await fetchMovie(movieId))
        setStatus('ready')
      } catch (loadError) {
        setError(getErrorMessage(loadError))
        setStatus('error')
      }
    }
    loadMovie()
  }, [movieId])

  if (status === 'loading') return <p className="page-status">Đang tải chi tiết phim...</p>
  if (status === 'error') return <p className="form-error">Không thể tải chi tiết phim: {error}</p>

  return (
    <section className="movie-detail">
      <Link className="back-link" to="/movies">← Danh sách phim</Link>
      <div className="movie-detail-hero">
        {movie.posterUrl ? <img src={movie.posterUrl} alt="" /> : <div className="movie-poster-placeholder">Cinema</div>}
        <div>
          <p className="eyebrow">{movie.status === 'NOW_SHOWING' ? 'Đang chiếu' : 'Sắp chiếu'}</p>
          <h1>{movie.title}</h1>
          <p className="hero-copy">{movie.genre || 'Đang cập nhật'} · {movie.durationMinutes} phút · {movie.version}</p>
          <p>{movie.synopsis || 'Nội dung phim đang được cập nhật.'}</p>
        </div>
      </div>
      <dl className="detail-list">
        <div><dt>Đạo diễn</dt><dd>{movie.director || 'Đang cập nhật'}</dd></div>
        <div><dt>Diễn viên</dt><dd>{movie.castText || 'Đang cập nhật'}</dd></div>
        <div><dt>Hãng phim</dt><dd>{movie.studio || 'Đang cập nhật'}</dd></div>
      </dl>
      {movie.trailerUrl ? <a className="button" href={movie.trailerUrl} target="_blank" rel="noreferrer">Xem trailer</a> : null}
    </section>
  )
}
