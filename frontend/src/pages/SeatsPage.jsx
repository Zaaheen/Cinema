import { useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { fetchSeatMap } from '../api/catalog'
import { CinemaFooter, CinemaHeader } from '../components/CinemaSiteChrome'
import { getErrorMessage } from '../lib/errors'

const clock = new Intl.DateTimeFormat('vi-VN', { dateStyle: 'full', timeStyle: 'short', timeZone: 'Asia/Ho_Chi_Minh' })
const labels = { AVAILABLE: 'Còn trống', UNAVAILABLE: 'Không sử dụng', HELD: 'Đang giữ', OWN_HELD: 'Bạn đang giữ', SOLD: 'Đã bán', NOT_FOR_SALE: 'Chưa mở bán' }
const seatClass = { AVAILABLE: 'bg-surface-container-high text-on-surface', UNAVAILABLE: 'bg-surface-container-lowest text-outline opacity-50', HELD: 'bg-secondary-container/35 text-secondary', OWN_HELD: 'bg-primary-container/40 text-primary', SOLD: 'bg-error-container/40 text-error', NOT_FOR_SALE: 'bg-surface-container-lowest text-outline opacity-60' }

export default function SeatsPage() {
  const [params] = useSearchParams()
  const rawShowtimeId = params.get('showtimeId')
  const showtimeId = rawShowtimeId && /^\d+$/.test(rawShowtimeId) && Number(rawShowtimeId) > 0
    ? String(Number(rawShowtimeId))
    : null
  const showtimeIdError = rawShowtimeId && !showtimeId ? 'Mã suất chiếu không hợp lệ.' : ''
  const [request, setRequest] = useState(null)
  const [retry, setRetry] = useState(0)
  useEffect(() => {
    if (!showtimeId) return undefined
    const controller = new AbortController()
    fetchSeatMap(showtimeId, { signal: controller.signal })
      .then((seatMap) => setRequest({ showtimeId, retry, seatMap, error: '' }))
      .catch((error) => {
        if (error.name !== 'CanceledError' && error.name !== 'AbortError') {
          setRequest({ showtimeId, retry, seatMap: null, error: getErrorMessage(error) })
        }
      })
    return () => controller.abort()
  }, [showtimeId, retry])

  const isCurrentRequest = request?.showtimeId === showtimeId && request?.retry === retry
  const loading = Boolean(showtimeId) && !isCurrentRequest
  const seatMap = isCurrentRequest ? request?.seatMap : null
  const error = isCurrentRequest ? request?.error : ''
  const rows = useMemo(() => !seatMap ? [] : Object.entries(seatMap.seats.reduce((all, seat) => ({ ...all, [seat.rowLabel]: [...(all[seat.rowLabel] ?? []), seat] }), {})), [seatMap])

  return <div className="min-h-screen bg-surface text-on-surface"><CinemaHeader /><main className="pb-20 pt-24 md:pb-0"><section className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
    <Link to="/movies" className="inline-flex items-center gap-1 text-sm text-primary hover:underline"><span className="material-symbols-outlined text-base">arrow_back</span>Quay lại danh sách phim</Link>
    {!rawShowtimeId ? <Notice title="Chưa chọn suất chiếu">Màn sơ đồ ghế chỉ đọc cần một suất chiếu thật. Chọn suất chiếu từ trang chi tiết phim.</Notice> : null}
    {showtimeIdError ? <Notice title="Không thể xem sơ đồ ghế" error>{showtimeIdError}</Notice> : null}
    {loading ? <Notice title="Đang tải sơ đồ ghế" status /> : null}
    {error ? <div><Notice title="Không thể tải sơ đồ ghế" error>{error}</Notice><button type="button" className="mt-3 min-h-11 rounded-lg bg-primary-container px-5 py-2 font-bold text-on-primary" onClick={() => setRetry((value) => value + 1)}>Thử lại</button></div> : null}
    {seatMap ? <SeatMap seatMap={seatMap} rows={rows} /> : null}
  </section></main><CinemaFooter /></div>
}

function SeatMap({ seatMap, rows }) {
  const { showtime, salesOpen } = seatMap
  return <section className="mt-5 overflow-hidden rounded-2xl border border-surface-container-highest bg-surface-container-low shadow-2xl"><header className="flex flex-wrap items-center justify-between gap-4 border-b border-surface-container-highest bg-surface-container px-5 py-4"><div><div className="flex flex-wrap items-center gap-2 text-xs text-on-surface-variant"><span className="rounded bg-secondary-container/25 px-2 py-1 font-bold text-secondary">{showtime.roomName}</span><span>•</span><span>{clock.format(new Date(showtime.startsAt))}</span></div><h1 className="mt-1 font-headline-md text-2xl font-bold uppercase text-on-surface">Sơ đồ ghế</h1><p className="mt-1 text-sm text-on-surface-variant">{showtime.movieTitle}</p></div><div className={`rounded-lg px-3 py-2 text-sm font-semibold ${rows.length > 0 && salesOpen ? 'bg-secondary-container/20 text-secondary' : 'bg-surface-container-high text-on-surface-variant'}`}>{rows.length === 0 ? 'Sơ đồ ghế đang cập nhật' : salesOpen ? 'Suất chiếu đang mở bán' : 'Suất chiếu hiện chưa mở bán'}</div></header>
    <div className="p-4 sm:p-6"><div className="mx-auto max-w-4xl rounded-t-[50%] border-t-4 border-primary-container/80 pt-3 text-center text-xs font-bold uppercase tracking-[0.2em] text-on-surface-variant"><span className="material-symbols-outlined mr-1 align-text-bottom text-sm">videocam</span>Màn hình chiếu / Screen</div>
      {rows.length === 0 ? <p className="mt-8 rounded-xl bg-surface-container p-5 text-on-surface-variant">Sơ đồ ghế của phòng này đang được cập nhật.</p> : <><p className="mt-6 text-center text-xs text-on-surface-variant">Sơ đồ theo hàng và số ghế; kéo ngang để xem hết hàng trên màn hình nhỏ.</p><div className="mx-auto mt-6 max-w-4xl space-y-3 overflow-x-auto pb-3">{rows.map(([label, seats]) => <div key={label} className="flex min-w-max items-center justify-center gap-2" aria-label={`Hàng ${label}`}><span className="w-6 text-center text-sm font-bold text-on-surface-variant">{label}</span><div className="flex flex-nowrap justify-center gap-2">{seats.map((seat) => <span key={seat.id} title={`${label}${seat.seatNumber}: ${labels[seat.availability]}`} className={`grid h-8 w-8 flex-none place-items-center rounded-t-md text-xs font-bold ${seatClass[seat.availability]}`}>{seat.seatNumber}<span className="sr-only">, {labels[seat.availability]}</span></span>)}</div><span className="w-6 text-center text-sm font-bold text-on-surface-variant">{label}</span></div>)}</div></>}
      {rows.length > 0 ? <div className="mt-8 grid gap-2 rounded-xl bg-surface-container p-4 text-sm sm:grid-cols-3">{Object.entries(labels).map(([status, label]) => <div key={status} className="flex items-center gap-2"><i className={`h-3 w-3 rounded-sm ${seatClass[status]}`} /><span>{label}</span></div>)}</div> : null}
      <p className="mt-6 rounded-xl border border-surface-container-highest bg-surface-container p-4 text-sm text-on-surface-variant">Sơ đồ này chỉ để tham khảo. Chức năng chọn ghế trực tuyến đang được hoàn thiện.</p>
    </div></section>
}

function Notice({ title, children, error, status }) { return <section className={`mt-5 rounded-xl border p-5 ${error ? 'border-error bg-error-container/20 text-error' : 'border-surface-container-highest bg-surface-container text-on-surface'}`}><h1 className={`font-headline-sm text-xl font-bold ${error ? 'text-error' : 'text-on-surface'}`}>{title}</h1><p role={error ? 'alert' : status ? 'status' : undefined} className={`mt-2 text-sm ${error ? 'text-error' : 'text-on-surface-variant'}`}>{children}</p></section> }
