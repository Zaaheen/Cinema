import { Link } from 'react-router-dom'

export function NotFoundPage() {
  return (
    <section className="form-card">
      <h1>Không tìm thấy trang</h1>
      <p className="muted">Đường dẫn bạn truy cập không tồn tại.</p>
      <Link className="button" to="/">
        Về trang chủ
      </Link>
    </section>
  )
}
