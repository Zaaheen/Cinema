import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { getAdminMovies } from '../api/adminMovies'

const PAGE_SIZE = 12
const STATUS_OPTIONS = [
  { value: '', label: 'Tất cả' },
  { value: 'COMING_SOON', label: 'Sắp chiếu' },
  { value: 'NOW_SHOWING', label: 'Đang chiếu' },
  { value: 'STOPPED', label: 'Dừng chiếu' },
]

const dateFormatter = new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium' })

function formatReleaseDate(value) {
  return value ? dateFormatter.format(new Date(`${value}T12:00:00+07:00`)) : 'Chưa cập nhật'
}

function getErrorMessage(error) {
  return error.response?.data?.message || 'Không thể tải danh sách phim. Hãy thử lại.'
}

function isCanceled(error) {
  return error?.code === 'ERR_CANCELED' || error?.name === 'CanceledError'
}

export function AdminMovieListPage() {
  const [query, setQuery] = useState('')
  const [debouncedQuery, setDebouncedQuery] = useState('')
  const [status, setStatus] = useState('')
  const [page, setPage] = useState(0)
  const [retryKey, setRetryKey] = useState(0)
  const [state, setState] = useState({ loading: true, error: '', data: null })
  const latestRequest = useRef(0)

  useEffect(() => {
    const timer = window.setTimeout(() => setDebouncedQuery(query.trim()), 350)
    return () => window.clearTimeout(timer)
  }, [query])

  useEffect(() => {
    const controller = new AbortController()
    const requestId = ++latestRequest.current
    let active = true

    queueMicrotask(() => {
      if (!active) return
      setState((current) => ({ ...current, loading: true, error: '' }))
      getAdminMovies({ q: debouncedQuery || undefined, status: status || undefined, page, size: PAGE_SIZE }, { signal: controller.signal })
        .then((data) => {
          if (!active || requestId !== latestRequest.current) return
          if (data.totalPages > 0 && data.page >= data.totalPages && page > 0) {
            setPage(data.totalPages - 1)
            return
          }
          setState({ loading: false, error: '', data })
        })
        .catch((error) => {
          if (!active || requestId !== latestRequest.current || isCanceled(error)) return
          setState({ loading: false, error: getErrorMessage(error), data: null })
        })
    })

    return () => { active = false; controller.abort() }
  }, [debouncedQuery, page, retryKey, status])

  function changeQuery(event) {
    setQuery(event.target.value)
    setPage(0)
  }

  function changeStatus(event) {
    setStatus(event.target.value)
    setPage(0)
  }

  function submitSearch(event) {
    event.preventDefault()
    const nextQuery = query.trim()
    if (nextQuery === debouncedQuery) setRetryKey((value) => value + 1)
    else setDebouncedQuery(nextQuery)
  }

  const movies = state.data?.content || []
  const totalPages = state.data?.totalPages || 0
  const currentPage = state.data?.page ?? page
  const hasFilters = Boolean(debouncedQuery || status)

  return <section className="admin-movies" aria-labelledby="admin-movie-title">
    <header className="admin-movies__header">
      <div>
        <p className="eyebrow">DANH MỤC PHIM</p>
        <h2 id="admin-movie-title">Quản lý phim</h2>
        <p>Danh sách phim hiển thị trực tiếp từ hệ thống quản trị, bao gồm cả phim đã dừng chiếu.</p>
      </div>
      <Link className="admin-movies__create price-admin__primary" to="/admin/movies/new">Thêm phim</Link>
    </header>

    <search className="admin-movies__filters">
      <form aria-labelledby="admin-movie-filter-title" onSubmit={submitSearch}>
        <h3 id="admin-movie-filter-title" className="visually-hidden">Tìm và lọc phim</h3>
        <label htmlFor="admin-movie-query">Tìm theo tên phim
          <input id="admin-movie-query" name="q" type="search" value={query} onChange={changeQuery}
            placeholder="Nhập tên phim" autoComplete="off" autoCorrect="off" autoCapitalize="none" spellCheck="false" enterKeyHint="search" />
        </label>
        <label htmlFor="admin-movie-status">Trạng thái
          <select id="admin-movie-status" name="status" value={status} onChange={changeStatus}>
            {STATUS_OPTIONS.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
          </select>
        </label>
        <button type="submit">Tìm phim</button>
      </form>
    </search>

    <section className="admin-movies__surface" aria-labelledby="admin-movie-list-title">
      <div className="admin-movies__section-heading">
        <div><h3 id="admin-movie-list-title">Danh sách phim</h3><p>{state.data ? `${state.data.totalElements} phim` : 'Đang đồng bộ dữ liệu'}</p></div>
        {hasFilters ? <p className="admin-movies__filter-summary">Đang áp dụng bộ lọc</p> : null}
      </div>
      {state.error ? <div className="admin-movies__error" role="alert"><p>{state.error}</p><button type="button" onClick={() => setRetryKey((value) => value + 1)}>Thử lại</button></div> : null}
      {state.loading ? <MovieTableSkeleton /> : null}
      {!state.loading && !state.error && movies.length === 0 ? <EmptyState filtered={hasFilters} /> : null}
      {!state.loading && !state.error && movies.length > 0 ? <>
        <div className="admin-movies__table-wrap" tabIndex="0" role="region" aria-labelledby="admin-movie-list-title">
          <table>
            <caption className="visually-hidden">Danh sách phim quản trị</caption>
            <thead><tr><th scope="col">Phim</th><th scope="col">Trạng thái</th><th scope="col">Thể loại</th><th scope="col">Thời lượng</th><th scope="col">Khởi chiếu</th><th scope="col">Thao tác</th></tr></thead>
            <tbody>{movies.map((movie) => <tr key={movie.id}>
              <th scope="row"><div className="admin-movies__movie"><div className="admin-movies__poster">{movie.posterUrl ? <img src={movie.posterUrl} alt="" /> : <span aria-hidden="true">🎬</span>}</div><div><strong>{movie.title}</strong><span>{movie.version || 'Phiên bản đang cập nhật'}</span></div></div></th>
              <td><StatusBadge status={movie.status} /></td>
              <td>{movie.genres?.map((genre) => genre.name).join(', ') || movie.genre || 'Chưa cập nhật'}</td>
              <td>{movie.durationMinutes ? `${movie.durationMinutes} phút` : 'Chưa cập nhật'}</td>
              <td>{formatReleaseDate(movie.releaseDate)}</td>
              <td><Link className="admin-movies__edit" to={`/admin/movies/${movie.id}/edit`}>Chỉnh sửa<span className="visually-hidden"> phim {movie.title}</span></Link></td>
            </tr>)}</tbody>
          </table>
        </div>
        <Pagination page={currentPage} totalPages={totalPages} totalElements={state.data.totalElements} onPageChange={setPage} />
      </> : null}
    </section>
  </section>
}

function StatusBadge({ status }) {
  const label = STATUS_OPTIONS.find((option) => option.value === status)?.label || status || 'Chưa cập nhật'
  return <span className={`admin-movies__status admin-movies__status--${String(status || 'unknown').toLowerCase()}`}>{label}</span>
}

function MovieTableSkeleton() {
  return <div className="admin-movies__loading" role="status"><span className="visually-hidden">Đang tải danh sách phim</span>{[0, 1, 2, 3].map((row) => <div key={row} className="admin-movies__skeleton" aria-hidden="true" />)}</div>
}

function EmptyState({ filtered }) {
  return <div className="admin-movies__empty"><h4>{filtered ? 'Không tìm thấy phim phù hợp' : 'Chưa có phim trong danh mục'}</h4><p>{filtered ? 'Hãy thử đổi từ khóa hoặc trạng thái để mở rộng kết quả.' : 'Khi có phim được quản trị, danh sách sẽ hiển thị tại đây.'}</p></div>
}

function Pagination({ page, totalPages, totalElements, onPageChange }) {
  if (totalPages <= 1) return <p className="admin-movies__pagination-summary">Tổng cộng {totalElements} phim</p>
  return <nav className="admin-movies__pagination" aria-label="Phân trang danh sách phim"><p>Trang {page + 1} / {totalPages} · {totalElements} phim</p><div><button type="button" disabled={page <= 0} onClick={() => onPageChange(page - 1)}>Trang trước</button><button type="button" disabled={page >= totalPages - 1} onClick={() => onPageChange(page + 1)}>Trang sau</button></div></nav>
}
