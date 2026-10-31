import { useAuth } from '../auth/useAuth'

export function AccountPage() {
  const { user } = useAuth()

  if (!user) {
    return null
  }

  return (
    <section className="form-card">
      <h1>Tài khoản</h1>
      <dl className="detail-list">
        <div>
          <dt>Họ và tên</dt>
          <dd>{user.fullName}</dd>
        </div>
        <div>
          <dt>Tên đăng nhập</dt>
          <dd>{user.username}</dd>
        </div>
        <div>
          <dt>Email</dt>
          <dd>{user.email ?? 'Chưa cập nhật'}</dd>
        </div>
        <div>
          <dt>Số điện thoại</dt>
          <dd>{user.phone ?? 'Chưa cập nhật'}</dd>
        </div>
        <div>
          <dt>Vai trò</dt>
          <dd>{user.role}</dd>
        </div>
        <div>
          <dt>Mã thành viên</dt>
          <dd>{user.memberCode ?? 'Không có'}</dd>
        </div>
      </dl>
    </section>
  )
}
