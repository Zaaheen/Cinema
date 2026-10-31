import axios from 'axios'

export function getErrorMessage(error) {
  if (axios.isAxiosError(error)) {
    return error.response?.data?.message ?? 'Không thể kết nối tới máy chủ'
  }
  if (error instanceof Error) {
    return error.message
  }
  return 'Đã có lỗi xảy ra, vui lòng thử lại sau'
}
