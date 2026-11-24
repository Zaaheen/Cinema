import { useEffect, useMemo, useRef, useState } from 'react'
import { InformationCircleIcon, TicketIcon } from '@heroicons/react/24/outline'
import { Link, useParams } from 'react-router-dom'
import { fetchMovie, fetchMovies, fetchShowtimes } from '../api/catalog'
import { CinemaFooter, CinemaHeader } from '../components/CinemaSiteChrome'
import StitchMovieCard from '../components/StitchMovieCard'
import { getErrorMessage } from '../lib/errors'
import { loadYouTubeIframeApi, trailerEmbedErrorMessage, trailerEmbedUrl, trailerThumbnailUrl, trailerVideoId, trailerWatchUrl } from '../lib/trailerPlayer'

const zone = 'Asia/Ho_Chi_Minh'
const isoDay = new Intl.DateTimeFormat('en-CA', { timeZone: zone, year: 'numeric', month: '2-digit', day: '2-digit' })
const weekday = new Intl.DateTimeFormat('vi-VN', { timeZone: zone, weekday: 'short' })
const fullDate = new Intl.DateTimeFormat('vi-VN', { timeZone: zone, weekday: 'long', day: 'numeric', month: 'numeric', year: 'numeric' })
const releaseDate = new Intl.DateTimeFormat('vi-VN', { timeZone: zone, day: 'numeric', month: 'numeric', year: 'numeric' })
const clock = new Intl.DateTimeFormat('vi-VN', { timeZone: zone, hour: '2-digit', minute: '2-digit', hour12: false })
const atNoon = (day) => new Date(`${day}T12:00:00+07:00`)
const cinemaDay = (offset = 0) => { const day = atNoon(isoDay.format(new Date())); day.setUTCDate(day.getUTCDate() + offset); return isoDay.format(day) }
const validDate = (value) => value && !Number.isNaN(new Date(value).getTime())

export default function MovieDetailPage() {
  const { movieId } = useParams()
  const [movieRequest, setMovieRequest] = useState(null)
  const [movieRetry, setMovieRetry] = useState(0)
  const [date, setDate] = useState(() => cinemaDay())
  const [showtimeRequest, setShowtimeRequest] = useState(null)
  const [showtimeRetry, setShowtimeRetry] = useState(0)
  const [relatedRequest, setRelatedRequest] = useState(null)
  const dates = useMemo(() => Array.from({ length: 7 }, (_, index) => cinemaDay(index)), [])

  useEffect(() => {
    let live = true
    fetchMovie(movieId)
      .then((movie) => { if (live) setMovieRequest({ movieId, movieRetry, movie, error: '' }) })
      .catch((error) => { if (live) setMovieRequest({ movieId, movieRetry, movie: null, error: getErrorMessage(error), notFound: error.response?.status === 404 }) })
    return () => { live = false }
  }, [movieId, movieRetry])
  const movieLoading = movieRequest?.movieId !== movieId || movieRequest?.movieRetry !== movieRetry
  const movie = movieLoading ? null : movieRequest.movie
  const movieError = movieLoading ? '' : movieRequest.error

  useEffect(() => {
    if (!movie) return undefined
    let live = true
    fetchShowtimes(movie.id, date)
      .then((showtimes) => { if (live) setShowtimeRequest({ movieId: movie.id, date, showtimeRetry, showtimes: Array.isArray(showtimes) ? showtimes : [], error: '' }) })
      .catch((error) => { if (live) setShowtimeRequest({ movieId: movie.id, date, showtimeRetry, showtimes: [], error: getErrorMessage(error) }) })
    return () => { live = false }
  }, [movie, date, showtimeRetry])
  useEffect(() => {
    if (!movie || !['NOW_SHOWING', 'COMING_SOON'].includes(movie.status)) return undefined
    let live = true
    fetchMovies({ status: movie.status, page: 0, size: 8 })
      .then((page) => { if (live) setRelatedRequest({ movieId: movie.id, movies: (page?.content ?? []).filter((item) => item.id !== movie.id).slice(0, 4) }) })
      .catch(() => { if (live) setRelatedRequest({ movieId: movie.id, movies: [] }) })
    return () => { live = false }
  }, [movie])

  const showtimeLoading = !movie || showtimeRequest?.movieId !== movie.id || showtimeRequest?.date !== date || showtimeRequest?.showtimeRetry !== showtimeRetry
  const showtimes = showtimeLoading ? [] : showtimeRequest.showtimes
  const showtimeError = showtimeLoading ? '' : showtimeRequest.error
  const related = movie && relatedRequest?.movieId === movie.id ? relatedRequest.movies : []
  return <div className="cinema-public movie-detail-page"><CinemaHeader /><main className="movie-detail-main">
    {movieLoading ? <MovieDetailSkeleton /> : movieError || !movie ? <MovieFailure notFound={movieRequest?.notFound} onRetry={() => setMovieRetry((value) => value + 1)} /> : <>
      <MovieHero key={movie.id} movie={movie} />
      <ShowtimeSection date={date} dates={dates} onDateChange={setDate} onRetry={() => setShowtimeRetry((value) => value + 1)} loading={showtimeLoading} error={showtimeError} showtimes={showtimes} />
      {related.length > 0 && <RelatedMovies movies={related} status={movie.status} />}
      <MovieFacts movie={movie} />
    </>}
  </main><CinemaFooter /></div>
}

function SafeImage({ src, alt, className, loading = 'lazy', fallback }) {
  const [failed, setFailed] = useState(false)
  if (!src || failed) return fallback ?? null
  return <img src={src} alt={alt} className={className} loading={loading} onError={() => setFailed(true)} />
}

function MovieHero({ movie }) {
  const [expanded, setExpanded] = useState(false)
  const [backdropFailed, setBackdropFailed] = useState(false)
  const [thumbnailQuality, setThumbnailQuality] = useState('maxresdefault')
  const [thumbnailFailed, setThumbnailFailed] = useState(false)
  const hasSynopsis = Boolean(movie.synopsis?.trim())
  const longSynopsis = hasSynopsis && movie.synopsis.length > 240
  const trailerId = trailerVideoId(movie.trailerUrl)
  const hasReleaseDate = validDate(movie.releaseDate)
  const metadata = [hasReleaseDate ? movie.releaseDate.slice(0, 4) : null, movie.durationMinutes > 0 ? `${movie.durationMinutes} phút` : null, movie.version].filter(Boolean)
  const heroFacts = [
    ['Khởi chiếu', hasReleaseDate ? releaseDate.format(atNoon(movie.releaseDate)) : null],
    ['Thể loại', movie.genre],
    ['Đạo diễn', movie.director],
  ].filter(([, value]) => value)
  const thumbnailUrl = trailerId && !thumbnailFailed ? trailerThumbnailUrl(trailerId, thumbnailQuality) : null
  const imageUrl = thumbnailUrl || (!backdropFailed ? movie.backdropUrl : null)

  function tryNextImage() {
    if (thumbnailUrl && thumbnailQuality === 'maxresdefault' && !movie.backdropUrl) setThumbnailQuality('hqdefault')
    else if (thumbnailUrl) setThumbnailFailed(true)
    else setBackdropFailed(true)
  }

  return <section className={`movie-hero${imageUrl ? '' : ' movie-hero--no-image'}${trailerId ? '' : ' movie-hero--no-trailer'}`} aria-labelledby="movie-title">
    {imageUrl && <img key={imageUrl} src={imageUrl} alt="" className="movie-hero__backdrop" onError={tryNextImage} onLoad={(event) => { if (thumbnailUrl && thumbnailQuality === 'maxresdefault' && event.currentTarget.naturalWidth < 600) tryNextImage() }} />}
    <div className="movie-hero__blur" aria-hidden="true" />
    <div className="movie-hero__scrim" aria-hidden="true" />
    <div className="movie-detail-container movie-hero__inner"><div className="movie-hero__content">
      <nav className="movie-detail-breadcrumb" aria-label="Đường dẫn"><Link to="/movies">Phim</Link><span aria-hidden="true">/</span><span aria-current="page">Chi tiết phim</span></nav>
      <div className="movie-hero__labels">{movie.ageRating && <span className="movie-hero__age">{movie.ageRating}</span>}{movie.status === 'NOW_SHOWING' && <span>Đang chiếu</span>}{movie.status === 'COMING_SOON' && <span>Sắp chiếu</span>}</div>
      <h1 id="movie-title">{movie.title}</h1>
      {metadata.length > 0 && <p className="movie-hero__meta">{metadata.join(' · ')}</p>}
      <div className="movie-hero__actions">
        <a href="#showtimes" className="movie-detail-primary"><TicketIcon aria-hidden="true" />Xem lịch chiếu</a>
        <a href="#facts-title" className="movie-hero__secondary"><InformationCircleIcon aria-hidden="true" />Thông tin phim</a>
      </div>
      {(movie.posterUrl || hasSynopsis || heroFacts.length > 0) && <div className={`movie-hero__details${movie.posterUrl ? ' movie-hero__details--with-poster' : ''}`}>
        {movie.posterUrl && <div className="movie-hero__poster"><SafeImage key={movie.posterUrl} src={movie.posterUrl} alt={`Poster phim ${movie.title}`} className="movie-hero__poster-image" loading="eager" fallback={<span>Poster đang cập nhật</span>} /></div>}
        <div className="movie-hero__details-copy">
          {hasSynopsis && <div className="movie-hero__synopsis"><h2>Nội dung</h2><p className={!expanded && longSynopsis ? 'movie-hero__synopsis--clamped' : ''}>{movie.synopsis}</p>{longSynopsis && <button type="button" aria-expanded={expanded} onClick={() => setExpanded((value) => !value)}>{expanded ? 'Thu gọn nội dung' : 'Xem thêm nội dung'}</button>}</div>}
          {heroFacts.length > 0 && <dl className="movie-hero__facts">{heroFacts.map(([label, value]) => <div key={label}><dt>{label}</dt><dd>{value}</dd></div>)}</dl>}
        </div>
      </div>}
    </div>{trailerId && <div className="movie-hero__media"><TrailerPlayer key={trailerId} movie={movie} videoId={trailerId} /></div>}</div>
  </section>
}

function ShowtimeSection({ date, dates, onDateChange, onRetry, loading, error, showtimes }) {
  const rooms = useMemo(() => {
    const groups = new Map()
    for (const showtime of showtimes) {
      if (!validDate(showtime.startsAt)) continue
      const room = showtime.roomName?.trim() || 'Phòng chiếu chưa cập nhật'
      if (!groups.has(room)) groups.set(room, [])
      groups.get(room).push(showtime)
    }
    return [...groups.entries()].map(([room, items]) => [room, items.sort((a, b) => new Date(a.startsAt) - new Date(b.startsAt))])
  }, [showtimes])
  const count = rooms.reduce((total, [, items]) => total + items.length, 0)
  return <section id="showtimes" className="movie-showtimes movie-detail-container" aria-labelledby="showtimes-title">
    <div className="movie-detail-section-heading"><div><h2 id="showtimes-title">Lịch chiếu</h2><p>Chọn ngày để xem các suất chiếu tại rạp.</p></div></div>
    <div className="movie-showtimes__dates" role="group" aria-label="Chọn ngày chiếu">{dates.map((value) => <button key={value} type="button" aria-pressed={date === value} onClick={() => onDateChange(value)} className="movie-showtimes__date"><span>{value === dates[0] ? 'Hôm nay' : value === dates[1] ? 'Ngày mai' : weekday.format(atNoon(value))}</span><strong>{value.slice(8, 10)}</strong><small>tháng {Number(value.slice(5, 7))}</small></button>)}</div>
    <div className="movie-showtimes__summary"><h3>{fullDate.format(atNoon(date))}</h3><p role="status" aria-live="polite">{loading ? 'Đang tải lịch chiếu…' : error ? 'Không tải được lịch chiếu.' : count > 0 ? `${count} suất chiếu` : 'Chưa có suất chiếu'}</p></div>
    {loading ? <div className="movie-showtimes__loading" aria-hidden="true"><div className="movie-detail-skeleton movie-detail-skeleton--room" /><div className="movie-detail-skeleton movie-detail-skeleton--room" /></div> : error ? <div className="movie-detail-state movie-detail-state--error" role="alert"><h3>Không tải được lịch chiếu.</h3><p>Vui lòng thử lại sau ít phút.</p><button type="button" onClick={onRetry}>Thử lại</button></div> : count === 0 ? <div className="movie-detail-state"><h3>Không có suất chiếu ngày này.</h3><p>Vui lòng chọn ngày khác.</p></div> : <><div className="movie-showtimes__rooms">{rooms.map(([room, items]) => <article key={room} className="movie-showtimes__room"><header><h4>{room}</h4><span>{items.length} suất</span></header><div className="movie-showtimes__slots">{items.map((showtime) => <ShowtimeSlot key={showtime.id} showtime={showtime} />)}</div></article>)}</div><p className="movie-showtimes__note">Chọn giờ chiếu để xem sơ đồ ghế. Chức năng đặt vé trực tuyến đang được hoàn thiện.</p></>}
  </section>
}

function ShowtimeSlot({ showtime }) {
  const start = clock.format(new Date(showtime.startsAt))
  const end = validDate(showtime.endsAt) ? clock.format(new Date(showtime.endsAt)) : null
  const label = `${start}${end ? ` đến ${end}` : ''}, ${showtime.roomName || 'phòng chưa cập nhật'}, xem sơ đồ ghế`
  const content = <><strong>{start}</strong>{end && <small>Đến {end}</small>}</>
  return Number.isInteger(showtime.id) && showtime.id > 0
    ? <Link className="movie-showtimes__slot" to={`/booking/seats?showtimeId=${showtime.id}`} aria-label={label}>{content}</Link>
    : <span className="movie-showtimes__slot movie-showtimes__slot--disabled" aria-label={`${start}, chưa thể xem sơ đồ ghế`}>{content}</span>
}

function RelatedMovies({ movies, status }) {
  return <section className="movie-detail-related movie-detail-container" aria-labelledby="related-title"><div className="movie-detail-section-heading"><div><h2 id="related-title">{status === 'NOW_SHOWING' ? 'Phim đang chiếu khác' : 'Phim sắp chiếu khác'}</h2></div><Link to="/movies">Xem tất cả <span aria-hidden="true">→</span></Link></div><div className="movie-detail-related__grid">{movies.map((movie) => <StitchMovieCard key={movie.id} movie={movie} />)}</div></section>
}

function MovieFacts({ movie }) {
  const facts = [
    ['Tên phim', movie.title], ['Thể loại', movie.genre], ['Đạo diễn', movie.director], ['Diễn viên', movie.castText], ['Hãng sản xuất', movie.studio],
    ['Thời lượng', movie.durationMinutes > 0 ? `${movie.durationMinutes} phút` : null], ['Ngày khởi chiếu', validDate(movie.releaseDate) ? releaseDate.format(atNoon(movie.releaseDate)) : null], ['Phiên bản', movie.version], ['Phân loại độ tuổi', movie.ageRating],
  ].filter(([, value]) => value)
  if (!facts.length) return null
  return <section className="movie-detail-facts movie-detail-container" aria-labelledby="facts-title"><h2 id="facts-title">Thông tin phim</h2><dl>{facts.map(([label, value]) => <div key={label}><dt>{label}</dt><dd>{value}</dd></div>)}</dl></section>
}

function MovieDetailSkeleton() {
  return <div className="movie-detail-container movie-detail-loading" role="status" aria-label="Đang tải thông tin phim"><div className="movie-detail-skeleton movie-detail-skeleton--poster" /><div className="movie-detail-loading__copy"><div className="movie-detail-skeleton movie-detail-skeleton--title" /><div className="movie-detail-skeleton movie-detail-skeleton--line" /><div className="movie-detail-skeleton movie-detail-skeleton--line" /></div><div className="movie-detail-skeleton movie-detail-skeleton--room" /><span className="sr-only">Đang tải thông tin phim và lịch chiếu…</span></div>
}

function MovieFailure({ notFound, onRetry }) {
  return <div className="movie-detail-container movie-detail-failure" role="alert"><h1>{notFound ? 'Không tìm thấy phim.' : 'Không tải được thông tin phim.'}</h1><p>{notFound ? 'Phim này hiện không có trong danh sách.' : 'Vui lòng kiểm tra kết nối và thử lại.'}</p><div><Link to="/movies">Quay lại danh sách phim</Link>{!notFound && <button type="button" onClick={onRetry}>Thử lại</button>}</div></div>
}

function TrailerPlayer({ movie, videoId }) {
  const dialogRef = useRef(null)
  const triggerRef = useRef(null)
  const [playing, setPlaying] = useState(false)
  const [videoLoading, setVideoLoading] = useState(false)
  const [embedError, setEmbedError] = useState('')
  const watchUrl = trailerWatchUrl(videoId)
  const metadata = [movie.genre, movie.durationMinutes > 0 ? `${movie.durationMinutes} phút` : null].filter(Boolean)
  useEffect(() => {
    if (!playing) return undefined
    let live = true
    let player
    loadYouTubeIframeApi()
      .then((YT) => { if (live && document.getElementById('cinema-trailer-iframe')) player = new YT.Player('cinema-trailer-iframe', { events: { onError: (event) => { if (live) setEmbedError(trailerEmbedErrorMessage(event.data)) } } }) })
      .catch(() => { if (live) setEmbedError('Không thể kiểm tra trình phát YouTube. Hãy mở trailer trên YouTube nếu video không phát.') })
    return () => { live = false; player?.destroy() }
  }, [playing])
  function openTrailer() { setEmbedError(''); setVideoLoading(true); dialogRef.current?.showModal(); setPlaying(true) }
  return <>
    <button ref={triggerRef} type="button" className="movie-hero__play" aria-label={`Xem trailer phim ${movie.title}`} onClick={openTrailer}>
      <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="M8 5.5v13l10-6.5L8 5.5Z" fill="currentColor" /></svg>
    </button>
    <dialog ref={dialogRef} className="movie-trailer" aria-labelledby="movie-trailer-title" onClose={() => { setPlaying(false); triggerRef.current?.focus() }}>
      <h2 id="movie-trailer-title" className="sr-only">Trailer {movie.title}</h2>
      <button type="button" className="movie-trailer__close" onClick={() => dialogRef.current?.close()} aria-label="Đóng trailer">×</button>
      {playing && <div className="movie-trailer__video">
        <iframe id="cinema-trailer-iframe" title={`Trailer ${movie.title}`} src={`${trailerEmbedUrl(videoId, window.location.origin)}&autoplay=1`} onLoad={() => setVideoLoading(false)} allow="autoplay; accelerometer; encrypted-media; gyroscope; picture-in-picture; web-share" allowFullScreen referrerPolicy="strict-origin-when-cross-origin" />
        {videoLoading && <span role="status" className="movie-trailer__loading">Đang tải trailer…</span>}
      </div>}
      <div className={`movie-trailer__details${movie.posterUrl ? ' movie-trailer__details--with-poster' : ''}`}>
        {movie.posterUrl && <div className="movie-trailer__poster"><SafeImage src={movie.posterUrl} alt={`Poster phim ${movie.title}`} className="movie-trailer__poster-image" fallback={<span>Poster đang cập nhật</span>} /></div>}
        <div className="movie-trailer__copy">
          <h3>{movie.title}</h3>
          {metadata.length > 0 && <p className="movie-trailer__meta">{metadata.join(' · ')}</p>}
          {movie.synopsis?.trim() && <p className="movie-trailer__synopsis">{movie.synopsis}</p>}
          {embedError && <p role="alert" className="movie-trailer__error">{embedError}</p>}
          <div className="movie-trailer__actions">
            <a href="#showtimes" className="movie-detail-primary" onClick={() => dialogRef.current?.close()}>Xem lịch chiếu</a>
            <button type="button" onClick={() => dialogRef.current?.close()}>Đóng</button>
            <a href={watchUrl} target="_blank" rel="noopener noreferrer">Xem trên YouTube <span className="sr-only">(mở trong thẻ mới)</span></a>
          </div>
        </div>
      </div>
    </dialog>
  </>
}
