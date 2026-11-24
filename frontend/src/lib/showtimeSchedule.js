export const CINEMA_ZONE = 'Asia/Ho_Chi_Minh'

export function cinemaDate(value = new Date()) {
  return new Intl.DateTimeFormat('en-CA', { timeZone: CINEMA_ZONE, year: 'numeric', month: '2-digit', day: '2-digit' }).format(value)
}

export function cinemaTime(value) {
  return new Intl.DateTimeFormat('en-GB', { timeZone: CINEMA_ZONE, hour: '2-digit', minute: '2-digit', hourCycle: 'h23' }).format(new Date(value))
}

export function isValidSlot(date, time, durationMinutes, now = Date.now()) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(date) || !/^\d{2}:\d{2}$/.test(time)) return false
  const [hours, minutes] = time.split(':').map(Number)
  const startMinutes = hours * 60 + minutes
  if (startMinutes < 8 * 60 || startMinutes >= 23 * 60 || minutes % 15 !== 0) return false
  if (!Number.isInteger(Number(durationMinutes)) || Number(durationMinutes) <= 0) return false
  if (startMinutes + Number(durationMinutes) > 23 * 60) return false
  const start = Date.parse(`${date}T${time}:00+07:00`)
  return Number.isFinite(start) && start > now && cinemaDate(new Date(start)) === date
}

export function startsAtWithOffset(date, time) {
  return `${date}T${time}:00+07:00`
}

export function showtimeError(error) {
  const message = error?.response?.data?.message
  if (message === 'SHOWTIME_ROOM_CONFLICT') return 'Phòng đã có suất chiếu trùng lịch hoặc chưa đủ 15 phút chuẩn bị.'
  if (message === 'SHOWTIME_HAS_BOOKING_HISTORY') return 'Suất chiếu đã có lịch sử đặt vé, không thể sửa hoặc hủy.'
  if (message === 'SHOWTIME_NOT_EDITABLE') return 'Suất chiếu này không còn được phép sửa hoặc hủy.'
  return message || 'Không thể lưu lịch chiếu. Vui lòng thử lại.'
}
