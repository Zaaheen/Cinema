import { Link, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { roleHome, roleLabel } from '../lib/roles'

export function MainLayout() {
  const { user, isAuthenticated, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = async () => {
    await logout()
    navigate('/')
  }

  return (
    <div className="app-shell">
      <header className="site-header">
        <Link className="brand" to="/">
          Cinema
        </Link>
        <nav className="site-nav">
          <Link to="/">Trang chủ</Link>
          <Link to="/movies">Phim</Link>
          {isAuthenticated ? (
            <>
              <Link to={roleHome(user?.role)}>{roleLabel(user?.role)}</Link>
              {user?.role === 'MEMBER' ? <Link to="/account">Tài khoản</Link> : null}
              <button type="button" className="link-button" onClick={handleLogout}>
                Đăng xuất
              </button>
            </>
          ) : (
            <>
              <Link to="/login">Đăng nhập</Link>
              <Link to="/register">Đăng ký</Link>
            </>
          )}
        </nav>
      </header>

      <main className="site-main">
        <Outlet />
      </main>

      <footer className="site-footer">
        <span>Cinema Management - SE03</span>
      </footer>
    </div>
  )
}
