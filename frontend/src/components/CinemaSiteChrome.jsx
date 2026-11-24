import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { roleHome, roleLabel } from '../lib/roles'

const navLinkClass = 'rounded-md px-3 py-2 text-sm text-on-surface-variant transition-colors hover:bg-surface-container hover:text-on-surface'

export function CinemaHeader() {
  const { user, isAuthenticated, logout } = useAuth()
  const navigate = useNavigate()
  const [menuOpen, setMenuOpen] = useState(false)
  const member = user?.role === 'MEMBER'
  const handleLogout = async () => { setMenuOpen(false); await logout(); navigate('/') }

  return <header className="cinema-header fixed top-0 z-50 w-full">
    <div className="mx-auto flex h-20 max-w-7xl items-center justify-between gap-4 px-margin-mobile lg:px-margin">
      <div className="flex flex-shrink-0 items-center gap-5">
        <Link to="/" className="cinema-header__brand font-headline-lg text-headline-lg text-on-surface">CINEMA<span>.VN</span></Link>
        <span className="hidden items-center gap-1.5 text-xs text-on-surface-variant sm:inline-flex"><span className="material-symbols-outlined text-[17px]">location_on</span>TP. Hồ Chí Minh</span>
      </div>
      <nav className="hidden items-center gap-1 lg:flex" aria-label="Điều hướng chính">
        <Link className={navLinkClass} to="/movies">Phim chiếu</Link>
        <Link className={navLinkClass} to="/movies#showtimes">Lịch chiếu</Link>
        <Link className={navLinkClass} to="/promotions">Ưu đãi</Link>
      </nav>
      <div className="relative flex flex-shrink-0 items-center gap-2">
        {!isAuthenticated ? <>
          <Link to="/login" className="hidden rounded-md px-3 py-2 text-sm text-on-surface-variant transition-colors hover:text-on-surface sm:inline-flex">Đăng nhập</Link>
          <Link to="/register" className="cinema-header__register rounded-md px-3 py-2 text-sm">Đăng ký</Link>
        </> : <>
          <button type="button" className="flex items-center gap-2 rounded-md p-1 text-left transition-colors hover:bg-surface-container" aria-expanded={menuOpen} aria-haspopup="menu" aria-label="Mở menu tài khoản" onClick={() => setMenuOpen((open) => !open)}>
            <span className="hidden max-w-32 truncate text-sm text-on-surface sm:block">{user?.fullName || user?.username || roleLabel(user?.role)}</span>
            <span className="grid h-9 w-9 place-items-center rounded-full border border-surface-container-highest bg-surface-container text-on-surface"><span className="material-symbols-outlined text-[20px]">person</span></span>
          </button>
          {menuOpen ? <div className="absolute right-0 top-12 z-50 w-60 overflow-hidden rounded-lg border border-surface-container-highest bg-surface-container-low p-1" role="menu">
            <p className="px-3 py-2 text-xs text-outline">{roleLabel(user?.role)}</p>
            {member ? <>
              <Link className="menu-item" role="menuitem" to="/member" onClick={() => setMenuOpen(false)}>Không gian thành viên</Link>
              <Link className="menu-item" role="menuitem" to="/account" onClick={() => setMenuOpen(false)}>Hồ sơ của tôi</Link>
            </> : <Link className="menu-item" role="menuitem" to={roleHome(user?.role)} onClick={() => setMenuOpen(false)}>Khu vực {roleLabel(user?.role).toLowerCase()}</Link>}
            <button type="button" className="menu-item w-full text-left text-error" role="menuitem" onClick={handleLogout}>Đăng xuất</button>
          </div> : null}
        </>}
      </div>
    </div>
  </header>
}

export function CinemaFooter() {
  const { isAuthenticated, user } = useAuth()
  const mobileAccountLink = isAuthenticated ? (user?.role === 'MEMBER' ? '/account' : roleHome(user?.role)) : '/login'

  return <>
    <footer className="cinema-footer w-full bg-surface-container-low text-sm text-on-surface-variant">
      <div className="mx-auto max-w-7xl px-margin-mobile py-10 lg:px-margin">
        <div className="mb-10 grid grid-cols-1 gap-8 md:grid-cols-2 lg:grid-cols-5">
          <div className="space-y-2 lg:col-span-2"><div className="cinema-header__brand font-headline-lg text-2xl text-on-surface">CINEMA<span className="text-primary">.VN</span></div><p className="max-w-md leading-relaxed">Rạp chiếu phim tại TP. Hồ Chí Minh.</p></div>
          <div><h2 className="mb-3 text-base font-medium text-on-surface">Phòng chiếu Cinema</h2><ul className="space-y-2"><li>• IMAX Laser 3D</li><li>• Dolby Atmos</li><li>• CineLounge VIP</li></ul></div>
          <div><h2 className="mb-3 text-base font-medium text-on-surface">Hỗ trợ</h2><ul className="space-y-2"><li>Chính sách bảo mật</li><li>Điều kiện hoàn vé</li><li>Chăm sóc khách hàng</li></ul></div>
        </div>
        <div className="border-t border-surface-container-high pt-5 text-outline">© 2025 Cinema.vn Vietnam Aggregate Co., Ltd.</div>
      </div>
    </footer>
    <nav className="cinema-mobile-nav fixed bottom-0 left-0 right-0 z-50 flex h-16 items-center justify-around bg-surface-container-low px-4 md:hidden" aria-label="Điều hướng di động">
      <MobileLink to="/movies" icon="movie" label="Phim" />
      <MobileLink to="/movies#showtimes" icon="calendar_today" label="Lịch chiếu" />
      <MobileLink to="/promotions" icon="local_offer" label="Ưu đãi" />
      <MobileLink to={mobileAccountLink} icon="account_circle" label={isAuthenticated ? 'Tôi' : 'Đăng nhập'} />
    </nav>
  </>
}

function MobileLink({ to, icon, label }) { return <Link className="flex flex-col items-center gap-1 text-on-surface-variant" to={to}><span className="material-symbols-outlined text-[22px]">{icon}</span><span className="text-xs">{label}</span></Link> }
