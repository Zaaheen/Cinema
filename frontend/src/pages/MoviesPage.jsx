import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { fetchMovies } from '../api/catalog'
import { CinemaFooter, CinemaHeader } from '../components/CinemaSiteChrome'
import StitchMovieCard from '../components/StitchMovieCard'

const PAGE_SIZE = 12
const STATUS = { now: 'NOW_SHOWING', soon: 'COMING_SOON' }

export default function MoviesPage() {
  const [activeTab, setActiveTab] = useState('now')
  const [query, setQuery] = useState('')
  const [submittedQuery, setSubmittedQuery] = useState('')
  const [page, setPage] = useState(0)
  const [reload, setReload] = useState(0)
  const [state, setState] = useState({ error: '', requestKey: '', result: emptyResult() })
  const requestKey = `${activeTab}:${page}:${submittedQuery}`
  const loading = state.requestKey !== requestKey

  useEffect(() => {
    const controller = new AbortController()
    fetchMovies({ q: submittedQuery || undefined, status: STATUS[activeTab], page, size: PAGE_SIZE }, { signal: controller.signal })
      .then((result) => setState({ error: '', requestKey, result }))
      .catch((error) => {
        if (error.code === 'ERR_CANCELED' || error.name === 'CanceledError') return
        setState({ error: error.message || 'Không thể tải danh sách phim.', requestKey, result: emptyResult() })
      })
    return () => controller.abort()
  }, [activeTab, page, reload, requestKey, submittedQuery])

  const selectTab = (tab) => { setActiveTab(tab); setPage(0) }
  const submitSearch = (event) => { event.preventDefault(); setPage(0); setSubmittedQuery(query.trim()) }
  const resetFilters = () => { setQuery(''); setSubmittedQuery(''); setActiveTab('now'); setPage(0) }
  const { result } = state
  const tabLabel = activeTab === 'now' ? 'Phim Đang Chiếu' : 'Phim Sắp Chiếu'

  return (
    <div className="min-h-screen bg-surface pb-20 font-body-md text-body-md text-on-surface antialiased md:pb-0">
      <CinemaHeader />
      <main className="min-h-[calc(100dvh-5rem)] pt-20">
        <section className="relative overflow-clip">
          <div aria-hidden="true" className="pointer-events-none absolute -top-32 left-1/2 h-[340px] w-[850px] -translate-x-1/2 rounded-full bg-primary-container/15 blur-[140px]" />
          <div aria-hidden="true" className="pointer-events-none absolute right-[-100px] top-16 h-96 w-96 rounded-full bg-secondary/10 blur-[120px]" />
          <div className="relative z-10 mx-auto max-w-7xl px-margin-mobile pb-8 pt-6 lg:px-margin">
            <nav aria-label="Breadcrumb" className="mb-4 flex items-center gap-2 font-label-md text-outline">
              <Link className="flex items-center gap-1 transition-colors hover:text-primary" to="/"><span className="material-symbols-outlined text-[16px]">home</span>Trang chủ</Link>
              <span aria-hidden="true" className="material-symbols-outlined text-[14px] text-outline-variant">chevron_right</span>
              <span className="font-semibold text-on-surface">Danh sách phim</span>
            </nav>
            <div className="flex flex-col justify-between gap-6 border-b border-surface-container-high/60 pb-6 lg:flex-row lg:items-end">
              <div className="max-w-3xl space-y-3">
                <p className="inline-flex items-center gap-2 rounded-full bg-surface-container px-3 py-1 font-label-sm uppercase tracking-wider text-primary"><span className="h-1.5 w-1.5 animate-pulse rounded-full bg-primary-container" />Dữ liệu từ hệ thống Cinema</p>
                <h1 className="font-display-lg text-display-lg font-extrabold leading-tight tracking-tight text-on-surface">Khám Phá <span className="bg-gradient-to-r from-primary-container via-tertiary-fixed to-secondary bg-clip-text text-transparent">Phim Chiếu Rạp</span></h1>
                <p className="max-w-2xl font-body-md text-on-surface-variant">Danh sách phim và trạng thái công chiếu được cập nhật trực tiếp từ hệ thống Cinema.</p>
              </div>
              <div className="rounded-xl bg-surface-container/90 px-5 py-3.5 text-left shadow-md backdrop-blur-md"><div className="flex items-center gap-2"><span className="material-symbols-outlined text-[20px] text-primary-container">movie_filter</span><span className="font-headline-md font-bold text-on-surface">{loading ? '—' : result.totalElements}</span></div><p className="mt-0.5 font-label-sm uppercase tracking-wider text-outline">{tabLabel.toLowerCase()}</p></div>
            </div>

            <section aria-labelledby="movie-filters" className="mt-8 space-y-5 rounded-2xl bg-surface-container p-5 shadow-xl lg:p-6">
              <h2 id="movie-filters" className="sr-only">Tìm kiếm và lọc phim</h2>
              <form className="grid items-center gap-3 lg:grid-cols-12" onSubmit={submitSearch}>
                <div className="relative lg:col-span-9"><label className="sr-only" htmlFor="movie-search-input">Tìm theo tên phim</label><span aria-hidden="true" className="material-symbols-outlined absolute left-3.5 top-1/2 -translate-y-1/2 text-[20px] text-outline">search</span><input id="movie-search-input" className="w-full rounded-xl bg-surface-container-lowest py-3 pl-11 pr-4 font-body-md text-on-surface placeholder:text-outline focus:outline-none focus:ring-1 focus:ring-primary-container" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Tìm tên phim..." type="search" autoComplete="off" /></div>
                <button className="flex items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-primary-container to-inverse-primary px-5 py-3 font-label-lg font-bold text-on-primary shadow-[0_0_16px_rgba(255,69,162,.4)] transition-opacity hover:opacity-95 lg:col-span-3" type="submit"><span className="material-symbols-outlined text-[20px]">search</span>Tìm kiếm</button>
              </form>
              <div className="flex flex-wrap items-center justify-between gap-4 pt-2"><div className="inline-flex rounded-xl bg-surface-container-lowest p-1.5">{Object.entries(STATUS).map(([tab, status]) => <button key={status} className={`flex items-center gap-2 rounded-lg px-5 py-2 font-label-lg transition-all ${activeTab === tab ? 'bg-primary-container font-bold text-on-primary shadow-[0_0_14px_rgba(255,69,162,.45)]' : 'font-medium text-on-surface-variant hover:text-on-surface'}`} type="button" onClick={() => selectTab(tab)}><span className="material-symbols-outlined text-[18px]">{tab === 'now' ? 'play_circle' : 'calendar_month'}</span>{tab === 'now' ? 'Phim Đang Chiếu' : 'Phim Sắp Chiếu'}</button>)}</div><button className="flex items-center gap-1.5 font-label-md text-outline transition-colors hover:text-primary" type="button" onClick={resetFilters}><span className="material-symbols-outlined text-[18px]">refresh</span>Đặt lại bộ lọc</button></div>
              <div className="flex flex-wrap items-center gap-2 border-t border-surface-container-high pt-3"><span className="font-label-sm text-outline">Đang áp dụng:</span><span className="inline-flex items-center gap-1.5 rounded-full bg-surface-container-high px-2.5 py-1 font-label-sm text-on-surface">{tabLabel} ({loading ? '—' : result.totalElements})</span>{submittedQuery && <span className="inline-flex items-center gap-1.5 rounded-full bg-surface-container-high px-2.5 py-1 font-label-sm text-on-surface">“{submittedQuery}”<button aria-label="Bỏ từ khóa tìm kiếm" className="text-outline hover:text-primary" type="button" onClick={() => { setQuery(''); setSubmittedQuery(''); setPage(0) }}><span className="material-symbols-outlined text-[14px]">close</span></button></span>}<span className="font-label-sm text-outline">Các bộ lọc thể loại, định dạng, độ tuổi và rạp sẽ mở khi API hỗ trợ.</span></div>
            </section>

            <div className="mt-8 flex items-center justify-between gap-4"><h2 className="font-headline-sm font-bold text-on-surface">Danh Sách Phim <span className="text-primary-container">({loading ? '—' : result.totalElements})</span></h2><p className="hidden font-label-sm text-outline sm:block">Chọn thẻ phim để xem chi tiết và lịch chiếu.</p></div>
            {loading ? <MovieSkeletons /> : state.error ? <ErrorState onRetry={() => setReload((value) => value + 1)} /> : result.content.length ? <div className="mt-6 grid grid-cols-2 gap-4 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 lg:gap-6">{result.content.map((movie) => <StitchMovieCard key={movie.id} movie={movie} />)}</div> : <EmptyState />}
            {!loading && !state.error && <Pagination page={page} totalPages={result.totalPages} onChange={setPage} />}
          </div>
        </section>
      </main>
      <CinemaFooter />
    </div>
  )
}

function Pagination({ page, totalPages, onChange }) { if (totalPages <= 1) return null; const pages = Array.from({ length: totalPages }, (_, index) => index); return <nav aria-label="Phân trang danh sách phim" className="mt-10 flex flex-wrap justify-center gap-2"><button aria-label="Trang trước" className="grid h-10 w-10 place-items-center rounded-lg bg-surface-container text-outline disabled:opacity-40" type="button" disabled={page === 0} onClick={() => onChange(page - 1)}>‹</button>{pages.map((index) => <button aria-current={index === page ? 'page' : undefined} className={`grid h-10 min-w-10 place-items-center rounded-lg px-3 ${index === page ? 'bg-primary-container font-bold text-on-primary' : 'bg-surface-container text-on-surface-variant hover:text-on-surface'}`} key={index} type="button" onClick={() => onChange(index)}>{index + 1}</button>)}<button aria-label="Trang sau" className="grid h-10 w-10 place-items-center rounded-lg bg-surface-container text-outline disabled:opacity-40" type="button" disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)}>›</button></nav> }
function MovieSkeletons() { return <div className="mt-6 grid grid-cols-2 gap-4 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 lg:gap-6">{Array.from({ length: 8 }, (_, index) => <div className="aspect-[2/3] animate-pulse rounded-2xl bg-surface-container" key={index} />)}</div> }
function EmptyState() { return <section className="mt-6 rounded-2xl bg-surface-container p-10 text-center"><span className="material-symbols-outlined text-4xl text-primary">movie_off</span><h3 className="mt-3 font-headline-sm font-bold">Chưa có phim phù hợp</h3><p className="mt-2 text-on-surface-variant">Hệ thống chưa có phim công khai khớp với bộ lọc hiện tại.</p></section> }
function ErrorState({ onRetry }) { return <section className="mt-6 rounded-2xl border border-error/40 bg-error-container/20 p-10 text-center"><h3 className="font-headline-sm font-bold text-error">Không thể tải danh sách phim</h3><p className="mt-2 text-on-surface-variant">Hãy kiểm tra kết nối rồi thử lại.</p><button className="mt-4 rounded-xl bg-primary-container px-5 py-2.5 font-label-md font-bold text-on-primary" type="button" onClick={onRetry}>Thử lại</button></section> }
function emptyResult() { return { content: [], page: 0, size: PAGE_SIZE, totalElements: 0, totalPages: 0 } }
