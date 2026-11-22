import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { fetchMovie, fetchMovies, fetchShowtimes } from '../api/catalog'
import { CinemaFooter, CinemaHeader } from '../components/CinemaSiteChrome'
import { getErrorMessage } from '../lib/errors'
import { loadYouTubeIframeApi, trailerEmbedErrorMessage, trailerEmbedUrl, trailerVideoId, trailerWatchUrl } from '../lib/trailerPlayer'

const zone = 'Asia/Ho_Chi_Minh'
const isoDay = new Intl.DateTimeFormat('en-CA', { timeZone: zone, year: 'numeric', month: '2-digit', day: '2-digit' })
const dayText = new Intl.DateTimeFormat('vi-VN', { timeZone: zone, weekday: 'short', day: '2-digit', month: '2-digit' })
const clock = new Intl.DateTimeFormat('vi-VN', { timeZone: zone, hour: '2-digit', minute: '2-digit', hour12: false })
const released = new Intl.DateTimeFormat('vi-VN', { timeZone: zone, dateStyle: 'medium' })
const cinemaDay = (offset) => { const d = new Date(`${isoDay.format(new Date())}T12:00:00+07:00`); d.setUTCDate(d.getUTCDate() + offset); return isoDay.format(d) }
export default function MovieDetailPage() {
  const { movieId } = useParams()
  const [movieRequest, setMovieRequest] = useState(null)
  const [date, setDate] = useState(() => cinemaDay(0)); const [showtimeRequest, setShowtimeRequest] = useState(null); const [related, setRelated] = useState([])
  const dates = useMemo(() => Array.from({ length: 7 }, (_, i) => cinemaDay(i)), [])
  useEffect(() => { let live = true; fetchMovie(movieId).then((value) => { if (live) setMovieRequest({ movieId, movie: value, error: '' }) }).catch((e) => live && setMovieRequest({ movieId, movie: null, error: getErrorMessage(e) })); return () => { live = false } }, [movieId])
  const loading = movieRequest?.movieId !== movieId
  const movie = loading ? null : movieRequest.movie
  const error = loading ? '' : movieRequest.error
  useEffect(() => { if (!movie) return undefined; let live = true; fetchShowtimes(movie.id, date).then((value) => live && setShowtimeRequest({ movieId: movie.id, date, showtimes: value, error: '' })).catch((e) => live && setShowtimeRequest({ movieId: movie.id, date, showtimes: [], error: getErrorMessage(e) })); return () => { live = false } }, [movie, date])
  useEffect(() => { if (!movie) return undefined; let live = true; fetchMovies({ status: movie.status, page: 0, size: 8 }).then((page) => live && setRelated(page.content.filter((item) => item.id !== movie.id))).catch(() => live && setRelated([])); return () => { live = false } }, [movie])
  if (loading) return <Status text="Đang tải thông tin phim..." />
  if (error || !movie) return <Status error text={error || 'Không tìm thấy phim.'} />
  const showtimeLoading = showtimeRequest?.movieId !== movie.id || showtimeRequest?.date !== date
  const showtimeError = showtimeLoading ? '' : showtimeRequest.error
  const showtimes = showtimeLoading ? [] : showtimeRequest.showtimes
  const showtimeState = { loading: showtimeLoading, error: showtimeError }
  const rooms = Object.entries(showtimes.reduce((all, showtime) => ({ ...all, [showtime.roomName]: [...(all[showtime.roomName] ?? []), showtime] }), {}))
  return <div className="min-h-screen bg-surface text-on-surface"><CinemaHeader /><main className="pb-20 pt-20 md:pb-0">
    <section className="relative border-b border-surface-container-highest bg-surface-container-lowest"><div className="absolute inset-0 h-[290px] bg-cover bg-center opacity-55" style={movie.backdropUrl ? { backgroundImage: `url(${movie.backdropUrl})` } : undefined} /><div className="absolute inset-0 h-[290px] bg-gradient-to-t from-surface-container-lowest via-surface-container-lowest/65 to-surface/20" /><div className="relative mx-auto grid max-w-7xl gap-5 px-4 pb-6 pt-12 sm:px-6 md:grid-cols-[190px_1fr] lg:px-8"><article className="mx-auto w-full max-w-[220px] self-end overflow-hidden rounded-xl border border-surface-container-highest bg-surface-container shadow-xl md:mx-0 md:max-w-none"><div className="aspect-[2/3] bg-surface-container-highest">{movie.posterUrl ? <img src={movie.posterUrl} alt={`Poster ${movie.title}`} className="block h-full w-full object-cover" fetchPriority="high" /> : <div className="grid h-full place-items-center text-outline">Chưa có poster</div>}</div><p className="px-3 py-2 text-xs text-on-surface-variant">{movie.durationMinutes} phút · {movie.version}</p></article><div className="self-end pb-2"><div className="flex flex-wrap gap-1.5"><Pill>{movie.ageRating ?? 'Chưa phân loại'}</Pill><Pill>{movie.genre ?? 'Đang cập nhật thể loại'}</Pill><span className="self-center text-xs text-outline">· {movie.releaseDate ? released.format(new Date(`${movie.releaseDate}T12:00:00+07:00`)) : 'Chưa có ngày phát hành'}</span></div><h1 className="mt-3 font-display-lg text-4xl font-extrabold tracking-tight md:text-5xl">{movie.title}</h1><p className="mt-1 text-sm italic text-on-surface-variant">{movie.status === 'NOW_SHOWING' ? 'Đang chiếu tại Cinema' : 'Sắp ra mắt tại Cinema'}</p><div className="mt-4 flex flex-wrap gap-3 rounded-xl bg-surface-container/85 p-3 text-sm"><span><b className="text-secondary">{movie.durationMinutes}</b> phút</span><span>{movie.version}</span><span>{movie.status === 'NOW_SHOWING' ? 'Đang chiếu' : 'Sắp chiếu'}</span></div><p className="mt-4 max-w-4xl leading-relaxed text-on-surface-variant">{movie.synopsis || 'Nội dung phim đang được cập nhật.'}</p><div className="mt-4 flex flex-wrap gap-2"><a href="#showtimes" className="inline-flex min-h-10 items-center gap-2 rounded-lg bg-primary-container px-4 py-2 font-bold text-on-primary hover:bg-tertiary-container"><span className="material-symbols-outlined">confirmation_number</span>Xem lịch chiếu</a>{trailerVideoId(movie.trailerUrl) ? <TrailerPlayer title={movie.title} videoId={trailerVideoId(movie.trailerUrl)} /> : <span className="inline-flex min-h-10 items-center rounded-lg bg-surface-container-high px-4 py-2 text-sm text-outline">Trailer đang cập nhật</span>}</div></div></div></section>
    <section className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8"><div className="rounded-xl border border-surface-container-highest bg-surface-container-low p-4"><p className="font-label-sm uppercase tracking-wide text-secondary">Ưu đãi phim</p><p className="mt-1 font-semibold">Ưu đãi theo phim chưa có dữ liệu hợp lệ để công bố.</p></div></section>
    <section id="showtimes" className="mx-auto max-w-7xl scroll-mt-24 px-4 py-5 sm:px-6 lg:px-8"><p className="font-label-sm uppercase tracking-wide text-primary">Đặt vé nhanh</p><h2 className="font-headline-xl text-3xl font-bold">Lịch chiếu & Rạp Vé</h2><div className="mt-5 flex gap-2 overflow-x-auto pb-2">{dates.map((value) => <button key={value} type="button" onClick={() => setDate(value)} aria-pressed={date === value} className={`min-w-20 rounded-lg border px-3 py-2 text-center ${date === value ? 'border-primary-container bg-primary-container text-on-primary' : 'border-surface-container-highest bg-surface-container-low text-on-surface-variant'}`}><span className="block text-[10px] uppercase">{value === cinemaDay(0) ? 'Hôm nay' : dayText.format(new Date(`${value}T12:00:00+07:00`))}</span><b>{value.slice(8, 10)}/{value.slice(5, 7)}</b></button>)}</div>{showtimeState.loading ? <p className="mt-5 text-on-surface-variant" role="status">Đang tải suất chiếu...</p> : null}{showtimeState.error ? <p className="mt-5 text-error" role="alert">{showtimeState.error}</p> : null}{!showtimeState.loading && !showtimeState.error && rooms.length === 0 ? <p className="mt-5 rounded-xl bg-surface-container-low p-5 text-on-surface-variant">Chưa có suất chiếu được công bố cho ngày này.</p> : <div className="mt-5 space-y-4">{rooms.map(([room, items]) => <article key={room} className="rounded-xl border border-surface-container-highest bg-surface-container-low p-4"><div className="flex items-center justify-between"><h3 className="font-headline-sm text-lg font-bold">{room}</h3><span className="text-xs text-outline">Suất được API công bố</span></div><div className="mt-3 flex flex-wrap gap-2">{items.map((showtime) => <div key={showtime.id} className="min-w-24 rounded-lg bg-surface-container-lowest p-3"><b className="text-secondary">{clock.format(new Date(showtime.startsAt))}</b><p className="mt-1 text-xs text-outline">Kết thúc {clock.format(new Date(showtime.endsAt))}</p>{Number.isInteger(showtime.id) && showtime.id > 0 ? <Link to={`/booking/seats?showtimeId=${showtime.id}`} className="mt-2 inline-flex text-[11px] font-semibold text-primary hover:underline">Xem sơ đồ ghế</Link> : <p className="mt-2 text-[11px] text-outline">Không có mã suất chiếu hợp lệ</p>}</div>)}</div></article>)}</div>}</section>
    <section className="mx-auto grid max-w-7xl gap-5 px-4 py-8 sm:px-6 lg:grid-cols-2 lg:px-8"><Info title="Đội ngũ sáng tạo">API hiện chỉ cung cấp chuỗi đạo diễn/diễn viên, chưa có hồ sơ và ảnh nhân sự.<div className="mt-3 rounded-lg bg-surface-container-lowest p-3"><b>{movie.director || 'Đạo diễn đang cập nhật'}</b><p className="mt-1 text-sm text-on-surface-variant">{movie.castText || 'Diễn viên đang cập nhật'}</p></div></Info><Info title="Đánh giá từ khán giả">API chưa cung cấp điểm đánh giá hoặc nhận xét đã xác minh.</Info><Info title="Tuyển tập phim">API chưa cung cấp gallery/video chính thức.</Info><Info title="Câu hỏi thường gặp">Thông tin bổ sung cho phim này đang được cập nhật.</Info></section>
    <section className="mx-auto max-w-7xl px-4 pb-12 sm:px-6 lg:px-8"><div className="flex items-end justify-between"><div><p className="font-label-sm uppercase tracking-wide text-secondary">Đang chiếu</p><h2 className="font-headline-xl text-3xl font-bold">Phim đang chiếu nổi bật</h2></div><Link to="/movies" className="text-primary hover:underline">Xem tất cả →</Link></div>{related.length ? <div className="mt-5 grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">{related.map((item) => <Link key={item.id} to={`/movies/${item.id}`} className="overflow-hidden rounded-xl border border-surface-container-highest bg-surface-container-low hover:border-primary"><div className="aspect-[2/3] bg-surface-container-highest">{item.posterUrl ? <img src={item.posterUrl} alt={`Poster ${item.title}`} loading="lazy" className="block h-full w-full object-cover" /> : null}</div><p className="p-3 font-semibold">{item.title}</p></Link>)}</div> : <p className="mt-5 text-on-surface-variant">Chưa có phim liên quan được công bố.</p>}</section>
  </main><CinemaFooter /></div>
}
function Pill({ children }) { return <span className="rounded bg-surface-container-high px-2 py-1 text-[10px] font-bold uppercase tracking-wide text-primary">{children}</span> }
function Info({ title, children }) { return <section className="rounded-xl border border-surface-container-highest bg-surface-container-low p-5"><h2 className="font-headline-sm text-lg font-bold">{title}</h2><div className="mt-3 text-sm text-on-surface-variant">{children}</div></section> }
function Status({ text, error }) { return <div className="min-h-screen bg-surface pt-24 text-on-surface"><CinemaHeader /><main className="mx-auto max-w-5xl px-4 py-10"><Link to="/movies" className="text-primary">← Danh sách phim</Link><p role={error ? 'alert' : 'status'} className={`mt-5 rounded-xl p-5 ${error ? 'bg-error-container text-error' : 'bg-surface-container-low text-on-surface-variant'}`}>{text}</p></main></div> }
function TrailerPlayer({ title, videoId }) {
  const dialogRef = useRef(null)
  const [playing, setPlaying] = useState(false)
  const [embedError, setEmbedError] = useState('')
  const watchUrl = trailerWatchUrl(videoId)

  useEffect(() => {
    if (!playing) return undefined
    let live = true
    let player
    loadYouTubeIframeApi()
      .then((YT) => {
        if (!live || !document.getElementById('cinema-trailer-iframe')) return
        player = new YT.Player('cinema-trailer-iframe', {
          events: {
            onError: (event) => {
              if (live) setEmbedError(trailerEmbedErrorMessage(event.data))
            },
          },
        })
      })
      .catch(() => {
        if (live) setEmbedError('Không thể kiểm tra trình phát YouTube. Nếu video không phát, hãy mở trên YouTube.')
      })
    return () => {
      live = false
      player?.destroy()
    }
  }, [playing])

  function openTrailer() {
    dialogRef.current?.showModal()
    setEmbedError('')
    setPlaying(true)
  }

  function closeTrailer() {
    dialogRef.current?.close()
  }

  return <>
    <button type="button" onClick={openTrailer} className="inline-flex min-h-10 items-center gap-2 rounded-lg bg-surface-container-high px-4 py-2 font-semibold">
      <span className="material-symbols-outlined text-primary" aria-hidden="true">play_circle</span>Xem trailer
    </button>
    <dialog ref={dialogRef} onClose={() => setPlaying(false)} aria-label={`Trailer ${title}`} className="w-[min(92vw,960px)] max-w-none rounded-xl border border-surface-container-highest bg-surface-container-low p-4 text-on-surface backdrop:bg-black/80">
      <div className="mb-3 flex items-center justify-between gap-4"><h2 className="text-lg font-bold">Trailer {title}</h2><button type="button" onClick={closeTrailer} className="rounded-lg border border-surface-container-highest px-3 py-2 focus-visible:outline-2 focus-visible:outline-primary">Đóng</button></div>
      {playing && <iframe id="cinema-trailer-iframe" title={`Trailer ${title}`} src={trailerEmbedUrl(videoId, window.location.origin)} className="aspect-video w-full rounded-lg" allow="accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture; web-share" allowFullScreen referrerPolicy="strict-origin-when-cross-origin" />}
      {embedError ? <p role="alert" className="mt-3 text-sm text-error">{embedError}</p> : <p className="mt-3 text-sm text-on-surface-variant">Nếu YouTube báo video không khả dụng, hãy xem trực tiếp trên YouTube.</p>}
      <a href={watchUrl} target="_blank" rel="noopener noreferrer" className="mt-2 inline-block text-primary underline">Xem trên YouTube</a>
    </dialog>
  </>
}
