import axios from 'axios'

export function getErrorMessage(error) {
  if (axios.isAxiosError(error)) {
    const response = error.response?.data
    const fieldErrors = response?.errors
      ?.map((item) => item.message)
      .filter(Boolean)
    if (fieldErrors?.length) {
      return `${response.message ?? 'Dữ liệu không hợp lệ'}: ${fieldErrors.join(', ')}`
    }
    return response?.message ?? 'Không thể kết nối tới máy chủ'
  }
  if (error instanceof Error) {
    return error.message
  }
  return 'Đã có lỗi xảy ra, vui lòng thử lại sau'
}
