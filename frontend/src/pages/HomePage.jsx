import { Link } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { roleHome, roleLabel } from '../lib/roles'

export function HomePage() {
  const { isAuthenticated, user } = useAuth()

  return (
    <section className="hero">
      <p className="eyebrow">Hệ thống quản lý rạp chiếu phim</p>
      <h1>Đặt vé nhanh, chọn ghế dễ, quản lý rạp gọn.</h1>
      <p className="hero-copy">
        Khám phá phim đang chiếu, xem thông tin chi tiết và sẵn sàng cho trải nghiệm đặt vé.
      </p>

      <div className="hero-actions">
        <Link className="button button-secondary" to="/movies">Xem phim</Link>
        {isAuthenticated ? (
          <>
            <Link className="button" to={roleHome(user?.role)}>
              Không gian {roleLabel(user?.role)}
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
          <p>Danh sách và chi tiết phim công khai đã sẵn sàng.</p>
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
