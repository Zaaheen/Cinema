import { Link } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { roleLabel } from '../lib/roles'

const DASHBOARDS = {
  ADMIN: {
    eyebrow: 'Quản trị',
    title: 'Trung tâm quản trị rạp',
    description: 'Theo dõi cấu hình rạp và chuẩn bị quản lý danh mục, nhân sự, khuyến mãi và báo cáo.',
    cards: [
      ['Danh mục phim', 'Quản lý phim sẽ dùng các API /admin sau khi phần CRUD được triển khai.'],
      ['Vận hành rạp', 'Phòng chiếu, ghế và suất chiếu thuộc phạm vi Sprint 2.'],
      ['Báo cáo', 'Doanh thu và nhân sự chưa có API được duyệt trong Sprint 1.'],
    ],
  },
  EMPLOYEE: {
    eyebrow: 'Nhân viên',
    title: 'Quầy phục vụ',
    description: 'Khu vực dành cho nhân viên vận hành và hỗ trợ khách hàng tại rạp.',
    cards: [
      ['Tra cứu thành viên', 'API tra cứu thành viên sẽ được bổ sung cùng nghiệp vụ nội bộ.'],
      ['Bán vé tại quầy', 'Luồng bán vé chỉ mở khi booking và payment được duyệt, triển khai.'],
      ['Danh sách phim', 'Xem catalog công khai và hỗ trợ khách lựa chọn phim.'],
    ],
  },
  MEMBER: {
    eyebrow: 'Thành viên',
    title: 'Không gian thành viên',
    description: 'Khám phá phim, quản lý hồ sơ và chuẩn bị cho luồng đặt vé.',
    cards: [
      ['Xem phim', 'Tìm kiếm phim đang chiếu và phim sắp chiếu.', '/movies'],
      ['Tài khoản', 'Cập nhật hồ sơ hoặc đổi mật khẩu.', '/account'],
      ['Vé và điểm', 'Sẽ mở khi nghiệp vụ booking và điểm được triển khai.'],
    ],
  },
}

export function RoleDashboard({ role }) {
  const { user } = useAuth()
  const dashboard = DASHBOARDS[role]

  return (
    <section className="role-dashboard">
      <p className="eyebrow">{dashboard.eyebrow}</p>
      <h1>{dashboard.title}</h1>
      <p className="hero-copy">Chào {user?.fullName}. Bạn đang đăng nhập với vai trò {roleLabel(role)}.</p>
      <div className="feature-grid">
        {dashboard.cards.map(([title, description, href]) => (
          <article className="feature-card" key={title}>
            <h2>{title}</h2>
            <p>{description}</p>
            {href ? <Link className="button button-secondary" to={href}>Mở chức năng</Link> : null}
          </article>
        ))}
      </div>
    </section>
  )
}
