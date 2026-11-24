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
  const tabLabel = activeTab === 'now' ? 'Phim đang chiếu' : 'Phim sắp chiếu'

  return (
    <div className="cinema-public min-h-screen bg-surface pb-20 text-on-surface md:pb-0">
      <CinemaHeader />
      <main className="min-h-[calc(100dvh-5rem)] pt-20">
        <section className="cinema-catalog">
          <div className="cinema-page-inner">
            <nav aria-label="Breadcrumb" className="mb-4 flex items-center gap-2 font-label-md text-outline">
              <Link className="flex items-center gap-1 transition-colors hover:text-primary" to="/"><span className="material-symbols-outlined text-[16px]">home</span>Trang chủ</Link>
              <span aria-hidden="true" className="material-symbols-outlined text-[14px] text-outline-variant">chevron_right</span>
              <span className="font-semibold text-on-surface">Danh sách phim</span>
            </nav>
            <div className="cinema-page-heading">
              <div><p className="cinema-kicker">Phim tại Cinema</p><h1>Chọn phim để xem</h1><p>Xem phim đang chiếu hoặc sắp ra mắt, rồi chọn suất phù hợp với bạn.</p></div>
              <p className="cinema-page-heading__count"><strong>{loading ? '—' : result.totalElements}</strong><span>{tabLabel.toLowerCase()}</span></p>
            </div>

            <section aria-labelledby="movie-filters" className="cinema-filters">
              <h2 id="movie-filters" className="sr-only">Tìm kiếm và lọc phim</h2>
              <form className="grid items-center gap-3 lg:grid-cols-12" onSubmit={submitSearch}>
                <div className="relative lg:col-span-9"><label className="sr-only" htmlFor="movie-search-input">Tìm theo tên phim</label><span aria-hidden="true" className="material-symbols-outlined absolute left-3.5 top-1/2 -translate-y-1/2 text-[20px] text-outline">search</span><input id="movie-search-input" className="cinema-search-input" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Tên phim" type="search" autoComplete="off" /></div>
                <button className="cinema-button cinema-button--primary lg:col-span-3" type="submit">Tìm phim</button>
              </form>
              <div className="cinema-filter-bottom"><div className="cinema-tabs">{Object.entries(STATUS).map(([tab, status]) => <button key={status} aria-pressed={activeTab === tab} type="button" onClick={() => selectTab(tab)}>{tab === 'now' ? 'Đang chiếu' : 'Sắp chiếu'}</button>)}</div><button className="cinema-filter-reset" type="button" onClick={resetFilters}>Đặt lại</button></div>
              {submittedQuery && <p className="cinema-filter-query">Kết quả cho “{submittedQuery}” <button aria-label="Bỏ từ khóa tìm kiếm" type="button" onClick={() => { setQuery(''); setSubmittedQuery(''); setPage(0) }}>×</button></p>}
            </section>

            <div className="cinema-list-heading"><h2>{tabLabel}</h2><p>{loading ? 'Đang tải phim…' : `${result.totalElements} phim`}</p></div>
            {loading ? <MovieSkeletons /> : state.error ? <ErrorState onRetry={() => setReload((value) => value + 1)} /> : result.content.length ? <div className="mt-6 grid grid-cols-2 gap-4 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 lg:gap-6">{result.content.map((movie) => <StitchMovieCard key={movie.id} movie={movie} />)}</div> : <EmptyState />}
            {!loading && !state.error && <Pagination page={page} totalPages={result.totalPages} onChange={setPage} />}
          </div>
        </section>
      </main>
      <CinemaFooter />
    </div>
  )
}

function Pagination({ page, totalPages, onChange }) { if (totalPages <= 1) return null; const pages = Array.from({ length: totalPages }, (_, index) => index); return <nav aria-label="Phân trang danh sách phim" className="mt-10 flex flex-wrap justify-center gap-2"><button aria-label="Trang trước" className="grid h-11 w-11 place-items-center rounded-lg bg-surface-container text-outline disabled:opacity-40" type="button" disabled={page === 0} onClick={() => onChange(page - 1)}>‹</button>{pages.map((index) => <button aria-current={index === page ? 'page' : undefined} className={`grid h-11 min-w-11 place-items-center rounded-lg px-3 ${index === page ? 'bg-primary-container font-bold text-on-primary' : 'bg-surface-container text-on-surface-variant hover:text-on-surface'}`} key={index} type="button" onClick={() => onChange(index)}>{index + 1}</button>)}<button aria-label="Trang sau" className="grid h-11 w-11 place-items-center rounded-lg bg-surface-container text-outline disabled:opacity-40" type="button" disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)}>›</button></nav> }
function MovieSkeletons() { return <div className="mt-6 grid grid-cols-2 gap-4 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 lg:gap-6" aria-label="Đang tải phim">{Array.from({ length: 8 }, (_, index) => <div className="cinema-skeleton aspect-[2/3]" key={index} />)}</div> }
function EmptyState() { return <section className="cinema-state mt-6"><h3>Chưa có phim phù hợp</h3><p>Thử tìm tên khác hoặc chuyển sang phim sắp chiếu.</p></section> }
function ErrorState({ onRetry }) { return <section className="mt-6 rounded-2xl border border-error/40 bg-error-container/20 p-10 text-center"><h3 className="font-headline-sm font-bold text-error">Không thể tải danh sách phim</h3><p className="mt-2 text-on-surface-variant">Hãy kiểm tra kết nối rồi thử lại.</p><button className="mt-4 rounded-xl bg-primary-container px-5 py-2.5 font-label-md font-bold text-on-primary" type="button" onClick={onRetry}>Thử lại</button></section> }
function emptyResult() { return { content: [], page: 0, size: PAGE_SIZE, totalElements: 0, totalPages: 0 } }
