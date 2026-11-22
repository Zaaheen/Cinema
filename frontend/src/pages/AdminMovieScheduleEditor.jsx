import { useEffect, useRef, useState } from 'react'
import { toast } from 'react-toastify'
import { cancelAdminShowtime, createAdminShowtime, getAdminRooms, getAdminShowtimes, updateAdminShowtime } from '../api/adminShowtimes'
import { cinemaDate, cinemaTime, isValidSlot, showtimeError, startsAtWithOffset } from '../lib/showtimeSchedule'
import { getErrorMessage } from '../lib/errors'

export function AdminMovieScheduleEditor({ movieId, movieTitle, durationMinutes, movieStatus, durationHasUnsavedChanges }) {
  const [date, setDate] = useState(() => cinemaDate())
  const [rooms, setRooms] = useState([])
  const [roomsState, setRoomsState] = useState({ loading: true, error: '' })
  const [schedule, setSchedule] = useState({ loading: true, error: '', items: [] })
  const [retryKey, setRetryKey] = useState(0)
  const [roomId, setRoomId] = useState('')
  const [time, setTime] = useState('')
  const [editingId, setEditingId] = useState(null)
  const [saving, setSaving] = useState(false)
  const [actionError, setActionError] = useState('')
  const mutationInFlight = useRef(false)
  const [currentTime, setCurrentTime] = useState(() => Date.now())

  useEffect(() => {
    const timer = window.setInterval(() => setCurrentTime(Date.now()), 60_000)
    return () => window.clearInterval(timer)
  }, [])

  useEffect(() => {
    const controller = new AbortController()
    let active = true
    getAdminRooms({ signal: controller.signal })
      .then((data) => { if (active) { setRooms(data); setRoomsState({ loading: false, error: '' }) } })
      .catch((error) => { if (active && error?.code !== 'ERR_CANCELED') setRoomsState({ loading: false, error: getErrorMessage(error) }) })
    return () => { active = false; controller.abort() }
  }, [])

  useEffect(() => {
    const controller = new AbortController()
    let active = true
    queueMicrotask(() => { if (active) setSchedule((current) => ({ ...current, loading: true, error: '' })) })
    getAdminShowtimes(date, { signal: controller.signal })
      .then((items) => { if (active) setSchedule({ loading: false, error: '', items }) })
      .catch((error) => { if (active && error?.code !== 'ERR_CANCELED') setSchedule({ loading: false, error: getErrorMessage(error), items: [] }) })
    return () => { active = false; controller.abort() }
  }, [date, retryKey])

  const activeRooms = rooms.filter((room) => room.status === 'ACTIVE')
  const canSchedule = movieStatus === 'COMING_SOON' || movieStatus === 'NOW_SHOWING'
  const selected = schedule.items.find((item) => item.id === editingId)
  const invalidSlot = Boolean(date && time) && !isValidSlot(date, time, durationMinutes)
  const disabledReason = durationHasUnsavedChanges ? 'Lưu thay đổi thời lượng phim trước khi chỉnh lịch chiếu.' : !canSchedule ? 'Trạng thái phim hiện không cho phép tạo suất chiếu.' : ''

  function resetEdit() {
    setEditingId(null)
    setRoomId('')
    setTime('')
    setActionError('')
  }

  async function submit(event) {
    event.preventDefault()
    if (mutationInFlight.current || !roomId || !isValidSlot(date, time, durationMinutes) || disabledReason) return
    const payload = { roomId: Number(roomId), startsAt: startsAtWithOffset(date, time) }
    mutationInFlight.current = true
    setSaving(true)
    setActionError('')
    try {
      if (editingId) await updateAdminShowtime(editingId, payload)
      else await createAdminShowtime({ movieId: Number(movieId), ...payload })
      toast.success(editingId ? 'Đã cập nhật suất chiếu.' : 'Đã tạo suất chiếu.')
      resetEdit()
      setRetryKey((value) => value + 1)
    } catch (error) {
      setActionError(showtimeError(error))
    } finally {
      mutationInFlight.current = false
      setSaving(false)
    }
  }

  async function cancel(showtime) {
    if (mutationInFlight.current || !window.confirm(`Hủy suất chiếu ${cinemaTime(showtime.startsAt)} tại ${showtime.roomName}?`)) return
    mutationInFlight.current = true
    setSaving(true)
    setActionError('')
    try {
      await cancelAdminShowtime(showtime.id)
      toast.success('Đã hủy suất chiếu.')
      resetEdit()
      setRetryKey((value) => value + 1)
    } catch (error) {
      setActionError(showtimeError(error))
    } finally {
      mutationInFlight.current = false
      setSaving(false)
    }
  }

  function beginEdit(showtime) {
    setEditingId(showtime.id)
    setRoomId(String(showtime.roomId))
    setTime(cinemaTime(showtime.startsAt))
    setActionError('')
  }

  return <section className="admin-schedule admin-movie-form__card" aria-labelledby="admin-schedule-title">
    <header><p className="eyebrow">LỊCH CHIẾU</p><h3 id="admin-schedule-title">Lịch chiếu · {movieTitle}</h3><p>Mỗi suất cần cách suất trước trong cùng phòng ít nhất 15 phút. Backend sẽ kiểm tra xung đột và lịch sử đặt vé.</p></header>
    <label className="admin-movie-form__field" htmlFor="admin-schedule-date">Ngày chiếu
      <input id="admin-schedule-date" type="date" value={date} onChange={(event) => { setDate(event.target.value); resetEdit() }} />
    </label>
    {roomsState.loading ? <p role="status">Đang tải phòng chiếu…</p> : null}
    {roomsState.error ? <p className="price-admin__error" role="alert">Không tải được phòng chiếu: {roomsState.error}</p> : null}
    {!roomsState.loading && !roomsState.error && activeRooms.length === 0 ? <p>Chưa có phòng ACTIVE. Không thể tạo suất chiếu.</p> : null}
    {disabledReason ? <p className="admin-schedule__notice">{disabledReason}</p> : null}
    {actionError ? <p className="price-admin__error" role="alert">{actionError}</p> : null}
    {activeRooms.length > 0 ? <form className="admin-schedule__form" onSubmit={submit}>
      <label className="admin-movie-form__field" htmlFor="admin-schedule-room">Phòng chiếu
        <select id="admin-schedule-room" value={roomId} onChange={(event) => setRoomId(event.target.value)} required disabled={saving || Boolean(disabledReason)}>
          <option value="">Chọn phòng</option>{activeRooms.map((room) => <option key={room.id} value={room.id}>{room.name}</option>)}
        </select>
      </label>
      <label className="admin-movie-form__field" htmlFor="admin-schedule-time">Giờ bắt đầu (Asia/Ho_Chi_Minh)
        <input id="admin-schedule-time" type="time" min="08:00" max="22:45" step="900" value={time} onChange={(event) => setTime(event.target.value)} required disabled={saving || Boolean(disabledReason)} />
      </label>
      <div className="admin-schedule__actions">
        <button className="price-admin__primary" type="submit" disabled={saving || Boolean(disabledReason) || !roomId || !time || invalidSlot}>{saving ? 'Đang lưu…' : editingId ? 'Lưu suất chiếu' : 'Thêm suất chiếu'}</button>
        {editingId ? <button type="button" disabled={saving} onClick={resetEdit}>Bỏ chỉnh sửa</button> : null}
      </div>
      {invalidSlot ? <p className="price-admin__field-error" role="alert">Giờ bắt đầu phải ở tương lai, theo mốc 15 phút, trong 08:00–23:00 và phim kết thúc trước 23:00 cùng ngày.</p> : null}
      {selected ? <p className="admin-schedule__notice">Đang sửa suất #{selected.id}. Thay đổi chỉ được lưu khi backend xác nhận chưa có lịch sử đặt vé.</p> : null}
    </form> : null}

    <div className="admin-schedule__list" aria-live="polite">
      <div className="admin-schedule__list-heading"><h4>Suất chiếu ngày {date || '—'}</h4><button type="button" onClick={() => setRetryKey((value) => value + 1)} disabled={schedule.loading}>Tải lại</button></div>
      {schedule.loading ? <p role="status">Đang tải lịch chiếu…</p> : null}
      {schedule.error ? <p className="price-admin__error" role="alert">Không tải được lịch chiếu: {schedule.error}</p> : null}
      {!schedule.loading && !schedule.error && schedule.items.length === 0 ? <p>Ngày này chưa có suất chiếu.</p> : null}
      {!schedule.loading && !schedule.error && schedule.items.length > 0 ? <div className="admin-schedule__scroll" tabIndex="0" role="region" aria-label="Danh sách suất chiếu">
        <table><caption className="visually-hidden">Suất chiếu theo phòng trong ngày</caption>
          <thead><tr><th scope="col">Giờ</th><th scope="col">Phòng</th><th scope="col">Phim</th><th scope="col">Trạng thái</th><th scope="col">Thao tác</th></tr></thead>
          <tbody>{schedule.items.map((showtime) => {
            const editable = showtime.movieId === Number(movieId) && showtime.status === 'SCHEDULED' && !showtime.hasBookingHistory && new Date(showtime.startsAt).getTime() > currentTime && !disabledReason
            return <tr key={showtime.id}><td>{cinemaTime(showtime.startsAt)}–{cinemaTime(showtime.endsAt)}</td><td>{showtime.roomName}</td><td>{showtime.movieTitle}</td><td>{showtime.status}{showtime.hasBookingHistory ? ' · Có lịch sử đặt vé' : ''}</td><td>{editable ? <div className="admin-schedule__row-actions"><button type="button" disabled={saving} onClick={() => beginEdit(showtime)}>Sửa</button><button type="button" disabled={saving} onClick={() => cancel(showtime)}>Hủy suất</button></div> : '—'}</td></tr>
          })}</tbody>
        </table>
      </div> : null}
    </div>
  </section>
}
