import { useEffect, useRef, useState } from 'react'
import {
  cancelShowtime,
  createShowtimes,
  fetchAdminRooms,
  fetchAdminShowtimes,
  updateShowtime,
} from '../api/catalog'
import { getErrorMessage } from '../lib/errors'

const ZONE = 'Asia/Ho_Chi_Minh'
const OPEN_MINUTES = 8 * 60
const CLOSE_MINUTES = 23 * 60
const SLOT_MINUTES = 15
const BUFFER_MINUTES = 15
const SLOT_COUNT = (CLOSE_MINUTES - OPEN_MINUTES) / SLOT_MINUTES

function todayInCinemaZone() {
  return new Intl.DateTimeFormat('en-CA', { timeZone: ZONE }).format(new Date())
}

function formatTime(value) {
  return new Intl.DateTimeFormat('vi-VN', {
    timeZone: ZONE,
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
  }).format(new Date(value))
}

function formatDate(value) {
  return new Intl.DateTimeFormat('vi-VN', {
    timeZone: ZONE,
    weekday: 'short',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(new Date(`${value}T12:00:00+07:00`))
}

function slotStart(date, index) {
  const minutes = OPEN_MINUTES + index * SLOT_MINUTES
  const hour = String(Math.floor(minutes / 60)).padStart(2, '0')
  const minute = String(minutes % 60).padStart(2, '0')
  return `${date}T${hour}:${minute}:00+07:00`
}

function localDate(value) {
  return new Intl.DateTimeFormat('en-CA', { timeZone: ZONE }).format(new Date(value))
}

function overlaps(startA, endA, startB, endB) {
  return Date.parse(startA) < Date.parse(endB) && Date.parse(endA) > Date.parse(startB)
}

export function MovieScheduleEditor({ durationMinutes, movieId, durationHasUnsavedChanges = false, onInitialSchedulesChange }) {
  const [rooms, setRooms] = useState([])
  const [roomStatus, setRoomStatus] = useState('loading')
  const [roomError, setRoomError] = useState('')
  const [dates, setDates] = useState(() => [todayInCinemaZone()])
  const [activeDate, setActiveDate] = useState(() => todayInCinemaZone())
  const [dateInput, setDateInput] = useState('')
  const [scheduleResult, setScheduleResult] = useState({ key: '', status: 'loading', showtimes: [], error: '' })
  const [drafts, setDrafts] = useState([])
  const [selectedStart, setSelectedStart] = useState(null)
  const [editingShowtimeId, setEditingShowtimeId] = useState(null)
  const [isSaving, setIsSaving] = useState(false)
  const [refreshKey, setRefreshKey] = useState(0)
  const [currentTime, setCurrentTime] = useState(() => Date.now())
  const pointerDown = useRef(false)
  const validDuration = Number.isFinite(Number(durationMinutes)) && Number(durationMinutes) > 0
  const scheduleRequestKey = `${activeDate}:${refreshKey}`
  const scheduleStatus = scheduleResult.key === scheduleRequestKey ? scheduleResult.status : 'loading'
  const scheduleError = scheduleResult.key === scheduleRequestKey ? scheduleResult.error : ''
  const showtimes = scheduleResult.key === scheduleRequestKey ? scheduleResult.showtimes : []

  useEffect(() => {
    let active = true
    fetchAdminRooms()
      .then((data) => {
        if (!active) return
        setRooms(data)
        setRoomStatus(data.length ? 'ready' : 'empty')
      })
      .catch((error) => {
        if (!active) return
        setRoomError(getErrorMessage(error))
        setRoomStatus('error')
      })
    return () => { active = false }
  }, [])

  useEffect(() => {
    const timer = window.setInterval(() => setCurrentTime(Date.now()), 60_000)
    return () => window.clearInterval(timer)
  }, [])

  useEffect(() => {
    let active = true
    fetchAdminShowtimes(activeDate)
      .then((data) => {
        if (!active) return
        setScheduleResult({ key: scheduleRequestKey, status: 'ready', showtimes: data, error: '' })
      })
      .catch((error) => {
        if (!active) return
        setScheduleResult({ key: scheduleRequestKey, status: 'error', showtimes: [], error: getErrorMessage(error) })
      })
    return () => { active = false }
  }, [activeDate, refreshKey, scheduleRequestKey])

  useEffect(() => {
    if (!movieId) onInitialSchedulesChange(drafts.map(({ roomId, startsAt }) => ({ roomId, startsAt })))
  }, [drafts, movieId, onInitialSchedulesChange])

  const activeShowtimes = showtimes.filter((item) => item.status === 'SCHEDULED' || item.status === 'ONGOING')

  function addDate() {
    if (!dateInput) return
    setDates((current) => current.includes(dateInput) ? current : [...current, dateInput].sort())
    setActiveDate(dateInput)
    setDateInput('')
    setSelectedStart(null)
  }

  function chooseStart(roomId, startsAt) {
    setSelectedStart({ roomId, startsAt })
  }

  function isSlotBlocked(roomId, index) {
    if (!validDuration) return false
    const start = slotStart(activeDate, index)
    const end = new Date(Date.parse(start) + Number(durationMinutes) * 60_000).toISOString()
    const requestedEnd = new Date(Date.parse(end) + BUFFER_MINUTES * 60_000).toISOString()
    return activeShowtimes.some((item) => {
      if (item.roomId !== roomId || item.id === editingShowtimeId) return false
      const occupiedEnd = new Date(Date.parse(item.endsAt) + BUFFER_MINUTES * 60_000).toISOString()
      return overlaps(start, requestedEnd, item.startsAt, occupiedEnd)
    }) || drafts.some((draft) => {
      if (draft.roomId !== roomId || draft.date !== activeDate) return false
      const draftEnd = new Date(Date.parse(draft.startsAt) + Number(durationMinutes) * 60_000 + BUFFER_MINUTES * 60_000).toISOString()
      return overlaps(start, requestedEnd, draft.startsAt, draftEnd)
    })
  }

  function isStartValid(roomId, index) {
    if (!validDuration) return false
    const start = slotStart(activeDate, index)
    if (Date.parse(start) <= currentTime) return false
    const end = Date.parse(start) + Number(durationMinutes) * 60_000
    const endInstant = new Date(end).toISOString()
    const endLocalTime = new Intl.DateTimeFormat('en-GB', {
      timeZone: ZONE,
      hour: '2-digit',
      minute: '2-digit',
      hourCycle: 'h23',
    }).format(new Date(end))
    return localDate(endInstant) === activeDate && endLocalTime <= '23:00' && !isSlotBlocked(roomId, index)
  }

  function addDraft() {
    if (!selectedStart || !validDuration || durationHasUnsavedChanges) return
    const date = localDate(selectedStart.startsAt)
    const duplicate = drafts.some((draft) => draft.roomId === selectedStart.roomId && draft.startsAt === selectedStart.startsAt)
    if (duplicate) {
      setScheduleError('Suất chiếu này đã có trong danh sách chờ lưu.')
      return
    }
    setDrafts((current) => [...current, { ...selectedStart, date, key: `${selectedStart.roomId}-${selectedStart.startsAt}` }]
      .sort((a, b) => a.startsAt.localeCompare(b.startsAt)))
    setSelectedStart(null)
    setScheduleError('')
  }

  async function saveDrafts() {
    if (!movieId || drafts.length === 0 || durationHasUnsavedChanges) return
    setIsSaving(true)
    setScheduleError('')
    try {
      await createShowtimes(movieId, drafts.map(({ roomId, startsAt }) => ({ roomId, startsAt })))
      setDrafts([])
      setSelectedStart(null)
      setRefreshKey((value) => value + 1)
    } catch (error) {
      setScheduleError(getErrorMessage(error))
    } finally {
      setIsSaving(false)
    }
  }

  async function saveEdit() {
    if (!editingShowtimeId || !selectedStart || durationHasUnsavedChanges) return
    setIsSaving(true)
    setScheduleError('')
    try {
      await updateShowtime(editingShowtimeId, selectedStart)
      setEditingShowtimeId(null)
      setSelectedStart(null)
      setRefreshKey((value) => value + 1)
    } catch (error) {
      setScheduleError(getErrorMessage(error))
    } finally {
      setIsSaving(false)
    }
  }

  async function removeShowtime(showtimeId) {
    setIsSaving(true)
    setScheduleError('')
    try {
      await cancelShowtime(showtimeId)
      setRefreshKey((value) => value + 1)
    } catch (error) {
      setScheduleError(getErrorMessage(error))
    } finally {
      setIsSaving(false)
    }
  }

  function beginEdit(showtime) {
    setEditingShowtimeId(showtime.id)
    setSelectedStart({ roomId: showtime.roomId, startsAt: showtime.startsAt })
    const date = localDate(showtime.startsAt)
    if (!dates.includes(date)) setDates((current) => [...current, date].sort())
    setActiveDate(date)
  }

  const selectedEnd = selectedStart && validDuration
    ? new Date(Date.parse(selectedStart.startsAt) + Number(durationMinutes) * 60_000)
    : null

  return (
    <section className="admin-movie-section movie-schedule-editor">
      <div className="admin-movie-section-heading">
        <span className="admin-movie-step">04</span>
        <div>
          <h2>Lịch chiếu theo phòng</h2>
          <p>Chọn ngày, phòng và giờ bắt đầu. Giờ kết thúc được tính từ thời lượng phim; giữa hai suất cần 15 phút chuẩn bị.</p>
        </div>
      </div>

      {!validDuration ? <p className="schedule-notice">Nhập thời lượng phim để chọn suất chiếu.</p> : null}
      {durationHasUnsavedChanges ? (
        <p className="schedule-notice">Thời lượng đã thay đổi. Hãy lưu thông tin phim trước khi chỉnh lịch để giờ kết thúc được tính đúng.</p>
      ) : null}
      {roomError ? <p className="form-error" role="alert">Không tải được danh sách phòng: {roomError}</p> : null}
      {roomStatus === 'empty' ? <p className="schedule-notice">Chưa có phòng chiếu đang hoạt động.</p> : null}

      <div className="schedule-date-controls">
        <label className="admin-movie-field">
          <span>Thêm ngày chiếu</span>
          <input type="date" min={todayInCinemaZone()} value={dateInput} onChange={(event) => setDateInput(event.target.value)} />
        </label>
        <button className="button button-secondary" type="button" onClick={addDate} disabled={!dateInput}>Thêm ngày</button>
      </div>

      <div className="schedule-date-tabs" role="tablist" aria-label="Ngày chiếu">
        {dates.map((date) => (
          <button
            className={date === activeDate ? 'schedule-date-tab is-active' : 'schedule-date-tab'}
            type="button"
            role="tab"
            aria-selected={date === activeDate}
            key={date}
            onClick={() => { setActiveDate(date); setSelectedStart(null) }}
          >
            {formatDate(date)}
          </button>
        ))}
      </div>

      {editingShowtimeId ? (
        <div className="schedule-selection-banner">
          Đang sửa suất chiếu #{editingShowtimeId}. Chọn phòng/giờ mới, sau đó lưu thay đổi.
          <button type="button" className="text-button" onClick={() => { setEditingShowtimeId(null); setSelectedStart(null) }}>Bỏ sửa</button>
        </div>
      ) : null}

      {selectedStart ? (
        <div className="schedule-selection-banner">
          Đã chọn {formatTime(selectedStart.startsAt)} – {selectedEnd ? formatTime(selectedEnd) : '—'}
          {' '}({Number(durationMinutes) || 0} phút), phòng {rooms.find((room) => room.id === selectedStart.roomId)?.name ?? ''}.
          {!editingShowtimeId ? (
            <button type="button" className="text-button" disabled={durationHasUnsavedChanges} onClick={addDraft}>Thêm vào danh sách suất</button>
          ) : null}
        </div>
      ) : null}

      {scheduleError ? <p className="form-error" role="alert">{scheduleError}</p> : null}
      {scheduleStatus === 'loading' ? <p className="page-status">Đang tải lịch phòng...</p> : null}
      {scheduleStatus === 'error' ? <p className="form-error">Không tải được lịch phòng.</p> : null}

      {roomStatus === 'ready' && scheduleStatus === 'ready' ? (
        <>
          <div className="schedule-legend">
            <span><i className="schedule-legend-free" /> Còn trống</span>
            <span><i className="schedule-legend-busy" /> Đã đặt / thời gian chuẩn bị</span>
            <span><i className="schedule-legend-selected" /> Khoảng phim đang chọn</span>
          </div>
          <div className="schedule-grid-scroll">
            <div className="schedule-grid" style={{ '--schedule-room-count': rooms.length }}>
              <div className="schedule-time-axis" aria-hidden="true">
                <div className="schedule-axis-spacer" />
                {Array.from({ length: SLOT_COUNT }, (_, index) => (
                  <div className="schedule-time-cell" key={index}>
                    {index % 4 === 0 ? `${String(8 + Math.floor(index / 4)).padStart(2, '0')}:00` : ''}
                  </div>
                ))}
              </div>
              {rooms.map((room) => (
                <div className="schedule-room-lane" key={room.id}>
                  <h3>{room.name}</h3>
                  <div className="schedule-room-slots" onPointerUp={() => { pointerDown.current = false }} onPointerLeave={() => { pointerDown.current = false }}>
                    {Array.from({ length: SLOT_COUNT }, (_, index) => {
                      const startsAt = slotStart(activeDate, index)
                      const startInstant = new Date(startsAt).toISOString()
                      const endInstant = new Date(Date.parse(startInstant) + SLOT_MINUTES * 60_000).toISOString()
                      const occupied = activeShowtimes.find((item) => {
                        if (item.roomId !== room.id || item.id === editingShowtimeId) return false
                        const occupiedUntil = new Date(Date.parse(item.endsAt) + BUFFER_MINUTES * 60_000).toISOString()
                        return overlaps(startInstant, endInstant, item.startsAt, occupiedUntil)
                      })
                      const selected = selectedStart?.roomId === room.id
                        && Date.parse(startsAt) >= Date.parse(selectedStart.startsAt)
                        && Date.parse(startsAt) < Date.parse(selectedStart.startsAt) + Number(durationMinutes || 0) * 60_000
                      const allowed = isStartValid(room.id, index)
                      let className = 'schedule-slot'
                      if (occupied) className += ' is-occupied'
                      if (selected) className += ' is-selected'
                      return (
                        <button
                          className={className}
                          type="button"
                          key={`${room.id}-${index}`}
                          disabled={!allowed || durationHasUnsavedChanges}
                          aria-label={`${room.name}, ${formatTime(startsAt)}${occupied ? `, đã kín bởi ${occupied.movieTitle}` : ''}`}
                          title={occupied ? `${occupied.movieTitle} · ${formatTime(occupied.startsAt)}–${formatTime(occupied.endsAt)}` : `${formatTime(startsAt)} · ${Number(durationMinutes) || 0} phút`}
                          onPointerDown={() => {
                          if (!allowed || durationHasUnsavedChanges) return
                            pointerDown.current = true
                            chooseStart(room.id, startsAt)
                          }}
                          onPointerEnter={() => {
                            if (pointerDown.current && allowed && !durationHasUnsavedChanges) chooseStart(room.id, startsAt)
                          }}
                          onClick={() => { if (allowed && !durationHasUnsavedChanges) chooseStart(room.id, startsAt) }}
                        >
                          {occupied && Date.parse(startsAt) <= Date.parse(occupied.startsAt)
                            ? `${occupied.movieTitle} · ${formatTime(occupied.startsAt)}`
                            : index % 4 === 0 ? formatTime(startsAt) : ''}
                        </button>
                      )
                    })}
                  </div>
                </div>
              ))}
            </div>
          </div>

          {drafts.length ? (
            <div className="schedule-draft-list">
              <h3>Suất mới chờ lưu ({drafts.length})</h3>
              {drafts.map((draft) => (
                <div className="schedule-list-item" key={draft.key}>
                  <span>{formatDate(draft.date)} · {rooms.find((room) => room.id === draft.roomId)?.name} · {formatTime(draft.startsAt)}–{formatTime(new Date(Date.parse(draft.startsAt) + Number(durationMinutes) * 60_000))}</span>
                  <button type="button" className="text-button" onClick={() => setDrafts((current) => current.filter((item) => item.key !== draft.key))}>Bỏ</button>
                </div>
              ))}
              {movieId ? (
                <button className="button button-secondary" type="button" onClick={saveDrafts} disabled={isSaving || durationHasUnsavedChanges}>
                  {isSaving ? 'Đang lưu...' : 'Lưu các suất mới'}
                </button>
              ) : <p className="schedule-notice">Các suất này sẽ được lưu cùng phim khi nhấn “Tạo phim”.</p>}
            </div>
          ) : null}

          {movieId ? (
            <div className="schedule-existing-list">
              <h3>Các suất ngày {formatDate(activeDate)}</h3>
              {showtimes.length ? showtimes.map((showtime) => (
                <div className={`schedule-list-item ${showtime.status === 'CANCELLED' ? 'is-cancelled' : ''}`} key={showtime.id}>
                  <span>
                    {showtime.movieTitle} · {showtime.roomName} · {formatTime(showtime.startsAt)}–{formatTime(showtime.endsAt)} · {showtime.status}
                    {showtime.hasBookings ? ' · Đã có booking/vé' : ''}
                  </span>
                  {showtime.status === 'SCHEDULED' ? (
                    <span className="schedule-item-actions">
                      <button type="button" className="text-button" disabled={showtime.hasBookings || isSaving || durationHasUnsavedChanges} onClick={() => beginEdit(showtime)}>Sửa</button>
                      <button type="button" className="text-button is-danger" disabled={showtime.hasBookings || isSaving || durationHasUnsavedChanges} onClick={() => removeShowtime(showtime.id)}>Hủy suất</button>
                    </span>
                  ) : null}
                </div>
              )) : <p className="schedule-notice">Chưa có suất chiếu trong ngày này.</p>}
            </div>
          ) : null}

          {editingShowtimeId && selectedStart ? (
            <button className="button button-secondary" type="button" onClick={saveEdit} disabled={isSaving || durationHasUnsavedChanges}>
              {isSaving ? 'Đang lưu...' : 'Lưu thay đổi suất chiếu'}
            </button>
          ) : null}
        </>
      ) : null}
    </section>
  )
}
