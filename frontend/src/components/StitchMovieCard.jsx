import { useState } from 'react'
import { Link } from 'react-router-dom'

export default function StitchMovieCard({ movie }) {
  const [imageFailed, setImageFailed] = useState(false)
  const hasPoster = movie.posterUrl && !imageFailed

  return <Link to={`/movies/${movie.id}`} aria-label={`Xem chi tiết phim ${movie.title}`} className="cinema-movie-card group block h-full text-on-surface focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-primary-container">
    <article>
      <div className={`cinema-movie-card__poster${hasPoster ? '' : ' cinema-movie-card__poster--empty'}`}>
        {hasPoster
          ? <img src={movie.posterUrl} alt="" loading="lazy" onError={() => setImageFailed(true)} />
          : <div className="cinema-movie-card__fallback"><span>CINEMA.VN</span><strong>{movie.title}</strong><small>{movie.status === 'COMING_SOON' ? 'Sắp chiếu' : 'Đang chiếu'}</small></div>}
        {movie.ageRating && <small className="cinema-movie-card__rating">{movie.ageRating}</small>}
      </div>
      <div className="cinema-movie-card__body">
        <div className="cinema-movie-card__meta"><span>{movie.genre || 'Chưa cập nhật thể loại'}</span><span>{movie.durationMinutes ? `${movie.durationMinutes} phút` : ''}</span></div>
        <h3>{movie.title}</h3>
        {movie.version && <p className="cinema-movie-card__version">{movie.version}</p>}
        <span className="cinema-movie-card__link">Xem phim <span aria-hidden="true">→</span></span>
      </div>
    </article>
  </Link>
}
