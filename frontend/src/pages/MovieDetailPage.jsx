import { useEffect, useRef, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { fetchMovie, fetchShowtimes } from '../api/catalog'
import { useAuth } from '../auth/useAuth'
import { getErrorMessage } from '../lib/errors'

function formatTime(dateTime) {
  return new Intl.DateTimeFormat('vi-VN', {
    timeZone: 'Asia/Ho_Chi_Minh',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(dateTime))
}

function getYoutubeEmbedUrl(value) {
  if (!value?.trim()) return null

  let source = value.trim()
  if (/<iframe\b/i.test(source)) {
    const src = source.match(/\bsrc\s*=\s*(?:"([^"]+)"|'([^']+)'|([^\s>]+))/i)
    source = (src?.[1] ?? src?.[2] ?? src?.[3] ?? '').replaceAll('&amp;', '&')
  }
  if (source.startsWith('//')) source = `https:${source}`

  let url
  try {
    url = new URL(source)
  } catch {
    return null
  }
  if (url.protocol !== 'https:') return null

  const host = url.hostname.toLowerCase()
  const youtubeHosts = new Set([
    'youtube.com',
    'www.youtube.com',
    'm.youtube.com',
    'youtube-nocookie.com',
    'www.youtube-nocookie.com',
  ])
  let videoId = ''

  if (host === 'youtu.be' || host === 'www.youtu.be') {
    videoId = url.pathname.split('/').filter(Boolean)[0] ?? ''
  } else if (youtubeHosts.has(host)) {
    if (url.pathname === '/watch') {
      videoId = url.searchParams.get('v') ?? ''
    } else {
      videoId = url.pathname.match(/^\/(?:embed|shorts|live)\/([^/]+)/)?.[1] ?? ''
    }
  }

  if (!/^[A-Za-z0-9_-]{6,}$/.test(videoId)) return null
  return `https://www.youtube-nocookie.com/embed/${encodeURIComponent(videoId)}?autoplay=1&rel=0`
}

export function MovieDetailPage() {
  const { movieId } = useParams()
  const { user } = useAuth()
  const [movie, setMovie] = useState(null)
  const [status, setStatus] = useState('loading')
  const [error, setError] = useState(null)
  const [selectedDate, setSelectedDate] = useState(() =>
    new Intl.DateTimeFormat('en-CA', {
      timeZone: 'Asia/Ho_Chi_Minh',
    }).format(new Date()),
  )
  const [showtimes, setShowtimes] = useState([])
  const [showtimeStatus, setShowtimeStatus] = useState('loading')
  const [showtimeError, setShowtimeError] = useState(null)
  const [isTrailerOpen, setIsTrailerOpen] = useState(false)
  const trailerButtonRef = useRef(null)
  const trailerCloseButtonRef = useRef(null)
  const trailerEmbedUrl = getYoutubeEmbedUrl(movie?.trailerUrl)

  useEffect(() => {
    let active = true

    async function loadMovie() {
      try {
        const movieData = await fetchMovie(movieId)
        if (!active) return
        setMovie(movieData)
        setStatus('ready')
      } catch (loadError) {
        if (!active) return
        setError(getErrorMessage(loadError))
        setStatus('error')
      }
    }

    loadMovie()
    return () => { active = false }
  }, [movieId])

  useEffect(() => {
    let active = true

    async function loadShowtimes() {
      setShowtimeStatus('loading')
      setShowtimeError(null)
      try {
        const data = await fetchShowtimes(movieId, selectedDate)
        if (!active) return
        setShowtimes(data)
        setShowtimeStatus(data.length > 0 ? 'ready' : 'empty')
      } catch (loadError) {
        if (!active) return
        setShowtimeError(getErrorMessage(loadError))
        setShowtimeStatus('error')
      }
    }

    loadShowtimes()
    return () => { active = false }
  }, [movieId, selectedDate])

  useEffect(() => {
    if (!isTrailerOpen) return undefined

    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    trailerCloseButtonRef.current?.focus()

    function closeOnEscape(event) {
      if (event.key === 'Escape') {
        setIsTrailerOpen(false)
        trailerButtonRef.current?.focus()
      }
    }

    window.addEventListener('keydown', closeOnEscape)
    return () => {
      window.removeEventListener('keydown', closeOnEscape)
      document.body.style.overflow = previousOverflow
    }
  }, [isTrailerOpen])

  function closeTrailer() {
    setIsTrailerOpen(false)
    trailerButtonRef.current?.focus()
  }

  if (status === 'loading') return <p className="page-status">Đang tải chi tiết phim...</p>
  if (status === 'error') {
    return <p className="form-error">Không thể tải chi tiết phim: {error}</p>
  }

  return (
    <section className="movie-detail">
      <Link className="back-link" to="/movies">← Danh sách phim</Link>

      <div className="movie-detail-hero">
        {movie.posterUrl ? (
          <img src={movie.posterUrl} alt={`Poster phim ${movie.title}`} />
        ) : (
          <div className="movie-poster-placeholder">Cinema</div>
        )}
        <div>
          <p className="eyebrow">
            {movie.status === 'NOW_SHOWING' ? 'Đang chiếu' : 'Sắp chiếu'}
          </p>
          <h1>{movie.title}</h1>
          <p className="hero-copy">
            {movie.genre || 'Đang cập nhật'} · {movie.durationMinutes} phút · {movie.version}
          </p>
          <p>{movie.synopsis || 'Nội dung phim đang được cập nhật.'}</p>
        </div>
      </div>

      <dl className="detail-list">
        <div><dt>Đạo diễn</dt><dd>{movie.director || 'Đang cập nhật'}</dd></div>
        <div><dt>Diễn viên</dt><dd>{movie.castText || 'Đang cập nhật'}</dd></div>
        <div><dt>Hãng phim</dt><dd>{movie.studio || 'Đang cập nhật'}</dd></div>
      </dl>

      {movie.trailerUrl ? (
        <button
          className="button"
          type="button"
          aria-haspopup="dialog"
          onClick={() => setIsTrailerOpen(true)}
          ref={trailerButtonRef}
        >
          Xem trailer
        </button>
      ) : null}
      {user?.role === 'ADMIN' ? (
        <Link className="button button-secondary" to={`/admin/movies/${movie.id}/edit`}>
          Chỉnh sửa phim
        </Link>
      ) : null}

      <section className="showtime-section">
        <h2>Lịch chiếu phim</h2>
        <label htmlFor="showtime-date">Chọn ngày xem</label>
        <input
          id="showtime-date"
          type="date"
          value={selectedDate}
          onChange={(event) => setSelectedDate(event.target.value)}
        />

        {showtimeStatus === 'loading' ? (
          <p className="page-status">Đang tải lịch chiếu...</p>
        ) : null}
        {showtimeStatus === 'error' ? (
          <p className="form-error">Không thể tải lịch chiếu: {showtimeError}</p>
        ) : null}
        {showtimeStatus === 'empty' ? (
          <p className="page-status">Không có suất chiếu trong ngày này.</p>
        ) : null}
        {showtimeStatus === 'ready' ? (
          <div className="showtime-list">
            {showtimes.map((showtime) => (
              <article className="showtime-card" key={showtime.id}>
                <h3>{formatTime(showtime.startsAt)}</h3>
                <p>Phòng chiếu: {showtime.roomName}</p>
                <p>Kết thúc: {formatTime(showtime.endsAt)}</p>
              </article>
            ))}
          </div>
        ) : null}
      </section>

      {isTrailerOpen ? (
        <div className="trailer-modal-backdrop" onClick={(event) => {
          if (event.target === event.currentTarget) closeTrailer()
        }}>
          <section
            className="trailer-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="trailer-modal-title"
          >
            <header className="trailer-modal-header">
              <h2 id="trailer-modal-title">Trailer: {movie.title}</h2>
              <button
                className="trailer-modal-close"
                type="button"
                aria-label="Đóng trailer"
                onClick={closeTrailer}
                ref={trailerCloseButtonRef}
              >
                <span aria-hidden="true">×</span>
              </button>
            </header>
            {trailerEmbedUrl ? (
              <div className="trailer-player">
                <iframe
                  src={trailerEmbedUrl}
                  title={`Trailer phim ${movie.title}`}
                  allow="autoplay; encrypted-media; picture-in-picture; web-share"
                  referrerPolicy="strict-origin-when-cross-origin"
                  allowFullScreen
                />
              </div>
            ) : (
              <p className="trailer-unavailable" role="alert">
                Không thể phát trailer này. Hãy nhập URL YouTube hoặc mã nhúng iframe YouTube.
              </p>
            )}
          </section>
        </div>
      ) : null}
    </section>
  )
}
