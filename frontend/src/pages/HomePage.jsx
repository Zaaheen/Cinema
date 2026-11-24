import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { fetchHome } from '../services/homeService'
import { CinemaFooter, CinemaHeader } from '../components/CinemaSiteChrome'
import StitchMovieCard from '../components/StitchMovieCard'
import { heroSlides } from '../lib/homeHeroSlides'

const time = new Intl.DateTimeFormat('vi-VN', {
  hour: '2-digit', minute: '2-digit', timeZone: 'Asia/Ho_Chi_Minh',
})
const dayParts = new Intl.DateTimeFormat('en-GB', {
  day: '2-digit', month: '2-digit', year: 'numeric', timeZone: 'Asia/Ho_Chi_Minh',
})
const weekday = new Intl.DateTimeFormat('vi-VN', { weekday: 'short', timeZone: 'Asia/Ho_Chi_Minh' })
const fullDate = new Intl.DateTimeFormat('vi-VN', { weekday: 'long', day: 'numeric', month: 'numeric', year: 'numeric', timeZone: 'Asia/Ho_Chi_Minh' })
const shortDate = new Intl.DateTimeFormat('vi-VN', { day: 'numeric', month: 'numeric', year: 'numeric', timeZone: 'Asia/Ho_Chi_Minh' })
const hour = new Intl.DateTimeFormat('en-GB', { hour: '2-digit', hourCycle: 'h23', timeZone: 'Asia/Ho_Chi_Minh' })
const dateKey = (value) => {
  const parts = Object.fromEntries(dayParts.formatToParts(value).map((part) => [part.type, part.value]))
  return `${parts.year}-${parts.month}-${parts.day}`
}
const dateValue = (key) => new Date(`${key}T12:00:00+07:00`)
const addDays = (key, count) => { const day = dateValue(key); day.setUTCDate(day.getUTCDate() + count); return dateKey(day) }

const emptyHome = () => ({ banners: [], nowShowing: [], comingSoon: [], showtimes: [], promotions: [] })

export default function HomePage() {
  const [state, setState] = useState({ loading: true, error: '', data: emptyHome() })
  const [reload, setReload] = useState(0)

  useEffect(() => {
    let active = true
    fetchHome()
      .then((data) => { if (active) setState({ loading: false, error: '', data }) })
      .catch((error) => { if (active) setState({ loading: false, error: error.message || 'Không thể tải trang chủ.', data: emptyHome() }) })
    return () => { active = false }
  }, [reload])

  const load = () => {
    setState((current) => ({ ...current, loading: true, error: '' }))
    setReload((value) => value + 1)
  }
  const { data } = state

  return <div className="cinema-public home-page min-h-screen bg-surface text-on-surface">
    <CinemaHeader />
    <main className="pt-20 pb-20 md:pb-0">
      <Hero banners={data.banners} nowShowing={data.nowShowing} />
      <section className="cinema-section">
        <SectionHeading eyebrow="Trên màn ảnh" title="Phim đang chiếu" to="/movies" />
        <MovieStrip movies={data.nowShowing} showtime={data.showtimes?.find((item) => data.nowShowing?.some((movie) => movie.id === item.movieId))} loading={state.loading} empty="Hiện chưa có phim đang chiếu." />
      </section>
      <section className="cinema-section cinema-section--raised" id="showtimes">
        <SectionHeading eyebrow="Một rạp · nhiều trải nghiệm" title="Lịch chiếu tại Cinema" />
        <HomeSchedule items={data.showtimes} movies={[...(data.nowShowing || []), ...(data.comingSoon || [])]} loading={state.loading} />
      </section>
      <section className="cinema-section">
        <SectionHeading eyebrow="Sắp lên màn ảnh" title="Phim sắp chiếu" to="/movies" />
        <MovieStrip movies={data.comingSoon} loading={state.loading} empty="Hiện chưa có phim sắp chiếu." />
      </section>
      <section className="cinema-section cinema-section--raised">
        <SectionHeading eyebrow="Dành cho buổi xem của bạn" title="Ưu đãi đang áp dụng" to="/promotions" />
        <PromotionStrip items={data.promotions} loading={state.loading} />
      </section>
      {state.error && <section className="cinema-section"><div className="cinema-state cinema-state--error" role="alert"><h2>Không thể tải một phần nội dung</h2><p>{state.error}</p><button type="button" onClick={load}>Thử lại</button></div></section>}
    </main>
    <CinemaFooter />
  </div>
}

function Hero({ banners, nowShowing }) {
  const slides = heroSlides(banners, nowShowing)
  const [index, setIndex] = useState(0)
  const [paused, setPaused] = useState(false)
  const [userPaused, setUserPaused] = useState(false)
  const count = slides.length
  const activeIndex = count ? index % count : 0
  const slide = slides[activeIndex] || { key: 'empty', title: 'Chưa có phim đang chiếu', description: 'Khám phá các phim sắp chiếu và quay lại khi lịch chiếu được cập nhật.', target: '/movies' }

  useEffect(() => {
    if (count < 2 || paused || userPaused) return undefined
    const timer = window.setInterval(() => {
      if (!document.hidden && !window.matchMedia('(prefers-reduced-motion: reduce)').matches) setIndex((current) => (current + 1) % count)
    }, 6500)
    return () => window.clearInterval(timer)
  }, [count, paused, userPaused])

  const meta = [slide.movie?.genre, slide.movie?.durationMinutes > 0 ? `${slide.movie.durationMinutes} phút` : null, slide.movie?.version].filter(Boolean)
  const label = count ? 'Đang chiếu' : 'Cinema'
  return <section className="home-hero" aria-label="Phim nổi bật" aria-roledescription="carousel" onMouseEnter={() => setPaused(true)} onMouseLeave={() => setPaused(false)} onFocusCapture={() => setPaused(true)} onBlurCapture={(event) => { if (!event.currentTarget.contains(event.relatedTarget)) setPaused(false) }}>
    <HeroVisual key={slide.key} slide={slide} />
    <div className="home-hero__shade" aria-hidden="true" />
    <div className="home-hero__inner"><div className="home-hero__copy" key={`copy-${slide.key}`}>
      <p className="home-hero__label">Phim nổi bật <span>·</span> {label}</p>
      <h1>{slide.title}</h1>
      {meta.length > 0 && <p className="home-hero__meta">{meta.join(' · ')}</p>}
      <p className="home-hero__description">{slide.description || 'Xem thông tin phim và lịch chiếu tại Cinema.'}</p>
      <div className="home-hero__actions">
        <Link to={slide.target} className="cinema-button cinema-button--primary">Khám phá phim <span aria-hidden="true">→</span></Link>
        <a href="#showtimes" className="home-hero__secondary">Xem lịch chiếu</a>
      </div>
    </div>
      {count > 1 && <div className="home-hero__controls" aria-label="Điều khiển phim nổi bật">
        <span className="home-hero__count">{String(activeIndex + 1).padStart(2, '0')} / {String(count).padStart(2, '0')}</span>
        <div className="home-hero__dots">{slides.map((item, position) => <button key={item.key} type="button" className={position === activeIndex ? 'home-hero__dot home-hero__dot--active' : 'home-hero__dot'} aria-label={`Hiển thị ${item.title}`} aria-current={position === activeIndex ? 'true' : undefined} onClick={() => setIndex(position)} />)}</div>
        <button type="button" className="home-hero__pause" aria-label={userPaused ? 'Tiếp tục tự chuyển phim' : 'Tạm dừng tự chuyển phim'} aria-pressed={userPaused} onClick={() => setUserPaused((value) => !value)}>{userPaused ? <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m8 5 11 7-11 7V5Z" fill="currentColor" /></svg> : <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M7 5h3v14H7zm7 0h3v14h-3z" fill="currentColor" /></svg>}</button>
        <button type="button" className="home-hero__arrow" aria-label="Phim trước" onClick={() => setIndex((current) => (current + count - 1) % count)}>←</button>
        <button type="button" className="home-hero__arrow" aria-label="Phim tiếp theo" onClick={() => setIndex((current) => (current + 1) % count)}>→</button>
      </div>}
    </div>
  </section>
}

function HeroVisual({ slide }) {
  const [failed, setFailed] = useState(false)
  return <div className="home-hero__visual" aria-hidden="true">
    {slide.image && !failed ? <img src={slide.image} alt="" onError={() => setFailed(true)} /> : <div className="home-hero__visual-fallback"><span>CINEMA.VN</span><strong>Trên màn ảnh</strong></div>}
  </div>
}

function SectionHeading({ eyebrow, title, to }) {
  return <div className="cinema-section__heading"><div><p className="cinema-kicker">{eyebrow}</p><h2>{title}</h2></div>{to && <Link className="cinema-text-link" to={to}>Xem tất cả <span aria-hidden="true">→</span></Link>}</div>
}

function MovieStrip({ movies = [], showtime, loading, empty }) {
  if (loading) return <div className="home-movie-grid" aria-label="Đang tải phim">{[0, 1, 2, 3].map((index) => <div key={index} className="cinema-skeleton cinema-skeleton--poster" />)}</div>
  if (!movies.length) return <Empty text={empty} />
  if (movies.length === 1 && showtime?.movieId === movies[0].id) {
    const movie = movies[0]
    return <div className="home-spotlight">
      <div className="home-spotlight__poster"><StitchMovieCard movie={movie} /></div>
      <div className="home-spotlight__copy"><p className="cinema-kicker">Đang chiếu tại Cinema</p><h3>{movie.title}</h3>
        <p>{movie.synopsis || 'Xem thông tin phim và lịch chiếu tại rạp.'}</p>
        <div className="home-spotlight__meta">{[movie.genre, movie.version, movie.durationMinutes > 0 ? `${movie.durationMinutes} phút` : null].filter(Boolean).map((item) => <span key={item}>{item}</span>)}</div>
        <div className="home-spotlight__next"><span>Suất gần nhất</span><strong>{time.format(new Date(showtime.startsAt))}</strong><span>{showtime.roomName}</span></div>
        <Link to={`/movies/${movie.id}`} className="cinema-text-link">Xem phim và lịch chiếu <span aria-hidden="true">→</span></Link>
      </div>
    </div>
  }
  return <div className="home-movie-grid">{movies.map((movie) => <StitchMovieCard key={movie.id} movie={movie} />)}</div>
}

function HomeSchedule({ items = [], movies = [], loading }) {
  const [selectedDate, setSelectedDate] = useState(null)
  const [today] = useState(() => dateKey(new Date()))
  const dates = [...new Set([...Array.from({ length: 7 }, (_, index) => addDays(today, index)), ...items.map((item) => dateKey(new Date(item.startsAt)))])].sort()
  const firstShowtimeDate = items.length ? dateKey(new Date(items.reduce((first, item) => new Date(item.startsAt) < new Date(first.startsAt) ? item : first).startsAt)) : null
  const activeDate = selectedDate && dates.includes(selectedDate) ? selectedDate : firstShowtimeDate || today
  const selectedItems = items.filter((item) => dateKey(new Date(item.startsAt)) === activeDate).sort((a, b) => new Date(a.startsAt) - new Date(b.startsAt))
  const movieById = new Map(movies.map((movie) => [movie.id, movie]))
  const groups = new Map()
  for (const item of selectedItems) {
    if (!groups.has(item.movieId)) groups.set(item.movieId, { movie: movieById.get(item.movieId), title: item.movieTitle, poster: item.moviePosterUrl, rooms: new Map() })
    const rooms = groups.get(item.movieId).rooms
    const roomName = item.roomName || 'Phòng chiếu đang cập nhật'
    if (!rooms.has(roomName)) rooms.set(roomName, [])
    rooms.get(roomName).push(item)
  }
  const roomCount = new Set(items.map((item) => item.roomName).filter(Boolean)).size

  return <div className="home-schedule">
    <div className="home-schedule__venue"><div className="home-schedule__mark" aria-hidden="true">C</div><div><p className="cinema-kicker">Rạp chiếu duy nhất</p><h3>Cinema · TP. Hồ Chí Minh</h3><p>Chọn ngày và giờ chiếu phù hợp với bạn.</p></div><div className="home-schedule__venue-count"><strong>{roomCount}</strong><span>phòng có lịch</span></div></div>
    <div className="home-schedule__dates" role="group" aria-label="Chọn ngày chiếu">{dates.map((date) => {
      const count = items.filter((item) => dateKey(new Date(item.startsAt)) === date).length
      return <button key={date} type="button" aria-pressed={activeDate === date} onClick={() => setSelectedDate(date)} className="home-schedule__date"><span>{date === today ? 'Hôm nay' : date === addDays(today, 1) ? 'Ngày mai' : weekday.format(dateValue(date))}</span><strong>{date.slice(8, 10)}</strong><small>{count ? `${count} suất` : `Tháng ${Number(date.slice(5, 7))}`}</small></button>
    })}</div>
    <div className="home-schedule__day-heading"><h3>{fullDate.format(dateValue(activeDate))}</h3><span>{loading ? 'Đang tải lịch chiếu' : `${selectedItems.length} suất chiếu`}</span></div>
    {loading ? <div className="home-schedule__loading" aria-label="Đang tải lịch chiếu"><div className="cinema-skeleton cinema-skeleton--row" /><div className="cinema-skeleton cinema-skeleton--row" /></div> : groups.size === 0 ? <div className="home-schedule__empty"><h4>Chưa có suất chiếu trong ngày này</h4><p>Chọn ngày khác để xem lịch tại Cinema.</p></div> : <div className="home-schedule__films">{[...groups.entries()].map(([movieId, group]) => <article key={movieId} className="home-schedule__film">
      <SchedulePoster src={group.movie?.posterUrl || group.poster} title={group.title} />
      <div className="home-schedule__film-body"><div className="home-schedule__film-heading"><div>{group.movie?.ageRating && <span className="home-schedule__age">{group.movie.ageRating}</span>}<h4>{group.title}</h4><p>{[group.movie?.genre, group.movie?.durationMinutes > 0 ? `${group.movie.durationMinutes} phút` : null, group.movie?.version || group.rooms.values().next().value?.[0]?.movieVersion].filter(Boolean).join(' · ')}</p></div><Link to={`/movies/${movieId}`}>Thông tin phim <span aria-hidden="true">→</span></Link></div>
        {[...group.rooms.entries()].map(([roomName, showtimes]) => <div className="home-schedule__room" key={roomName}><div className="home-schedule__room-heading"><strong>{roomName}</strong><span>{showtimes.length} suất</span></div><div className="home-schedule__times">{showtimes.map((item) => {
          const start = new Date(item.startsAt)
          const end = item.endsAt ? new Date(item.endsAt) : null
          const period = Number(hour.format(start)) < 12 ? 'Sáng' : Number(hour.format(start)) < 18 ? 'Chiều' : 'Tối'
          return <Link key={item.id} className="home-schedule__time" data-period={period} to={`/booking/seats?showtimeId=${item.id}`} aria-label={`${group.title}, ${roomName}, ${time.format(start)}${end ? ` đến ${time.format(end)}` : ''}, xem sơ đồ ghế`}><span className="home-schedule__period">{period}</span><strong>{time.format(start)}</strong><small>{end ? `Đến ${time.format(end)}` : 'Xem sơ đồ ghế'}</small></Link>
        })}</div></div>)}
      </div>
    </article>)}</div>}
    <p className="home-schedule__note">Chọn giờ chiếu để xem sơ đồ ghế. Đặt vé trực tuyến đang được hoàn thiện.</p>
  </div>
}

function SchedulePoster({ src, title }) {
  const [failed, setFailed] = useState(false)
  return <div className="home-schedule__poster">{src && !failed ? <img src={src} alt="" loading="lazy" onError={() => setFailed(true)} /> : <span aria-hidden="true">CINEMA</span>}<span className="sr-only">{title}</span></div>
}

function PromotionStrip({ items = [], loading }) {
  if (loading) return <div className="home-promotions" aria-label="Đang tải ưu đãi">{[0, 1, 2].map((index) => <div key={index} className="cinema-skeleton cinema-skeleton--row" />)}</div>
  if (!items.length) return <Empty text="Hiện chưa có ưu đãi hiệu lực." />
  return <div className="home-promotions">{items.map((item) => <HomePromotionCard key={item.id} item={item} />)}</div>
}

function HomePromotionCard({ item }) {
  const [imageFailed, setImageFailed] = useState(false)
  const discount = item.discountType === 'PERCENT' ? `${item.discountValue}%` : `${Number(item.discountValue).toLocaleString('vi-VN')} ₫`
  return <Link to={`/promotions/${item.id}`} className="home-promotion"><div className="home-promotion__visual">{item.imageUrl && !imageFailed ? <img src={item.imageUrl} alt="" loading="lazy" onError={() => setImageFailed(true)} /> : <div className="home-promotion__visual-fallback"><span>Ưu đãi tại Cinema</span><strong>Giảm {discount}</strong></div>}</div><div className="home-promotion__body"><span className="home-promotion__eyebrow">Đang áp dụng</span><h3>{item.title}</h3><p>{item.description || 'Xem điều kiện áp dụng của ưu đãi này.'}</p><div className="home-promotion__foot"><span>{item.endsAt ? `Đến ${shortDate.format(new Date(item.endsAt))}` : 'Xem thời hạn áp dụng'}</span><span>Xem chi tiết <span aria-hidden="true">→</span></span></div></div></Link>
}

function Empty({ text }) { return <div className="cinema-state"><p>{text}</p></div> }
