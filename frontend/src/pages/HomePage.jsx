import { Link } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'

export function HomePage() {
  const { isAuthenticated, user } = useAuth()

  return (
    <section className="hero">
      <p className="eyebrow">Hệ thống quản lý rạp chiếu phim</p>
      <h1>Đặt vé nhanh, chọn ghế dễ, quản lý rạp gọn.</h1>
      <p className="hero-copy">
        Khung ứng dụng đã sẵn sàng cho Sprint 1. Đăng nhập để bắt đầu sử dụng các chức năng
        dành cho thành viên.
      </p>

      <div className="hero-actions">
        {isAuthenticated ? (
          <>
            <Link className="button" to="/account">
              Tài khoản của {user?.fullName}
            </Link>
            <span className="badge">{user?.role}</span>
          </>
        ) : (
          <>
            <Link className="button" to="/login">
              Đăng nhập
            </Link>
            <Link className="button button-secondary" to="/register">
              Tạo tài khoản
            </Link>
          </>
        )}
      </div>

      <div className="feature-grid">
        <article className="feature-card">
          <h2>Lịch chiếu</h2>
          <p>Danh sách phim và suất chiếu sẽ được nối API ở Sprint 2.</p>
        </article>
        <article className="feature-card">
          <h2>Đặt vé</h2>
          <p>Giữ ghế 5 phút, chống đặt trùng bằng ràng buộc database.</p>
        </article>
        <article className="feature-card">
          <h2>Thành viên</h2>
          <p>Tích điểm sau thanh toán và quản lý vé điện tử.</p>
        </article>
      </div>
    </section>
  )
}
