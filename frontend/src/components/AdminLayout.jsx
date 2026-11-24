import { useState } from 'react'
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'

const NAVIGATION = [
  { to: '/admin/movies', label: 'Phim' },
  { to: '/admin/promotions', label: 'Khuyến mãi' },
  { to: '/admin/seat-prices', label: 'Bảng giá vé' },
]

const TITLES = {
  '/admin/movies': 'Quản lý phim',
  '/admin/promotions': 'Quản lý khuyến mãi',
  '/admin/seat-prices': 'Bảng giá vé',
}

export function AdminLayout() {
  const { logout, user } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [navigationOpen, setNavigationOpen] = useState(false)
  const title = TITLES[location.pathname] || (location.pathname.includes('/movies/') ? 'Quản lý phim' : location.pathname.includes('/promotions/') ? 'Quản lý khuyến mãi' : 'Quản trị Cinema')

  async function handleLogout() {
    await logout()
    navigate('/login', { replace: true })
  }

  function closeNavigation() {
    setNavigationOpen(false)
  }

  return (
    <div className="admin-shell">
      <aside id="admin-navigation" className={`admin-sidebar${navigationOpen ? ' admin-sidebar--open' : ''}`} aria-label="Điều hướng quản trị">
        <div className="admin-sidebar__brand">CINEMA<span>.VN</span></div>
        <p className="admin-sidebar__caption">QUẢN TRỊ RẠP</p>
        <nav className="admin-sidebar__nav">
          {NAVIGATION.map((item) => (
            <NavLink key={item.to} to={item.to} onClick={closeNavigation} className={({ isActive }) => `admin-sidebar__link${isActive ? ' admin-sidebar__link--active' : ''}`}>
              {item.label}
            </NavLink>
          ))}
        </nav>
      </aside>
      {navigationOpen ? <button className="admin-shell__scrim" type="button" aria-label="Đóng điều hướng" onClick={closeNavigation} /> : null}
      <div className="admin-content">
        <header className="admin-topbar">
          <button className="admin-topbar__menu" type="button" aria-expanded={navigationOpen} aria-controls="admin-navigation" onClick={() => setNavigationOpen((open) => !open)}>Menu</button>
          <div><p className="eyebrow">CINEMA ADMIN</p><h1>{title}</h1></div>
          <div className="admin-topbar__account"><span>{user?.fullName || user?.username}</span><button type="button" onClick={handleLogout}>Đăng xuất</button></div>
        </header>
        <main className="admin-main"><Outlet /></main>
      </div>
    </div>
  )
}
